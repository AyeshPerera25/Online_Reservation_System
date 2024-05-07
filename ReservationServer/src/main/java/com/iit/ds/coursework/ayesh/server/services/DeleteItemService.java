package com.iit.ds.coursework.ayesh.server.services;

import com.iit.ds.coursework.ayesh.grpc.server.DeleteItemRequest;
import com.iit.ds.coursework.ayesh.grpc.server.DeleteItemResponse;
import com.iit.ds.coursework.ayesh.grpc.server.UpdateItemResponse;
import com.iit.ds.coursework.ayesh.server.ReservationServerCore;
import com.iit.ds.coursework.ayesh.grpc.server.DeleteItemServiceGrpc;
import io.grpc.stub.StreamObserver;

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
            itemID = request.getId();
            System.out.println("Deleting listed item on the system...  Item ID: "+ itemID);
            server.deleteItemInDB(itemID);
            response = populateResponse(true, "Delete Item Has Successes!",itemID);
            System.out.println(" Item has deleted on the system! Item ID: "+ itemID);
        }catch (Exception e){
            System.out.println("Delete items has Failed! | "+ e.getMessage());
            response = populateResponse(false, e.getMessage(),request.getId());
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private DeleteItemResponse populateResponse(boolean status, String description, String itemId) {
        return DeleteItemResponse.newBuilder()
                .setId(itemId)
                .setStatus(status)
                .setDescription(description)
                .build();
    }
}
