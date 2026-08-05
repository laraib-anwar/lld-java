import java.util.*;

// ==========================================
// 1. Core Domain Models
// ==========================================
class Player {
    private final String id;
    private final String name;
    private int currentPosition;

    public Player(String id, String name) {
        this.id = id;
        this.name = name;
        this.currentPosition = 0; // Starts outside the board at position 0
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getCurrentPosition() { return currentPosition; }
    public void setCurrentPosition(int currentPosition) { this.currentPosition = currentPosition; }
}

enum JumperType {
    SNAKE, LADDER
}

class BoardJumper {
    private final JumperType type;
    private final int startPosition;
    private final int endPosition;

    public BoardJumper(JumperType type, int startPosition, int endPosition) {
        if (type == JumperType.SNAKE && startPosition <= endPosition) {
            throw new IllegalArgumentException("Snakes must push players backward.");
        }
        if (type == JumperType.LADDER && startPosition >= endPosition) {
            throw new IllegalArgumentException("Ladders must pull players forward.");
        }
        this.type = type;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }

    public JumperType getType() { return type; }
    public int getEndPosition() { return endPosition; }
    public int getStartPosition() { return startPosition; }
}

// ==========================================
// 2. Board Configuration Layout
// ==========================================
class Board {
    private final int boardSize;
    private final Map<Integer, BoardJumper> jumpers;

    public Board(int boardSize) {
        this.boardSize = boardSize;
        this.jumpers = new HashMap<>();
    }

    public void addJumper(BoardJumper jumper) {
        jumpers.put(jumper.getStartPosition(), jumper);
    }

    public int getBoardSize() { return boardSize; }

    /**
     * Resolves cascading board position adjustments recursively.
     */
    public int resolveNewPosition(int position) {
        if (position > boardSize) return position; // Handled outside by game mechanics

        if (jumpers.containsKey(position)) {
            BoardJumper jumper = jumpers.get(position);
            System.out.println(" ↳ ⚡ " + jumper.getType() + " encountered at " + position + "! Moving to " + jumper.getEndPosition());
            // Recursive lookup handles cascading elements (e.g., a snake dropping a player onto a ladder)
            return resolveNewPosition(jumper.getEndPosition());
        }
        return position;
    }
}

// ==========================================
// 3. Strategy Pattern: Dice Engine
// ==========================================
interface DiceStrategy {
    int roll();
}

class StandardDice implements DiceStrategy {
    private final Random random = new Random();
    private final int numberOfDice;

    public StandardDice(int numberOfDice) {
        this.numberOfDice = numberOfDice;
    }

    @Override
    public int roll() {
        int totalValue = 0;
        for (int i = 0; i < numberOfDice; i++) {
            totalValue += random.nextInt(6) + 1; // 1 to 6
        }
        return totalValue;
    }
}

// ==========================================
// 4. Central System Game Orchestrator
// ==========================================
class GameSession {
    private final Board board;
    private final Queue<Player> playerTurnQueue;
    private final DiceStrategy dice;
    private Player winner;

    public GameSession(Board board, List<Player> players, DiceStrategy dice) {
        this.board = board;
        this.dice = dice;
        this.playerTurnQueue = new LinkedList<>(players);
        this.winner = null;
    }

    public boolean isGameOver() {
        return winner != null;
    }

    /**
     * Executes a single turn transaction from the FIFO queue.
     */
    public void playTurn() {
        if (isGameOver()) {
            System.out.println("ℹ️ Game is already complete. Winner: " + winner.getName());
            return;
        }

        // Dequeue current active player
        Player activePlayer = playerTurnQueue.poll();
        int oldPosition = activePlayer.getCurrentPosition();
        
        int diceValue = dice.roll();
        int intermediatePosition = oldPosition + diceValue;
        int finalPosition = intermediatePosition;

        System.out.print("🎲 [" + activePlayer.getName() + "] rolled a " + diceValue + " | Moving from " + oldPosition + " → " + intermediatePosition);

        // Validation Checks against Board Threshold Boundaries
        if (intermediatePosition <= board.getBoardSize()) {
            finalPosition = board.resolveNewPosition(intermediatePosition);
            activePlayer.setCurrentPosition(finalPosition);
        } else {
            System.out.print(" | ❌ Roll exceeds board bounds, turn skipped.");
        }
        System.out.println();

        // Win-State Check
        if (activePlayer.getCurrentPosition() == board.getBoardSize()) {
            this.winner = activePlayer;
            System.out.println("\n🎉🏆 [VICTORY] " + activePlayer.getName() + " reached exactly cell " + board.getBoardSize() + " and won the game!");
            return;
        }

        // Re-enqueue the player to continue the turn rotation sequence
        playerTurnQueue.add(activePlayer);
    }
}

// ==========================================
// 5. Main Execution Driver
// ==========================================
public class SnakeAndLadders {
    public static void main(String[] args) {
        System.out.println("=== INITIALIZING SNAKES AND LADDERS SIMULATOR ===\n");

        // Step 1: Initialize an 100-cell board arena
        Board board = new Board(100);

        // Step 2: Inject structural anomalies (Snakes and Ladders)
        board.addJumper(new BoardJumper(JumperType.LADDER, 4, 25));
        board.addJumper(new BoardJumper(JumperType.LADDER, 13, 46));
        board.addJumper(new BoardJumper(JumperType.LADDER, 33, 49));
        board.addJumper(new BoardJumper(JumperType.LADDER, 50, 93));

        board.addJumper(new BoardJumper(JumperType.SNAKE, 27, 5));
        board.addJumper(new BoardJumper(JumperType.SNAKE, 43, 18));
        board.addJumper(new BoardJumper(JumperType.SNAKE, 62, 22));
        board.addJumper(new BoardJumper(JumperType.SNAKE, 99, 41));

        // Step 3: Register players
        List<Player> participants = Arrays.asList(
            new Player("P1", "Rahul"),
            new Player("P2", "Amit"),
            new Player("P3", "Priya")
        );

        // Step 4: Configure the Dice rolling strategy engine (Single Dice configuration)
        DiceStrategy singleDice = new StandardDice(1);

        // Step 5: Provision and spin the Game execution environment
        GameSession game = new GameSession(board, participants, singleDice);

        System.out.println("--- GAME LOOP ACTIVE ---");
        int turnCounter = 1;
        
        // Loop execution safely terminates once a win state resolves
        while (!game.isGameOver() && turnCounter <= 50) { // Safety cap of 50 turns for evaluation logs
            game.playTurn();
            turnCounter++;
        }
    }
}