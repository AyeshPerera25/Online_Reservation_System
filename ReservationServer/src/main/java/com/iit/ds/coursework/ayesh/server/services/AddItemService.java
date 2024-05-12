package com.iit.ds.coursework.ayesh.server.services;


import com.iit.ds.coursework.ayesh.grpc.server.Item;
import com.iit.ds.coursework.ayesh.resources.transaction.DistributedTxListener;
import com.iit.ds.coursework.ayesh.server.ReservationServerCore;
import com.iit.ds.coursework.ayesh.grpc.server.AddItemRequest;
import com.iit.ds.coursework.ayesh.grpc.server.AddItemResponse;
import com.iit.ds.coursework.ayesh.grpc.server.AddItemServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.zookeeper.KeeperException;

import java.util.List;

public class AddItemService extends AddItemServiceGrpc.AddItemServiceImplBase implements DistributedTxListener {
    private final ReservationServerCore server;
    private Item item = null;
    private boolean isTxnStarted;
    private String description;
    private String requestID = null;

    public AddItemService(ReservationServerCore server) {
        this.server = server;
    }

    @Override
    public void addItem(AddItemRequest request, StreamObserver<AddItemResponse> responseObserver) {
        AddItemResponse response;

        if (server.isMaster()) { // Master Role to Add Item to System
            try {
                synchronized (server) {
                    System.out.println("Adding item to the system as Master... ");
                    item = populateItem(request.getItem()); // Generate item with unique ID
                    requestID = "ADD" + item.getId();
                    preValidate();
                    server.startDistributedTxn(requestID, this); // initiate the distributed transaction on the system
                    updateSlaveServers();
                    server.performTxnCommit();
                }
                response = populateResponse(true, "Item Successfully Added To The System! Item No: " + item.getId() + " | Item Name: " + item.getName(), item.getId());
                System.out.println("Adding item to the system has successes!  Item No: " + item.getId() + " | Item Name: " + item.getName());
            } catch (Exception e) {
                System.out.println("Adding item to the system has Failed!  Item No: " + item.getId() + " | Item Name: " + item.getName());
                if (isTxnStarted) {
                    System.out.println("Initiating Global Abort..");
                    server.performTxnCommitAbort();
                }
                System.out.println(e.getMessage());
                response = populateResponse(false, e.getMessage(), item != null ? item.getId() : "");
            }
        } else { // Slave Role to Add Item to System
            try {
                if (request.getIsMasterReq()) {  // Slave Role if the request from Master
                    item = request.getItem();
                    requestID = request.getReqID().isEmpty() ? "ADD" + item : request.getReqID();
                    synchronized (server) {
                        System.out.println("Adding item to the local DB as Slave on Master command...");
                        preValidate();
                        server.startDistributedTxn(requestID, this);
                        server.voteCommit();
                    }
                    response = populateResponse(true, "Item Successfully Added To The Slave System! Item No: " + item.getId() + " | Item Name: " + item.getName(), item.getId());
                    System.out.println("Adding item to the system has successes!  Item No: " + item.getId() + " | Item Name: " + item.getName());
                } else { // Slave Role if the request from Client
                    if (!server.isServerReady()) { // Block getting request from clients until server get synced
                        throw new RuntimeException("Bootstrap server initiation not completed!");
                    }
                    item = request.getItem();
                    response = updateMasterServer();
                }
            } catch (Exception e) {
                System.out.println("Adding item to the system has failed!  Item No: " + item.getId() + " | Item Name: " + item.getName());
                if (isTxnStarted) {
                    System.out.println("Initiating vote Abort..");
                    server.voteAbort();
                }
                System.out.println(e.getMessage());
                response = populateResponse(false, e.getMessage(), item != null ? item.getId() : "");
            }
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void preValidate() {
        if (server.isItemAlreadyExist(item.getId())) {
            throw new RuntimeException("Item already exist on the system! - Item No: " + item.getId() + " | Item Name: " + item.getName());
        }
    }

    private AddItemResponse updateMasterServer() throws InterruptedException, KeeperException {
        AddItemResponse response;
        String[] currentMasterData;

        try {
            System.out.println("Updating added item to the Master...");
            currentMasterData = server.getCurrentMasterData();
            response = callServerAddItemUpdate(false, currentMasterData[0], Integer.parseInt(currentMasterData[1]));
        } catch (Exception e) {
            throw new RuntimeException("Send Add Item Update to Master has failed! Due to:" + e.getMessage());
        }
        return response;
    }

    private void updateSlaveServers() {
        List<String[]> slaveServerData;
        try {
            System.out.println("Updating added item to the Slave Servers...");
            slaveServerData = server.getSlaveServerData();
            if (slaveServerData.isEmpty()) {
                System.out.println("Slave Server Update Skipped! Unable to find data on Slave Servers.");
                return;
            }
            slaveServerData.forEach(data -> callServerAddItemUpdate(true, data[0], Integer.parseInt(data[1])));
            System.out.println("Slave servers successfully updated on added item! ItemNo: " + item.getId());
        } catch (Exception e) {
            throw new RuntimeException("Update Slave Servers Failed! Reason: " + e.getMessage());
        }
    }

    private AddItemResponse callServerAddItemUpdate(boolean isSentByMaster, String IPAddress, int port) {
        ManagedChannel channel;
        AddItemServiceGrpc.AddItemServiceBlockingStub clientStub;
        AddItemRequest request;
        AddItemResponse response;

        System.out.println("Call Server " + IPAddress + ":" + port);
        channel = ManagedChannelBuilder.forAddress(IPAddress, port)
                .usePlaintext()
                .build();
        clientStub = AddItemServiceGrpc.newBlockingStub(channel);
        request = AddItemRequest.newBuilder()
                .setReqID(requestID != null ? requestID : "")
                .setItem(item)
                .setIsMasterReq(isSentByMaster)
                .build();
        response = clientStub.addItem(request);
        if (!response.getStatus()) { //if request get failed the abort the update
            throw new RuntimeException("Update Add Item Failed! on server: " + IPAddress + ":" + port + " Due to: " + response.getDescription());
        }
        return response;
    }

    private AddItemResponse populateResponse(boolean status, String description, String id) {
        return AddItemResponse.newBuilder()
                .setId(id)
                .setStatus(status)
                .setDescription(description)
                .build();
    }

    private Item populateItem(Item requestItem) {
        return Item.newBuilder()
                .setId(String.valueOf(System.currentTimeMillis()))
                .setDescription(requestItem.getDescription())
                .setAvailableQty(requestItem.getAvailableQty())
                .setType(requestItem.getType())
                .setName(requestItem.getName())
                .setPrice(requestItem.getPrice())
                .setSellerId(requestItem.getSellerId())
                .build();
    }

    @Override
    public void setTxnStarted(boolean txnStarted) {
        this.isTxnStarted = txnStarted;
    }

    @Override
    public void onGlobalCommit() {
        try {
            if (item != null) {
                server.addItemToDB(item); // add item to the DB
            }
            System.out.println("Add item has committed to the local system successfully! ItemNo: " + item.getId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onGlobalAbort() {
        System.out.println("Transaction Aborted by the Coordinator");
    }
}
