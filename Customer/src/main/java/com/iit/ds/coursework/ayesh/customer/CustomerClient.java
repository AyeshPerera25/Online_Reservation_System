package com.iit.ds.coursework.ayesh.customer;

import com.iit.ds.coursework.ayesh.grpc.server.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CustomerClient {
    private  final  String serverIP;
    private  final  int serverPort;
    private boolean isLogged =false;
    private ManagedChannel channel = null;
    private String clientID;
    private String clientName;
    private final Scanner scanner;
    private PlaceReservationServiceGrpc.PlaceReservationServiceBlockingStub placeReservationServiceBlockingStub;
    private GetAllItemsServiceGrpc.GetAllItemsServiceBlockingStub getAllItemsServiceBlockingStub;


    public CustomerClient(String serverIP, int serverPort) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        String ip;
        int port;
        CustomerClient client;
        Scanner userInput = new Scanner(System.in);

        System.out.println("========== Enter Connecting Server IP and Port ========== ");
        System.out.print("Server IP: ");
        ip = userInput.nextLine().trim();
        System.out.print("Server Port: ");
        port = Integer.parseInt(userInput.nextLine().trim());
        System.out.println("================================================");
        client= new CustomerClient(ip, port);
        try {
            client.initializeConnection();
            client.processUserLogin();
            client.loadCustomerPortal();
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
                .forAddress(serverIP, serverPort)
                .usePlaintext()
                .build();
        placeReservationServiceBlockingStub = PlaceReservationServiceGrpc.newBlockingStub(channel);
        getAllItemsServiceBlockingStub = GetAllItemsServiceGrpc.newBlockingStub(channel);
    }

    private void processUserLogin() {
        String loginID; //todo update proper login with User ID and Pwd
        String name;
        boolean isLoginSuccess;
        System.out.println("\n---------- Customer Login Portal ----------[Enter 'exit' to exit]");
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
            loadCustomerPortal();
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
        System.out.println("\nCustomer portal shutting down....");
        System.exit(1);
    }

    private void loadCustomerPortal() {
        int service;
        System.out.println("\n-------- Welcome to The Customer Portal " + clientName + " (Id: " + clientID + " ) --------[Enter 'exit' to exit]");
        service = loadCustomerServices();
        processUserServiceRequest(service);
    }

    private void printStackTrace(Exception e){
        System.out.print("\n Required to print Stack Trace? (Yes: y , No: n) :");
        boolean isPrintStackTraceRequired = scanner.nextLine().trim().equalsIgnoreCase("y");
        if (isPrintStackTraceRequired) {
            System.out.println("\nPrinting Stack Trace: ");
            e.printStackTrace();
        }
    }

    private int loadCustomerServices() {
        String action;
        System.out.println("\n~~~~~~~~~~ Customer Service Menu ~~~~~~~~~~");
        System.out.println("1 - Load All Items");
        System.out.println("2 - Make Reservation");
        System.out.print("Enter Service No: ");
        action = readUserInput();
        if(!(action.equals("1") || action.equals("2"))){
            System.out.println("\nInvalid Input! Enter the given service no only.");
            return loadCustomerServices();
        }
        return Integer.parseInt(action);
    }
    private void processUserServiceRequest(int service){
        switch (service){
            case 1:
                processGetAllItems();
                break;
            case 2:
                processMakeReservation();
                break;
            default:
                System.out.println("\nService can't identified!.");
                break;
        }
        loadCustomerPortal();
    }

    private void processMakeReservation() {
        ReservationResponse response;

        try {
            System.out.println("\nInitiating New Reservation...");
            response = placeNewReservationToItem();
            System.out.println("Reservation placed successfully! - ItemID: "+response.getResId());
        }catch (Exception e){
            System.out.println("Placing Reservation Has Failed! due to:"+e.getMessage());
            printStackTrace(e);
        }
    }

    private ReservationResponse placeNewReservationToItem(){
        List<Item> itemList;
        Item resItem;
        ReservationRequest request;
        ReservationResponse response;

        itemList = loadAllItemsFromSystem().getItemsList();
        if(itemList.isEmpty()){
            throw new RuntimeException("No item still listed on the system!");
        }
        System.out.println("\n~~~~~~~~~ :Reservation Portal ~~~~~~~~~ [Enter 'exit' to exit | 'menu' to Main Menu]");

        resItem = getReservationItem(itemList);
        printItem(resItem);
        request = populateReservationRequest(resItem);
        response = placeReservationServiceBlockingStub.placeReservation(request);
        if(!response.getStatus()){
            throw new RuntimeException(response.getDescription());
        }
        return response;
    }

    private Item getReservationItem(List<Item> itemList) {
        String resItemID;
        List<Item> selectedItemList;
        Item resItem;

        System.out.print("Enter the itemId to place reservation: ");
        resItemID = readUserInput();
        selectedItemList = itemList.stream().filter(itm -> itm.getId().equals(resItemID)).collect(Collectors.toList());
        if(selectedItemList.isEmpty()){
            System.out.println("No item listed  in system using itemID: "+resItemID+" | Please Try Again!");
            return getReservationItem(itemList);
        }
        resItem = selectedItemList.get(0);
        if(resItem.getReservationsCount() == resItem.getAvailableQty()) {
            System.out.println("Item reach maximum reservation quantity | Please Try Another Item!");
            return getReservationItem(itemList);
        }
        return resItem;
    }

    private ReservationRequest populateReservationRequest(Item item){
        String day ;
        String month ;
        String year ;
        Date date ;
        Date today;
        String strDate;
        String resId;

        System.out.print("\n Enter the Reservation Day:");
        day = readUserInput();
        System.out.print("\n Enter the Reservation Month:");
        month = readUserInput();
        System.out.print("\n Enter the Reservation Year:");
        year = readUserInput();

        strDate = day+"/"+month+"/"+year;
        resId = day+month+year;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        try {
            date = sdf.parse(strDate);
        }catch (ParseException e){
            System.out.println("\n Invalid date insert due to: "+e.getMessage()+" | Try Again!");
            return populateReservationRequest(item);
        }
        today = new Date();
        if(today.after(date)){
            System.out.println("\n Invalid date insert - Date: " + strDate+" | Enter the future date and Try Again!");
            return populateReservationRequest(item);
        }
        if(!item.getReservationsMap().isEmpty() && item.getReservationsMap().containsKey(resId)){
            System.out.println("\n The Date has reserved already- Date: "+strDate+" | Please Select Another Date!");
            return populateReservationRequest(item);
        }

        return ReservationRequest.newBuilder()
                .setItemId(item.getId())
                .setCustomerId(clientID)
                .setReservationDate(strDate)
                .setIsMasterReq(false)
                .build();
    }

    private void processGetAllItems() {
        GetAllItemResponse response;;
        try {
            System.out.println("\nInitiating Load all items....");
            response = loadAllItemsFromSystem();
            if(!response.getItemsList().isEmpty()){
                response.getItemsList().forEach(this::printItem);
            }else {
                System.out.println("No item listed in the system ");
            }
        }catch (Exception e){
            System.out.println("Process Getting All Item Has Failed! due to:"+e.getMessage());
            printStackTrace(e);
        }
    }
    private GetAllItemResponse loadAllItemsFromSystem(){
        GetAllItemRequest request;
        GetAllItemResponse response;;
        request = GetAllItemRequest.newBuilder()
                .setId(clientID)
                .setIsServer(false)
                .build();
        response = getAllItemsServiceBlockingStub.getAllItems(request);
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
        System.out.println("Available Qty: " + (item.getAvailableQty() - item.getReservationsCount()));
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
        System.out.println("\n~~~~~~ Reserved Date ~~~~~~");
        System.out.println(reservation.getDate());
    }
}
