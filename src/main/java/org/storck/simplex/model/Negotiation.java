package org.storck.simplex.model;

import java.util.List;

public class Negotiation {

    private Proposal proposal;
    private List<Participant> participants;

    public Negotiation(Proposal proposal, List<Participant> participants) {
        this.proposal = proposal;
        this.participants = participants;
    }

    // getters and setters

}