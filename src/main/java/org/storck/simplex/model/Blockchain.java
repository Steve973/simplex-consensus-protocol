package org.storck.simplex.model;

import java.util.List;

public record Blockchain<T>(int height, List<Block<T>> blocks) {
}
