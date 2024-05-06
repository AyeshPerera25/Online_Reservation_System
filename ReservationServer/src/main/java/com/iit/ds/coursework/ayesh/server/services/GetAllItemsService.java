package com.iit.ds.coursework.ayesh.server.services;

import com.iit.ds.coursework.ayesh.grpc.server.*;
import com.iit.ds.coursework.ayesh.server.ReservationServerCore;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;

public class GetAllItemsService extends GetAllItemsServiceGrpc.GetAllItemsServiceImplBase {

    private final ReservationServerCore server;

    public GetAllItemsService(ReservationServerCore server) {
        this.server = server;
    }

    @Override
    public void getAllItems(GetAllItemRequest request , StreamObserver<GetAllItemResponse> responseObserver){
        String custId ;
        GetAllItemResponse response;
        try{
            custId = request.getCustId();
            System.out.println("Request received from customer Id: "+ custId);
            List<Item> itemList = server.getAllItemsFromDB();
            response = populateResponse(true,"Sending All Items Has Successes!",itemList);
            System.out.println("Sending all items! Customer Id: "+custId);
        }catch (Exception e){
            System.out.println("Sending all items has Failed! | "+ e.getMessage());
            response = populateResponse(false, e.getMessage(),new ArrayList<>());
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private GetAllItemResponse populateResponse(boolean status, String description, List<Item> items) {
        return GetAllItemResponse.newBuilder()
                .setStatus(status)
                .setDescription(description)
                .addAllItems(items)
                .build();
    }
}
