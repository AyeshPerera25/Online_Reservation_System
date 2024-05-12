package com.iit.ds.coursework.ayesh.server;


import com.iit.ds.coursework.ayesh.grpc.server.*;
import com.iit.ds.coursework.ayesh.resources.synchronization.DistributedMasterLock;
import com.iit.ds.coursework.ayesh.resources.transaction.DistributedTx;
import com.iit.ds.coursework.ayesh.resources.transaction.DistributedTxCoordinator;
import com.iit.ds.coursework.ayesh.resources.transaction.DistributedTxListener;
import com.iit.ds.coursework.ayesh.resources.transaction.DistributedTxParticipant;
import com.iit.ds.coursework.ayesh.server.services.*;
import com.iit.ds.coursework.ayesh.server.utility.MasterCampaignManagerThread;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ReservationServerCore {

    public static final String ZOOKEEPER_URL = "127.0.0.1:2181";
    public final String SERVER_NAME = "RESERVATION_SERVER";
    public final String SERVER_ADDRESS;
    private final String serverIp;
    private final int serverPort;
    private final AddItemService addItemService;
    private final DeleteItemService deleteItemService;
    private final GetAllItemsService getAllItemsService;
    private final GetMyItemsService getMyItemsService;
    private final MakeReservationService makeReservationService;
    private final UpdateItemService updateItemService;
    private final DistributedMasterLock distributedMasterLock;
    private final AtomicBoolean isMaster = new AtomicBoolean(false);
    private final AtomicBoolean serverReady = new AtomicBoolean(false);
    private final HashMap<String, Item> db = new HashMap<>(); // Maintain a in memory hashmap as a DB
    private byte[] currentMasterNodeData;
    private DistributedTx transaction;


    public ReservationServerCore(String serverIp, int serverPort) throws IOException, InterruptedException, KeeperException {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        SERVER_ADDRESS = serverIp + ":" + serverPort;
        this.distributedMasterLock = new DistributedMasterLock(SERVER_NAME, SERVER_ADDRESS);
        this.addItemService = new AddItemService(this);
        this.deleteItemService = new DeleteItemService(this);
        this.getAllItemsService = new GetAllItemsService(this);
        this.getMyItemsService = new GetMyItemsService(this);
        this.makeReservationService = new MakeReservationService(this);
        this.updateItemService = new UpdateItemService(this);
        this.transaction = new DistributedTxParticipant();
    }

    public static void main(String[] args) {
        String ip;
        int port;
        Scanner userInput = new Scanner(System.in);

        try {
            System.out.println("========== Enter Server IP and Port ========== ");
            System.out.print("Server IP: ");
            ip = userInput.nextLine().trim();
            System.out.print("Server Port: ");
            port = Integer.parseInt(userInput.nextLine().trim());
            System.out.println("================================================");

            System.out.println("Initiating Reservation Server.....");
            DistributedMasterLock.setZooKeeperUrl(ZOOKEEPER_URL);
            DistributedTx.setZooKeeperURL(ZOOKEEPER_URL);
            ReservationServerCore serverCore = new ReservationServerCore(ip, port);
            serverCore.initiateCompeteMasterCampaign(); // Initiate competition to become master
            serverCore.startServer(); //Initiate services and start server
        } catch (Exception e) {
            System.out.println("Internal Server Failure!, Cause: " + e.getMessage());
            System.out.println("\n Required to print Stack Trace? (Yes: y , No: n) :");
            boolean isPrintStackTraceRequired = userInput.nextLine().trim().equalsIgnoreCase("y");
            if (isPrintStackTraceRequired) {
                System.out.println("\nPrinting Stack Trace: ");
                e.printStackTrace();
            }
            System.out.println("\nReservation Server Shutting Down!....");
            System.exit(1);
        }
    }

    private void startServer() throws IOException, InterruptedException, KeeperException {
        //Add Services to the server
        Server server = ServerBuilder.forPort(serverPort)
                .addService(addItemService)
                .addService(deleteItemService)
                .addService(getAllItemsService)
                .addService(getMyItemsService)
                .addService(makeReservationService)
                .addService(updateItemService)
                .build();
        //Start Server
        server.start();
        getSyncWithOthers();
        serverReady.set(true);
        System.out.println("Initiating Reservation Server has Succeed! : server running on : " + serverIp + " : " + serverPort);
        server.awaitTermination();
    }

    public void initiateCompeteMasterCampaign() {
        Thread masterCampaignManager = new Thread(new MasterCampaignManagerThread(distributedMasterLock, this));
        masterCampaignManager.start();
    }

    private void getSyncWithOthers() throws InterruptedException, KeeperException {
        List<Item> itemList;
        ManagedChannel channel;
        GetAllItemsServiceGrpc.GetAllItemsServiceBlockingStub getAllItemsServiceBlockingStub;
        GetAllItemRequest request;
        GetAllItemResponse response;

        System.out.println("Starting System Sync..");
        byte[] currentMasterNodeData = distributedMasterLock.getMasterData();
        String[] decodedMasterAddress = (new String(currentMasterNodeData)).split(":");
        String masterServerIP = decodedMasterAddress[0].trim();
        int masterServerPort = Integer.parseInt(decodedMasterAddress[1].trim());
        if (!Arrays.equals(currentMasterNodeData, distributedMasterLock.getServerData())) { //Check  if there are other registered servers and Master servers
            System.out.println("Initializing connecting to Master Server to Sync at: " + masterServerIP + " : " + masterServerPort);
            channel = ManagedChannelBuilder
                    .forAddress(masterServerIP, masterServerPort)
                    .usePlaintext()
                    .build();
            getAllItemsServiceBlockingStub = GetAllItemsServiceGrpc.newBlockingStub(channel);

            System.out.println("Connected to the Master Server at: " + masterServerIP + " : " + masterServerPort + " | Start sync data...");
            request = GetAllItemRequest.newBuilder()
                    .setId(serverIp + ":" + serverPort)
                    .setIsServer(true)
                    .build();
            response = getAllItemsServiceBlockingStub.getAllItems(request);
            if (!response.getStatus() && !response.getDescription().equals("System has no Items listed!")) {
                throw new RuntimeException(response.getDescription());
            }

            System.out.println("Updating Local DB...");
            if (!response.getItemsList().isEmpty()) {
                response.getItemsList().forEach(this::insertSyncDBData);
                System.out.println("DB get synced with Master successfully!");
            } else {
                System.out.println("No Items to update!");
            }
        } else {
            System.out.println("System Sync Skipped! Due to current server is the master");
        }
    }

    private void insertSyncDBData(Item item) {
        if (!db.containsKey(item.getId())) {
            db.put(item.getId(), item);
            System.out.println("DB get synced with item no: " + item.getId());
        }
    }

    public boolean isServerReady() {
        return serverReady.get();
    }

    public boolean isMaster() {
        return isMaster.get();
    }

    public void setIsMaster(boolean isMaster) {
        this.isMaster.set(isMaster);
    }

    public synchronized void setCurrentMasterNodeData(byte[] masterNodeData) {
        this.currentMasterNodeData = masterNodeData;
    }

    public List<String[]> getSlaveServerData() throws KeeperException, InterruptedException {
        List<String[]> result = new ArrayList<>();
        List<byte[]> slaveServerData = distributedMasterLock.getSlaveData();

        if (!slaveServerData.isEmpty()) {
            slaveServerData.forEach(data -> result.add((new String(data)).split(":")));
        }
        return result;
    }

    public String[] getCurrentMasterData() throws InterruptedException, KeeperException {
        byte[] masterData = distributedMasterLock.getMasterData();
        return (new String(masterData)).split(":");
    }

    public void initiateTheMaster() {
        System.out.println("I got the Master lock. Now acting as Master and Txn Coordinator");
        isMaster.set(true);
        transaction = new DistributedTxCoordinator();
    }

    public void startDistributedTxn(String id, DistributedTxListener listener) {
        try {
            transaction.setTxnListener(listener);
            transaction.start(id, String.valueOf(UUID.randomUUID()));
            listener.setTxnStarted(true);
        } catch (Exception e) {
            listener.setTxnStarted(false);
            throw new RuntimeException("Starting Distributed Txn Failed! Due to: " + e.getMessage());
        }
    }

    public void performTxnCommit() throws InterruptedException, KeeperException {
        boolean commitStatus = ((DistributedTxCoordinator) transaction).perform();
        if (!commitStatus) {
            throw new RuntimeException(" Distributed Transaction Abort!");
        }
    }

    public void performTxnCommitAbort() {
        ((DistributedTxCoordinator) transaction).sendGlobalAbort();
    }

    public void voteCommit() {
        ((DistributedTxParticipant) transaction).voteCommit();
    }

    public void voteAbort() {
        ((DistributedTxParticipant) transaction).voteAbort();
    }

    public void makeReservation(String itemID, String custID, String date) {
        Item item;
        String resId;
        Reservation reservation;
        Map<String, Reservation> updatedReservationMap;

        item = db.get(itemID);
        resId = String.join("", (date.trim().split("/"))); // Generate Key for reservation map
        reservation = Reservation.newBuilder()
                .setCustId(custID)
                .setId(resId)
                .setDate(date)
                .setDescription("Reservation has placed by customer ID: " + custID + " on: " + date)
                .build();
        updatedReservationMap = new HashMap<>(item.getReservationsMap());
        updatedReservationMap.put(resId, reservation); //Place the reservation
        item = item.toBuilder().clearReservations().putAllReservations(updatedReservationMap).build();
        updateItemInDB(item); // Update the DB
    }


    //DB CRUD operations
    public void addItemToDB(Item item) {
        db.put(item.getId(), item); // Item added to the DB
    }

    public boolean isItemAlreadyExist(String itemID) {
        return db.containsKey(itemID);
    }

    public List<Item> getMyItemsFromDB(String sellerID) {
        List<Item> itemList;
        if (sellerID == null || sellerID.isEmpty()) {
            throw new RuntimeException("Invalid Seller ID : " + sellerID);
        }
        return db.values().stream().filter(itm -> sellerID.equals(itm.getSellerId())).collect(Collectors.toList()); //Return Sellers All Items
    }

    public List<Item> getAllItemsFromDB() {
        if (db.isEmpty()) {
            throw new RuntimeException("System has no Items listed!");
        }
        return new ArrayList<>(db.values()); //Return all available Items
    }

    public Item getItem(String itemID) {
        if (!db.containsKey(itemID)) {
            throw new RuntimeException("Item has not listed on the system! - ItemID : " + itemID);
        }
        return db.get(itemID);
    }

    public void updateItemInDB(Item item) {
        db.replace(item.getId(), item); // Item update on DB
    }

    public void deleteItemInDB(String itemID) {
        db.remove(itemID); // Item Remove on DB
    }


}
