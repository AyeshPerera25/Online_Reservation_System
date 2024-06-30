package com.iit.ds.coursework.ayesh.seller;

import com.iit.ds.coursework.ayesh.grpc.server.*;
import com.iit.ds.coursework.ayesh.resources.registration.NameServiceClient;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SellerClient {

    private String serverIP;
    private int serverPort;
    private final Scanner scanner;
    private boolean isLogged = false;
    private ManagedChannel channel = null;
    private String clientID;
    private String clientName;
    private static Map<String, NameServiceClient.ServiceDetails> serverDetailMap = new HashMap<>();;
    private static final String initServerID = "server";
    private static String regServerID ;
    private static final String initServerIp = "127.0.0.1";
    private static final int initServerPort = 11436;
    private static int regServerPort ;
    public static final String NAME_SERVICE_ADDRESS = "http://localhost:2379";
    private AddItemServiceGrpc.AddItemServiceBlockingStub addItemServiceBlockingStub;
    private DeleteItemServiceGrpc.DeleteItemServiceBlockingStub deleteItemServiceBlockingStub;
    private GetMyItemServiceGrpc.GetMyItemServiceBlockingStub getMyItemServiceBlockingStub;
    private UpdateItemServiceGrpc.UpdateItemServiceBlockingStub updateItemServiceBlockingStub;


    public SellerClient(String serverIP, int serverPort) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args)  {
        String ip;
        int port;
        SellerClient client;
        NameServiceClient.ServiceDetails serviceDetails;

        updateServerDetails();
        serviceDetails = selectServer();
        ip = serviceDetails.getIPAddress();
        port = serviceDetails.getPort();

        client = new SellerClient(ip, port);
        try {
            client.initializeConnection();
            Thread.sleep(1000);
            client.processUserLogin();
            client.loadSellerPortal();

        } catch (Exception e) {
            System.out.println("Internal Server Failure!, Cause: " + e.getMessage());
            client.printStackTrace(e);
        } finally {
            client.systemShutDown();
        }
    }

    private static void updateServerDetails() {

        int serverNo = 0;
        String serverName ;
        int port = initServerPort-1;
        NameServiceClient.ServiceDetails serviceDetails = null;
        NameServiceClient client;
        System.out.println(" ~~ Loading Servers....");
        try {
            client = new NameServiceClient(NAME_SERVICE_ADDRESS);
            do {
                serverNo += 1;
                serverName = initServerID + serverNo;
                port += 1;
                serviceDetails = client.findOnceService(serverName);
                if (serviceDetails != null && !serverDetailMap.containsKey(serverName)) {
                    serverDetailMap.put(serverName, serviceDetails);
                }
            } while (serviceDetails != null);
        }catch (Exception e){
            System.out.println("Server Detail Update Failed! due to: "+e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }
    private static NameServiceClient.ServiceDetails selectServer(){
        NameServiceClient.ServiceDetails serviceDetails;
        List<String> name = new ArrayList<>();
        int serverNo= 0;
        Scanner scanner1 = new Scanner(System.in);
        System.out.println("========== Select Connecting Server ========== ");
        for(String serverName : serverDetailMap.keySet()){
            serverNo++;
            System.out.println("["+serverNo+"] "+serverName);
            name.add(serverName);
        }
        System.out.print(" Select the number for connecting server: ");
        int number = Integer.parseInt(scanner1.nextLine().trim());
        System.out.println("================================================");
        regServerID = name.get(number-1);
        return serverDetailMap.get(name.get(number-1));
    }

    private void initializeConnection() {
        System.out.println("Initializing Connecting to server at " + serverIP + ":" + serverPort);
        channel = ManagedChannelBuilder
                .forAddress(serverIP, serverPort)
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
        if (!isLoginSuccess) {
            isLogged = false;
            System.out.println("\nLogin Failed! | Try to re-login");
            processUserLogin();
        }
        isLogged = true;
        System.out.println("\nLogin Successfully ! UserID: " + clientID + " UserName: " + clientName);
    }

    private String readUserInput() {
        String userInput = scanner.nextLine().trim();
        if (userInput.equalsIgnoreCase("exit")) {
            systemShutDown();
        } else if (isLogged && userInput.equalsIgnoreCase("menu")) {
            loadSellerPortal();
        }
        return userInput;
    }

    private boolean validateLogin(String userID, String name) {
        System.out.println("Validating Login..."); //todo update this proper login validate with server
        this.clientID = userID;
        this.clientName = name;
        return true;
    }

    private void systemShutDown() {
        if (channel != null) {
            channel.shutdown();
        }
        System.out.println("Seller portal shutting down....");
        System.exit(1);
    }

    private void loadSellerPortal() {
        int service;
        System.out.println("\n-------- Welcome to The Seller Portal " + clientName + " (Id: " + clientID + " ) --------[Enter 'exit' to exit]");
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
        System.out.print("Enter Service No: ");
        action = readUserInput();
        if (!(action.equals("1") || action.equals("2") || action.equals("3") || action.equals("4"))) {
            System.out.println("\nInvalid Input! Enter the given service no only.");
            return loadSellerServices();
        }
        return Integer.parseInt(action);
    }

    private void printStackTrace(Exception e) {
        System.out.print("\n Required to print Stack Trace? (Yes: y , No: n) :");
        boolean isPrintStackTraceRequired = scanner.nextLine().trim().equalsIgnoreCase("y");
        if (isPrintStackTraceRequired) {
            System.out.println("\nPrinting Stack Trace: ");
            e.printStackTrace();
        }
    }

    private void processUserServiceRequest(int service) {
        switch (service) {
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
                processDeleteItem();
                break;
            default:
                System.out.println("\nService can't identified!.");
                break;
        }
        loadSellerPortal();
    }

    private void processDeleteItem() {
        DeleteItemResponse response;

        try {
            System.out.println("\nDelete Item Form Loading.... [Enter 'exit' to exit | 'menu' to Main Menu]");
            response = deleteItemOnSystem();
            System.out.println("Item has deleted successfully! - ItemID: " + response.getId());
        } catch (Exception e) {
            System.out.println("Process Delete Item Has Failed! due to:" + e.getMessage());
            printStackTrace(e);
        }
    }

    private DeleteItemResponse deleteItemOnSystem() throws InterruptedException {
        List<Item> itemList;
        Item deleteItem;
        DeleteItemRequest request;
        DeleteItemResponse response;
        boolean finalConfirm;

        itemList = loadSellersItemsFromSystem().getItemsList();
        if (itemList.isEmpty()) {
            throw new RuntimeException("Seller has no item listed on the system");
        }
        deleteItem = getRequiredToDeleteItemID(itemList);
        printItem(deleteItem);
        finalConfirm = getFinalConfirmToDelete(deleteItem);
        if (!finalConfirm) {
            throw new RuntimeException("Item has not deleted on the system due to seller not confirmed! itemId: " + deleteItem.getId());
        }
        request = DeleteItemRequest.newBuilder()
                .setItemID(deleteItem.getId())
                .setIsMasterReq(false)
                .build();
        response = deleteItemServiceBlockingStub.removeItem(request);
        if (!response.getStatus()) {
            throw new RuntimeException(response.getDescription());
        }
        return response;
    }

    private boolean getFinalConfirmToDelete(Item deleteItem) {
        System.out.println("\n-----------------------------------");
        System.out.print("\nConfirm to delete item on id: " + deleteItem.getId() + " (Yes: y , No: n) :");
        boolean finalConfirmation = scanner.nextLine().trim().equalsIgnoreCase("y");
        System.out.println("\n-----------------------------------");
        return finalConfirmation;
    }

    private Item getRequiredToDeleteItemID(List<Item> itemList) throws InterruptedException {
        String itemId;
        List<Item> selectedItemList;
        Thread.sleep(1000);
        System.out.print("\nEnter the required to delete itemID: ");
        itemId = readUserInput();
        selectedItemList = itemList.stream().filter(itm -> itm.getId().equals(itemId)).collect(Collectors.toList());
        if (selectedItemList.isEmpty()) {
            System.out.println("No item listed under the sellers name using itemID: " + itemId + " | Please Try Again!");
            return getRequiredToDeleteItemID(itemList);
        }
        return selectedItemList.get(0);
    }

    private void processUpdateItem() {
        UpdateItemResponse response;

        try {
            System.out.println("\nUpdating Item Form Loading.... [Enter 'exit' to exit | 'menu' to Main Menu]");
            response = updateItemOnSystem();
            System.out.println("Item has updated successfully! - ItemID: " + response.getId());
        } catch (Exception e) {
            System.out.println("Process Update Item Has Failed! due to:" + e.getMessage());
            printStackTrace(e);
        }
    }

    private UpdateItemResponse updateItemOnSystem() throws InterruptedException {
        Item item;
        List<Item> itemList;
        Item newItem;
        UpdateItemRequest request;
        UpdateItemResponse response;

        itemList = loadSellersItemsFromSystem().getItemsList();
        if (itemList.isEmpty()) {
            throw new RuntimeException("Seller has no item listed on the system");
        }
        item = getRequiredToUpdateItem(itemList);
        printItem(item);
        newItem = getUpdateItem(item);
        request = UpdateItemRequest.newBuilder()
                .setItem(newItem)
                .setIsMasterReq(false)
                .build();
        response = updateItemServiceBlockingStub.updateItem(request);
        if (!response.getStatus()) {
            throw new RuntimeException(response.getDescription());
        }
        return response;
    }

    private Item getUpdateItem(Item item) {
        String name;
        String type;
        String price;
        String availableQty;
        String description;
        Item newItem;
        System.out.println("\n~~~~~~~~~ :Update Item Form: ~~~~~~~~~ [Enter 'exit' to exit | 'menu' to Main Menu | leave empty to set original]");
        System.out.print("\nEnter update item name: ");
        name = readUserInput();
        if (name.isEmpty()) {
            System.out.print(item.getName());
        }
        System.out.print("\nEnter update item type: ");
        type = readUserInput();
        if (type.isEmpty()) {
            System.out.print(item.getType());
        }
        System.out.print("\nEnter update item price: ");
        price = readUserInput();
        if (price.isEmpty()) {
            System.out.print(item.getPrice());
        }
        System.out.print("\nEnter update item available quantity: ");
        availableQty = readUserInput();
        if (availableQty.isEmpty()) {
            System.out.print(item.getAvailableQty());
        }

        if (!availableQty.isEmpty() && Double.parseDouble(availableQty) < item.getReservationsCount()) {
            System.out.println("There are " + item.getReservationsCount() + " reservations cannot reduce the quantity lower than it | Try Again!");
            return getUpdateItem(item);
        }
        if (!price.isEmpty() && Double.parseDouble(price) <= 0) {
            System.out.println("Price cannot be " + Double.parseDouble(price) + " price must be greater than zero | Try Again!");
            return getUpdateItem(item);
        }

        System.out.print("\nEnter update item description: ");
        description = readUserInput();
        newItem = item.toBuilder()
                .setName(name.isEmpty() ? item.getName() : name)
                .setType(type.isEmpty() ? item.getType() : type)
                .setPrice(price.isEmpty() ? item.getPrice() : Double.parseDouble(price))
                .setAvailableQty(availableQty.isEmpty() ? item.getAvailableQty() : Double.parseDouble(availableQty))
                .setDescription(description.isEmpty() ? item.getDescription() : description)
                .build();
        return newItem;
    }

    public Item getRequiredToUpdateItem(List<Item> itemList) throws InterruptedException {
        String itemId;
        List<Item> selectedItemList;
        Thread.sleep(1000);
        System.out.print("\nEnter the required to update itemID: ");
        itemId = readUserInput();
        selectedItemList = itemList.stream().filter(itm -> itm.getId().equals(itemId)).collect(Collectors.toList());
        if (selectedItemList.isEmpty()) {
            System.out.println("No item listed under the sellers name using itemID: " + itemId + " | Please Try Again!");
            return getRequiredToUpdateItem(itemList);
        }
        return selectedItemList.get(0);
    }

    private void processAddItem() {
        AddItemResponse response;

        try {
            System.out.println("\nAdding New Item....");
            response = addNewItemToSystem();
            System.out.println("Item has added successfully! - ItemID: " + response.getId());
        } catch (Exception e) {
            System.out.println("Process Add Item Has Failed! due to:" + e.getMessage());
            printStackTrace(e);
        }
    }

    private AddItemResponse addNewItemToSystem() {
        Item newItem;
        AddItemRequest request;
        AddItemResponse response;

        newItem = populateNewItem();
        request = AddItemRequest.newBuilder()
                .setItem(newItem)
                .setIsMasterReq(false)
                .build();
        response = addItemServiceBlockingStub.addItem(request);
        if (!response.getStatus()) {
            throw new RuntimeException(response.getDescription());
        }
        return response;
    }

    private Item populateNewItem() {
        String name;
        String type;
        double price;
        double availableQty;
        String description;
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
        GetMyItemsResponse response;
        ;
        try {
            System.out.println("Getting sellers items....");
            response = loadSellersItemsFromSystem();
            if (!response.getItemsList().isEmpty()) {
                response.getItemsList().forEach(this::printItem);
            } else {
                System.out.println("No item listed in the system under sellerID: " + clientID);
            }
        } catch (Exception e) {
            System.out.println("Process Getting All Sellers Item Has Failed! due to:" + e.getMessage());
            printStackTrace(e);
        }
    }

    private GetMyItemsResponse loadSellersItemsFromSystem() {
        GetMyItemsRequest request;
        GetMyItemsResponse response;
        ;
        request = GetMyItemsRequest.newBuilder()
                .setSellerId(clientID)
                .build();
        response = getMyItemServiceBlockingStub.getMyItems(request);
        if (!response.getStatus()) {
            throw new RuntimeException(response.getDescription());
        }
        return response;
    }

    private void printItem(Item item) {
        System.out.println("\n ------------ Item -----------------");
        System.out.println("Item ID: " + item.getId());
        System.out.println("Name: " + item.getName());
        System.out.println("Type: " + item.getType());
        System.out.println("Price: " + item.getPrice());
        System.out.println("Available Qty: " + item.getAvailableQty());
        System.out.println("Description: " + item.getDescription());
        System.out.println("Sellers ID: " + item.getSellerId());
        Map<String, Reservation> reservations = item.getReservationsMap();
        if (!reservations.isEmpty()) {
            reservations.values().forEach(this::printReservation);
        } else {
            System.out.println("No Reservations Yet!");
        }
        System.out.println("========================================");
    }

    private void printReservation(Reservation reservation) {
        System.out.println("\n~~~~~~ Reservation ~~~~~~");
        System.out.println("Res ID: " + reservation.getId());
        System.out.println("Date : " + reservation.getDate());
        System.out.println("Customer Id: " + reservation.getCustId());
        System.out.println("Description: " + reservation.getDescription());
    }


}
