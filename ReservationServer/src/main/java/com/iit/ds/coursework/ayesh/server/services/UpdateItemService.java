package com.iit.ds.coursework.ayesh.server.services;

import com.iit.ds.coursework.ayesh.grpc.server.*;
import com.iit.ds.coursework.ayesh.server.ReservationServerCore;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;

public class UpdateItemService extends UpdateItemServiceGrpc.UpdateItemServiceImplBase {

    private final ReservationServerCore server;

    public UpdateItemService(ReservationServerCore server) {
        this.server = server;
    }

    @Override
    public void updateItem(UpdateItemRequest request, StreamObserver<UpdateItemResponse> responseObserver){
        Item item;
        UpdateItemResponse response;
        try{
            item = request.getItem();
            System.out.println("Updating listed item on the system...  Item ID: "+ item.getId());
            server.updateItemInDB(item);
            response = populateResponse(true, "Update Item Has Successes!",request.getItem().getId());
            System.out.println(" Item has updated on the system! Item ID: "+item.getId());
        }catch (Exception e){
            System.out.println("Update items has Failed! | "+ e.getMessage());
            response = populateResponse(false, e.getMessage(),request.getItem().getId());
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private UpdateItemResponse populateResponse(boolean status, String description, String itemId) {
        return UpdateItemResponse.newBuilder()
                .setId(itemId)
                .setStatus(status)
                .setDescription(description)
                .build();
    }
}
