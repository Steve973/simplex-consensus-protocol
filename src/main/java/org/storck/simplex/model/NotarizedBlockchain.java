package org.storck.simplex.model;

import java.util.List;

public record NotarizedBlockchain<T>(List<NotarizedBlock<T>> blocks) {
}
