package org.storck.simplex.networking.api.protocol;

import org.storck.simplex.networking.api.message.NetworkMessage;
import org.storck.simplex.networking.api.message.ProtocolMessage;

import java.util.Collection;

/**
 * Defines the methods that a service implementing the Simplex consensus
 * protocol must implement in order to participate in the protocol and
 * interoperate with the
 * {@link org.storck.simplex.networking.api.network.PeerNetworkClient}.
 *
 * @param <T> the type of transactions supported by the protocol
 */
public interface ConsensusProtocolService<T> {

    /**
     * Processes a network message received by the consensus protocol service.
     *
     * @param message the network message to process
     */
    void processNetworkMessage(NetworkMessage message);

    /**
     * When the network client receives a protocol message from a peer, it uses this
     * method to provide the message to the service.
     *
     * @param message the protocol message to process
     */
    void processProtocolMessage(ProtocolMessage message);

    /**
     * When the client has transactions to send to other peers, it uses this method
     * to provide the transactions to the service.
     *
     * @param transactions the collection of transactions to receive
     */
    void processTransactions(Collection<T> transactions);

    /**
     * When the network is ready, it notifies the service to start participating in
     * the protocol through this method.
     *
     * @throws InterruptedException if interrupted while awaiting completion from
     *     the iteration service
     */
    void start() throws InterruptedException;

    /**
     * Stops the protocol.
     */
    void stop();
}
