package org.storck.simplex.model;

import java.util.Set;

public record VoteRegistryEntry<T>(Block<T> block, Set<Vote> votes) {
}
