package org.storck.simplex.model;

import java.util.Set;

public record NotarizedBlock<T>(Block<T> block, Set<Vote> votes) {
}
