package model.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import app.StartApp;
import bot.Bot;
import model.map.Building;
import model.map.Serializable.GameMap;
import model.map.Serializable.Overlay;
import model.map.Serializable.Position;
import model.map.Serializable.Terrain;
import model.unit.MovementType;
import model.unit.TargetClass;
import model.unit.Unit;
import model.unit.UnitType;
import replay.Action;
import replay.Replay;
import replay.TurnRecord;
import replay.records.BuildingIntegrityRecord;
import replay.records.DamageRecord;
import replay.records.MoveRecord;
import replay.records.RepairRecord;
import replay.records.UnitPurchaseRecord;

/**
 * @class Game
 * @brief The main class holding values of the current game
 */
public class Game {

    public static final int STARTING_WEALTH = 1000; ///< Starting money for each player
    public static final int CITY_INCOME = 200; ///< Income from one city per completed turn

    private final GameMap gameMap;

    private final Map<Position, Unit> units_map = new HashMap<>(); ///< Position to unit
    private final Map<Position, Building> buildings = new HashMap<>(); ///< Position to an owned building

    private final Map<String, Integer> playerWealth = new HashMap<>(); ///< Track the wealth of each player

    private final List<GameObserver> observers = new ArrayList<>();

    private int current_turn = 1; ///< The current turn number
    private String current_player = "P1"; ///< The currently active player

    private List<Position> last_movement_path = new ArrayList<>(); ///< The most recently computed movement path

    private Replay replay; ///< The replay of that game
    private boolean replayMode; ///< If the game is in replay mode

    private Position selectedFactory;

    private boolean p1Bot = false;
    private boolean p2Bot = false;

    /**
     * @brief The constructor of the Game class
     * 
     * @param map The array map - already converted to the Terrain enum
     * @param overlay The map of all overlays
     * @param players List of all players
     */
    public Game(Terrain[][] map, Overlay[][] overlay, String[] players) {
        this.gameMap = new GameMap(map, overlay);

        // Set each player's starting money
        for(String player : players)
            playerWealth.put(player, STARTING_WEALTH);

        replayMode = false;
        this.replay = new Replay(this);
    }

    /**
     * @brief Start a new game from a loaded replay
     *
     * @param players List of all players
     * @param replay Loaded replay
     */
    public Game(String[] players, Replay replay){
        this.gameMap = replay.getMap();
        this.replay = new Replay(replay);
        this.units_map.putAll(replay.getUnits());

        // Heal all damaged units in the replay
        for(Map.Entry<Position, Unit> entry : units_map.entrySet()){
            Unit unit = entry.getValue();
            unit.setCurrentHp(unit.getUnitType().getMaxHP());

            unit.setPosition(entry.getKey());
        }

        // Set each player's starting money
        for(String player : players)
            playerWealth.put(player, STARTING_WEALTH);

        // Loaded replay is running in replay mode
        replayMode = true;

        // Set the initial ownership of buildings
        setOwnership();
    }

    /**
     * @brief Create a copy of the game for a bot to read
     *
     * @param game game object to copy
     */
    private Game(Game game){
        gameMap = game.gameMap;

        // Deep-copy units so the bot can simulate/read without mutating real objects
        for (Map.Entry<Position, Unit> entry : game.units_map.entrySet()) {
            units_map.put(entry.getKey(), new Unit(entry.getValue()));
        }

        // Deep-copy buildings for the same reason
        for (Map.Entry<Position, Building> entry : game.buildings.entrySet()) {
            buildings.put(entry.getKey(), new Building(entry.getValue()));
        }

        playerWealth.putAll(game.playerWealth);
        observers.addAll(game.observers);
        current_turn = game.current_turn;
        current_player = game.current_player;
        last_movement_path.addAll(game.last_movement_path);
        selectedFactory = game.selectedFactory;
        p1Bot = game.p1Bot;
        p2Bot = game.p2Bot;
        replayMode = game.replayMode;
        replay = game.replay == null ? null : new Replay(game.replay);
    }

    /**
     * @brief Get the number of columns in the game
     * 
     * @return The number of the columns.
     */
    public int getColumns() {
        return this.gameMap.getColumns();
    }

    /**
     * @brief Get the number of rows in the game
     * 
     * @return The number of the rows.
     */
    public int getRows() {
        return this.gameMap.getRows();
    }

    /**
     * @brief Get the last movement path used by the game
     * 
     * @return Copy of the last movement path
     */
    public List<Position> getLastMovementPath() {
        return new ArrayList<>(last_movement_path);
    }

    /**
     * @brief Get the unit currently standing at the given position
     * 
     * @param pos The position to inspect
     * @return The unit at the position, or null if none is there
     */
    public Unit getUnitAt(Position pos) {
        return units_map.get(pos);
    }

    /**
     * @brief Get the terrain at the position
     * 
     * @param pos The position where to get the terrain from
     * 
     * @return The terrain at the position
     */
    public Terrain getTerrain(Position pos) {
        return gameMap.getTerrainsMap()[pos.row()][pos.column()];
    }

    /**
     * @brief Get the overlay at the position
     * 
     * @param pos The position where to get the overlay from
     * 
     * @return The overlay at the position
     */
    public Overlay getOverlay(Position pos) {
        return gameMap.getOverlayMap()[pos.row()][pos.column()];
    }

    /**
     * @brief Get the unit at the position
     * 
     * @param pos The position where to get the unit from
     * 
     * @return The unit at the position
     */
    public Unit getUnit(Position pos) {
        return units_map.get(pos);
    }

    /**
     * @brief Add observer to the active list of observers
     * 
     * @param observer The observer class to be added
     */
    public void addObserver(GameObserver observer) {
        // Check we can add it
        if (observer != null && !observers.contains(observer)) {
            // Then add it
            observers.add(observer);
        }
    }

    /**
     * @brief Remove an observer from the list of active ones
     * 
     * @param observer The observer to remove
     */
    public void removeObserver(GameObserver observer) {
        observers.remove(observer);
    }

    /**
     * @brief Notify all the currently linked observers
     * 
     * @param event The event to be notified of
     */
    private void notifyObservers(GameEvent event) {
        for (GameObserver observer : List.copyOf(observers)) {
            observer.update(event);
        }
    }

    /**
     * @brief Create a new unit within the game
     * 
     * @param type The unit type -> Infantry/Tannk/artilerry
     * @param owner Who wons this unit within the game
     * @param row On which row the unit stands
     * @param column On which column the unit stands
     * 
     * @return The newly created Unit within the game.
     */
    public Unit createUnit(String type, String owner, int row, int column) {
        // Check the owner validity
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Owner cannot be empty.");
        }

        // Only two players are currently supported
        if (!"P1".equals(owner) && !"P2".equals(owner)) {
            throw new IllegalArgumentException("Unsupported owner: " + owner);
        }

        // Get the unit values based on its type
        UnitType unit_type = UnitType.convert(type);

        // Enforce faction legality
        if (!isUnitTypeAllowedForPlayer(owner, unit_type)) {
            throw new IllegalArgumentException("Unit type " + unit_type.getName() + " does not belong to " + owner + ".");
        }

        // Set its position
        Position position = new Position(row, column);

        // Check that the position is valid
        if (!isInside(position)) {
            throw new IllegalArgumentException("Position is outside the game.");
        }

        // Check that the position is not yet occupied
        if (units_map.containsKey(position)) {
            throw new IllegalArgumentException("Map position is already occupied.");
        }

        // Reject impassable starting placement
        Terrain terrain = getTerrain(position);
        if (unit_type.getMovementCost(terrain) == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Unit cannot be placed on impassable terrain.");
        }

        // Create the unit if all passes
        Unit unit = new Unit(unit_type, owner, position);
        units_map.put(position, unit);

        return unit;
    }

    /**
     * @brief Move a unit from one position to another if the move is valid
     * 
     * @param from Starting position
     * @param to Target position
     * @param ignoreChecks Force the move
     * 
     * @return True if movement was performed, false otherwise
     */
    public boolean moveUnit(Position from, Position to, boolean ignoreChecks) {

        // Do not allow real gameplay moves after victory
        if (!ignoreChecks && isGameFinished()) {
            return false;
        }

        // Basic null protection
        if (from == null || to == null) {
            return false;
        }

        // Check that there is any unit at the position even
        Unit unit = this.units_map.get(from);
        if (unit == null) {
            return false;
        }

        // Check that the unit belongs to the active player
        if (!ignoreChecks && !unit.getOwner().equals(this.current_player)) {
            return false;
        }

        // Check that the unit has not yet fully played this turn
        if (!ignoreChecks && unit.hasAlreadyPlayed()) {
            return false;
        }

        // Check that the unit has not already moved this turn
        if (!ignoreChecks && unit.hasMovedThisTurn()) {
            return false;
        }

        // Get the exact path
        List<Position> path = getMovementPath(from, to);
        if (!ignoreChecks && path.isEmpty()) {
            return false;
        }

        // Keep the path in memory for later use/debugging
        this.last_movement_path = new ArrayList<>(path);

        // Move the unit within the map
        units_map.remove(from);
        unit.setPosition(to);
        units_map.put(to, unit);

        // Mark that the unit has moved this turn
        unit.setMovedThisTurn(true);

        // Check whether the unit can still attack after moving
        List<Position> attackable_tiles = getAttackableTiles(to);

        // If there are no legal attacks left, finish the action immediately
        if (attackable_tiles.isEmpty()) {
            unit.setAlreadyPlayed(true);
        }

        // Add record of the move
        if(!replayMode && replay != null)
            replay.addMove(from, to);

        // Notify that the game state changed
        notifyObservers(new GameEvent(GameEvent.Type.UNIT_MOVED, unit, from, to));

        return true;
    }

    /**
     * @brief Given the current position, perform weighted pathfinding and find all the reachable positions viable
     * 
     * @param pos The position of the unit
     */
    public List<Position> getReachableTiles(Position pos) {
        if (isGameFinished()) {
            return List.of();
        }

        // Check that the unit is even at the position
        Unit unit = this.units_map.get(pos);
        if (unit == null) {
            // Return a list of nothing -> No unit, can't go anywhere
            return List.of();
        }

        // Check that the unit has not already finished its turn
        if (unit.hasAlreadyPlayed()) {
            return List.of();
        }

        // Check that the unit has not already moved this turn
        if (unit.hasMovedThisTurn()) {
            return List.of();
        }

        // Check that the unit belongs to the currently active player
        if (!unit.getOwner().equals(this.current_player)) {
            return List.of();
        }

        // Run the path search
        SearchResult result = runMovementSearch(unit, pos);

        // Collect all reachable tiles except the origin
        List<Position> reachable = new ArrayList<>();
        for (Position tile : result.best.keySet()) {
            if (!tile.equals(pos)) {
                reachable.add(tile);
            }
        }

        // Sort by closest position first
        Collections.sort(reachable, new PositionComparator());
        return reachable;
    }

    /**
     * @brief Check if the Positional node is within the map
     * 
     * @param pos The position we are checking for
     * 
     * @return Returns true if inside, false if outside
     */
    private boolean isInside(Position pos) {
        boolean row_check = pos.row() >= 0 && pos.row() < gameMap.getRows();
        boolean column_check = pos.column() >= 0 && pos.column() < gameMap.getColumns();
        return row_check && column_check;
    }

    /**
     * @brief Get the effective defence bonus = terrain + overlay
     * 
     * @param pos The position of the defence bonus.
     */
    public int getCombinedDefenceBonus(Position pos) {
        return getTerrain(pos).getDefenceBonus() + getOverlay(pos).getDefenceBonus();
    }

    /**
     * @brief Get the effective movement cost at a tile for a concrete unit
     */
    public int getMovementCost(Position pos, Unit unit) {
        return switch (unit.getUnitType().getMovementType()) {
            case INFANTRY -> getCombinedMovementInfantry(pos);
            case VEHICLE -> getCombinedMovementVehicle(pos);
        };
    }

    /**
     * @brief Combine terrain movement cost with overlay modifier.
     *        Impassable terrain stays impassable.
     * 
     * @param base_cost The standart cost of terrain movement
     * @param overlay_modifier The modifier added to the movement by the overlay
     */
    private static int combineMoveCost(int base_cost, int overlay_modifier) {
        // Maximum cost still stays maximum
        if (base_cost == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        // Return the combined cost
        long combined = (long) base_cost + overlay_modifier;
        return combined >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) combined;
    }

    public int getCombinedMovementInfantry(Position pos) {
        return combineMoveCost(getTerrain(pos).getInfantryMovementCost(), getOverlay(pos).getInfantryMovementCost());
    }

    public int getCombinedMovementVehicle(Position pos) {
        return combineMoveCost(getTerrain(pos).getVehicleMovementCost(), getOverlay(pos).getVehicleMovementCost());
    }

        /**
     * @brief Get the current turn number
     * 
     * @return The currently active turn number
     */
    public int getCurrentTurn() {
        return this.current_turn;
    }

    /**
     * @brief Get the currently active player
     * 
     * @return The owner string of the active player
     */
    public String getCurrentPlayer() {
        return this.current_player;
    }

    /**
     * @brief Set which player starts as currently active
     * 
     * This is mainly used by the startup menu so the user can choose
     * which faction they want to begin with.
     * 
     * @param player The player identifier ("P1" or "P2")
     */
    public void setCurrentPlayer(String player) {
        // Only two players are supported
        if (!"P1".equals(player) && !"P2".equals(player)) {
            throw new IllegalArgumentException("Unsupported player: " + player);
        }

        this.current_player = player;
    }

    /**
     * @brief Get the wealth of each player
     *
     * @return The map of players and their wealth
     */
    public Map<String, Integer> getPlayerWealth() {
        return playerWealth;
    }

    /**
     * @brief Get all neighboring tiles around the given tile on the hex map
     * 
     * @param pos The position whose neighbors we want
     * @return List of all neighboring tiles
     */
    private List<Position> getNeighborTiles(Position pos) {
        List<Position> neighbors = new ArrayList<>();

        int row = pos.row();
        int col = pos.column();

        // Even and odd rows are shifted differently in the hex map
        if (row % 2 == 0) {
            neighbors.add(new Position(row - 1, col - 1));
            neighbors.add(new Position(row - 1, col));
            neighbors.add(new Position(row, col - 1));
            neighbors.add(new Position(row, col + 1));
            neighbors.add(new Position(row + 1, col - 1));
            neighbors.add(new Position(row + 1, col));
        } else {
            neighbors.add(new Position(row - 1, col));
            neighbors.add(new Position(row - 1, col + 1));
            neighbors.add(new Position(row, col - 1));
            neighbors.add(new Position(row, col + 1));
            neighbors.add(new Position(row + 1, col));
            neighbors.add(new Position(row + 1, col + 1));
        }

        return neighbors;
    }

        /**
     * @brief Shift the game to the next turn
     */
    public void nextTurn() {
        // Skip next turn if game is over
        if (isGameFinished()) {
            return;
        }

        String ending_player = this.current_player;
        boolean wasReplaying = replayMode;

        // Check for bots taking over while replaying
        if ((p1Bot && ending_player.equals("P1")) || (p2Bot && ending_player.equals("P2"))) {
            if (isReplayMode()) {
                setReplayMode(false);
                if (replay != null) {
                    replay.branchTimeline();
                }
            }
        }

        // If running replay mode, advance the replay and do not generate new turn effects
        if (replayMode) {
            TurnRecord turnRecord = (replay != null) ? replay.getCurrentTurn() : null;
            if (turnRecord != null) {
                revertActions(turnRecord, false);
                replay.advanceTurn();
            }

            replayMode = replay != null && !replay.isAtEnd();
            this.current_player = swapPlayers(this.current_player);
            this.current_turn++;
            notifyObservers(new GameEvent());
            return;
        }

        TurnRecord currentTurnRecord = (!wasReplaying && replay != null) ? replay.getCurrentTurn() : null;

        // End-of-turn repairs for the player who just played
        repairUnitsForPlayer(ending_player, currentTurnRecord);

        // End-of-turn capture for the player who just played
        processCapturesForPlayer(ending_player, currentTurnRecord);

        // End-of-turn income for the player who just played
        int income = addIncomeForPlayer(ending_player);

        // Store the completed turn before switching to the next player
        if (!wasReplaying && replay != null) {
            replay.addNextTurn(ending_player, income);
        }

        // Switch the active player
        this.current_player = swapPlayers(this.current_player);

        // Advance the turn counter
        this.current_turn++;

        // Reset units of the now-active player
        for (Unit unit : units_map.values()) {
            if (unit.getOwner().equals(this.current_player)) {
                unit.setAlreadyPlayed(false);
                unit.setMovedThisTurn(false);
            }
        }

        // Let bot 1 play
        if (p1Bot && this.current_player.equals("P1") && !isGameFinished()) {
            Bot.makeTurn(this, new Game(this), this.current_player);
        }

        // Let bot 2 play
        if (p2Bot && this.current_player.equals("P2") && !isGameFinished()) {
            Bot.makeTurn(this, new Game(this), this.current_player);
        }

        notifyObservers(new GameEvent());
    }

    /**
     * @brief Shift the game to the previous turn
     */
    public void prevTurn(){

        // Revert any moves made in an unfinished turn
        if (!replayMode && replay != null) {
            TurnRecord currentTurnRecord = replay.getCurrentTurn();
            if (currentTurnRecord != null && (!currentTurnRecord.getActionList().isEmpty())) {
                revertActions(currentTurnRecord, true);
                currentTurnRecord.getActionList().clear();
            }
        }

        // Check if it is the first turn
        if(this.current_turn <= 1){
            notifyObservers(new GameEvent());
            return;
        }

        // Revert the turn
        this.current_turn--;
        this.current_player = swapPlayers(this.current_player);

        // Put the game in replay mode
        replayMode = true;

        // Get the record of the reverted turn
        TurnRecord turnRecord = replay.goBackward();
        if(turnRecord == null) {
            notifyObservers(new GameEvent());
            return;
        }

        // Restore the previouse player wealth
        revertPlayerWealth(turnRecord);

        // Revert the building changes
        revertBuildingChanges(turnRecord);

        // Revert repairs
        revertRepairs(turnRecord);

        // Revert all playerActions
        revertActions(turnRecord, true);

        // Notify the observers that the game changed
        notifyObservers(new GameEvent());
    }

    /**
     * @brief Method to revert any action made by a player
     *
     * @param turnRecord recording of the previous turn
     * @param revert should the method revert or go forward
     */
    private void revertActions(TurnRecord turnRecord, boolean revert){
        ArrayList<Action> actionList = turnRecord.getActionList();

        if(revert){
            for(int i = actionList.size() - 1; i >= 0; i--){ ///< Loop over all actions backwards
                Action action = actionList.get(i);
                if(action.getActionEnum() == Action.ActionEnum.DAMAGE){ ///< Revert damage
                    DamageRecord damageRecord = action.getDamageRecord();
                    Unit unit = getUnit(damageRecord.position());
                    if(unit == null)
                        throw new RuntimeException("Replay is corrupted at " + damageRecord.position().toString());
                    unit.setCurrentHp(unit.getCurrentHp() + damageRecord.damage());
                }
                else if(action.getActionEnum() == Action.ActionEnum.MOVE){ ///< Revert move
                    MoveRecord moveRecord = action.getMoveRecord();
                    Unit unit = getUnit(moveRecord.pos2());
                    if(unit == null)
                        throw new RuntimeException("Replay is corrupted when moving from " + moveRecord.pos2() + " to " + moveRecord.pos1());
                    moveUnit(moveRecord.pos2(), moveRecord.pos1(), true);
                    unit.setMovedThisTurn(false);
                    unit.setAlreadyPlayed(false);
                }
                else if(action.getActionEnum() == Action.ActionEnum.BUY){ ///< Revert buy action
                    UnitPurchaseRecord unitPurchaseRecord = action.getUnitPurchaseRecord();
                    units_map.remove(unitPurchaseRecord.position());
                    int newWealth = playerWealth.get(this.current_player) + unitPurchaseRecord.unitType().getPrice();
                    playerWealth.put(this.current_player, newWealth);
                }
                else if(action.getActionEnum() == Action.ActionEnum.DESTROY){ ///< Revert destruction of a unit
                    Unit destroyedUnit = action.getUnitDestroyed();
                    units_map.put(action.getDestroyPosition(), destroyedUnit);
                }
            }
        }
        else{
            for(int i = 0; i < actionList.size(); i++){ ///< Loop over all actions forwards
                Action action = actionList.get(i);
                if(action.getActionEnum() == Action.ActionEnum.DAMAGE){ ///< Redo all damage actions
                    DamageRecord damageRecord = action.getDamageRecord();
                    Unit unit = getUnit(damageRecord.position());
                    if(unit == null)
                        throw new RuntimeException("Replay is corrupted at " + damageRecord.position());
                    unit.takeDamage(damageRecord.damage());
                    if(unit.isDestroyed())
                        this.units_map.remove(unit.getPosition());
                }
                else if(action.getActionEnum() == Action.ActionEnum.MOVE){ ///< Redo all move actions
                    MoveRecord moveRecord = action.getMoveRecord();
                    Unit unit = getUnit(moveRecord.pos1());
                    if(unit == null)
                        throw new RuntimeException("Replay is corrupted " + moveRecord.pos1().toString());
                    unit.setMovedThisTurn(false);
                    unit.setAlreadyPlayed(false);
                    moveUnit(moveRecord.pos1(), moveRecord.pos2(), true);
                }
                else if(action.getActionEnum() == Action.ActionEnum.BUY){ ///< Redo all buy actions
                    UnitPurchaseRecord unitPurchaseRecord = action.getUnitPurchaseRecord();
                    selectedFactory = unitPurchaseRecord.position();
                    if(!buyUnit(unitPurchaseRecord.unitType()))
                        throw new RuntimeException("Replay is corrupted " + unitPurchaseRecord.unitType() + ", at " + unitPurchaseRecord.position() + ", by " + this.current_player);
                }
            }
        }
    }

    /**
     * @brief Revert the repairs made that turn
     *
     * @param turnRecord Turn record to revert
     */
    private void revertRepairs(TurnRecord turnRecord){
        ArrayList<RepairRecord> repairList = turnRecord.getRepairList();

        for(int i = repairList.size() - 1; i >= 0; i--){
            RepairRecord repairRecord = repairList.get(i);

            Unit unit = getUnit(repairRecord.position());
            if(unit == null)
                throw new RuntimeException("Failed to get a unit to repair at " + repairRecord.position());

            unit.setCurrentHp(unit.getCurrentHp() - 20);
            playerWealth.put(unit.getOwner(), playerWealth.get(unit.getOwner()) + repairRecord.cost());
        }
    }

    /**
     * @brief Revert the building changes made that turn
     *
     * @param turnRecord Turn record to revert
     */
    private void revertBuildingChanges(TurnRecord turnRecord){
        ArrayList<BuildingIntegrityRecord> birList = turnRecord.getBirList();

        for(int i = birList.size() - 1; i >= 0; i--){
            BuildingIntegrityRecord bir = birList.get(i);

            if(bir.prevOwner() == null){
                buildings.remove(bir.buildingPos());
                continue;
            }

            Building building = getBuilding(bir.buildingPos());
            building.setIntegrity(building.getIntegrity() - bir.change());
            building.setOwner(bir.prevOwner());
        }
    }

    /**
     * @brief Revert the player wealth
     *
     * @param turnRecord Turn record to revert
     */
    private void revertPlayerWealth(TurnRecord turnRecord){
        if (turnRecord == null || turnRecord.getIncomeRecord() == null) {
            return;
        }

        String player = turnRecord.getIncomeRecord().player();
        int currentWealth = playerWealth.get(player);
        playerWealth.put(player, currentWealth - turnRecord.getIncomeRecord().income());
    }

    /**
     * @brief Get the other player
     *
     * @param current_player The first player
     *
     * @return The otherp player
     */
    public String swapPlayers(String current_player){
        if (current_player.equals("P1")) {
            return "P2";
        } else {
            return "P1";
        }
    }

    /**
     * @brief Perform weighted search for a unit and remember the parent nodes for path reconstruction
     * 
     * @param unit The unit for which the movement is computed
     * @param start The starting position of the unit
     * 
     * @return Holder with both best score and parent maps
     */
    private SearchResult runMovementSearch(Unit unit, Position start) {
        // The map of the reachable positions with the best cost possible
        Map<Position, Integer> best = new HashMap<>();
        // Map for reconstructing the path
        Map<Position, Position> parent = new HashMap<>();
        // The search queue to explore -> Dijkstra
        PriorityQueue<SearchNode> frontier = new PriorityQueue<>(new SearchNodeComparator());

        // Start at the unit origin
        best.put(start, 0);
        frontier.add(new SearchNode(start, 0));

        // Explore until nothing remains
        while (!frontier.isEmpty()) {
            SearchNode current_node = frontier.poll();

            Integer best_so_far = best.get(current_node.pos);
            if (best_so_far != null && current_node.score > best_so_far) {
                continue;
            }

            // Analyze each of the neighbors
            for (Position neigh : getNeighborTiles(current_node.pos)) {
                // Check map bounds
                if (!isInside(neigh)) {
                    continue;
                }

                // Ignore occupied positions
                if (units_map.containsKey(neigh)) {
                    continue;
                }

                // Compute the full movement cost = terrain + overlay modifier
                int move_cost = getMovementCost(neigh, unit);
                if (move_cost == Integer.MAX_VALUE) {
                    continue;
                }

                // Compute total score to get there
                int neigh_score = current_node.score + move_cost;
                if (neigh_score > unit.getUnitType().getMovement()) {
                    continue;
                }

                // Compare to the current best found score
                Integer curr_score = best.get(neigh);
                if (curr_score == null || neigh_score < curr_score) {
                    best.put(neigh, neigh_score);
                    parent.put(neigh, current_node.pos);
                    frontier.add(new SearchNode(neigh, neigh_score));
                }
            }
        }

        return new SearchResult(best, parent);
    }

    /**
     * @brief Reconstruct the path from start to goal using the parent map
     * 
     * @param parent The parent map created by the weighted search
     * @param start The starting position
     * @param goal The target position
     * 
     * @return Ordered list of positions from start to goal. Empty if not reachable.
     */
    private List<Position> reconstructPath(Map<Position, Position> parent, Position start, Position goal) {
        List<Position> reversed = new ArrayList<>();

        // If the goal is not the start and no parent exists, then it was not reached
        if (!goal.equals(start) && !parent.containsKey(goal)) {
            return new ArrayList<>();
        }

        // Walk backwards from the goal
        Position current = goal;
        reversed.add(current);

        while (!current.equals(start)) {
            current = parent.get(current);

            // Safety guard in case something goes wrong
            if (current == null) {
                return new ArrayList<>();
            }

            reversed.add(current);
        }

        // Reverse into start -> ... -> goal order
        List<Position> result = new ArrayList<>();
        for (int i = reversed.size() - 1; i >= 0; i--) {
            result.add(reversed.get(i));
        }

        return result;
    }

    /**
     * @brief Get the exact movement path from one position to another
     * 
     * @param from The starting position
     * @param to The target position
     * 
     * @return Ordered list of tiles from start to end. Empty if movement is invalid.
     */
    public List<Position> getMovementPath(Position from, Position to) {
        // Check that there is an unit on the start tile
        Unit unit = units_map.get(from);
        if (unit == null) {
            return new ArrayList<>();
        }

        // Respect turn ownership
        if (!unit.getOwner().equals(this.current_player)) {
            return new ArrayList<>();
        }

        // Respect the already-played flag
        if (unit.hasAlreadyPlayed()) {
            return new ArrayList<>();
        }

        // End must be inside map and unoccupied
        if (!isInside(to)) {
            return new ArrayList<>();
        }

        if (units_map.containsKey(to)) {
            return new ArrayList<>();
        }

        // Run the weighted search
        SearchResult result = runMovementSearch(unit, from);

        // Check that the tile is actually reachable
        if (!result.best.containsKey(to)) {
            return new ArrayList<>();
        }

        // Reconstruct and return the exact path
        return reconstructPath(result.parent, from, to);
    }

    // Classes for search algorithm

    /**
     * @class SearchNode
     * @brief Class to hold the value of a node when deciding upon weighted search
     */
    private class SearchNode {
        Position pos; ///< The node's position within the map
        int score; ///< Its score determining the least amount of movements necessary to get there

        /**
         * @brief Initialisation of the SearchNode class
         * 
         * @param pos_ Position within map
         * @param score_ Least amount to get there yet found
         */
        SearchNode(Position pos_, int score_) {
            this.pos = pos_;
            this.score = score_;
        }
    }

    /**
     * @class PositionComparator
     * @brief Used to compare two positions between each other
     */
    private class PositionComparator implements Comparator<Position> {

        /**
         * @brief Compare two positions
         * 
         * @param one The first position for comparison
         * @param two The second position to compare
         */
        @Override
        public int compare(Position one, Position two) {
            Integer compared = Integer.compare(one.row(), two.row());
            if (compared != 0) {
                return compared;
            }
            return Integer.compare(one.column(), two.column());
        }
    }

    /**
     * @class SearchNodeComparator
     * @brief Used to compare two Search positions between each other based on a score
     */
    private class SearchNodeComparator implements Comparator<SearchNode> {

        /**
         * @brief Compare two Search positions
         * 
         * @param one The first Search position for comparison
         * @param two The second Search  to compare
         */
        @Override
        public int compare(SearchNode one, SearchNode two) {
            return Integer.compare(one.score, two.score);
        }
    }

    /**
     * @class SearchResult
     * @brief Holder for pathfinding results
     */
    private class SearchResult {
        Map<Position, Integer> best; ///< The best score found for each tile
        Map<Position, Position> parent; ///< The parent tile from which the node was reached

        /**
         * @brief Constructor of the SearchResult class
         * 
         * @param best_ Best score map
         * @param parent_ Parent map for path reconstruction
         */
        SearchResult(Map<Position, Integer> best_, Map<Position, Position> parent_) {
            this.best = best_;
            this.parent = parent_;
        }
    }

    /**
     * @brief Given the unit position, find all enemy-occupied tiles that are attackable
     * 
     * @param attacker_position The position of the attacking unit
     * @return List of all enemy tiles that are within the unit's attack range
     */
    public List<Position> getAttackableTiles(Position attacker_position) {
        if (isGameFinished()) {
            return List.of();
        }

        // Check that there is even a unit at the position
        Unit attacker = this.units_map.get(attacker_position);
        if (attacker == null) {
            return List.of();
        }

        // Indirect-fire units cannot attack after moving
        if (attacker.hasMovedThisTurn() && attacker.getUnitType().getMinAttackRange() > 1) {
            return List.of();
        }

        // Check that the unit belongs to the current player
        if (!attacker.getOwner().equals(this.current_player)) {
            return List.of();
        }

        // Check that the unit has not already finished its turn
        if (attacker.hasAlreadyPlayed()) {
            return List.of();
        }

        // Find all tiles in the attack range
        Set<Position> tiles_in_range = getTilesInRange(
                attacker_position,
                attacker.getUnitType().getMinAttackRange(),
                attacker.getUnitType().getMaxAttackRange()
        );

        // Keep only those that contain enemy units
        List<Position> attackable_tiles = new ArrayList<>();

        for (Position tile : tiles_in_range) {
            Unit target = this.units_map.get(tile);

            // Ignore empty tiles
            if (target == null) {
                continue;
            }

            // Ignore own units
            if (target.getOwner().equals(attacker.getOwner())) {
                continue;
            }

            attackable_tiles.add(tile);
        }

        // Sort the output so it is stable and predictable
        Collections.sort(attackable_tiles, new PositionComparator());

        return attackable_tiles;
    }

        /**
     * @brief Check whether a unit type is legal for the given player
     * 
     * @param player The player identifier
     * @param unitType The unit type
     * @return True if the unit type belongs to the player's faction
     */
    private boolean isUnitTypeAllowedForPlayer(String player, UnitType unitType) {
        if (player == null || unitType == null) {
            return false;
        }

        return unitType.belongsToPlayer(player);
    }

    /**
     * @brief Check whether this unit may capture buildings
     * 
     * For now only infantry-class units may capture.
     * 
     * @param unit The unit to test
     * @return True if the unit may capture
     */
    private boolean isCaptureUnit(Unit unit) {
        return unit != null && unit.getUnitType().getMovementType() == MovementType.INFANTRY;
    }

    /**
     * @brief Check whether the unit may be repaired at its current tile
     * 
     * @param position The unit position
     * @param unit The unit to check
     * @return True if the unit may repair here
     */
    private boolean canRepairUnitAt(Position position, Unit unit) {
        if (position == null || unit == null || unit.isDestroyed()) {
            return false;
        }

        Building building = getBuilding(position);
        if (building == null) {
            return false;
        }

        if (!unit.getOwner().equals(building.getOwner())) {
            return false;
        }

        return building.isCity() || building.isFactory() || building.isHQ();
    }

    /**
     * @brief Repair all valid units of the given player
     *
     * @param player The player whose turn is ending
     * @param currentTurnRecord Replay record for the completed turn
     */
    private void repairUnitsForPlayer(String player, TurnRecord currentTurnRecord) {
        for (Map.Entry<Position, Unit> entry : units_map.entrySet()) {
            Position position = entry.getKey();
            Unit unit = entry.getValue();

            if (!unit.getOwner().equals(player)) {
                continue;
            }

            if (!canRepairUnitAt(position, unit)) {
                continue;
            }

            if (unit.getCurrentHp() >= unit.getUnitType().getMaxHP()) {
                continue;
            }

            int repairCost = Math.max(100, (int) (unit.getUnitType().getPrice() / 10.0));
            int ownerWealth = playerWealth.get(unit.getOwner());

            if (ownerWealth < repairCost) {
                continue;
            }

            unit.setCurrentHp(Math.min(unit.getCurrentHp() + 20, unit.getUnitType().getMaxHP()));
            playerWealth.put(unit.getOwner(), ownerWealth - repairCost);

            if (currentTurnRecord != null) {
                currentTurnRecord.addRepairedUnit(position, repairCost);
            }
        }
    }

    /**
     * @brief Process building capture for the player whose turn is ending
     *
     * @param player The player whose infantry can capture this turn
     * @param currentTurnRecord Replay record for the completed turn
     */
    private void processCapturesForPlayer(String player, TurnRecord currentTurnRecord) {
        // Reset partial capture progress where the current player is not actively capturing
        buildings.forEach((position, building) -> {
            if (building.isFull()) {
                return;
            }

            Unit unit = getUnit(position);

            if (unit == null || !unit.getOwner().equals(player) || !isCaptureUnit(unit) || unit.getOwner().equals(building.getOwner())) {
                if (currentTurnRecord != null) {
                    currentTurnRecord.addBir(position, building.getMaxIntegrity() - building.getIntegrity(), building.getOwner());
                }

                building.setIntegrity(building.getMaxIntegrity());
            }
        });

        // Capture logic - infantry only
        for (Unit unit : units_map.values()) {
            if (!unit.getOwner().equals(player)) {
                continue;
            }

            if (!isCaptureUnit(unit)) {
                continue;
            }

            Position unitPosition = unit.getPosition();
            Terrain terrain = getTerrain(unitPosition);

            if (!Terrain.isBuilding(terrain)) {
                continue;
            }

            Building building = getBuilding(unitPosition);

            // Neutral / untracked building becomes owned immediately on first infantry occupation
            if (building == null) {
                buildings.put(unitPosition, new Building(unit.getOwner(), terrain));

                if (currentTurnRecord != null) {
                    currentTurnRecord.addBir(unitPosition, 0, null);
                }

                continue;
            }

            if (!unit.getOwner().equals(building.getOwner())) {
                String previousOwner = building.getOwner();
                building.setIntegrity(building.getIntegrity() - 1);

                if (building.getIntegrity() <= 0) {
                    building.setOwner(player);
                    building.setIntegrity(building.getMaxIntegrity());

                    if (currentTurnRecord != null) {
                        currentTurnRecord.addBir(unitPosition, -1 + building.getMaxIntegrity(), previousOwner);
                    }

                    if (building.isHQ()) {
                        StartApp.updateScreen(this);
                    }
                } else {
                    if (currentTurnRecord != null) {
                        currentTurnRecord.addBir(unitPosition, -1, previousOwner);
                    }
                }
            }
        }
    }

    /**
     * @brief Add city income to one player
     *
     * @param player The player receiving income
     * @return The amount of income received
     */
    private int addIncomeForPlayer(String player) {
        int income = 0;

        for (Building building : buildings.values()) {
            if (!player.equals(building.getOwner())) {
                continue;
            }

            if (building.isCity()) {
                income += CITY_INCOME;
            }
        }

        playerWealth.put(player, playerWealth.get(player) + income);
        return income;
    }

    /**
     * @brief Count the owned HQs of one player
     * 
     * @param player The player identifier
     * @return Number of HQs owned by that player
     */
    private int getOwnedHqCount(String player) {
        int count = 0;

        for (Building building : buildings.values()) {
            if (!building.isHQ()) {
                continue;
            }

            if (player.equals(building.getOwner())) {
                count++;
            }
        }

        return count;
    }

    /**
     * @brief Find all tiles whose graph-distance from the start lies within the given interval
     * 
     * @param start The starting tile
     * @param min_range The minimum allowed distance
     * @param max_range The maximum allowed distance
     * @return Set of tiles lying in the requested range interval
     */
    private Set<Position> getTilesInRange(Position start, int min_range, int max_range) {
        // Keep track of visited tiles and their distance from the start
        Map<Position, Integer> distance = new HashMap<>();

        // BFS queue
        ArrayDeque<Position> queue = new ArrayDeque<>();

        // Result set
        Set<Position> tiles_in_range = new HashSet<>();

        // Start from the given tile
        distance.put(start, 0);
        queue.add(start);

        while (!queue.isEmpty()) {
            Position current = queue.removeFirst();
            int current_distance = distance.get(current);

            // If the current tile is in the valid interval, add it
            if (!current.equals(start)
                    && current_distance >= min_range
                    && current_distance <= max_range) {
                tiles_in_range.add(current);
            }

            // Do not expand further once we reached the maximum distance
            if (current_distance >= max_range) {
                continue;
            }

            // Expand the neighboring tiles
            for (Position neigh : getNeighborTiles(current)) {
                // Ignore tiles outside the board
                if (!isInside(neigh)) {
                    continue;
                }

                // Ignore already visited tiles
                if (distance.containsKey(neigh)) {
                    continue;
                }

                distance.put(neigh, current_distance + 1);
                queue.addLast(neigh);
            }
        }

        return tiles_in_range;
    }

    /**
     * @brief Perform an attack from one unit onto another unit
     * 
     * @param attacker_position The position of the attacking unit
     * @param target_position The position of the attacked unit
     * @return True if the attack was valid and performed, false otherwise
     */
    public boolean attackUnit(Position attacker_position, Position target_position) {
        // Do not allow attacks after victory
        if (isGameFinished()) {
            return false;
        }

        // Check that neither of the positions is null
        if (attacker_position == null || target_position == null) {
            return false;
        }

        // Check that the attacking unit even exists
        Unit attacker = this.units_map.get(attacker_position);
        if (attacker == null) {
            return false;
        }

        // Check that the attacker belongs to the current player
        if (!attacker.getOwner().equals(this.current_player)) {
            return false;
        }

        // Check that the attacker has not already finished its action this turn
        if (attacker.hasAlreadyPlayed()) {
            return false;
        }

        // Check that the target is one of the legal attackable tiles
        List<Position> attackable_tiles = getAttackableTiles(attacker_position);
        if (!attackable_tiles.contains(target_position)) {
            return false;
        }

        // Get the defender
        Unit defender = this.units_map.get(target_position);
        if (defender == null) {
            return false;
        }

        // Deal the main attack damage
        int defender_hp_before = defender.getCurrentHp();
        int attack_damage = computeAttackDamage(attacker, attacker_position, target_position);
        defender.takeDamage(attack_damage);

        // Record the damage dealt
        if (!replayMode && replay != null && replay.getCurrentTurn() != null) {
            replay.getCurrentTurn().addDamageRecord(target_position, attack_damage);
        }

        System.out.println(
                "[COMBAT] " + attacker.getUnitType().getName()
                + " attacks " + defender.getUnitType().getName()
                + " for " + attack_damage
                + " damage (" + defender_hp_before + " -> " + defender.getCurrentHp() + ")"
        );

        // If the defender has been destroyed, remove it and finish immediately
        if (defender.isDestroyed()) {
            this.units_map.remove(target_position);

            // Mark the attacking unit as already played
            attacker.setAlreadyPlayed(true);

            // Record the destroyed unit
            if (!replayMode && replay != null && replay.getCurrentTurn() != null) {
                replay.getCurrentTurn().addDestroyedUnit(defender, target_position);
            }

            notifyObservers(new GameEvent(GameEvent.Type.UNIT_DESTROYED, defender, attacker_position, target_position));

            return true;
        }

        // If the defender survived, check whether it can counterattack
        // Counterattack ignores whose turn it is, but still obeys range rules
        if (isTileAttackableByRange(target_position, attacker_position)) {
            int attacker_hp_before = attacker.getCurrentHp();
            int counter_damage = computeAttackDamage(defender, target_position, attacker_position);  
            attacker.takeDamage(counter_damage);


            // Record the damage dealt
            if (!replayMode && replay != null && replay.getCurrentTurn() != null) {
                replay.getCurrentTurn().addDamageRecord(attacker_position, counter_damage);
            }

            System.out.println(
                    "[COMBAT] " + defender.getUnitType().getName()
                    + " counterattacks " + attacker.getUnitType().getName()
                    + " for " + counter_damage
                    + " damage (" + attacker_hp_before + " -> " + attacker.getCurrentHp() + ")"
            );

            // Remove the attacker if it got destroyed by the counterattack
            if (attacker.isDestroyed()) {
                System.out.println("[COMBAT] " + attacker.getUnitType().getName() + " destroyed.");
                this.units_map.remove(attacker_position);

                // Record the destroyed unit
                if (!replayMode && replay != null && replay.getCurrentTurn() != null) {
                    replay.getCurrentTurn().addDestroyedUnit(attacker, attacker_position);
                }
            }
        }

        // Mark the attacking unit as already played
        if (this.units_map.containsKey(attacker_position)) {
            attacker.setAlreadyPlayed(true);
        }

        // TODO: Later add a dedicated attack event once the observer/game event
        //       structure for combat is expanded
        notifyObservers(new GameEvent(GameEvent.Type.UNIT_ATTACKED, attacker, attacker_position, target_position));

        return true;
    }

    /**
     * @brief Compute the combat damage done by the attacker onto the defender tile
     * 
     * @param attacker The attacking unit
     * @param attacker_position The position of the attacker
     * @param defender_position The position of the defender
     * @return The final damage after defence reduction
     */
    private int computeAttackDamage(Unit attacker, Position attacker_position, Position defender_position) {
        // Get the unit that is being attacked
        Unit defender = this.units_map.get(defender_position);
        if (defender == null) {
            return 0;
        }

        // Compute the geometric attack distance
        int distance = getTileDistance(attacker_position, defender_position);
        if (distance == Integer.MAX_VALUE) {
            return 0;
        }

        // Determine the broad target class
        TargetClass target_class = getTargetClass(defender.getUnitType());

        // Get the computed armament-based attack value
        int base_damage = attacker.getUnitType().getComputedDamageAgainst(defender.getUnitType(), distance);

        // Reduce the damage by the tile defence
        int defence_bonus = getCombinedDefenceBonus(defender_position);
        int reduced_damage = base_damage - defence_bonus * 2;

        // Always deal at least one damage if the attack was legal
        if (reduced_damage < 1) {
            reduced_damage = 1;
        }

        return reduced_damage;
    }

    /**
     * @brief Check whether the attacker at one tile can attack the target tile by range only
     * 
     * @param attacker_position The position of the attacking unit
     * @param target_position The target tile position
     * @return True if the target lies within the min-max attack range interval
     */
    private boolean isTileAttackableByRange(Position attacker_position, Position target_position) {
        // Check that there even is a unit at the attacker position
        Unit attacker = this.units_map.get(attacker_position);
        if (attacker == null) {
            return false;
        }

        // Find all tiles in the attack range
        Set<Position> tiles_in_range = getTilesInRange(
                attacker_position,
                attacker.getUnitType().getMinAttackRange(),
                attacker.getUnitType().getMaxAttackRange()
        );

        return tiles_in_range.contains(target_position);
    }

        /**
     * @brief Finish the action of the current unit without attacking
     * 
     * @param unit_position The position of the unit
     * @return True if the unit action was ended, false otherwise
     */
    public boolean finishUnitAction(Position unit_position) {
        // Do not allow actions after victory
        if (isGameFinished()) {
            return false;
        }

        // Check that the input position exists
        if (unit_position == null) {
            return false;
        }

        // Check that the unit exists
        Unit unit = this.units_map.get(unit_position);
        if (unit == null) {
            return false;
        }

        // Check that the unit belongs to the current player
        if (!unit.getOwner().equals(this.current_player)) {
            return false;
        }

        // Check that it has not already fully acted
        if (unit.hasAlreadyPlayed()) {
            return false;
        }

        unit.setAlreadyPlayed(true);

        // TODO: Later add dedicated action-end observer event
        notifyObservers(new GameEvent());

        return true;
    }

    /**
     * @brief Compute the graph-distance between two tiles on the hex map
     * 
     * This uses the same neighbor model as movement and attack range.
     * Terrain cost and occupancy are ignored here, because this is only geometric
     * attack distance, not pathfinding cost.
     * 
     * @param from Starting tile
     * @param to Target tile
     * @return The graph-distance between the tiles, or Integer.MAX_VALUE if unreachable
     */
    private int getTileDistance(Position from, Position to) {
        // Basic null protection
        if (from == null || to == null) {
            return Integer.MAX_VALUE;
        }

        // Same tile means zero distance
        if (from.equals(to)) {
            return 0;
        }

        // BFS state
        Map<Position, Integer> distance = new HashMap<>();
        ArrayDeque<Position> queue = new ArrayDeque<>();

        distance.put(from, 0);
        queue.addLast(from);

        while (!queue.isEmpty()) {
            Position current = queue.removeFirst();
            int current_distance = distance.get(current);

            // Expand all valid hex neighbors
            for (Position neigh : getNeighborTiles(current)) {
                // Ignore tiles outside the board
                if (!isInside(neigh)) {
                    continue;
                }

                // Ignore already visited tiles
                if (distance.containsKey(neigh)) {
                    continue;
                }

                int next_distance = current_distance + 1;
                distance.put(neigh, next_distance);

                // If we reached the target, return immediately
                if (neigh.equals(to)) {
                    return next_distance;
                }

                queue.addLast(neigh);
            }
        }

        // In a connected board this should normally not happen
        return Integer.MAX_VALUE;
    }

    /**
     * @brief Convert the target unit type into the broad target class
     * 
     * @param unit_type The target unit type
     * @return SOFT for infantry, HARD for vehicles
     */
    private TargetClass getTargetClass(UnitType unit_type) {
        if (unit_type == null) {
            throw new IllegalArgumentException("Target unit type cannot be null.");
        }

        return switch (unit_type.getMovementType()) {
            case INFANTRY -> TargetClass.SOFT;
            case VEHICLE -> TargetClass.HARD;
        };
    }

    /**
     * @brief At the start of the game run this method to set the ownership of initial buildings.
     *
     */
    public void setOwnership(){
        buildings.clear();

        for (Position position : units_map.keySet()) {
            Terrain terrain = getTerrain(position);
            if (!Terrain.isBuilding(terrain)) {
                continue;
            }

            buildings.put(position, new Building(units_map.get(position).getOwner(), terrain));
        }
    }

    /**
     * @brief Explicitly assign ownership of one building tile
     *
     * @param owner Owner identifier
     * @param row Building row
     * @param column Building column
     */
    public void setBuildingOwnership(String owner, int row, int column) {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Building owner cannot be empty.");
        }

        if (!"P1".equals(owner) && !"P2".equals(owner)) {
            throw new IllegalArgumentException("Unsupported building owner: " + owner);
        }

        Position position = new Position(row, column);
        if (!isInside(position)) {
            throw new IllegalArgumentException("Building ownership position is outside the map: " + position);
        }

        Terrain terrain = getTerrain(position);
        if (!Terrain.isBuilding(terrain)) {
            throw new IllegalArgumentException("Building ownership can only be assigned to building tiles: " + position);
        }

        buildings.put(position, new Building(owner, terrain));
    }

    /**
     * @brief Setter of the replay
     *
     * @param replay The new replay
     */
    public void setReplay(Replay replay) {
        this.replay = replay;
    }

    /**
     * @brief Get an owned building at a specific position
     *
     * @return A building at the desired position
     */
    public Building getBuilding(Position position){
        return buildings.get(position);
    }

    /**
     * @brief Get the replay of the current game
     *
     * @return Replay of the game
     */
    public Replay getReplay() {
        return replay;
    }

    /**
     * @brief Get the game map
     *
     * @return The game map
     */
    public GameMap getGameMap() {
        return gameMap;
    }

    /**
     * @brief Get the map of all units in the game
     *
     * @return Map of units
     */
    public Map<Position, Unit> getUnits_map() {
        return units_map;
    }

    /**
     * @brief Check if the game is in replay mode
     *
     * @return True if in replay mode
     */
    public boolean isReplayMode() {
        return replayMode;
    }

    /**
     * @brief Set the replay mode of the game
     *
     * @param replayMode new replay mode boolean
     */
    public void setReplayMode(boolean replayMode) {
        this.replayMode = replayMode;
    }

    /**
     * @brief Set the currently selected factory
     *
     * @param selectedFactory position of the currently selected factory
     */
    public void setSelectedFactory(Position selectedFactory) {
        this.selectedFactory = selectedFactory;
    }

    /**
     * @brief Check if a player can afford a specific unit
     *
     * @param player The player that should afford the unit
     * @param unitType The unit type that the player should afford
     *
     * @return true if the player can afford the unit
     */
    public boolean canAffor(String player, UnitType unitType){
        if (player == null || unitType == null) {
            return false;
        }

        Integer wealth = playerWealth.get(player);
        if (wealth == null) {
            return false;
        }

        if (!isUnitTypeAllowedForPlayer(player, unitType)) {
            return false;
        }

        return unitType.getPrice() <= wealth;
    }

    /**
     * @brief Create the unit and update the player wealth
     *
     * @param unitType The unit the player wants to buy
     *
     * @return If unit was bought successfully
     */
    public boolean buyUnit(UnitType unitType){
        if (isGameFinished()) {
            return false;
        }

        if (unitType == null) {
            return false;
        }

        if (selectedFactory == null) {
            return false;
        }

        Building building = getBuilding(selectedFactory);
        if (building == null || !building.isFactory()) {
            return false;
        }

        if (!this.current_player.equals(building.getOwner())) {
            return false;
        }

        if (!isUnitTypeAllowedForPlayer(this.current_player, unitType)) {
            return false;
        }

        if (!canAffor(this.current_player, unitType)) {
            return false;
        }

        if (getUnit(selectedFactory) != null) {
            return false;
        }

        Unit unit = new Unit(unitType, this.current_player, selectedFactory);
        unit.setMovedThisTurn(true);
        unit.setAlreadyPlayed(true);

        units_map.put(selectedFactory, unit);
        playerWealth.put(this.current_player, playerWealth.get(this.current_player) - unitType.getPrice());

        if (!replayMode && replay != null && replay.getCurrentTurn() != null) {
            replay.getCurrentTurn().addPurchasedUnit(unitType, selectedFactory);
        }

        notifyObservers(new GameEvent(GameEvent.Type.UNIT_PURCHASED, unit, null, selectedFactory));
        return true;
    }

    /**
     * @brief Get the list of all buildings
     *
     * @return List of buildings
     */
    public Map<Position, Building> getBuildings() {
        return buildings;
    }

    /**
     * @brief Set either player to be a bot
     *
     * @param player Specific player
     * @param isBot Will the player be a bot
     */
    public void setPlayerBot(String player, boolean isBot){
        if(player.equals("P1")){
            p1Bot = isBot;
        }
        else if(player.equals("P2")){
            p2Bot = isBot;
        }
    }

    /**
     * @brief Check if the game is finished
     *
     * @return True if the game is finished
     */
    private boolean isGameFinished(){
        int p1HQCount = getOwnedHqCount("P1");
        int p2HQCount = getOwnedHqCount("P2");

        return (p1HQCount > 0 && p2HQCount == 0) || (p2HQCount > 0 && p1HQCount == 0);
    }

    /**
     * @brief Get the winner or return null if game is not finished
     *
     * @return Winner player
     */
    public String getWinner(){
        int p1HQCount = getOwnedHqCount("P1");
        int p2HQCount = getOwnedHqCount("P2");

        if (p1HQCount > 0 && p2HQCount == 0) {
            return "P1";
        }

        if (p2HQCount > 0 && p1HQCount == 0) {
            return "P2";
        }

        return null;
    }
}
