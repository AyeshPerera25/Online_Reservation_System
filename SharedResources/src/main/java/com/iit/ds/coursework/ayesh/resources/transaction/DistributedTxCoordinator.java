package com.iit.ds.coursework.ayesh.resources.transaction;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class DistributedTxCoordinator extends DistributedTx {

    public DistributedTxCoordinator() {
    }

    @Override
    void onStartTransaction(String transactionId, String participantId) {
        try {
            currentTransaction = "/" + transactionId;
            zooKeeperClient.createNode(currentTransaction, true, CreateMode.PERSISTENT, "".getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean perform() throws KeeperException, InterruptedException {
        List<String> childrenNodePaths = zooKeeperClient.getChildrenNodePaths(currentTransaction);
        boolean result = true;
        byte[] data;
        System.out.println("Child count :" + childrenNodePaths.size());
        for (String path : childrenNodePaths) {
            path = currentTransaction + "/" + path;
            System.out.println("Checking path :" + path);
            data = zooKeeperClient.getNodeData(path, false);
            String dataString = new String(data);
            if (!TxnVote.VOTE_COMMIT.getVote().equals(dataString)) {
                System.out.println("Child " + path + " caused the transaction to abort. Sending GLOBAL_ABORT");
                sendGlobalAbort();
                result = false;
                return result;
            }
        }
        System.out.println("All nodes are okay to commit the transaction. Sending GLOBAL_COMMIT");
        sendGlobalCommit();
        return result;
    }

    public void sendGlobalCommit() {
        try {
            if (currentTransaction != null) {
                System.out.println("Sending global commit for " + currentTransaction);
                zooKeeperClient.write(currentTransaction, TxnVote.GLOBAL_COMMIT.getVote().getBytes(StandardCharsets.UTF_8));
                txnListener.onGlobalCommit();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            txnListener.setTxnStarted(false);
            reset();
        }
    }

    public void sendGlobalAbort() {
        try {
            if (currentTransaction != null) {
                System.out.println("Sending global abort for " + currentTransaction);
                zooKeeperClient.write(currentTransaction, TxnVote.GLOBAL_ABORT.getVote().getBytes(StandardCharsets.UTF_8));
                txnListener.onGlobalAbort();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            txnListener.setTxnStarted(false);
            reset();
        }
    }

    private void reset() {
        try {
            zooKeeperClient.forceDelete(currentTransaction);
            currentTransaction = null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
