package com.iit.ds.coursework.ayesh.server.services;

import com.iit.ds.coursework.ayesh.grpc.server.*;
import com.iit.ds.coursework.ayesh.server.ReservationServerCore;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.List;

public class DeleteItemService extends DeleteItemServiceGrpc.DeleteItemServiceImplBase {

    private final ReservationServerCore server;

    public DeleteItemService(ReservationServerCore server) {
        this.server = server;
    }

    @Override
    public void removeItem(DeleteItemRequest request, StreamObserver<DeleteItemResponse> responseObserver) {
        String itemID;
        DeleteItemResponse response;

        try {
            if (server.isMaster()) { // Master Role to Delete Item to System
                synchronized (server) {
                    System.out.println("Delete item from the system as Master... ");
                    itemID = request.getId();
                    updateSlaveServers(itemID);
                    server.deleteItemInDB(itemID); // Delete item from Master DB
                }
                response = populateResponse(true, "Delete Item Has Successes!",itemID);
                System.out.println(" Item has deleted on the system! Item ID: "+ itemID);
            } else { // Slave Role to Delete Item to System
                if(request.getIsMasterReq()){  // Slave Role if the request from Master
                    itemID = request.getId();
                    synchronized (server) {
                        System.out.println("Deleting item from the local DB as Slave on Master command...");
                        server.deleteItemInDB(itemID); // delete item on local db
                    }
                    response = populateResponse(true, "Item Successfully Deleted from The Slave System! Item ID: " + itemID, itemID);
                    System.out.println("Delete item from the system has successes!  Item ID: " + itemID);
                }else { // Slave Role if the request from Client
                    if(!server.isServerReady()){ // Block getting request from clients until server get synced
                        throw new RuntimeException("Bootstrap server initiation not completed!");
                    }
                    response = updateMasterServer(request);
                }
            }
        }catch (Exception e){
            System.out.println("Delete items has Failed! | "+ e.getMessage());
            response = populateResponse(false, e.getMessage(),request.getId());
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private DeleteItemResponse updateMasterServer(DeleteItemRequest request) {
        DeleteItemResponse response;
        String []  currentMasterData;

        try {
            System.out.println("Updating delete item to the Master...");
            currentMasterData = server.getCurrentMasterData();
            response = callServerDeleteItemUpdate(request.getId(),false, currentMasterData[0], Integer.parseInt(currentMasterData[1]));
        }catch (Exception e){
            throw new RuntimeException("Send Delete Item Update to Master has failed! Due to:"+e.getMessage());
        }
        return response;
    }

    private void updateSlaveServers(String itemID) {
        List<String[]> slaveServerData;
        try{
            System.out.println("Updating delete item to the Slave Servers...");
            slaveServerData = server.getSlaveServerData();
            if(slaveServerData.isEmpty()){
                System.out.println("Slave Server Update Skipped! Unable to find data on Slave Servers.");
                return;
            }
            slaveServerData.forEach(data -> callServerDeleteItemUpdate(itemID, true, data[0], Integer.parseInt(data[1])));
            System.out.println("Slave servers successfully updated on delete item! ItemID: "+itemID);
        }catch (Exception e){
            throw new RuntimeException("Update Slave Servers Failed! Reason: "+e.getMessage());
        }
    }

    private DeleteItemResponse callServerDeleteItemUpdate(String itemID, boolean isSentByMaster, String IPAddress, int port) {
        ManagedChannel channel;
        DeleteItemServiceGrpc.DeleteItemServiceBlockingStub clientStub;
        DeleteItemRequest request;
        DeleteItemResponse response;

        System.out.println("Call Server " + IPAddress + ":" + port);
        channel = ManagedChannelBuilder.forAddress(IPAddress, port)
                .usePlaintext()
                .build();
        clientStub = DeleteItemServiceGrpc.newBlockingStub(channel);
        request = DeleteItemRequest.newBuilder()
                .setId(itemID)
                .setIsMasterReq(isSentByMaster)
                .build();
        response = clientStub.removeItem(request);
        if(!response.getStatus()){ //if request get failed the abort the update
            throw new RuntimeException("Update Delete Item Failed! on server: "+IPAddress+":"+port+" Due to: "+response.getDescription());
        }
        return response;
    }

    private DeleteItemResponse populateResponse(boolean status, String description, String itemId) {
        return DeleteItemResponse.newBuilder()
                .setId(itemId)
                .setStatus(status)
                .setDescription(description)
                .build();
    }
}
