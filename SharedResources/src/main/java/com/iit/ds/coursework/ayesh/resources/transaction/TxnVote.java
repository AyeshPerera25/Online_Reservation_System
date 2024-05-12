package com.iit.ds.coursework.ayesh.resources.transaction;

public enum TxnVote {

    VOTE_COMMIT("vote_commit"),
    VOTE_ABORT("vote_abort"),
    GLOBAL_COMMIT("global_commit"),
    GLOBAL_ABORT("global_abort");

    private final String vote;

    private TxnVote(String vote) {
        this.vote = vote;
    }

    public String getVote() {
        return vote;
    }
}
