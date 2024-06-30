package com.iit.ds.coursework.ayesh.resources.transaction;

import com.iit.ds.coursework.ayesh.resources.synchronization.ZooKeeperClient;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.io.IOException;

public abstract class DistributedTx implements Watcher {

    protected static String zooKeeperUrl;
    protected String currentTransaction;
    protected ZooKeeperClient zooKeeperClient;
    protected DistributedTxListener txnListener;

    public DistributedTx() {
    }

    public static void setZooKeeperURL(String url) {
        zooKeeperUrl = url;
    }

    public void start(String transactionId, String participantId) throws IOException {
        zooKeeperClient = new ZooKeeperClient(zooKeeperUrl, 5000, this);
        onStartTransaction(transactionId, participantId);
    }

    public String getCurrentTransaction() {
        return currentTransaction;
    }

    public void setCurrentTransaction(String currentTransaction) {
        this.currentTransaction = currentTransaction;
    }

    public ZooKeeperClient getZooKeeperClient() {
        return zooKeeperClient;
    }

    public void setZooKeeperClient(ZooKeeperClient zooKeeperClient) {
        this.zooKeeperClient = zooKeeperClient;
    }

    public DistributedTxListener getTxnListener() {
        return txnListener;
    }

    public void setTxnListener(DistributedTxListener txnListener) {
        this.txnListener = txnListener;
    }

    abstract void onStartTransaction(String transactionId, String participantId);

    @Override
    public void process(WatchedEvent watchedEvent) {
    }
}
