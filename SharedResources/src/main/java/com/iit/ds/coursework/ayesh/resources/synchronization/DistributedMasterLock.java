package com.iit.ds.coursework.ayesh.resources.synchronization;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class DistributedMasterLock implements Watcher {

    private static final String lockProcessPath = "/lp_";
    public static String zooKeeperUrl;
    CountDownLatch zooKeeperConnectionWaitLatch = new CountDownLatch(1);
    CountDownLatch zooKeeperEventListenerLatch;
    private final ZooKeeperClient zooKeeperClient;
    private  String rootPath;
    private String serverNodePath;
    private byte[] serverData; // saving server details
    private boolean isMasterLockAcquired = false;

    private String watchedNode;

    public DistributedMasterLock(String lockName, String data) throws IOException, KeeperException, InterruptedException {
        this.rootPath = "/" + lockName;
        this.serverData = data.getBytes(StandardCharsets.UTF_8);
        zooKeeperClient = new ZooKeeperClient(zooKeeperUrl, 5000, this);
        zooKeeperConnectionWaitLatch.await();
        if (!zooKeeperClient.checkNodeExists(rootPath)) {
            createRootNode();
        }
        createChildNode();
    }

    public static void setZooKeeperUrl(String zooKeeperUrl) {
        DistributedMasterLock.zooKeeperUrl = zooKeeperUrl;
    }

    private void createRootNode() throws InterruptedException, UnsupportedEncodingException, KeeperException {
        rootPath = zooKeeperClient.createNode(rootPath, false, CreateMode.PERSISTENT, serverData);
        System.out.println("Root node created at " + rootPath);
    }

    private void createChildNode() throws InterruptedException, UnsupportedEncodingException, KeeperException {
        serverNodePath = zooKeeperClient.createNode(rootPath + lockProcessPath, false, CreateMode.EPHEMERAL_SEQUENTIAL, serverData);
        System.out.println("Server child node created at " + serverNodePath);
    }

    public void acquireMasterLock() throws KeeperException, InterruptedException {
        String masterNodePath = findMasterNodePath();
        if (masterNodePath.equals(serverNodePath)) {
            isMasterLockAcquired = true;
        } else {
            do {
                System.out.println("Master Lock is currently acquired by node " + masterNodePath + " .. hence waiting..");
                zooKeeperEventListenerLatch = new CountDownLatch(1);
                watchedNode = masterNodePath;
                zooKeeperClient.addWatcherToNode(masterNodePath);
                zooKeeperEventListenerLatch.await();
                masterNodePath = findMasterNodePath(); //Find new master (Next Smallest node path)
            } while (!masterNodePath.equals(serverNodePath));
            isMasterLockAcquired = true; // Become the new Master
        }
    }

    public boolean tryAcquireMasterLock() throws KeeperException, InterruptedException, UnsupportedEncodingException {
        String masterNodePath = findMasterNodePath();
        if (masterNodePath.equals(serverNodePath)) {
            isMasterLockAcquired = true;
        }
        return isMasterLockAcquired;
    }

    public void releaseMasterLock() throws KeeperException, InterruptedException {
        if (!isMasterLockAcquired) {
            throw new IllegalStateException("Lock needs to be acquired first to release");
        } zooKeeperClient.deleteNode(serverNodePath);
        isMasterLockAcquired = false;
    }

    public byte[] getMasterData() throws KeeperException, InterruptedException { // Get the data of the current master lock holder
        return  zooKeeperClient.getNodeData(findMasterNodePath(), true);
    }
    public List<byte[]> getSlaveData() throws KeeperException, InterruptedException { // Get the data of other non lock holders.
        List<byte[]> result = new ArrayList<>();
        List<String> childrenNodePaths = zooKeeperClient.getChildrenNodePaths(rootPath);
        for (String path : childrenNodePaths) {
            path = rootPath + "/" + path;
            if (!path.equals(serverNodePath)) {
                byte[] data = zooKeeperClient.getNodeData(path, false);
                result.add(data);
            }
        }
        return  result;
    }

    private String findMasterNodePath() throws KeeperException, InterruptedException { //Find Smallest node path
        List<String> childrenNodePaths = null;
        childrenNodePaths = zooKeeperClient.getChildrenNodePaths(rootPath);
        Collections.sort(childrenNodePaths);
        String smallestPath = childrenNodePaths.get(0);
        smallestPath = rootPath + "/" + smallestPath;
        return smallestPath;
    }

    public byte[] getServerData() {
        return serverData;
    }

    @Override
    public void process(WatchedEvent event) {
        Event.KeeperState state = event.getState();
        Event.EventType type = event.getType();
        if (Event.KeeperState.SyncConnected == state) {
            if (Event.EventType.None == type) {
                // Identify successful connection
                System.out.println("Successful connected to the server");
                zooKeeperConnectionWaitLatch.countDown();
            }
        }
        if (Event.EventType.NodeDeleted.equals(type)) {
            if (watchedNode != null && zooKeeperEventListenerLatch != null && event.getPath().equals(watchedNode)) {
                System.out.println("Master Node Delete Event Received! Trying to get the master lock.."); zooKeeperEventListenerLatch.countDown();
            }
        }
    }
}

