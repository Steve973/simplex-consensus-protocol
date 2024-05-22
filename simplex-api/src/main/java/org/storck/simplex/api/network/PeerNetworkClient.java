package org.storck.simplex.api.network;

import org.storck.simplex.api.protocol.ConsensusProtocolService;
import org.storck.simplex.api.protocol.FinalizeProtocolMessage;
import org.storck.simplex.api.protocol.ProposalProtocolMessage;
import org.storck.simplex.api.protocol.VoteProtocolMessage;

/**
 * Specifies the methods that a {@link PeerNetworkClient} implementation must
 * include in order for the {@link ConsensusProtocolService} implementation to
 * communicate on the network to other players/peers.
 */
public interface PeerNetworkClient {

    /**
     * Broadcasts a vote to all peers in the network.
     *
     * @param vote the vote to broadcast
     */
    void broadcastVote(VoteProtocolMessage vote);

    /**
     * Broadcasts a proposal to all peers in the network.
     *
     * @param proposal the proposal to broadcast
     */
    void broadcastProposal(ProposalProtocolMessage proposal);

    /**
     * Broadcasts a finalize message to all peers in the network.
     *
     * @param finalize the finalize message to broadcast
     */
    void broadcastFinalize(FinalizeProtocolMessage finalize);

    /**
     * Get the network delta seconds.
     *
     * @return the network delta seconds
     */
    int getNetworkDeltaSeconds();
}
