package com.iit.ds.coursework.ayesh.server.services;

import com.iit.ds.coursework.ayesh.grpc.server.*;
import com.iit.ds.coursework.ayesh.server.ReservationServerCore;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;

public class GetMyItemsService extends GetMyItemServiceGrpc.GetMyItemServiceImplBase {

    private final ReservationServerCore server;

    public GetMyItemsService(ReservationServerCore server) {
        this.server = server;
    }

    @Override
    public void getMyItems (GetMyItemsRequest request , StreamObserver<GetMyItemsResponse> responseObserver){
        String sellerId ;
        GetMyItemsResponse response;
        try{
            sellerId = request.getSellerId();
            System.out.println("Request received from sellers Id: "+sellerId);
            List<Item> itemList = server.getMyItemsFromDB(sellerId);
            response = populateResponse(true,"Sending Sellers Items Has Successes!",itemList);
            System.out.println("Sending sellers all items! Sellers Id: "+sellerId);
        }catch (Exception e){
            System.out.println("Sending sellers all items has Failed! | "+ e.getMessage());
            response = populateResponse(false, e.getMessage(),new ArrayList<>());
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private GetMyItemsResponse populateResponse(boolean status, String description, List<Item> items) {
        return GetMyItemsResponse.newBuilder()
                .setStatus(status)
                .setDescription(description)
                .addAllItems(items)
                .build();
    }
}
