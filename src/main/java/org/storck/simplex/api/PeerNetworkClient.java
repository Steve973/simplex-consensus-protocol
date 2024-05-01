package org.storck.simplex.api;

import org.storck.simplex.model.SignedProposal;
import org.storck.simplex.model.SignedVote;

import java.util.Collection;

public interface PeerNetworkClient {

    void broadcastVote(SignedVote vote);

    <T> void broadcastProposal(SignedProposal<T> proposal);

    <T> Collection<T> getPendingTransactions();
}
