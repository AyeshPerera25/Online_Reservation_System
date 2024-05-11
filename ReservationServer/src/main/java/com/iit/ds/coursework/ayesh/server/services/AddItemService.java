package com.iit.ds.coursework.ayesh.server.services;


import com.iit.ds.coursework.ayesh.grpc.server.Item;
import com.iit.ds.coursework.ayesh.server.ReservationServerCore;
import com.iit.ds.coursework.ayesh.grpc.server.AddItemRequest;
import com.iit.ds.coursework.ayesh.grpc.server.AddItemResponse;
import com.iit.ds.coursework.ayesh.grpc.server.AddItemServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.zookeeper.KeeperException;

import java.util.Date;
import java.util.List;

public class AddItemService extends AddItemServiceGrpc.AddItemServiceImplBase {
    private final ReservationServerCore server;

    public AddItemService(ReservationServerCore server) {
        this.server = server;
    }

    @Override
    public void addItem(AddItemRequest request, StreamObserver<AddItemResponse> responseObserver) {
        AddItemResponse response;
        Item item = null;

        try {
            if (server.isMaster()) { // Master Role to Add Item to System
                synchronized (server) {
                    System.out.println("Adding item to the system as Master... ");
                    item = populateItem(request.getItem()); // Generate item with unique ID
                    updateSlaveServers(item);
                    server.addItemToDB(item); // add item to Master DB
                }
                response = populateResponse(true, "Item Successfully Added To The System! Item No: " + item.getId() + " | Item Name: " + item.getName(), item.getId());
                System.out.println("Adding item to the system has successes!  Item No: " + item.getId() + " | Item Name: " + item.getName());
            } else { // Slave Role to Add Item to System
                if(request.getIsMasterReq()){  // Slave Role if the request from Master
                    item = request.getItem();
                    synchronized (server) {
                        System.out.println("Adding item to the local DB as Slave on Master command...");
                        server.addItemToDB(request.getItem()); // add item to Slave DB
                    }
                    response = populateResponse(true, "Item Successfully Added To The Slave System! Item No: " + item.getId() + " | Item Name: " + item.getName(), item.getId());
                    System.out.println("Adding item to the system has successes!  Item No: " + item.getId() + " | Item Name: " + item.getName());
                }else { // Slave Role if the request from Client
                    if(!server.isServerReady()){ // Block getting request from clients until server get synced
                     throw new RuntimeException("Bootstrap server initiation not completed!");
                    }
                    response = updateMasterServer(request);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            response = populateResponse(false, e.getMessage(), item != null? item.getId():"");
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private AddItemResponse updateMasterServer(AddItemRequest request) throws InterruptedException, KeeperException {
        AddItemResponse response;
        String []  currentMasterData;

        try {
            System.out.println("Updating added item to the Master...");
            currentMasterData = server.getCurrentMasterData();
            response = callServerAddItemUpdate(request.getItem(),false, currentMasterData[0], Integer.parseInt(currentMasterData[1]));
        }catch (Exception e){
            throw new RuntimeException("Send Add Item Update to Master has failed! Due to:"+e.getMessage());
        }
        return response;
    }

    private void updateSlaveServers(Item item) {
        List<String[]> slaveServerData;
        try{
            System.out.println("Updating added item to the Slave Servers...");
            slaveServerData = server.getSlaveServerData();
            if(slaveServerData.isEmpty()){
                System.out.println("Slave Server Update Skipped! Unable to find data on Slave Servers.");
                return;
            }
            slaveServerData.forEach(data -> callServerAddItemUpdate(item, true, data[0], Integer.parseInt(data[1])));
            System.out.println("Slave servers successfully updated on added item! ItemNo: "+item.getId());
        }catch (Exception e){
            throw new RuntimeException("Update Slave Servers Failed! Reason: "+e.getMessage());
        }
    }

    private AddItemResponse callServerAddItemUpdate(Item item, boolean isSentByMaster, String IPAddress, int port) {
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
                .setItem(item)
                .setIsMasterReq(isSentByMaster)
                .build();
        response = clientStub.addItem(request);
        if(!response.getStatus()){ //if request get failed the abort the update
            throw new RuntimeException("Update Add Item Failed! on server: "+IPAddress+":"+port+" Due to: "+response.getDescription());
        }
        return response;
    }

    private AddItemResponse populateResponse(boolean status, String description , String id) {
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
}
