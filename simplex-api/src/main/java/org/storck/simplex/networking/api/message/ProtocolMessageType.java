package org.storck.simplex.networking.api.message;

/**
 * The types of protocol message between players in a consensus protocol.
 */
public enum ProtocolMessageType {

    /**
     * For a message requesting blockchain state synchronization.
     */
    SYNC_MESSAGE,

    /**
     * For messages containing a new block proposal.
     */
    PROPOSAL_MESSAGE,

    /**
     * For messages containing a vote for a block proposal.
     */
    VOTE_MESSAGE,

    /**
     * For block finalization messages.
     */
    FINALIZE_MESSAGE,

    /**
     * For unspecified protocol messages.
     */
    OTHER
}
