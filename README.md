# Concurrent Card Game

[![Java CI with Maven](https://github.com/lqxh2025/concurrent-card-game/actions/workflows/maven.yml/badge.svg)](https://github.com/lqxh2025/concurrent-card-game/actions/workflows/maven.yml)

## Overview

This is a Java multithreaded card-game simulation in which multiple `Player` threads concurrently draw and discard cards through a set of shared decks arranged in a ring. Each player repeatedly draws a card from the deck on its left and discards a card to the deck on its right; the first player to hold four cards of the same denomination wins.

The portfolio version was refactored from an academic project into a standalone Maven-based Java 17 project, with an automated JUnit 5 test suite and improved concurrency handling to address race conditions found during review.

## Key Features

- Multithreaded player execution, with each player running as an independent thread
- Synchronized shared card decks for thread-safe draw/discard operations
- `wait`/`notifyAll` coordination for blocking and waking player threads
- Shared `GameState` component that coordinates a single winner across all players
- Graceful shutdown when another player wins, so waiting players wake and exit cleanly
- Card-conservation protection during concurrent shutdown, so a card drawn immediately before the game ends is not lost
- Deterministic concurrency regression tests that reproduce specific thread interleavings without relying on chance
- Executable Maven JAR for running the game directly

## Concurrency Design

- Each `Player` runs in its own thread, drawing from a left deck and discarding to a right deck, with decks shared between players in a ring
- `CardDeck` synchronizes all access to its internal card queue
- `GameState` coordinates a single winner across all players
- Blocked players are explicitly awakened when the game ends, rather than being left waiting indefinitely
- Cards drawn immediately before game-over are restored to their source deck rather than lost

This is a high-level summary; see the source and tests for implementation detail.

## Project Structure

```
src/main/java/com/qiuhan/cardgame/
src/test/java/com/qiuhan/cardgame/
src/test/resources/packs/
```

Main classes:

- `Card` — an immutable playing card with a denomination
- `CardDeck` — a thread-safe deck supporting draw and discard operations
- `Player` — a runnable representing one player's game loop
- `GameState` — shared state coordinating the single winner
- `CardGame` — entry point that loads a pack, distributes cards, and runs the game

## Running the Project

Build and package:

```
mvn clean package
```

Run:

```
java -jar target/concurrent-card-game-1.0.0.jar
```

The program prompts for:

1. the number of players
2. the path to a valid pack file

For example, with two players you can supply the pack file included in the test resources:

```
Please enter the number of players: 2
Please enter location of pack to load: src/test/resources/packs/two.txt
```

Only the two prompts themselves are shown above; the remaining game output depends on the contents and order of the pack file and is not reproduced here.

## Running Tests

```
mvn test
```

22 JUnit 5 tests currently cover core card/deck behaviour, winner coordination, player behaviour, and deterministic concurrency regression scenarios. This reflects the scenarios exercised so far, not complete coverage.

## Concurrency Bugs Identified and Fixed

Two concurrency defects were identified during review and fixed:

1. **Blocked-player shutdown.** A player waiting on an empty deck could remain blocked indefinitely after another player had already won, since the shared game state had no way to notify decks that the game had ended. The fix propagates game-end signalling to registered decks so waiting threads wake and terminate cleanly.

2. **Card conservation after draw.** A player could draw a card and then observe game-over before adding it to its hand, causing the card to disappear from the system. A deterministic, `CountDownLatch`-based regression test reproduces this interleaving, and the fix restores the card to its source deck before the player exits.

## Technology

- Java 17
- Maven
- JUnit 5
- Java concurrency primitives: `synchronized`, `wait`/`notifyAll`, `volatile`, `CountDownLatch` (in tests)

## Background

Originally developed in a pair-programming coursework setting. The application code and tests were implemented by Qiuhan Li, while the portfolio version was independently refactored, tested and documented by Qiuhan Li.
