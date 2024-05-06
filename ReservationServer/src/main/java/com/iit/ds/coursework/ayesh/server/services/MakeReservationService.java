package com.iit.ds.coursework.ayesh.server.services;

import com.iit.ds.coursework.ayesh.grpc.server.*;
import com.iit.ds.coursework.ayesh.server.ReservationServerCore;
import io.grpc.stub.StreamObserver;

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
            itemId =request.getItemId();
            custId = request.getCustomerId();
            date = request.getReservationDate();
            System.out.println("Placing reservation..... - Date: "+date+ " CustomerID: "+custId);
            server.makeReservation(itemId,custId,date);
            response = populateResponse(true,"Placing Reservation Has Successes!",date);
            System.out.println("Reservation has placed! - Date: "+date+ " CustomerID: "+custId);
        }catch (Exception e){
            System.out.println("Placing reservation has  Failed! | "+ e.getMessage());
            response = populateResponse(false, e.getMessage(),request.getReservationDate());
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private ReservationResponse populateResponse(boolean status, String description, String resID) {
        return ReservationResponse.newBuilder()
                .setStatus(status)
                .setDescription(description)
                .setResId(resID)
                .build();
    }

}
