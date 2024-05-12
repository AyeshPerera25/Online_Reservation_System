package com.iit.ds.coursework.ayesh.server.services;

import com.iit.ds.coursework.ayesh.grpc.server.*;
import com.iit.ds.coursework.ayesh.resources.transaction.DistributedTxListener;
import com.iit.ds.coursework.ayesh.server.ReservationServerCore;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.List;

public class MakeReservationService extends PlaceReservationServiceGrpc.PlaceReservationServiceImplBase implements DistributedTxListener {
    private final ReservationServerCore server;
    private String itemId = null;
    private String custId = null;
    private String date = null;
    private boolean isTxnStarted;
    private String requestID = null;

    public MakeReservationService(ReservationServerCore server) {
        this.server = server;
    }

    @Override
    public void placeReservation(ReservationRequest request, StreamObserver<ReservationResponse> responseObserver) {
        ReservationResponse response;

        if (server.isMaster()) { // Master Role to Palace Reservation to the Item on System
            itemId = request.getItemId();
            custId = request.getCustomerId();
            date = request.getReservationDate();
            requestID = "RESRV" + String.valueOf(System.currentTimeMillis());
            try {
                synchronized (server) {
                    System.out.println("Placing reservation as Master..... - Date: " + date + " CustomerID: " + custId + " ItemId: " + itemId);
                    preValidate();
                    server.startDistributedTxn(requestID, this); // initiate the distributed transaction on the system
                    updateSlaveServers();
                    server.performTxnCommit();
                }
                response = populateResponse(true, "Placing Reservation Has Successes!", date);
                System.out.println("Reservation has placed! - Date: " + date + " CustomerID: " + custId);
            } catch (Exception e) {
                System.out.println("Placing reservation has  Failed! | " + e.getMessage());
                if (isTxnStarted) {
                    System.out.println("Initiating Global Abort..");
                    server.performTxnCommitAbort();
                }
                response = populateResponse(false, e.getMessage(), request.getReservationDate());
                e.printStackTrace();
            }
        } else { // Slave Role to Place Reservation to Item on System
            itemId = request.getItemId();
            custId = request.getCustomerId();
            date = request.getReservationDate();
            try {
                if (request.getIsMasterReq()) {  // Slave Role if the request from Master
                    requestID = request.getReqID();
                    synchronized (server) {
                        System.out.println("Updating reservation item to the local DB as Slave on Master command...");
                        preValidate();
                        server.startDistributedTxn(requestID, this);
                        server.voteCommit();
                    }
                    response = populateResponse(true, "Reservation Successfully Added To The Slave System! Item No: " + itemId, date);
                    System.out.println("Placing reservation item to the system has successes!  Item No: " + itemId);
                } else { // Slave Role if the request from Client
                    if (!server.isServerReady()) { // Block getting request from clients until server get synced
                        throw new RuntimeException("Bootstrap server initiation not completed!");
                    }
                    response = updateMasterServer();
                }
            } catch (Exception e) {
                System.out.println("Placing reservation has  Failed! | " + e.getMessage());
                if (isTxnStarted) {
                    System.out.println("Initiating Global Abort..");
                    server.voteAbort();
                }
                response = populateResponse(false, e.getMessage(), request.getReservationDate());
                e.printStackTrace();
            }
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void preValidate() {
        Item item;
        String resId;
        if (!server.isItemAlreadyExist(itemId)) {
            throw new RuntimeException("Item has not listed on the system! - ItemID : " + itemId);
        }
        item = server.getItem(itemId);
        resId = String.join("", (date.trim().split("/"))); // Generate Key for reservation map
        if (item.getReservationsMap().containsKey(resId)) {
            throw new RuntimeException("Already has reservation! - on date : " + date);
        }
        if (item.getAvailableQty() == item.getReservationsCount()) {
            throw new RuntimeException("Maximum reservation has reached! - on ItemID: " + item.getId());
        }
    }

    private ReservationResponse updateMasterServer() {
        ReservationResponse response;
        String[] currentMasterData;

        try {
            System.out.println("Updating reservation item to the Master...");
            currentMasterData = server.getCurrentMasterData();
            response = callServerReservationItemUpdate(false, currentMasterData[0], Integer.parseInt(currentMasterData[1]));
        } catch (Exception e) {
            throw new RuntimeException("Send Reservation Item Update to Master has failed! Due to:" + e.getMessage());
        }
        return response;
    }

    private void updateSlaveServers() {
        List<String[]> slaveServerData;
        try {
            System.out.println("Updating reservation item to the Slave Servers...");
            slaveServerData = server.getSlaveServerData();
            if (slaveServerData.isEmpty()) {
                System.out.println("Slave Server Update Skipped! Unable to find data on Slave Servers.");
                return;
            }
            slaveServerData.forEach(data -> callServerReservationItemUpdate(true, data[0], Integer.parseInt(data[1])));
            System.out.println("Slave servers successfully updated on reservation item! ItemNo: " + itemId);
        } catch (Exception e) {
            throw new RuntimeException("Update Slave Servers Failed! Reason: " + e.getMessage());
        }
    }

    private ReservationResponse callServerReservationItemUpdate(boolean isSentByMaster, String IPAddress, int port) {
        ManagedChannel channel;
        PlaceReservationServiceGrpc.PlaceReservationServiceBlockingStub clientStub;
        ReservationRequest request;
        ReservationResponse response;

        System.out.println("Call Server " + IPAddress + ":" + port);
        channel = ManagedChannelBuilder.forAddress(IPAddress, port)
                .usePlaintext()
                .build();
        clientStub = PlaceReservationServiceGrpc.newBlockingStub(channel);
        request = ReservationRequest.newBuilder()
                .setReqID(requestID != null ? requestID : "")
                .setItemId(itemId)
                .setCustomerId(custId)
                .setReservationDate(date)
                .setIsMasterReq(isSentByMaster)
                .build();
        response = clientStub.placeReservation(request);
        if (!response.getStatus()) { //if request get failed the abort the update
            throw new RuntimeException("Update Reservation Item Failed! on server: " + IPAddress + ":" + port + " Due to: " + response.getDescription());
        }
        return response;
    }

    private ReservationResponse populateResponse(boolean status, String description, String resID) {
        return ReservationResponse.newBuilder()
                .setStatus(status)
                .setDescription(description != null ? description : "")
                .setResId(resID)
                .build();
    }

    @Override
    public void setTxnStarted(boolean txnStarted) {
        this.isTxnStarted = txnStarted;
    }

    @Override
    public void onGlobalCommit() {
        try {
            if (itemId != null && custId != null && date != null) {
                server.makeReservation(itemId, custId, date); // add reservation to local DB
            }
            System.out.println("Place Reservation item has committed to the local system successfully! ItemNo: " + itemId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onGlobalAbort() {
        System.out.println("Transaction Aborted by the Coordinator");
    }
}
