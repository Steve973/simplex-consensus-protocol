package org.storck.simplex.networking.api.network;

import org.storck.simplex.networking.api.protocol.ConsensusProtocolService;
import org.storck.simplex.networking.api.protocol.ProposalProtocolMessage;
import org.storck.simplex.networking.api.protocol.VoteProtocolMessage;

/**
 * Specifies the methods that a {@link PeerNetworkClient} implementation must
 * include in order for the {@link ConsensusProtocolService} implementation to
 * communicate on the network to other players/peers.
 */
public interface PeerNetworkClient {

    /**
     * Broadcasts a vote to all peers in the network.
     *
     * @param vote
     *            the vote to broadcast
     */
    void broadcastVote(VoteProtocolMessage vote);

    /**
     * Broadcasts a proposal to all peers in the network.
     *
     * @param proposal
     *            the proposal to broadcast
     */
    void broadcastProposal(ProposalProtocolMessage proposal);
}
