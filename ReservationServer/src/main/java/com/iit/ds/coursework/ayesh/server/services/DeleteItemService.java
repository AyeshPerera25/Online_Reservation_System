package com.iit.ds.coursework.ayesh.server.services;

import com.iit.ds.coursework.ayesh.grpc.server.*;
import com.iit.ds.coursework.ayesh.resources.transaction.DistributedTxListener;
import com.iit.ds.coursework.ayesh.server.ReservationServerCore;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.List;

public class DeleteItemService extends DeleteItemServiceGrpc.DeleteItemServiceImplBase implements DistributedTxListener {
    private final ReservationServerCore server;
    private String itemID = null;
    private boolean isTxnStarted;
    private String description;
    private String requestID = null;

    public DeleteItemService(ReservationServerCore server) {
        this.server = server;
    }

    @Override
    public void removeItem(DeleteItemRequest request, StreamObserver<DeleteItemResponse> responseObserver) {
        DeleteItemResponse response;

        if (server.isMaster()) { // Master Role to Delete Item to System
            try {
                synchronized (server) {
                    System.out.println("Delete item from the system as Master... ");
                    itemID = request.getItemID();
                    requestID = "DELETE" + String.valueOf(System.currentTimeMillis());
                    preValidate();
                    server.startDistributedTxn(requestID, this); // initiate the distributed transaction on the system
                    updateSlaveServers();
                    server.performTxnCommit();
                }
                response = populateResponse(true, "Delete Item Has Successes!", itemID);
                System.out.println(" Item has deleted on the system! Item ID: " + itemID);
            } catch (Exception e) {
                System.out.println("Delete item to the system has Failed!  Item No: " + itemID);
                if (isTxnStarted) {
                    System.out.println("Initiating Global Abort..");
                    server.performTxnCommitAbort();
                }
                System.out.println("Delete items has Failed! | " + e.getMessage());
                response = populateResponse(false, e.getMessage(), request.getItemID());
            }
        } else { // Slave Role to Delete Item to System
            try {
                if (request.getIsMasterReq()) {  // Slave Role if the request from Master
                    itemID = request.getItemID();
                    requestID = request.getReqID();
                    synchronized (server) {
                        System.out.println("Deleting item from the local DB as Slave on Master command...");
                        preValidate();
                        server.startDistributedTxn(requestID, this);
                        server.voteCommit();
                    }
                    response = populateResponse(true, "Item Successfully Deleted from The Slave System! Item ID: " + itemID, itemID);
                    System.out.println("Delete item from the system has successes!  Item ID: " + itemID);
                } else { // Slave Role if the request from Client
                    if (!server.isServerReady()) { // Block getting request from clients until server get synced
                        throw new RuntimeException("Bootstrap server initiation not completed!");
                    }
                    itemID = request.getItemID();
                    response = updateMasterServer();
                }
            } catch (Exception e) {
                System.out.println("Adding item to the system has failed!  Item No: " + itemID);
                if (isTxnStarted) {
                    System.out.println("Initiating vote Abort..");
                    server.voteAbort();
                }
                System.out.println("Delete items has Failed! | " + e.getMessage());
                response = populateResponse(false, e.getMessage(), request.getItemID());
            }
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void preValidate() {
        if (!server.isItemAlreadyExist(itemID)) {
            throw new RuntimeException("Item has not exist to remove on the system! - ItemID : " + itemID);
        }
    }

    private DeleteItemResponse updateMasterServer() {
        DeleteItemResponse response;
        String[] currentMasterData;

        try {
            System.out.println("Updating delete item to the Master...");
            currentMasterData = server.getCurrentMasterData();
            response = callServerDeleteItemUpdate(false, currentMasterData[0], Integer.parseInt(currentMasterData[1]));
        } catch (Exception e) {
            throw new RuntimeException("Send Delete Item Update to Master has failed! Due to:" + e.getMessage());
        }
        return response;
    }

    private void updateSlaveServers() {
        List<String[]> slaveServerData;
        try {
            System.out.println("Updating delete item to the Slave Servers...");
            slaveServerData = server.getSlaveServerData();
            if (slaveServerData.isEmpty()) {
                System.out.println("Slave Server Update Skipped! Unable to find data on Slave Servers.");
                return;
            }
            slaveServerData.forEach(data -> callServerDeleteItemUpdate(true, data[0], Integer.parseInt(data[1])));
            System.out.println("Slave servers successfully updated on delete item! ItemID: " + itemID);
        } catch (Exception e) {
            throw new RuntimeException("Update Slave Servers Failed! Reason: " + e.getMessage());
        }
    }

    private DeleteItemResponse callServerDeleteItemUpdate(boolean isSentByMaster, String IPAddress, int port) {
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
                .setReqID(requestID != null ? requestID : "")
                .setItemID(itemID)
                .setIsMasterReq(isSentByMaster)
                .build();
        response = clientStub.removeItem(request);
        if (!response.getStatus()) { //if request get failed the abort the update
            throw new RuntimeException("Update Delete Item Failed! on server: " + IPAddress + ":" + port + " Due to: " + response.getDescription());
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

    @Override
    public void setTxnStarted(boolean txnStarted) {
        this.isTxnStarted = txnStarted;
    }

    @Override
    public void onGlobalCommit() {
        try {
            if (itemID != null) {
                server.deleteItemInDB(itemID); // Delete item from local DB
            }
            System.out.println("Delete item has committed to the local system successfully! ItemNo: " + itemID);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onGlobalAbort() {
        System.out.println("Transaction Aborted by the Coordinator");
    }
}
