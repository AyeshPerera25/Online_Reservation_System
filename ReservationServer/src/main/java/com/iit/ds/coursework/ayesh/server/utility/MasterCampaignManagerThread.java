package com.iit.ds.coursework.ayesh.server.utility;


import com.iit.ds.coursework.ayesh.resources.synchronization.DistributedMasterLock;
import com.iit.ds.coursework.ayesh.server.ReservationServerCore;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread implementation that continuously tries to acquire the master lock and create its server the Master
 */

public class MasterCampaignManagerThread implements Runnable {
    private final DistributedMasterLock distributedMasterLock;
    private final ReservationServerCore server;
    private byte[] currentMasterData = null;

    public MasterCampaignManagerThread(DistributedMasterLock distributedMasterLock, ReservationServerCore server) {
        this.distributedMasterLock = distributedMasterLock;
        this.server = server;
    }

    @Override
    public void run() {
        boolean isMaster;
        byte[] masterNodeData;

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
            server.initiateTheMaster();
            currentMasterData = null;
        } catch (Exception e) {
            System.out.println("Master Campaign Failed! Reason: " + e.getMessage());
        }

    }
}
