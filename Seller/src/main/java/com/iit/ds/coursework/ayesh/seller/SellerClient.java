package com.iit.ds.coursework.ayesh.seller;

import com.iit.ds.coursework.ayesh.grpc.server.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

public class SellerClient {

    private  final  String serverIP;
    private  final  int serverPort;
    private boolean isLogged =false;
    private ManagedChannel channel = null;
    private String clientID;
    private String clientName;
    private final Scanner scanner;
    private AddItemServiceGrpc.AddItemServiceBlockingStub addItemServiceBlockingStub;
    private DeleteItemServiceGrpc.DeleteItemServiceBlockingStub deleteItemServiceBlockingStub;
    private GetMyItemServiceGrpc.GetMyItemServiceBlockingStub getMyItemServiceBlockingStub;
    private UpdateItemServiceGrpc.UpdateItemServiceBlockingStub updateItemServiceBlockingStub;


    public SellerClient(String serverIP, int serverPort) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        String ip;
        int port;
        SellerClient client;
        if (args.length != 2) {
            System.out.println("Input: Reservation Server <host> <port>");
            System.exit(1);
        }
        ip = args[0];
        port = Integer.parseInt(args[1].trim());
        client= new SellerClient(ip, port);
        try {
            client.initializeConnection();
            client.processUserLogin();
            client.loadSellerPortal();

        }catch (Exception e){
            System.out.println("Internal Server Failure!, Cause: " + e.getMessage());
            client.printStackTrace(e);
        }finally {
            client.systemShutDown();
        }
    }

    private void initializeConnection() {
        System.out.println("Initializing Connecting to server at " + serverIP + ":" + serverPort);
        channel = ManagedChannelBuilder
                .forAddress("localhost", 11436)
                .usePlaintext()
                .build();
        addItemServiceBlockingStub = AddItemServiceGrpc.newBlockingStub(channel);
        deleteItemServiceBlockingStub = DeleteItemServiceGrpc.newBlockingStub(channel);
        getMyItemServiceBlockingStub = GetMyItemServiceGrpc.newBlockingStub(channel);
        updateItemServiceBlockingStub = UpdateItemServiceGrpc.newBlockingStub(channel);

    }

    private void processUserLogin() {
        String loginID; //todo update proper login with User ID and Pwd
        String name;
        boolean isLoginSuccess;
        System.out.println("\n---------- Seller Login Portal ----------[Enter 'exit' to exit]");
        System.out.print("\nEnter login ID: ");
        loginID = readUserInput();
        System.out.print("\nEnter name: ");
        name = readUserInput();
        isLoginSuccess = validateLogin(loginID, name);
        if(!isLoginSuccess){
            isLogged = false;
            System.out.println("\nLogin Failed! | Try to re-login");
            processUserLogin();
        }
        isLogged = true;
        System.out.println("\nLogin Successfully ! UserID: "+clientID+" UserName: "+clientName);
    }

    private String readUserInput() {
        String userInput = scanner.nextLine().trim();
        if(userInput.equalsIgnoreCase("exit")){
            systemShutDown();
        }else if(isLogged && userInput.equalsIgnoreCase("menu")) {
            loadSellerPortal();
        }
        return userInput;
    }

    private boolean validateLogin(String userID,String name){
        System.out.println("Validating Login..."); //todo update this proper login validate with server
        this.clientID = userID;
        this.clientName = name;
        return true;
    }

    private void systemShutDown(){
        if(channel != null){
            channel.shutdown();
        }
        System.out.println("Seller portal shutting down....");
        System.exit(1);
    }

    private void loadSellerPortal() {
        int service;
        System.out.println("\n-------- Welcome to the seller portal " + clientName + " (Id: " + clientID + " ) --------[Enter 'exit' to exit]");
        service = loadSellerServices();
        processUserServiceRequest(service);
    }

    private int loadSellerServices() {
        String action;
        System.out.println("\n~~~~~~~~~~ Seller Service Menu ~~~~~~~~~~");
        System.out.println("1 - Load My Items");
        System.out.println("2 - Add Item");
        System.out.println("3 - Update Item");
        System.out.println("4 - Delete Item");
        action = readUserInput();
        if(!(action.equals("1") || action.equals("2") || action.equals("3") || action.equals("4"))){
            System.out.println("Invalid Input! Enter the given service no only.");
            loadSellerServices();
        }
        return Integer.parseInt(action);
    }

    private void printStackTrace(Exception e){
        System.out.print("\n Required to print Stack Trace? (Yes: y , No: n) :");
        boolean isPrintStackTraceRequired = scanner.nextLine().trim().equalsIgnoreCase("y");
        if (isPrintStackTraceRequired) {
            System.out.println("\nPrinting Stack Trace: ");
            e.printStackTrace();
        }
    }

    private void processUserServiceRequest(int service){
        switch (service){
            case 1:
                processGetMyItems();
                break;
            case 2:
                processAddItem();
                break;
            case 3:
                processUpdateItem();
                break;
            case 4:
                break;
            default:
                System.out.println("Service can't identified!.");
                break;
        }
        loadSellerPortal();
    }

    private void processUpdateItem() {
        UpdateItemResponse response;

        try{
            System.out.println("\nUpdating Item....");
            response = updateItemOnSystem();
            System.out.println("Item has updated successfully! - ItemID: "+response.getId());
        }catch (Exception e){
            System.out.println("Process Update Item Has Failed! due to:"+e.getMessage());
            printStackTrace(e);
        }
    }

    private UpdateItemResponse updateItemOnSystem(){
        Item item;
        List<Item> itemList;
        Item newItem;
        UpdateItemRequest request;
        UpdateItemResponse response;

        itemList = loadSellersItemsFromSystem().getItemsList();
        if(itemList.isEmpty()){
            throw new RuntimeException("Seller has no item listed on the system");
        }
        item = getRequiredToUpdateItem(itemList);
        printItem(item);
        newItem = getUpdateItem(item);
        request = UpdateItemRequest.newBuilder()
                .setItem(newItem)
                .build();
        response = updateItemServiceBlockingStub.updateItem(request);
        if(!response.getStatus()){
            throw new RuntimeException(response.getDescription());
        }
        return response;
    }

    private Item getUpdateItem(Item item) {
        String name ;
        String type ;
        double price ;
        double availableQty ;
        String description ;
        Item newItem;
        System.out.println("\n~~~~~~~~~ :Update Item Form: ~~~~~~~~~ [Enter 'exit' to exit | 'menu' to Main Menu]");
        System.out.print("\nEnter update item name: ");
        name = readUserInput();
        System.out.print("\nEnter update item type: ");
        type = readUserInput();
        System.out.print("\nEnter update item price: ");
        price = Double.parseDouble(readUserInput());
        System.out.print("\nEnter update item available quantity: ");
        availableQty = Double.parseDouble(readUserInput());
        System.out.print("\nEnter update item description: ");
        description = readUserInput();
        newItem = Item.newBuilder()
                .setId(item.getId())
                .setDescription(description)
                .setAvailableQty(availableQty)
                .setType(type)
                .setName(name)
                .setPrice(price)
                .setSellerId(clientID)
                .build();
        return newItem;
    }

    public Item getRequiredToUpdateItem(List<Item> itemList){
        String itemId;
        Item item;
        System.out.print("\nEnter the required to update itemID: ");
        itemId = readUserInput();
        item = itemList.stream().filter(itm -> itm.getId().equals(itemId)).collect(Collectors.toList()).getFirst();
        if(item == null){
            System.out.println("No item listed under the sellers name using itemID: "+itemId+" | Please Try Again!");
            return getRequiredToUpdateItem(itemList);
        }
        return item;
    }

    private void processAddItem() {
        AddItemResponse response;

        try {
            System.out.println("\nAdding New Item....");
            response =addNewItemToSystem();
            System.out.println("Item has added successfully! - ItemID: "+response.getId());
        }catch (Exception e){
            System.out.println("Process Add Item Has Failed! due to:"+e.getMessage());
            printStackTrace(e);
        }
    }

    private AddItemResponse addNewItemToSystem(){
        Item newItem;
        AddItemRequest request;
        AddItemResponse response;

        newItem = populateNewItem();
        request = AddItemRequest.newBuilder()
                .setItem(newItem)
                .build();
        response = addItemServiceBlockingStub.addItem(request);
        if(!response.getStatus()){
            throw new RuntimeException(response.getDescription());
        }
        return response;
    }

    private Item populateNewItem(){
        String name ;
        String type ;
        double price ;
        double availableQty ;
        String description ;
        Item newItem;
        System.out.println("\n~~~~~~~~~ :Add New Item Form: ~~~~~~~~~ [Enter 'exit' to exit | 'menu' to Main Menu]");
        System.out.print("\nEnter item name: ");
        name = readUserInput();
        System.out.print("\nEnter item type: ");
        type = readUserInput();
        System.out.print("\nEnter item price: ");
        price = Double.parseDouble(readUserInput());
        System.out.print("\nEnter item available quantity: ");
        availableQty = Double.parseDouble(readUserInput());
        System.out.print("\nEnter item description: ");
        description = readUserInput();
        newItem = Item.newBuilder()
                .setDescription(description)
                .setAvailableQty(availableQty)
                .setType(type)
                .setName(name)
                .setPrice(price)
                .setSellerId(clientID)
                .build();
        return newItem;
    }

    private void processGetMyItems() {
        GetMyItemsResponse response;;
        try {
            System.out.println("Getting sellers items....");
            response = loadSellersItemsFromSystem();
            if(!response.getItemsList().isEmpty()){
                response.getItemsList().forEach(this::printItem);
            }else {
                System.out.println("No item listed in the system under sellerID: "+clientID);
            }
        }catch (Exception e){
            System.out.println("Process Getting All Sellers Item Has Failed! due to:"+e.getMessage());
            printStackTrace(e);
        }
    }
    private GetMyItemsResponse loadSellersItemsFromSystem(){
        GetMyItemsRequest request;
        GetMyItemsResponse response;;
        request = GetMyItemsRequest.newBuilder()
                .setSellerId(clientID)
                .build();
        response = getMyItemServiceBlockingStub.getMyItems(request);
        if(!response.getStatus()){
            throw new RuntimeException(response.getDescription());
        }
        return response;
    }

    private void printItem(Item item){
        System.out.println("\n ------------ Item -----------------");
        System.out.println("Item ID: " + item.getId());
        System.out.println("Name: " + item.getName());
        System.out.println("Type: " + item.getType());
        System.out.println("Price: " + item.getPrice());
        System.out.println("Available Qty: " + item.getAvailableQty());
        System.out.println("Description: " + item.getDescription());
        System.out.println("Sellers ID: " + item.getSellerId());
        Map<String, Reservation> reservations = item.getReservationsMap();
        if(!reservations.isEmpty()){
            reservations.values().forEach(this::printReservation);
        }else {
            System.out.println("No Reservations Yet!");
        }
        System.out.println("========================================");

    }
    private void printReservation(Reservation reservation){
        System.out.println("\n~~~~~~ Reservation ~~~~~~");
        System.out.println("Res ID: " + reservation.getId());
        System.out.println("Customer Id: " + reservation.getCustId());
        System.out.println("Description: " + reservation.getDescription());
    }


}
