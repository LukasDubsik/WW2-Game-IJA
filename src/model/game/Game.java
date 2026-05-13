package model.game;

import java.util.*;

import model.map.*;
import model.map.Serializable.GameMap;
import model.map.Serializable.Overlay;
import model.map.Serializable.Terrain;
import model.unit.TargetClass;
import model.unit.Unit;
import model.unit.UnitType;
import replay.*;

/**
 * @class Game
 * @brief The main class holding values of the current game
 */
public class Game {

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
    
    /**
     * @brief The constructor of the Game class
     * 
     * @param map The array map - already converted to the Terrain enum
     * @param overlay The map of all overlays
     * @param players List of all players
     */
    public Game(Terrain[][] map, Overlay[][] overlay, String[] players) {
        this.gameMap = new GameMap(map, overlay);

        // Set each player's wealth to 0
        for(String player : players)
            playerWealth.put(player, 0);

        replayMode = false;
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
        for(Unit unit : units_map.values()){
            unit.setCurrentHp(unit.getUnitType().getMaxHP());
        }

        //Set each player's wealth to 0
        for(String player : players)
            playerWealth.put(player, 0);

        // Loaded replay is running in replay mode
        replayMode = true;

        // Set the initial ownership of buildings
        setOwnership();
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
        if (owner == null) {
            throw new IllegalArgumentException("Owner can't be empty!");
        }

        // Get the unit values based on its type
        UnitType unit_type = UnitType.convert(type);
        // Set its position
        Position position = new Position(row, column);

        // Check that the position is valid
        if (!isInside(position)) {
            throw new IllegalArgumentException("Position is outside the game!");
        }

        // Check that the position is not yet occupied
        // If the position has key (something is "standing" on it)
        if (units_map.containsKey(position)) {
            throw new IllegalArgumentException("Map position is already occupied");
        }

        // Create the unit if all passes
        Unit unit = new Unit(unit_type, owner, position);
        // Set the unit position within the game
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

        // Basic null protection
        if (from == null || to == null) {
            return false;
        }

        // Check that there is any unit at the position even
        Unit unit = this.units_map.get(from);
        if (!ignoreChecks && unit == null) {
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

        // Notify that the game state changed
        notifyObservers(new GameEvent());

        return true;
    }

    /**
     * @brief Given the current position, perform weighted pathfinding and find all the reachable positions viable
     * 
     * @param pos The position of the unit
     */
    public List<Position> getReachableTiles(Position pos) {
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

        boolean wasReplaying = replayMode;

        // If running replay mode get the turn from the replay record
        if(replayMode) {
            TurnRecord turnRecord = replay.getCurrentTurn();
            if(turnRecord != null) {

                // Revert the moves made in the replay
                revertMoves(turnRecord, false);

                // Revert the damage dealt in the replay
                revertDamage(turnRecord, false);

                // Revert units purchased
                revertPurchasedUnits(turnRecord, false);

                replay.advanceTurn();
            }
            replayMode = !replay.isAtEnd();
        }

        // Switch the active player
        this.current_player = swapPlayers(this.current_player);

        // Advance the turn counter
        this.current_turn++;

        for (Unit unit : units_map.values()) {
            if (unit.getOwner().equals(this.current_player)) {
                unit.setAlreadyPlayed(false);
                unit.setMovedThisTurn(false);
            }
        }

        // Reset capture progress
        buildings.forEach((position, building) -> {
            if(building.isFull())
                return;

            Unit unit = getUnit(position);
            if (unit == null){;
                replay.getCurrentTurn().addBir(position, building.getMaxIntegrity() - building.getIntegrity(), building.getOwner());
                building.setIntegrity(building.getMaxIntegrity());
            }
            else if(unit.getOwner().equals(building.getOwner())){
                replay.getCurrentTurn().addBir(position, building.getMaxIntegrity() - building.getIntegrity(), building.getOwner());
                building.setIntegrity(building.getMaxIntegrity());
            }
        });

        // Capture logic
        for (Unit unit : units_map.values()) {
            if(!unit.getOwner().equals(this.current_player))
                continue;

            Building building = getBuilding(unit.getPosition());
            if(building == null){
                Terrain terrain = getTerrain(unit.getPosition());
                if(!Terrain.isBuilding(terrain))
                    continue;

                buildings.put(unit.getPosition(), new Building(unit.getOwner(), terrain));
                replay.getCurrentTurn().addBir(unit.getPosition(), 0, null);

                continue;
            }

            if(!unit.getOwner().equals(building.getOwner())){
                building.setIntegrity(building.getIntegrity() - 1);

                if(building.getIntegrity() == 0){
                    building.setOwner(this.current_player);
                    building.setIntegrity(3);
                    replay.getCurrentTurn().addBir(unit.getPosition(), -1, swapPlayers(this.current_player));
                }
                else
                    replay.getCurrentTurn().addBir(unit.getPosition(), -1, this.current_player);
            }
        }

        // Income logic
        int prevWealth = playerWealth.get(swapPlayers(this.current_player));
        for(Building building : buildings.values()){
            if(building.getOwner().equals(this.current_player))
                continue;

            if(building.isCity()){
                Integer currentWealth = playerWealth.get(building.getOwner());
                playerWealth.put(building.getOwner(), currentWealth + 1000);
            }
        }
        if(!wasReplaying)
            replay.addNextTurn(playerWealth.get(swapPlayers(this.current_player)) - prevWealth);

        // TODO: Later, at turn start, also handle:
        //       - repairs
        //       - artillery move/attack state if separated

        // Notify the observers that the game changed
        notifyObservers(new GameEvent());
    }

    public void prevTurn(){

        // Revert any moves made in an unfinished turn
        if (!replayMode) {
            TurnRecord currentTurnRecord = replay.getCurrentTurn();
            if (currentTurnRecord != null &&
                    (!currentTurnRecord.getMoves().isEmpty() ||
                     !currentTurnRecord.getDamageList().isEmpty() ||
                     !currentTurnRecord.getUnitsDestroyed().isEmpty()))
            {

                // Restore all destroyed units in that turn
                revertDestroedUnits(currentTurnRecord);
                currentTurnRecord.getUnitsDestroyed().clear();

                // Restore all damage dealt in current turn
                revertDamage(currentTurnRecord, true);
                currentTurnRecord.getDamageList().clear();

                // Revert purchased units
                revertPurchasedUnits(currentTurnRecord, true);
                currentTurnRecord.getUnitsPurchased().clear();

                // Restore all moves in current turn
                revertMoves(currentTurnRecord, true);
                currentTurnRecord.getMoves().clear();
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

        replayMode = true;

        // Get the record of the reverted turn
        TurnRecord turnRecord = replay.goBackward();
        if(turnRecord == null) {
            notifyObservers(new GameEvent());
            return;
        }

        // Restore all destroyed units
        revertDestroedUnits(turnRecord);

        // Restore all damage dealt that turn
        revertDamage(turnRecord, true);

        // Revert purchased units
        revertPurchasedUnits(turnRecord, true);

        // Revert the moves
        revertMoves(turnRecord, true);

        // Revert the building changes
        revertBuildingChanges(turnRecord);

        // Restore the previouse player wealth
        revertPlayerWealth(turnRecord);

        // Notify the observers that the game changed
        notifyObservers(new GameEvent());
    }

    /**
     * @brief Revert the moves done that turn
     *
     * @param turnRecord Turn record to revert
     * @param revert revert or advance
     */
    private void revertMoves(TurnRecord turnRecord, boolean revert){
        if(revert){
            ArrayList<MoveRecord> moves = turnRecord.getMoves();

            for (int i = moves.size() - 1; i >= 0; i--) {
                MoveRecord moveRecord = moves.get(i);
                Unit unit = getUnit(moveRecord.pos2());
                if(unit == null)
                    throw new RuntimeException("Replay is corrupted " + moveRecord.pos2().toString());

                moveUnit(moveRecord.pos2(), moveRecord.pos1(), true);
                unit.setMovedThisTurn(false);
                unit.setAlreadyPlayed(false);
            }
        }
        else{
            for (MoveRecord moveRecord : turnRecord.getMoves()) {
                Unit unit = getUnit(moveRecord.pos1());
                if(unit == null)
                    throw new RuntimeException("Replay is corrupted " + moveRecord.pos1().toString());

                unit.setMovedThisTurn(false);
                unit.setAlreadyPlayed(false);
                moveUnit(moveRecord.pos1(), moveRecord.pos2(), true);
            }
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
     * @brief Revert the destroyed units that turn
     *
     * @param turnRecord Turn record to revert
     */
    private void revertDestroedUnits(TurnRecord turnRecord){
        ArrayList<Unit> destroyedUnits = turnRecord.getUnitsDestroyed();

        for(int i = destroyedUnits.size() - 1; i >= 0; i--){
            Unit unit = destroyedUnits.get(i);
            units_map.put(unit.getPosition(), unit);
        }
    }

    /**
     * @brief Revert the damage dealt that turn
     *
     * @param turnRecord Turn record to revert
     * @param revert revert or advance
     */
    private void revertDamage(TurnRecord turnRecord, boolean revert){
        ArrayList<DamageRecord> damageList = turnRecord.getDamageList();
        if (revert) {
            for(int i = damageList.size() - 1; i >= 0; i--){
                DamageRecord damageRecord = damageList.get(i);
                Unit unit = getUnit(damageRecord.position());

                if(unit == null)
                    throw new RuntimeException("Replay is corrupted at " + damageRecord.position().toString());

                unit.setCurrentHp(unit.getCurrentHp() + damageRecord.damage());
            }
        } else {
            for(int i = 0; i < damageList.size(); i++){
                DamageRecord damageRecord = damageList.get(i);
                Unit unit = getUnit(damageRecord.position());

                if(unit == null)
                    throw new RuntimeException("Replay is corrupted at " + damageRecord.position().toString());

                unit.takeDamage(damageRecord.damage());
                if(unit.isDestroyed())
                    this.units_map.remove(unit.getPosition());
            }
        }
    }

    /**
     * @brief Revert the units purchased that turn
     *
     * @param turnRecord Turn record to revert
     * @param revert revert or advance
     */
    private void revertPurchasedUnits(TurnRecord turnRecord, boolean revert){
        ArrayList<UnitPurchaseRecord> unitsPurchased = turnRecord.getUnitsPurchased();
        for(int i = unitsPurchased.size() - 1; i >= 0; i--){
            UnitPurchaseRecord unitPurchased = unitsPurchased.get(i);

            if(revert) {
                units_map.remove(unitPurchased.position());
                int newWealth = playerWealth.get(this.current_player) + unitPurchased.unitType().getPrice();
                playerWealth.put(this.current_player, newWealth);
            }
            else{
                selectedFactory = unitPurchased.position();
                buyUnit(unitPurchased.unitType());
            }
        }
    }

    /**
     * @brief Revert the player wealth
     *
     * @param turnRecord Turn record to revert
     */
    private void revertPlayerWealth(TurnRecord turnRecord){
        int currentWealth = playerWealth.get(current_player);
        playerWealth.put(current_player, currentWealth - turnRecord.getIncome());
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
        replay.getCurrentTurn().addDamageRecord(defender.getPosition(), attack_damage);

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
            replay.getCurrentTurn().addDestroyedUnit(defender);

            // TODO: Later add a dedicated attack event once the observer/game event
            //       structure for combat is expanded
            notifyObservers(new GameEvent());

            return true;
        }

        // If the defender survived, check whether it can counterattack
        // Counterattack ignores whose turn it is, but still obeys range rules
        if (isTileAttackableByRange(target_position, attacker_position)) {
            int attacker_hp_before = attacker.getCurrentHp();
            int counter_damage = computeAttackDamage(defender, target_position, attacker_position);  
            attacker.takeDamage(counter_damage);


            // Record the damage dealt
            replay.getCurrentTurn().addDamageRecord(attacker.getPosition(), counter_damage);

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
                replay.getCurrentTurn().addDestroyedUnit(attacker);
            }
        }

        // Mark the attacking unit as already played
        if (this.units_map.containsKey(attacker_position)) {
            attacker.setAlreadyPlayed(true);
        }

        // TODO: Later add a dedicated attack event once the observer/game event
        //       structure for combat is expanded
        notifyObservers(new GameEvent());

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
        for(Unit unit : units_map.values()){
            Terrain terrain = getTerrain(unit.getPosition());
            if(!Terrain.isBuilding(terrain))
                continue;

            buildings.put(unit.getPosition(), new Building(unit.getOwner(), terrain));
        }
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
        return unitType.getPrice() <= playerWealth.get(player);
    }

    /**
     * @brief Create the unit and update the player wealth
     *
     * @param unitType The unit the player wants to buy
     */
    public void buyUnit(UnitType unitType){

        if(!canAffor(this.current_player, unitType))
            return;

        if(getUnit(selectedFactory) != null)
            return;

        Unit unit = new Unit(unitType, this.current_player, selectedFactory);
        unit.setMovedThisTurn(true);
        unit.setAlreadyPlayed(true);

        units_map.put(selectedFactory, unit);
        playerWealth.put(this.current_player, playerWealth.get(this.current_player) - unitType.getPrice());

        replay.getCurrentTurn().addPurchasedUnit(unitType, selectedFactory);
    }
}
