package model.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import model.map.Position;
import model.map.Terrain;
import model.unit.Unit;
import model.unit.UnitType;

/**
 * @class Game
 * @brief The main class holding values of teh current game
 */
public class Game {

    // The values held by the class
    private final Terrain[][] map; ///< The map of the game
    private final Map<Position, Unit> units_map = new HashMap<>(); ///< Position to unit

    private final int rows; ///< Number of rows of the map
    private final int columns; ///< Number of ciolumns of the map

    private final List<GameObserver> observers = new ArrayList<>();
    
    /**
     * @brief The constructor of the Game class
     * 
     * @param map_ The array map - already converted to the Terrain enum
     */
    Game(Terrain[][] map_) {
        // Make sure the games are independent by copying the input map
        this.map = copyInputMap(map_);
        // Acquire some values for easier future analysis
        this.rows = map.length;
        this.columns = map[0].length;
    }

    /**
     * @brief Get the number of columns in the game
     * 
     * @return The number of the columns.
     */
    public int getColumns() {
        return this.columns;
    }

    /**
     * @brief Get the number of rows in the game
     * 
     * @return The number of the rows.
     */
    public int getRows() {
        return this.rows;
    }

    /**
     * @brief Get the terrain at the position
     * 
     * @param pos The position where to get the terrain from
     * 
     * @return The terrain at the position
     */
    public Terrain getTerrain(Position pos) {
        return map[pos.row()][pos.column()];
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
     * @brief Move the unit from position to the other
     * 
     * @param from The position to move from
     * @param to The position to move to
     * 
     * @return True if movement proceeded, false otherwise
     */
    public boolean moveUnit(Position from, Position to) {
        // Check that the position s for moving are valid
        if (from == null || to == null) {
            return false;
        }

        // Check at other possibilities

        // Can't move outside of teh board
        if (!isInside(to)) {
            return false;
        }

        // Can't start from outside the border
        if (!isInside(from)) {
            return false;
        }

        // Get the unit at the position
        Unit unit = this.units_map.get(from);
        
        // Check that there is any unit at the position even
        if (unit == null) {
            return false;
        }

        // Can move in the same position -> like artillery
        if (from.equals(to)) {
            notifyObservers(new GameEvent());
            return true;
        }

        // Can't move to an already occupied place
        if (units_map.containsKey(to)) {
            return false;
        }

        // Can only move to where it can access
        if (!getReachableTiles(from).contains(to)) {
            return false;
        }

        // Then we can move
        units_map.remove(from);
        unit.setPosition(to);
        units_map.put(to, unit);

        // Notify the observers
        notifyObservers(new GameEvent());

        return true;
    }

    /**
     * @brief Given the current position, perform weighted pathfinding and find all the reachbale positions viable
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

        // Initialize the values for search
        // The map of the reachable positions with the best cost possible
        Map<Position, Integer> best = new HashMap<>();
        // The search queu to explore -> Djikstra
        PriorityQueue<SearchNode> frontier = new PriorityQueue<>(new SearchNodeComparator());
        // The final set of reachable tiles
        Set<Position> reachable = new HashSet<>();

        // Start the frontier of expansion with the starting point
        // The score is zero as no cost to get where it is already there
        frontier.add(new SearchNode(pos, 0));

        // Add to the best cost map (since the start will always be the best -> can't go below zero)
        best.put(pos, 0);

        // Iterate until there are things to explore
        while (!frontier.isEmpty()) {
            // Pop the current node
            SearchNode current_node = frontier.poll();
            // Pop the node value that can be in the best found
            Integer best_so_far = best.get(current_node.pos);

            // If it is worse, break, we have already a better option
            if (best_so_far != null && current_node.score > best_so_far) {
                continue;
            }

            // Create a list of neighbors then, this node is worth exploring
            List<Position> neighbors = new ArrayList<>();
            // Add the individual neighbors
            neighbors.add(new Position(current_node.pos.row() - 1, current_node.pos.column()));
            neighbors.add(new Position(current_node.pos.row() + 1, current_node.pos.column()));
            neighbors.add(new Position(current_node.pos.row(), current_node.pos.column() - 1));
            neighbors.add(new Position(current_node.pos.row(), current_node.pos.column() + 1));

            // Then for each of the neighbors, compute cost
            for (Position neigh : neighbors) {
                // Check if it is inside the bounds - if not, skip
                if (!isInside(neigh)) {
                    continue;
                }

                // Get the current occupant
                Unit occupant = units_map.get(neigh);
                // Check if truly occupied
                boolean occupied = occupant != null;

                // If occupied by an enemy, skip, blocking a movement
                if (occupied && !occupant.getOwner().equals(unit.getOwner())) {
                    continue;
                }

                // Then compute a new value
                int move_score = unit.getUnitType().getMovementCost(getTerrainAtPosition(neigh));
                // Ignore infinite values -> Water
                if (move_score == Integer.MAX_VALUE) {
                    continue;
                }

                // The full score of movement
                int neigh_score = move_score + current_node.score;

                // If the value is larger thaan the unit can covcer, ignore
                if (neigh_score > unit.getUnitType().getMovement()) {
                    continue;
                }

                // Compare with the old best
                Integer curr_score = best.get(neigh);
                if (curr_score == null || neigh_score < curr_score) {
                    // Update the values
                    best.put(neigh, neigh_score);
                    frontier.add(new SearchNode(neigh, neigh_score));

                    // Add only those places that is not the origin -> tehnicaly can't reach where I already am
                    // Friendly tiles are pass through, but not viable end positions
                    if (!neigh.equals(pos) && !occupied) {
                        reachable.add(neigh);
                    }
                }
            }
        }

        // Return the results
        List<Position> result = new ArrayList<>(reachable);
        // Sort by closest position first
        Collections.sort(result, new PositionComparator());
        return result;
    }

    /**
     * @brief Check if the Positional node is within the map
     * 
     * @param pos The position we are checking for
     * 
     * @return Returns true if inside, false if outside
     */
    private boolean isInside(Position pos) {
        boolean row_check = pos.row() >= 0 && pos.row() < rows;
        boolean column_check = pos.column() >= 0 && pos.column() < columns;
        return row_check && column_check;
    }

    /**
     * @brief Get the terrain at the map's position
     * 
     * @param pos The position to which to get the terrain from
     * 
     * @return The terrain at the position.
     */
    private Terrain getTerrainAtPosition(Position pos) {
        return map[pos.row()][pos.column()];
    }

    /**
     * @brief Copies the incoming map into a new form to make sure the games stay independent
     * 
     * @param map_in The input map of Terrain enums
     * @return The copied map of the same type and form
     */
    private static Terrain[][] copyInputMap(Terrain[][] map_in) {
        // Create an empty holder of the same size
        Terrain[][] copy = new Terrain[map_in.length][];

        // Copy element by element
        for (int i = 0; i < map_in.length; i++) {
            copy[i] = Arrays.copyOf(map_in[i], map_in[i].length);
        }

        // Return the copied form
        return copy;
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
}
