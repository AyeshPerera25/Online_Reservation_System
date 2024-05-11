package com.iit.ds.coursework.ayesh.resources.synchronization;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class ZooKeeperClient {
    private final ZooKeeper zooKeeper;

    public ZooKeeperClient(String zooKeeperUrl, int sessionTimeout, Watcher watcher) throws IOException {
        zooKeeper = new ZooKeeper(zooKeeperUrl, sessionTimeout, watcher);
    }

    public String createNode(String path, boolean shouldWatch, CreateMode mode , byte[] nodeData) throws KeeperException, InterruptedException, UnsupportedEncodingException {
        return zooKeeper.create(path, nodeData, ZooDefs.Ids.OPEN_ACL_UNSAFE, mode);
    }

    public boolean checkNodeExists(String path) throws KeeperException, InterruptedException {
        Stat nodeStat = zooKeeper.exists(path, false);
        return (nodeStat != null);
    }

    public void deleteNode(String path) throws KeeperException, InterruptedException {
        zooKeeper.delete(path, -1);
    }

    public List<String> getChildrenNodePaths (String root) throws KeeperException, InterruptedException {
        return zooKeeper.getChildren(root, false);
    }

    public void addWatcherToNode(String path) throws KeeperException, InterruptedException {
        zooKeeper.exists(path, true);
    }

    public byte[] getNodeData(String path, boolean shouldWatch) throws KeeperException, InterruptedException { // to fetch the stored data
        return zooKeeper.getData(path, shouldWatch, null);
    }




}
