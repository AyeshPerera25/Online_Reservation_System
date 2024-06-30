package com.iit.ds.coursework.ayesh.resources.transaction;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.nio.charset.StandardCharsets;

public class DistributedTxParticipant extends DistributedTx implements Watcher {
    private static final String PARTICIPANT_PREFIX = "/txp_";
    private String transactionRoot;

    public DistributedTxParticipant() {
    }

    public void voteCommit() {
        try {
            if (currentTransaction != null) {
                System.out.println("Voting to commit the transaction " + currentTransaction);
                zooKeeperClient.write(currentTransaction, TxnVote.VOTE_COMMIT.getVote().getBytes(StandardCharsets.UTF_8));
            }
            txnListener.setTxnStarted(false); // Mark end the txn
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void voteAbort() {
        try {
            if (currentTransaction != null) {
                System.out.println("Voting to abort the transaction " + currentTransaction);
                zooKeeperClient.write(currentTransaction, TxnVote.VOTE_ABORT.getVote().getBytes(StandardCharsets.UTF_8));
            }
            txnListener.setTxnStarted(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void reset() {
        currentTransaction = null;
        transactionRoot = null;
    }

    @Override
    void onStartTransaction(String transactionId, String participantId) {
        try {
            transactionRoot = "/" + transactionId;
            currentTransaction = transactionRoot + PARTICIPANT_PREFIX + participantId;
            zooKeeperClient.createNode(currentTransaction, true, CreateMode.EPHEMERAL, "".getBytes(StandardCharsets.UTF_8));
            zooKeeperClient.addWatcherToNode(transactionRoot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleRootDataChange() {
        try {
            byte[] data = zooKeeperClient.getNodeData(transactionRoot, true);
            String dataString = new String(data);
            if (TxnVote.GLOBAL_COMMIT.getVote().equals(dataString)) {
                txnListener.onGlobalCommit();
            } else if (TxnVote.GLOBAL_ABORT.getVote().equals(dataString)) {
                txnListener.onGlobalAbort();
            } else {
                System.out.println("Unknown data change in the root : " + dataString);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(WatchedEvent event) {
        Event.EventType type = event.getType();
        if (Event.EventType.NodeDataChanged.equals(type)) {
            if (transactionRoot != null && event.getPath().equals(transactionRoot)) {
                handleRootDataChange();
            }
        }
        if (Event.EventType.NodeDeleted.equals(type)) {
            if (transactionRoot != null && event.getPath().equals(transactionRoot)) {
                reset();
            }
        }
    }
}
