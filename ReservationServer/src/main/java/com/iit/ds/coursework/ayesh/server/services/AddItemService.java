package com.iit.ds.coursework.ayesh.server.services;


import com.iit.ds.coursework.ayesh.grpc.server.Item;
import com.iit.ds.coursework.ayesh.server.ReservationServerCore;
import com.iit.ds.coursework.ayesh.grpc.server.AddItemRequest;
import com.iit.ds.coursework.ayesh.grpc.server.AddItemResponse;
import com.iit.ds.coursework.ayesh.grpc.server.AddItemServiceGrpc;
import io.grpc.stub.StreamObserver;

import java.util.Date;

public class AddItemService extends AddItemServiceGrpc.AddItemServiceImplBase {

    private final ReservationServerCore server;


    public AddItemService(ReservationServerCore server) {
        this.server = server;
    }

    @Override
    public void addItem(AddItemRequest request, StreamObserver<AddItemResponse> responseObserver) {
        AddItemResponse response;
        Item item;
        try {
            System.out.println("Adding item to the system... ");
            item = populateItem(request.getItem()); // Generate item with unique ID
            server.addItemToDB(item); // add item to DB
            response = populateResponse(true, "Item Successfully Added To The System! Item No: " + item.getId() + " | Item Name: " + item.getName(),item.getId());
            System.out.println("Adding item to the system has successes!  Item No: " + item.getId() + " | Item Name: " + item.getName());
        } catch (Exception e) {
            System.out.println(e.getMessage());
            response = populateResponse(false, e.getMessage(),"");
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
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
