package com.iit.ds.coursework.ayesh.server;


import com.iit.ds.coursework.ayesh.grpc.server.AddItemRequest;
import com.iit.ds.coursework.ayesh.grpc.server.Item;
import com.iit.ds.coursework.ayesh.grpc.server.Reservation;
import com.iit.ds.coursework.ayesh.server.services.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class ReservationServerCore {

    private final String  serverIp;
    private final int serverPort;
    private final AddItemService addItemService;
    private final DeleteItemService deleteItemService;
    private final GetAllItemsService getAllItemsService;
    private final GetMyItemsService getMyItemsService;
    private final MakeReservationService makeReservationService;
    private final UpdateItemService updateItemService;
    private final HashMap<String, Item> db = new HashMap<>(); // Maintain a in memory hashmap as a DB


    public static void main(String[] args) {
        String ip;
        int port;
        if (args.length != 2) {
            System.out.println("Input: Reservation Server <host> <port>");
            System.exit(1);
        }
        try {
            System.out.println("Initiating Reservation Server.....");
            ip = args[0].trim();
            port = Integer.parseInt(args[1].trim());
            ReservationServerCore serverCore = new ReservationServerCore(ip, port);
            serverCore.startServer(); //Initiate services and start server
        } catch (Exception e) {
            System.out.println("Internal Server Failure!, Cause: " + e.getMessage());
            Scanner userInput = new Scanner(System.in);
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

    public ReservationServerCore(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.addItemService = new AddItemService(this);
        this.deleteItemService = new DeleteItemService(this);
        this.getAllItemsService = new GetAllItemsService(this);
        this.getMyItemsService = new GetMyItemsService(this);
        this.makeReservationService = new MakeReservationService(this);
        this.updateItemService = new UpdateItemService(this);
    }

    private void startServer() throws IOException, InterruptedException {
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
        System.out.println("Initiating Reservation Server has Succeed! : server running on : " +serverIp+" : "+ serverPort);
        server.awaitTermination();
    }
    public  void makeReservation(String itemID, String custID , String date){
        Item item;
        Reservation reservation;
        if(!db.containsKey(itemID)){
            throw new RuntimeException("Item has not listed on the system! - ItemID : "+itemID);
        }
        item = db.get(itemID);
        if(item.getReservationsMap().containsKey(date)){
            reservation = item.getReservationsMap().get(date);
            throw new RuntimeException("Allergy has reservation! - on date : "+ reservation.getId());
        }
        if(item.getAvailableQty() == item.getReservationsCount()){
            throw new RuntimeException("Maximum reservation has reached! - on ItemID: "+item.getId());
        }
        reservation = Reservation.newBuilder()
                .setCustId(custID)
                .setId(date)
                .setDescription("Reservation has placed by customer ID: "+custID+" on: "+date)
                .build();
        item.getReservationsMap().put(date,reservation); //Place the reservation
        updateItemInDB(item); // Update the DB
    }


    //DB CRUD operations
    public void addItemToDB (Item item){
        if(db.containsKey(item.getId())){
            throw new RuntimeException("Item already exist on the system! - Item No: "+item.getId()+" | Item Name: "+ item.getName());
        }
        db.put(item.getId(),item); // Item added to the DB
    }

    public List<Item> getMyItemsFromDB(String sellerID){
        List<Item> itemList;
        if(sellerID == null || sellerID.isEmpty()){
            throw new RuntimeException("Invalid Seller ID : "+sellerID);
        }
        return db.values().stream().filter(itm -> sellerID.equals(itm.getSellerId())).collect(Collectors.toList()); //Return Sellers All Items
    }

    public List<Item> getAllItemsFromDB(){
        if(db.isEmpty()){
            throw new RuntimeException("System has no Items listed!");
        }
        return new ArrayList<>(db.values()); //Return all available Items
    }

    public void updateItemInDB(Item item){
        if(!db.containsKey(item.getId())){
            throw new RuntimeException("Item has not exist to update on the system! - ItemID : "+item.getId());
        }
        db.replace(item.getId(),item); // Item update on DB
    }

    public void deleteItemInDB(String itemID){
        if(!db.containsKey(itemID)){
            throw new RuntimeException("Item has not exist to remove on the system! - ItemID : "+itemID);
        }
        db.remove(itemID); // Item Remove on DB
    }


}
