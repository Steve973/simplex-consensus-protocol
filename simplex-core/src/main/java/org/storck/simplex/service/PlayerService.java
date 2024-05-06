package org.storck.simplex.service;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages all peers/players that are participating.
 */
public class PlayerService {

    /**
     * Map that stores the association between player IDs and their corresponding
     * public keys.
     */
    private final Map<String, PublicKey> playerIdsToPublicKeys;

    /**
     * Create an instance when the players are not yet known.
     */
    public PlayerService() {
        playerIdsToPublicKeys = new HashMap<>();
    }

    /**
     * Create an instance when at least one player is known.
     *
     * @param playerIdsToPublicKeys map with entries like [playerId -> publicKey]
     */
    public PlayerService(final Map<String, PublicKey> playerIdsToPublicKeys) {
        this();
        this.playerIdsToPublicKeys.putAll(playerIdsToPublicKeys);
    }

    /**
     * Given a playerId, get their public key from the map.
     *
     * @param playerId the ID of the player
     *
     * @return the public key of the player, or null if a player with the given ID
     *     is unknown
     */
    public PublicKey getPublicKey(final String playerId) {
        return playerIdsToPublicKeys.get(playerId);
    }

    /**
     * Add a player, with their public key, to the map.
     *
     * @param playerId the ID of the player to add
     * @param publicKey the public key of the player to add
     */
    public void addPlayer(final String playerId, final PublicKey publicKey) {
        playerIdsToPublicKeys.put(playerId, publicKey);
    }

    /**
     * Given a player ID, remove that player from the map.
     *
     * @param playerId the ID of the player to remove
     * 
     * @return the public key of the player that was removed; may be null if the
     *     player was not in the map
     */
    public PublicKey removePlayer(final String playerId) {
        return playerIdsToPublicKeys.remove(playerId);
    }

    /**
     * Get a list of all known player IDs.
     *
     * @return a list of all known player IDs
     */
    public List<String> getPlayerIds() {
        return new ArrayList<>(playerIdsToPublicKeys.keySet());
    }
}
