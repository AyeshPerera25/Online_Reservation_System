package com.iit.ds.coursework.ayesh.resources.transaction;

public interface DistributedTxListener {

    void setTxnStarted(boolean txnStarted);

    void onGlobalCommit();

    void onGlobalAbort();
}
