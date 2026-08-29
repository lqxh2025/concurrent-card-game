package com.qiuhan.cardgame;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CardGame {

    private int numPlayers;
    private List<Card> pack;
    private List<Player> players;
    private List<CardDeck> decks;
    private List<Thread> playerThreads;
    private GameState gameState;

    public CardGame() {
        this.players = new ArrayList<>();
        this.decks = new ArrayList<>();
        this.playerThreads = new ArrayList<>();
        this.gameState = new GameState();
    }

    public boolean loadPack(String filename) {
        pack = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                try {
                    int denomination = Integer.parseInt(line);
                    if (denomination < 0) {
                        System.out.println("Error: Pack contains negative number.");
                        return false;
                    }
                    pack.add(new Card(denomination));
                } catch (NumberFormatException e) {
                    System.out.println("Error: Pack contains non-integer value.");
                    return false;
                }
            }

            if (pack.size() != 8 * numPlayers) {
                System.out.println("Error: Pack should contain " + (8 * numPlayers) +
                                 " cards but contains " + pack.size() + " cards.");
                return false;
            }

            return true;

        } catch (IOException e) {
            System.out.println("Error reading pack file: " + e.getMessage());
            return false;
        }
    }

    public void distributeCards() {
        int cardIndex = 0;

        // Distribute 4 cards to each player
        for (int i = 0; i < 4; i++) {
            for (Player player : players) {
                player.addCard(pack.get(cardIndex++));
            }
        }

        // Fill decks
        while (cardIndex < pack.size()) {
            for (CardDeck deck : decks) {
                if (cardIndex < pack.size()) {
                    deck.addCard(pack.get(cardIndex++));
                }
            }
        }
    }

    public void initializeGame() throws IOException {
        // Create decks
        for (int i = 1; i <= numPlayers; i++) {
            decks.add(new CardDeck(i));
        }

        // Create players
        for (int i = 1; i <= numPlayers; i++) {
            CardDeck leftDeck = decks.get(i - 1);
            CardDeck rightDeck = decks.get(i % numPlayers);

            Player player = new Player(i, leftDeck, rightDeck, gameState);
            players.add(player);
        }
    }

    public void startGame() throws IOException {
        // Log initial hands
        for (Player player : players) {
            player.logInitialHand();
        }

        // Check for immediate wins
        for (Player player : players) {
            if (player.hasWinningHand()) {
                if (gameState.declareWinner(player.getPlayerNumber())) {
                    System.out.println("player " + player.getPlayerNumber() + " wins");
                    notifyAllPlayers(player.getPlayerNumber());
                    closeAllOutputs();
                    writeDeckOutputs();
                    return;
                }
            }
        }

        // Start threads
        for (Player player : players) {
            Thread thread = new Thread(player);
            playerThreads.add(thread);
            thread.start();
        }

        // Wait for threads
        for (Thread thread : playerThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Notify all players
        if (gameState.isGameWon()) {
            notifyAllPlayers(gameState.getWinnerNumber());
        }

        closeAllOutputs();
        writeDeckOutputs();
    }

    private void notifyAllPlayers(int winnerNumber) {
        for (Player player : players) {
            player.notifyGameWon(winnerNumber);
        }
    }

    private void closeAllOutputs() {
        for (Player player : players) {
            player.closeOutput();
        }
    }

    private void writeDeckOutputs() throws IOException {
        for (CardDeck deck : decks) {
            deck.writeOutput();
        }
    }

    public static void main(String[] args) {
        CardGame game = new CardGame();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("Please enter the number of players: ");
            try {
                game.numPlayers = scanner.nextInt();
                scanner.nextLine();

                if (game.numPlayers <= 0) {
                    System.out.println("Number of players must be positive.");
                    continue;
                }
                break;
            } catch (Exception e) {
                System.out.println("Invalid input. Please enter a positive integer.");
                scanner.nextLine();
            }
        }

        String packFile;
        while (true) {
            System.out.print("Please enter location of pack to load: ");
            packFile = scanner.nextLine().trim();

            if (game.loadPack(packFile)) {
                break;
            }
            System.out.println("Please provide a valid pack file.");
        }

        scanner.close();

        try {
            game.initializeGame();
            game.distributeCards();
            game.startGame();
        } catch (IOException e) {
            System.err.println("Error running game: " + e.getMessage());
        }
    }
}
