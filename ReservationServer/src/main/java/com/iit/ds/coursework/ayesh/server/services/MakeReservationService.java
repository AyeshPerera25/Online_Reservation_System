package com.iit.ds.coursework.ayesh.server.services;

import com.iit.ds.coursework.ayesh.grpc.server.*;
import com.iit.ds.coursework.ayesh.server.ReservationServerCore;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.List;

public class MakeReservationService extends PlaceReservationServiceGrpc.PlaceReservationServiceImplBase {

    private final ReservationServerCore server;

    public MakeReservationService(ReservationServerCore server) {
        this.server = server;
    }

    @Override
    public void placeReservation(ReservationRequest request, StreamObserver<ReservationResponse> responseObserver){
        String itemId;
        String custId;
        String date;
        ReservationResponse response;

        try {
            if (server.isMaster()) { // Master Role to Palce Reservation to the Item on System
                itemId =request.getItemId();
                custId = request.getCustomerId();
                date = request.getReservationDate();

                synchronized (server) {
                    System.out.println("Placing reservation as Master..... - Date: "+date+ " CustomerID: "+custId+" ItemId: "+itemId);
                    updateSlaveServers(itemId,custId,date);
                    server.makeReservation(itemId,custId,date);; // add reservation to Master DB
                }
                response = populateResponse(true,"Placing Reservation Has Successes!",date);
                System.out.println("Reservation has placed! - Date: "+date+ " CustomerID: "+custId);
            } else { // Slave Role to Place Reservation to Item on System
                if(request.getIsMasterReq()){  // Slave Role if the request from Master
                    itemId =request.getItemId();
                    custId = request.getCustomerId();
                    date = request.getReservationDate();

                    synchronized (server) {
                        System.out.println("Updating reservation item to the local DB as Slave on Master command...");
                        server.makeReservation(itemId,custId,date); // update reservation item on local Slave DB
                    }
                    response = populateResponse(true, "Reservation Successfully Added To The Slave System! Item No: " + itemId, date);
                    System.out.println("Placing reservation item to the system has successes!  Item No: " + itemId);
                }else { // Slave Role if the request from Client
                    if(!server.isServerReady()){ // Block getting request from clients until server get synced
                        throw new RuntimeException("Bootstrap server initiation not completed!");
                    }
                    response = updateMasterServer(request);
                }
            }
        }catch (Exception e){
            System.out.println("Placing reservation has  Failed! | "+ e.getMessage());
            response = populateResponse(false, e.getMessage(),request.getReservationDate());
            e.printStackTrace();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private ReservationResponse updateMasterServer(ReservationRequest request) {
        ReservationResponse response;
        String []  currentMasterData;

        try {
            System.out.println("Updating reservation item to the Master...");
            currentMasterData = server.getCurrentMasterData();
            response = callServerReservationItemUpdate(request.getItemId(), request.getCustomerId(),request.getReservationDate(), false, currentMasterData[0], Integer.parseInt(currentMasterData[1]));
        }catch (Exception e){
            throw new RuntimeException("Send Reservation Item Update to Master has failed! Due to:"+e.getMessage());
        }
        return response;
    }

    private void updateSlaveServers(String itemId, String custId, String date) {
        List<String[]> slaveServerData;
        try{
            System.out.println("Updating reservation item to the Slave Servers...");
            slaveServerData = server.getSlaveServerData();
            if(slaveServerData.isEmpty()){
                System.out.println("Slave Server Update Skipped! Unable to find data on Slave Servers.");
                return;
            }
            slaveServerData.forEach(data -> callServerReservationItemUpdate(itemId, custId, date, true, data[0], Integer.parseInt(data[1])));
            System.out.println("Slave servers successfully updated on reservation item! ItemNo: "+itemId);
        }catch (Exception e){
            throw new RuntimeException("Update Slave Servers Failed! Reason: "+e.getMessage());
        }
    }

    private ReservationResponse callServerReservationItemUpdate(String itemId, String custId, String date, boolean isSentByMaster, String IPAddress, int port) {
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
                .setItemId(itemId)
                .setCustomerId(custId)
                .setReservationDate(date)
                .setIsMasterReq(isSentByMaster)
                .build();
        response = clientStub.placeReservation(request);
        if(!response.getStatus()){ //if request get failed the abort the update
            throw new RuntimeException("Update Reservation Item Failed! on server: "+IPAddress+":"+port+" Due to: "+response.getDescription());
        }
        return response;
    }

    private ReservationResponse populateResponse(boolean status, String description, String resID) {
        return ReservationResponse.newBuilder()
                .setStatus(status)
                .setDescription(description != null? description : "")
                .setResId(resID)
                .build();
    }

}
