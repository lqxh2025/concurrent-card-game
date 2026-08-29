# Architecture

## System Overview

**CardGame**
- validates input (number of players, pack file)
- loads the pack
- distributes cards
- creates decks and players
- starts and joins player threads

**Player**
- implements `Runnable`
- owns a four-card hand
- draws from its left deck
- discards to its right deck
- checks/declares victory

**CardDeck**
- wraps a shared queue
- synchronizes draw/add operations
- uses `wait`/`notifyAll` when a deck is empty
- supports game-end signalling so blocked drawers can terminate

**GameState**
- stores whether the game has been won
- guarantees only one winner
- coordinates shutdown signalling to registered decks

## Ring Topology

For a 3-player game:

```
Deck 1 -> Player 1 -> Deck 2 -> Player 2 -> Deck 3 -> Player 3
  ^                                                        |
  |________________________________________________________|
```

Each player draws from the deck on its left and discards to the deck on its right. Player 1 draws from Deck 1 and discards to Deck 2; Player 2 draws from Deck 2 and discards to Deck 3; Player 3 draws from Deck 3 and discards back to Deck 1, closing the ring. Because each deck is simultaneously the discard target of one player and the draw source of the next, adjacent players share the same deck object — Deck 2, for example, is both Player 1's right deck and Player 2's left deck.

## Threading Model

- Each `Player` runs as its own Java `Thread`, started and later joined by `CardGame`.
- The main mutable state shared across player threads is held in `CardDeck` and `GameState` objects; each player's hand is primarily managed by its own thread.
- Coordination is monitor-based: `CardDeck` guards its internal queue with `synchronized` methods and uses Java's intrinsic `wait`/`notifyAll`, rather than a higher-level concurrency utility.
- `GameState` exposes its won/winner fields as `volatile` for visibility of simple reads, while the check-and-set that records a winner is done inside a `synchronized` method.
- Threads block via `wait()` rather than polling; there is no busy waiting anywhere in the design.
- There is no `ExecutorService` and no external concurrency library — the whole model is built from plain `Thread`s together with `synchronized`, `wait`/`notifyAll`, and `volatile` (`CountDownLatch` is used only in the test suite, to coordinate test timing, not in production code).

This coordinates draw and discard as individually synchronized operations; it does not make a whole player turn atomic (see Known Limitations).

## Shutdown Coordination

The shutdown sequence, at a high level:

1. A player declares a win by calling `GameState.declareWinner(playerNumber)`.
2. `GameState` records the winner exactly once, using a synchronized check-and-set so concurrent attempts cannot both succeed.
3. As part of that same call, `GameState` signals every `CardDeck` that has registered with it that the game has ended.
4. Any thread currently blocked inside a deck's `drawCard()` wakes up.
5. A woken (or newly arriving) player thread detects that the deck stayed empty because the game ended, and exits its loop instead of waiting indefinitely.

A second, related case is handled separately: if a player has already removed a real card from its left deck but then observes that the game has ended before that card is added to its hand, it returns the card to the deck it came from before exiting, rather than letting it disappear from the system.

## Concurrency Invariants

- Only one winner is ever recorded.
- All shared deck queue operations (draw, add, size) are synchronized.
- A player blocked on an empty deck must be able to terminate after the game ends.
- A successfully drawn card must remain accounted for by the game and must ultimately be owned by a hand or a deck.
- No card should disappear during shutdown, whether the player was blocked waiting or had just completed a draw.

## Testing Strategy

The concurrency-focused tests use coordination primitives such as `CountDownLatch` to force a specific thread interleaving deterministically, rather than relying on repeated probabilistic stress runs that may or may not reproduce a race. Two scenarios are covered this way:

- a player blocked waiting on an empty deck correctly terminates once another player has won;
- a card is conserved when the game ends in the narrow window immediately after a successful draw but before that card is added to the player's hand.

## Known Limitations

These are deliberate scope boundaries for the project as it currently stands, not oversights:

- Draw and discard are each individually synchronized, but a full player turn (draw, hand update, discard, win check) is not one atomic transaction.
- The single-player topology is not a primary supported or tested scenario.
- The project focuses on monitor-based Java concurrency (`synchronized`, `wait`/`notifyAll`, `volatile`) rather than a high-throughput production concurrency architecture.
