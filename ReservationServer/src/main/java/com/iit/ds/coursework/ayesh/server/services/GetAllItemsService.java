package com.iit.ds.coursework.ayesh.server.services;

import com.iit.ds.coursework.ayesh.grpc.server.*;
import com.iit.ds.coursework.ayesh.server.ReservationServerCore;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;

public class GetAllItemsService extends GetAllItemsServiceGrpc.GetAllItemsServiceImplBase {

    private final ReservationServerCore server;

    public GetAllItemsService(ReservationServerCore server) {
        this.server = server;
    }

    @Override
    public void getAllItems(GetAllItemRequest request, StreamObserver<GetAllItemResponse> responseObserver) {
        String custId;
        String serverAddress;
        GetAllItemResponse response;
        try {
            if (request.getIsServer()) {
                serverAddress = request.getId();
                synchronized (server) {
                    if (server.isMaster()) { // Role if current server is the Master
                        System.out.println("Start System Synchronization with server: " + serverAddress);
                        List<Item> itemList = server.getAllItemsFromDB();
                        response = populateResponse(true, "Sending All Items Has Successes!", itemList);
                        System.out.println("Sync with Slave Server Has Successes! Slave Server: " + serverAddress);
                    } else { // Role if current server resign from Master
                        String[] currentMasterData;

                        System.out.println("Get All item request sending to the Master...");
                        currentMasterData = server.getCurrentMasterData();
                        response = callServerGetAllItem(request, currentMasterData[0], Integer.parseInt(currentMasterData[1]));
                        System.out.println("Sending all items! To server: " + serverAddress);
                    }
                }
            } else {
                if (!server.isServerReady()) { // Block getting request from clients until server get synced
                    throw new RuntimeException("Bootstrap server initiation not completed!");
                }
                custId = request.getId();
                System.out.println("Request received from customer Id: " + custId);
                List<Item> itemList = server.getAllItemsFromDB();
                response = populateResponse(true, "Sending All Items Has Successes!", itemList);
                System.out.println("Sending all items! Customer Id: " + custId);
            }
        } catch (Exception e) {
            System.out.println("Sending all items has Failed! | " + e.getMessage());
            response = populateResponse(false, e.getMessage(), new ArrayList<>());
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private GetAllItemResponse callServerGetAllItem(GetAllItemRequest request, String IPAddress, int port) {
        ManagedChannel channel;
        GetAllItemsServiceGrpc.GetAllItemsServiceBlockingStub clientStub;
        GetAllItemResponse response;

        System.out.println("Call Server " + IPAddress + ":" + port);
        channel = ManagedChannelBuilder.forAddress(IPAddress, port)
                .usePlaintext()
                .build();
        clientStub = GetAllItemsServiceGrpc.newBlockingStub(channel);
        response = clientStub.getAllItems(request);
        if (!response.getStatus()) { //if request get failed the abort the update
            throw new RuntimeException("Get All Item Failed! on server: " + IPAddress + ":" + port + " Due to: " + response.getDescription());
        }
        return response;
    }

    private GetAllItemResponse populateResponse(boolean status, String description, List<Item> items) {
        return GetAllItemResponse.newBuilder()
                .setStatus(status)
                .setDescription(description)
                .addAllItems(items)
                .build();
    }
}
