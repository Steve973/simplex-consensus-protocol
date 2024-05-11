# Simplex Consensus Protocol: Java Implementation

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/4b9194bffb2c42f9902b05c1e8961dd1)](https://app.codacy.com/gh/Steve973/simplex-consensus-protocol/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![Open Issues](https://badgen.net/github/open-issues/Steve973/simplex-consensus-protocol)](https://github.com/Steve973/simplex-consensus-protocol/issues)
[![Closed Issues](https://badgen.net/github/closed-issues/Steve973/simplex-consensus-protocol)](https://github.com/Steve973/simplex-consensus-protocol/issues)
[![License](https://badgen.net/github/license/Steve973/simplex-consensus-protocol?cache=300)](https://github.com/Steve973/simplex-consensus-protocol/blob/main/LICENSE)

This project attempts to create a Java implementation of the Simplex Consensus Protocol.  For maximum flexibility and
usability, the library has been implemented in a layered approach.

## Layered Approach

- *Core library layer*:

   The core layer is the implementation of the base protocol, itself.  While the protocol relies on features including
   network connectivity, key exchange, message broadcast, etc., those features are not included in this level.  This is
   deliberate, so that users or teams that have their own peer-to-peer network already present and configured can still
   use the core library with their own network implementation and configuration.

- *Peer-to-peer network layer*:

   The p2p network layer utilizes the Java implementation of libp2p, along with WebRTC, to handle the setup of the
   peer-to-peer networking, including public key exchange, the actual transport concerns of the messages between peers,
   and anything that facilitates the protocol, but is not a direct concern of the Simplex Consensus protocol, itself.

- *Client interaction layer*:

   Since clients of the Simplex Consensus protocol need to submit their transactions to the other peers, and since they
   need to receive notifications of other peers' transactions, this layer allows the clients to exchange information
   and utilize the protocol without having any specific knowledge of the protocol operation.  The protocol remains a
   "black box" to the clients.  This layer is implemented with Spring Boot and provides REST endpoints.

## Simplex Consensus Protocol Description

Much of the following information was borrowed, verbatim, from the [Simplex Blog][1] website, which has been formatted
for Markdown for convenient display in this readme file.  Please verify all of this information by cross-checking it
with the aforementioned website, in case any changes have been made that this readme file does not reflect, or in the
event that any of this information has been transposed incorrectly.  All credit for this information goes to Benjamin
Chan at Cornell University, and anyone who worked with him.  The author(s) of this repository have not contributed
to the protocol specification.  Their intent is to create a high-quality Java implementation that fulfills this protocol
specification, and to share it with the community for use, or for further collaborative development.

The following text represents, as accurately as possible, the content of the [Simplex Blog][1]'s protocol description:

We start with some preliminaries and data structures that will set the stage for the main body of the protocol.

### Digital Signatures

Our protocol uses digital signatures, which requires a basic setup ahead of time.

- *Public key infrastructure*

  Every player has a public key, and a secret key (which it keeps secret), generated for the digital signature scheme.
  Each player's public key is known to every other player at the beginning of the protocol execution. For any string or
  message m, we denote `⟨m⟩i` to be the same message but signed by player `i`. That is, `m` is accompanied by a digital
  signature on `m` that is verifiable using player `i`'s public key.

- *Data Structures*

  Simplex uses a number of different data structures, many of which should be familiar to a blockchain enthusiast.

- *Blocks and block heights*

  The protocol operates on "blocks" of transactions. A block is a tuple `(h, parenthash, txs)`, where `h` is the height of
  the block, `parenthash` is a cryptographic hash of the parent blockchain, and `txs` is a set of transactions included in
  the block. Thanks to the parent hash, each block essentially "extends" and commits to a parent blockchain, which
  represents a history of transactions.

- *Genesis block*

  The genesis block is the default starting block `(0, ⊥, ⊥)` of height `0`. Here, `⊥` is the empty string "", representing
  some empty default value. The genesis block is always the first block in the blockchain.

- *Dummy blocks*

  In Simplex, sometimes we may mine a "dummy block" during periods of time when the network is faulty, or when there is
  too much malicious activity. A dummy block of height `h` is simply the tuple `(h, ⊥, ⊥)`. Note that the dummy block does
  not extend a specific parent chain.

- *Blockchains*

  A blockchain of height (or length) `h` is a sequence of blocks `b0, b1, …, bh` where `b0` is the genesis block, and for
  each of other bocks, either `bi` is the dummy block of height `i`, or `bi = (i, H(b0, …, bi-1), txs)` for some choice of
  transactions. Here, `H` is a publicly known collision-resistant hash function.

- *Notarized blocks and blockchains*

  A "notarization" for a block `b` (which may be the dummy block) is a set of signed messages of the form `⟨vote, h, b⟩i`
  from 2n/3 unique players `i∈{1, …, n}`. A "notarized block" is a block augmented by a notarization for that block. A
  "notarized blockchain" is a blockchain, where every block has a notarization.

### High Level Structure

The protocol runs `in` iterations `h` = 1, 2, 3, … where each player only advances to the next iteration `h` + 1 once it has
seen a notarized blockchain of length `h`. Thus, in each iteration `h`, the protocol must ensure that every honest player
eventually sees a new notarized block of height `h`, and moreover that this block extends some notarized blockchain of
length `h` - 1. In order to help shepherd players towards voting for the same block, each iteration `h` has a designated
"leader player", denote `Lh`, chosen randomly ahead of time and known to all players. More specifically:

- *Random leaders*

  For each iteration `h`, its leader `Lh` is chosen by hashing the iteration number `h` using some public hash function. In
  other words, `Lh = H*(h) mod n`, where `H*` is a predetermined hash function.

As a preview, in each iteration `h`, the leader `Lh` will propose a new block of height `h` extending a notarized blockchain
of `h` - 1. Players will then vote for this block, to get it notarized. Of course, we will have to handle the case of a
faulty network or faulty leader who sends invalid blocks, or sends different block proposals to different people. A
faulty leader may even pretend to be offline, and not send a block proposal at all. We handle this in the full
description below.

### The Full Protocol Description

We are now ready to describe the protocol in its entirety.

#### Starting iteration and protocol state

At the beginning of the execution, every player immediately enters iteration 1. As local state, each player keeps track
of which iteration it is currently in, and also stores all notarized blocks and messages that it has seen so far.

#### Execution

Each player `i`, on entering iteration `h` does the following:

1. *Backup step in case of faulty network or faulty leader*

   player `i` starts a new timer `Th` set to fire locally after `3Δ` seconds if player `i` still hasn't finished iteration `h`
   by then. If the timer fires, and player `i` is still in iteration `h`, player `i` sends to everyone a vote for the dummy
   block

   ```
   ⟨vote, h, ⊥h⟩i
   ```

   Here, `⊥h = (h, ⊥, ⊥)` is just the dummy block.

2. *Leader proposal*

   If player `i` is the leader, namely if `i` = `Lh`, then player `i` sends to everyone a block proposal of the form

   ```
   ⟨propose, h, b0, b1, …, bh⟩i
   ```

   where `bh` = `(h, H(b0, …, bh-1), txs)` is player `i`'s choice of a new block extending some blockchain `b0, …, bh-1` that is
   notarized in player `i`'s view. Note that the parent blockchain has length `h` - 1. player `i` should include in `txs` every
   transaction that it has seen that is not already in the parent blockchain.

3. *Voting step*

   player `i` waits to see the leader's block proposal. On seeing the first (and only the first) such proposal of the form
   `⟨propose, h, b0, …, bh⟩Lh`, player `i` first checks that the block is well-formed
   (i.e. that `bh = (h, H(b0, …, bh-1), txs)` for some set of transactions) and that `b0, …, bh-1` is indeed a notarized
   blockchain of length `h` - 1 in its view. If these checks succeed, then player `i` sends to everyone a vote

   ```
   ⟨vote, h, bh⟩i
   ```
   
   (Recall that on seeing 2n/3 votes for any given block `bh`, a player considers that block to be notarized.)

4. *Entering the next iteration*

   At any point during iteration `h`, if player `i` sees a notarized blockchain of length `h`, it moves on to iteration `h` + 1.
   At this point, if its timer `Th` has not fired yet, player `i` cancels the timer, and sends everyone a message

   ```
   ⟨finalize, h⟩i
   ```

5. *Finalizing transactions*

   Finally, at any point during the protocol execution, no matter the current iteration number:
   At any point, if player `i` sees 2n/3 `⟨finalize, h⟩j` messages for some iteration number `h`, each signed by a different
   player `j`  player `i` deems that iteration to be finalized. If an iteration `h` is finalized, whenever player `i` sees a
   notarized blockchain of length `h`, they can finalize all the transactions contained within (and output them to the
   client).

## Protocol full reference

Please see the [full paper][3] that describes this protocol in much more detail.  For a presentation-friendly version of
this description that provides some excellent diagrams that visualize some of these concepts, see the slides [PDF][2].

## Q & A

The protocol description on the [Simplex Blog][1] is thorough, clear, and concise.  Regardless, everyone comes from
different backgrounds and different levels of experience, so questions are bound to arise.  Ben Chan graciously answered
a number of questions.

- **Question 1:** If a player votes for the dummy block, do the other players do anything with that block, or is it simply
  a throw-away?

  > If the (non-dummy) block gets notarized, it still might be integrated into the chain by a future leader, or even
  > finalized. If it does not, then it is just thrown away.

- **Question 2:** I feel like something should be done with it, or the protocol description would probably instruct that
  the player simply let the iteration time out when not voting for the proposed block.

  > The dummy block is basically what we do when the iteration times out.

- **Question 3:** If a player experiences some sort of network outage, how do they get caught up with the blockchain?

  > When other players move to the next iteration, they send to everyone their view of the notarized blockchain so far;
  > this catches up any laggy nodes. (The bandwidth can be optimized in practice, i.e. see Victor Shoup's paper)

- **Question 4:** Can a new player participate some amount of time after the protocol has started, and the current players
  have been proposing/voting/etc for a number of iterations?  Or are all players pretty much set at the start with no
  late-comers allowed?

  > Latecomers are allowed, by the same reasoning as the network outage

- **Question 5:** Is the proposed block a *required* part of the vote, or can a definitive identifier be used to save
  space and bandwidth, like a hash?

  > We could vote on a hash, at the risk that the actual block never shows up and the data is lost. Most protocols will
  > only vote once they know the data is persisted on a majority of the network

- **Question 6:** If the iteration leader does not have a block of transactions to propose, what should it do?  Does the
  iteration simply time out?

  > Up to the leader, it can propose an empty block, or timeout.

- **Question 7:** I have still been searching around for information about how to synchronize either players who have
  fallen out of sync, or to allow latecomers to join and catch up.  I still have not found anything conclusive, but I have
  an idea as long as it doesn't violate the protocol itself.  My solution would be that an out of sync player or a
  latecomer would broadcast a block, and we can call it a "hello" block or a "sync" block.

  > In Simplex, it's handled in Step 4: "*Next iteration and finalize votes: On seeing a notarized blockchain of height h,
  > enter iteration h+1. At the same time, p multicasts its view of the notarized blockchain to everyone else. At this
  > point in time, if the timer `Th` did not fire yet: cancel `Th` (so it never fires) and multicast <finalize, h>*". The
  > second line, where "*p multicasts its view of the notarized blockchain to everyone else*", should synchronize the rest
  > of the network. In practice, it is likely better to implement this as a "pull" rather than a "push".

## References

[1]: https://simplex.blog/ "Simplex Consensus: Fast and Simple Consensus"
[2]: https://simplex.blog/slides-cornell23.pdf "Simplex Consensus Slides"
[3]: https://eprint.iacr.org/2023/463.pdf "Simplex Consensus: A Simple and Fast Consensus Protocol by Benjamin Y Chan, Cornell University"
