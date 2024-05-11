package com.iit.ds.coursework.ayesh.server.utility;


import com.iit.ds.coursework.ayesh.resources.synchronization.DistributedMasterLock;
import com.iit.ds.coursework.ayesh.server.ReservationServerCore;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread implementation that continuously tries to acquire the master lock and create its server the Master
 * */

public class MasterCampaignManagerThread implements Runnable{
    private byte[] currentMasterData = null;
    private final DistributedMasterLock distributedMasterLock;
    private final ReservationServerCore server;

    public MasterCampaignManagerThread(DistributedMasterLock distributedMasterLock, ReservationServerCore server) {
        this.distributedMasterLock = distributedMasterLock;
        this.server = server;
    }

    @Override
    public void run() {
        boolean isMaster;
        byte[] masterNodeData;
        Logger.getLogger("org.apache.zookeeper").setLevel(Level.SEVERE);
        Logger.getLogger("io.grpc").setLevel(Level.SEVERE);
        Logger.getLogger("io.netty").setLevel(Level.SEVERE);
        Logger.getLogger("org.apache.zookeeper.ClientCnxn").setLevel(Level.SEVERE);

        System.out.println("Start competing the Master Campaign.. ");
        try {
            isMaster = distributedMasterLock.tryAcquireMasterLock();
            while (!isMaster) {
                masterNodeData = distributedMasterLock.getMasterData();
                if (currentMasterData != masterNodeData) {
                    currentMasterData = masterNodeData;
                    server.setCurrentMasterNodeData(currentMasterData);
                }
                Thread.sleep(10000);
                isMaster = distributedMasterLock.tryAcquireMasterLock();
            }
            System.out.println("I got the Master lock. Now acting as Master");
            server.setIsMaster(true);
            currentMasterData = null;
        } catch (Exception e){
            System.out.println("Master Campaign Failed! Reason: "+e.getMessage());
        }

    }
}
