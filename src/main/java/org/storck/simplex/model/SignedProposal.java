package org.storck.simplex.model;

import java.util.Objects;

public record SignedProposal<T>(Proposal<T> proposal, byte[] signature) {

    public SignedProposal {
        Objects.requireNonNull(proposal);
        Objects.requireNonNull(signature);
    }
}
