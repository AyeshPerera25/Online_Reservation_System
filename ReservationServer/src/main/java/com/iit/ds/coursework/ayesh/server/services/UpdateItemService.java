package com.iit.ds.coursework.ayesh.server.services;

import com.iit.ds.coursework.ayesh.grpc.server.*;
import com.iit.ds.coursework.ayesh.resources.transaction.DistributedTxListener;
import com.iit.ds.coursework.ayesh.server.ReservationServerCore;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.List;

public class UpdateItemService extends UpdateItemServiceGrpc.UpdateItemServiceImplBase implements DistributedTxListener {
    private final ReservationServerCore server;
    private Item item = null;
    private boolean isTxnStarted;
    private String description;
    private String requestID = null;

    public UpdateItemService(ReservationServerCore server) {
        this.server = server;
    }

    @Override
    public void updateItem(UpdateItemRequest request, StreamObserver<UpdateItemResponse> responseObserver) {
        UpdateItemResponse response;

        if (server.isMaster()) { // Master Role to Update Item on System
            try {
                item = request.getItem();
                requestID = "UPDATE" + String.valueOf(System.currentTimeMillis());
                synchronized (server) {
                    System.out.println("Updating listed item on the system as Master...  Item ID: " + item.getId());
                    preValidate();
                    server.startDistributedTxn(requestID, this); // initiate the distributed transaction on the system
                    updateSlaveServers();
                    server.performTxnCommit();
                }
                response = populateResponse(true, "Update Item Has Successes!", request.getItem().getId());
                System.out.println(" Item has updated on the system! Item ID: " + item.getId());
            } catch (Exception e) {
                System.out.println("Update items has Failed! Item No: " + item.getId() + " | Item Name: " + item.getName());
                if (isTxnStarted) {
                    System.out.println("Initiating Global Abort..");
                    server.performTxnCommitAbort();
                }
                System.out.println(e.getMessage());
                response = populateResponse(false, e.getMessage(), request.getItem().getId());
            }
        } else {// Slave Role to Update Item to System
            try {
                if (request.getIsMasterReq()) {  // Slave Role if the request from Master
                    item = request.getItem();
                    requestID = request.getReqID();
                    synchronized (server) {
                        System.out.println("Updating item on the local DB as Slave on Master command...");
                        preValidate();
                        server.startDistributedTxn(requestID, this);
                        server.voteCommit();
                    }
                    response = populateResponse(true, "Item Successfully Updated on The Slave System! Item No: " + item.getId() + " | Item Name: " + item.getName(), item.getId());
                    System.out.println("Update item on the system has successes!  Item No: " + item.getId() + " | Item Name: " + item.getName());
                } else {// Slave Role if the request from Client
                    if (!server.isServerReady()) { // Block getting request from clients until server get synced
                        throw new RuntimeException("Bootstrap server initiation not completed!");
                    }
                    item = request.getItem();
                    response = updateMasterServer();
                }
            } catch (Exception e) {
                System.out.println("Update items has Failed! Item No: " + item.getId() + " | Item Name: " + item.getName());
                if (isTxnStarted) {
                    System.out.println("Initiating vote Abort..");
                    server.voteAbort();
                }
                System.out.println(e.getMessage());
                response = populateResponse(false, e.getMessage(), request.getItem().getId());
            }
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void preValidate() {
        if (!server.isItemAlreadyExist(item.getId())) {
            throw new RuntimeException("Item has not exist to update on the system! - ItemID : " + item.getId());
        }
    }

    private UpdateItemResponse updateMasterServer() {
        UpdateItemResponse response;
        String[] currentMasterData;

        try {
            System.out.println("Updating item to the Master...");
            currentMasterData = server.getCurrentMasterData();
            response = callServerItemUpdate(false, currentMasterData[0], Integer.parseInt(currentMasterData[1]));
        } catch (Exception e) {
            throw new RuntimeException("Send Item Update to Master has failed! Due to:" + e.getMessage());
        }
        return response;
    }

    private void updateSlaveServers() {
        List<String[]> slaveServerData;
        try {
            System.out.println("Updating item on the Slave Servers...");
            slaveServerData = server.getSlaveServerData();
            if (slaveServerData.isEmpty()) {
                System.out.println("Slave Server Update Skipped! Unable to find data on Slave Servers.");
                return;
            }
            slaveServerData.forEach(data -> callServerItemUpdate(true, data[0], Integer.parseInt(data[1])));
            System.out.println("Slave servers successfully updated on ! ItemNo: " + item.getId());
        } catch (Exception e) {
            throw new RuntimeException("Update Slave Servers Failed! Reason: " + e.getMessage());
        }
    }

    private UpdateItemResponse callServerItemUpdate(boolean isSentByMaster, String IPAddress, int port) {
        ManagedChannel channel;
        UpdateItemServiceGrpc.UpdateItemServiceBlockingStub clientStub;
        UpdateItemRequest request;
        UpdateItemResponse response;

        System.out.println("Call Server " + IPAddress + ":" + port);
        channel = ManagedChannelBuilder.forAddress(IPAddress, port)
                .usePlaintext()
                .build();
        clientStub = UpdateItemServiceGrpc.newBlockingStub(channel);
        request = UpdateItemRequest.newBuilder()
                .setReqID(requestID != null ? requestID : "")
                .setItem(item)
                .setIsMasterReq(isSentByMaster)
                .build();
        response = clientStub.updateItem(request);
        if (!response.getStatus()) { //if request get failed the abort the update
            throw new RuntimeException("Update Item Failed! on server: " + IPAddress + ":" + port + " Due to: " + response.getDescription());
        }
        return response;
    }

    private UpdateItemResponse populateResponse(boolean status, String description, String itemId) {
        return UpdateItemResponse.newBuilder()
                .setId(itemId)
                .setStatus(status)
                .setDescription(description)
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
                server.updateItemInDB(item); // Update item on local DB
            }
            System.out.println("Update item has committed to the local system successfully! ItemNo: " + item.getId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onGlobalAbort() {
        System.out.println("Transaction Aborted by the Coordinator");
    }
}
