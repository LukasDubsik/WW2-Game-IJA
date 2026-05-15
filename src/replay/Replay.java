package replay;

import model.game.Game;
import model.map.Serializable.Position;
import model.map.Serializable.GameMap;
import model.unit.Unit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @class Replay
 * @brief Holder of information of each replay of a specific game
 */
public class Replay implements Serializable {

    private final GameMap map; ///< Terrain data at the start of the game
    private final Map<Position, Unit> units = new HashMap<>(); ///< Position of units at the start of the game

    private final ArrayList<TurnRecord> turns = new ArrayList<>(); ///< List of all turns made in the game
    private int turnIndex = 0; ///< Current index of the current turn

    /**
     * @brief The constructor of the Replay class
     *
     * @param game Current game
     */
    public Replay(Game game) {
        this.map = game.getGameMap();
        for (Map.Entry<Position, Unit> entry : game.getUnits_map().entrySet()) {
            this.units.put(entry.getKey(), new Unit(entry.getValue()));
        }
        this.turns.add(0, new TurnRecord());
    }

    /**
     * @brief The constructor of the Replay class
     *
     * @param replay Loaded replay data
     */
    public Replay (Replay replay){
        this.map = replay.getMap();
        for (Map.Entry<Position, Unit> entry : replay.units.entrySet()) {
            this.units.put(entry.getKey(), new Unit(entry.getValue()));
        }
        for(TurnRecord turnRecord : replay.turns){
            this.turns.add(new TurnRecord(turnRecord));
        }
    }

    /**
     * @brief Get the initial game map
     *
     * @return The game map
     */
    public GameMap getMap() {
        return map;
    }

    /**
     * @brief Get all the units at the start of the game
     *
     * @return Map of units
     */
    public Map<Position, Unit> getUnits() {
        return units;
    }

    /**
     * @brief Adds a new move to the replay record
     *
     * @param pos1 The previous position of a unit
     * @param pos2 The new position of a unit
     */
    public void addMove(Position pos1, Position pos2) {
        truncateFuture();
        turns.get(turnIndex).addMove(pos1, pos2);
    }

    /**
     * @brief Add new turn of the game
     *
     * @param income Wealth the previous player got for that turn
     */
    public void addNextTurn(String player, int income) {
        truncateFuture();
        turns.get(turnIndex).setIncomeRecord(player, income);
        turnIndex++;
        turns.add(new TurnRecord());
    }

    /**
     * @brief Get the current turn record if there is any
     *
     * @return Record of the current turn
     */
    public TurnRecord getCurrentTurn() {
        if (turnIndex < turns.size())
            return turns.get(turnIndex);
        return null;
    }

    /**
     * @brief Move the turn index forward
     *
     */
    public void advanceTurn() {
        if (turnIndex < turns.size() - 1)
            turnIndex++;
    }

    /**
     * @brief Move the turn index backwards
     *
     */
    public TurnRecord goBackward() {
        if (turnIndex > 0) {
            turnIndex--;
            return turns.get(turnIndex);
        }
        return null;
    }

    /**
     * @brief Check if the record is finished
     *
     * @return True if there is no more turns in the replay
     */
    public boolean isAtEnd() {
        return turnIndex >= turns.size() - 1;
    }

    /**
     * @brief Remove the rest of turns if the player has taken over the game
     *
     */
    public void truncateFuture() {
        while (turns.size() > turnIndex + 1) {
            turns.remove(turns.size() - 1);
        }
    }

    /**
     * @brief Branches the replay timeline from the current state.
     *
     */
    public void branchTimeline() {
        truncateFuture();
        TurnRecord currentTurn = getCurrentTurn();
        if (currentTurn != null) {
            currentTurn.clearRecords();
        }
    }
}
