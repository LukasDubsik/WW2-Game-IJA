package bot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import app.StartApp;
import javafx.application.Platform;
import model.game.Game;
import model.map.Building;
import model.map.Serializable.Position;
import model.map.Serializable.Terrain;
import model.unit.Unit;
import model.unit.UnitType;

/**
 * @class Bot
 * @brief The class handling the bot logic
 */
public class Bot {

    /**
     * @brief Initiates the bot's turn asynchronously and updates the screen afterwards.
     *
     * @param game The main game instance
     * @param gameCopy A copy of the game instance used for safe calculations
     * @param player The string identifier of the bot player
     */
    public static void makeTurn(Game game, Game gameCopy, String player){
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                try{
                    makeTurnRunnable(game, gameCopy, player);
                }
                catch (Exception e){
                    e.printStackTrace();
                    throw new RuntimeException("Error in bot logic");
                }

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                Platform.runLater(() -> {
                    game.nextTurn();
                    StartApp.updateScreen(game);
                });
            }
        });

    }

    /**
     * @brief Executes the core sequence of the bot's turn: moving units out of factories, buying new units, and playing existing units.
     *
     * @param game The main game instance
     * @param gameCopy A copy of the game instance used for safe calculations
     * @param player The string identifier of the bot player
     */
    private static void makeTurnRunnable(Game game, Game gameCopy, String player){
        Map<Position, Building> ownedBuildings = ownedBuildings(gameCopy, player);
        Map<Position, Unit> ownedUnits = ownedUnits(gameCopy, player);
        int playerWealth = getPlayerWealth(gameCopy, player);
        int income = getIncome(ownedBuildings);
        Map<Position, Building> factories = getFactories(ownedBuildings);

        pushUnitsFromFactory(game, gameCopy, ownedUnits, factories);
        buyUnits(game, gameCopy, player, playerWealth, income, factories);
        playUnits(game, gameCopy, player, ownedUnits);
    }

    /**
     * @brief Iterates through all owned units and decides their optimal moves and attacks.
     *
     * @param game The main game instance
     * @param gameCopy A copy of the game instance used for safe calculations
     * @param player The string identifier of the bot player
     * @param ownedUnits A map of positions and units currently owned by the player
     */
    private static void playUnits(Game game, Game gameCopy, String player, Map<Position, Unit> ownedUnits){

        for(Position position : ownedUnits.keySet()){
            Unit unit = ownedUnits.get(position);

            if(unit.hasAlreadyPlayed())
                continue;

            List<Position> reachableTiles = gameCopy.getReachableTiles(position);
            
            Position move = getPriorityMove(gameCopy, player, unit, reachableTiles);
            if(move == null)
                continue;

            moveUnit(game, unit, move);
            attackUnits(game, gameCopy, unit);
        }
    }

    /**
     * @brief Commands a unit to attack the first available target in its attackable tiles.
     *
     * @param game The main game instance
     * @param gameCopy A copy of the game instance used for safe calculations
     * @param unit The unit performing the attack
     */
    private static void attackUnits(Game game, Game gameCopy, Unit unit){
        List<Position> attackableTiles = gameCopy.getAttackableTiles(unit.getPosition());
        if(attackableTiles.size() != 0){
            Platform.runLater(() -> {
                game.attackUnit(unit.getPosition(), attackableTiles.get(0));
            });
        }
    }

    /**
     * @brief Determines the most strategic position for a unit to move to based on priorities (factories, cities, enemies).
     *
     * @param gameCopy A copy of the game instance used for safe calculations
     * @param player The string identifier of the bot player
     * @param unit The unit to be moved
     * @param reachableTiles A list of positions the unit can move to
     *
     * @return The priority target Position, or null if no valid move is found
     */
    private static Position getPriorityMove(Game gameCopy, String player, Unit unit, List<Position> reachableTiles){
        Map<Position, Terrain> unownedBuildings = getUnownedBuildings(gameCopy);
        Map<Position, Unit> enemyUnits = getEnemyUnits(gameCopy, player);
        Map<Position, Building> enemyBuildings = getEnemyBuildings(gameCopy, player);

        Position factoryPos = isBuildingInReach(reachableTiles, unownedBuildings, Terrain.FACTORY);
        if(factoryPos != null)
            return factoryPos;

        Position cityPos = isBuildingInReach(reachableTiles, unownedBuildings, Terrain.CITY);
        if(cityPos != null)
            return cityPos;

        if(unownedBuildings.size() != 0){
            Position closestUnownedBuilding = getTarget(unit.getPosition(), reachableTiles, unownedBuildings.keySet().stream().toList());
            if(closestUnownedBuilding != null){
                return closestUnownedBuilding;
            }
        }

        if(enemyUnits.size() < 15){
            Position closestEnemyBuilding = getTarget(unit.getPosition(), reachableTiles, enemyBuildings.keySet().stream().toList());
            if(closestEnemyBuilding != null){
                return closestEnemyBuilding;
            }
        }

        if(enemyUnits.size() != 0){
            Position closestEnemyUnit = getTarget(unit.getPosition(), reachableTiles, enemyUnits.keySet().stream().toList());
            if(closestEnemyUnit != null)
                return closestEnemyUnit;
        }

        return null;
    }

    /**
     * @brief Finds the closest reachable tile to the closest desired target.
     *
     * @param unitPosition The current position of the unit
     * @param reachableTiles A list of positions the unit can move to
     * @param targetList A list of desired target positions
     *
     * @return The best reachable Position to advance towards the target, or null if not found
     */
    private static Position getTarget(Position unitPosition, List<Position> reachableTiles, List<Position> targetList){
        Position closestEnemyBuilding = getClosestPosition(unitPosition, targetList);
        if(closestEnemyBuilding != null){
            return getClosestPosition(closestEnemyBuilding, reachableTiles);
        }
        return null;
    }

    /**
     * @brief Retrieves all units not owned by the specified player.
     *
     * @param gameCopy A copy of the game instance used for safe calculations
     * @param player The string identifier of the bot player
     *
     * @return A map mapping positions to enemy units
     */
    private static Map<Position, Unit> getEnemyUnits(Game gameCopy, String player){
        Map<Position, Unit> enemyUnits = new HashMap<>();
        Map<Position, Unit> allUnits = new HashMap<>(gameCopy.getUnits_map());

        for(Position position : allUnits.keySet()){
            Unit unit = allUnits.get(position);
            if(!unit.getOwner().equals(player))
                enemyUnits.put(position, unit);
        }
        return enemyUnits;
    }

    /**
     * @brief Retrieves all buildings not owned by the specified player.
     *
     * @param gameCopy A copy of the game instance used for safe calculations
     * @param player The string identifier of the bot player
     *
     * @return A map mapping positions to enemy buildings
     */
    private static Map<Position, Building> getEnemyBuildings(Game gameCopy, String player){
        Map<Position, Building> enemyBuildings = new HashMap<>();
        Map<Position, Building> allBuildings = gameCopy.getBuildings();

        for(Position position : allBuildings.keySet()){
            Building building = allBuildings.get(position);
            if(!building.getOwner().equals(player))
                enemyBuildings.put(position, building);
        }

        return enemyBuildings;
    }

    /**
     * @brief Finds the closest position from a list of positions relative to a given starting position.
     *
     * @param checkPos The starting position to measure distance from
     * @param positions A list of positions to evaluate
     *
     * @return The closest Position, or null if the list is empty
     */
    private static Position getClosestPosition(Position checkPos, List<Position> positions){
        Position result = null;
        double min = Double.MAX_VALUE;
        for(Position position : positions){
            double distance = getEuclideanDistance(checkPos, position);
            if(distance < min){
                min = distance;
                result = position;
            }
        }
        return result;
    }

    /**
     * @brief Checks if a specific type of terrain building is within a list of reachable tiles.
     *
     * @param reachableTiles A list of positions the unit can move to
     * @param buildings A map of positions and their respective terrain types
     * @param building The specific terrain building type to look for
     *
     * @return The Position of the building if in reach, or null otherwise
     */
    private static Position isBuildingInReach(List<Position> reachableTiles, Map<Position, Terrain> buildings, Terrain building){
        for(Position position : reachableTiles){
            if(buildings.get(position) == building)
                return position;
        }

        return null;
    }

    /**
     * @brief Purchases units in empty factories based on affordability and available wealth.
     *
     * @param game The main game instance
     * @param gameCopy A copy of the game instance used for safe calculations
     * @param player The string identifier of the bot player
     * @param playerWealth The current wealth of the bot player
     * @param income The projected income of the bot player
     * @param factories A map of owned factories
     */
    private static void buyUnits(Game game, Game gameCopy, String player, int playerWealth, int income, Map<Position, Building> factories){
        List<UnitType> availableUnits = getAvailableUnits(player);

        for(Position position : factories.keySet()){
            if(gameCopy.getUnit(position) != null)
                continue;

            List<UnitType> affordableUnits = getAffordableUnits(playerWealth, availableUnits);

            buyRandomUnit(game, playerWealth, income, position, affordableUnits);
        }
    }

    /**
     * @brief Selects and purchases a random unit from affordable options using weighted probabilities.
     *
     * @param game The main game instance
     * @param playerWealth The current wealth of the bot player
     * @param income The projected income of the bot player
     * @param factoryPos The position of the factory where the unit will be bought
     * @param affordableUnits A list of unit types the player can afford
     */
    private static void buyRandomUnit(Game game, int playerWealth, int income, Position factoryPos, List<UnitType> affordableUnits){
        if(affordableUnits.size() == 0)
            return;

        int maxAffordableCost = Integer.MIN_VALUE;
        for(UnitType unitType : affordableUnits){
            if(unitType.getPrice() > maxAffordableCost)
                maxAffordableCost = unitType.getPrice();
        }

        double effectiveFund = playerWealth + (income * 0.5d);
        double multiplier = effectiveFund / maxAffordableCost;

        multiplier = Math.max(multiplier, 5.0);

        List<Double> weights = new ArrayList<>();
        for(UnitType unitType : affordableUnits){
            weights.addLast(Math.pow(unitType.getPrice(), multiplier));
        }

        UnitType randomUnit = getRandomUnit(affordableUnits, weights);
        if(randomUnit == null)
            return;

        Platform.runLater(() -> {
            game.setSelectedFactory(factoryPos);
            game.buyUnit(randomUnit);
        });

    }

    /**
     * @brief Picks a random unit from a list based on a provided weight distribution.
     *
     * @param affordableUnits A list of unit types
     * @param weights A list of numerical weights corresponding to each unit type
     *
     * @return The randomly selected UnitType, or null if none selected
     */
    private static UnitType getRandomUnit(List<UnitType> affordableUnits, List<Double> weights){
        double weightSum = 0;
        for(double weight : weights){
            weightSum += weight;
        }

        double currentDistribution = 0;
        List<Double> weightDistribution = new ArrayList<>();
        for(double weight : weights){
            double distribution = weight / weightSum;
            weightDistribution.addLast(distribution + currentDistribution);
            currentDistribution += distribution;
        }

        double random = Math.random();

        for(int i = 0; i < weightDistribution.size(); i++){
            double weight = weightDistribution.get(i);
            if(random < weight){
                if(i < affordableUnits.size())
                    return affordableUnits.get(i);
                else
                    throw new RuntimeException("Bot failed to buy units");
            }
        }

        return null;
    }

    /**
     * @brief Clears factories by moving existing units out so new ones can be produced.
     *
     * @param game The main game instance
     * @param gameCopy A copy of the game instance used for safe calculations
     * @param ownedUnits A map of all owned units
     * @param factories A map of all owned factories
     */
    private static void pushUnitsFromFactory(Game game, Game gameCopy, Map<Position, Unit> ownedUnits, Map<Position, Building> factories){
        for(Position position : factories.keySet()){
            if(ownedUnits.get(position) != null){
                pushUnitFromFactory(game, gameCopy, ownedUnits.get(position));
            }
        }
    }

    /**
     * @brief Moves a single unit off a factory tile to its furthest reachable position.
     *
     * @param game The main game instance
     * @param gameCopy A copy of the game instance used for safe calculations
     * @param unit The unit to move off the factory
     */
    private static void pushUnitFromFactory(Game game, Game gameCopy, Unit unit){
        Position furthestPosition = getFurthestPosition(gameCopy, unit);
        if(furthestPosition != null)
            moveUnit(game, unit, furthestPosition);
    }

    /**
     * @brief Calculates the furthest reachable position for a given unit.
     *
     * @param gameCopy A copy of the game instance used for safe calculations
     * @param unit The unit to calculate distance for
     *
     * @return The furthest reachable Position
     */
    private static Position getFurthestPosition(Game gameCopy, Unit unit){
        List<Position> tiles = gameCopy.getReachableTiles(unit.getPosition());
        List<Double> distances = new ArrayList<>();
        for(int i = 0; i < tiles.size(); i++){
            double distance = getEuclideanDistance(unit.getPosition(), tiles.get(i));
            distances.add(i, distance);
        }

        double max = Double.MIN_VALUE;
        int index = -1;
        for(int i = 0; i < distances.size(); i++){
            if(distances.get(i) > max){
                max = distances.get(i);
                index = i;
            }
        }
        if(index == -1)
            return null;
        return tiles.get(index);
    }

    /**
     * @brief Computes the straight-line distance between two positions.
     *
     * @param pos1 The first position
     * @param pos2 The second position
     *
     * @return The Euclidean distance as a double
     */
    private static double getEuclideanDistance(Position pos1, Position pos2){
        return Math.sqrt(Math.pow(pos2.row() - pos1.row(), 2) + Math.pow(pos2.column() - pos1.column(), 2));
    }

    /**
     * @brief Retrieves all buildings owned by a specific player.
     *
     * @param gameCopy A copy of the game instance used for safe calculations
     * @param player The string identifier of the player
     *
     * @return A map mapping positions to buildings owned by the player
     */
    private static Map<Position, Building> ownedBuildings(Game gameCopy, String player){
        Map<Position, Building> result = new HashMap<>();
        Map<Position, Building> allBuildings = gameCopy.getBuildings();

        for(Position position : allBuildings.keySet()){
            Building building = allBuildings.get(position);
            if(building.getOwner().equals(player))
                result.put(position, building);
        }
        return result;
    }

    /**
     * @brief Retrieves all units owned by a specific player.
     *
     * @param gameCopy A copy of the game instance used for safe calculations
     * @param player The string identifier of the player
     *
     * @return A map mapping positions to units owned by the player
     */
    private static Map<Position, Unit> ownedUnits(Game gameCopy, String player){
        Map<Position, Unit> result = new HashMap<>();
        Map<Position, Unit> allUnits = gameCopy.getUnits_map();

        for(Position position : allUnits.keySet()){
            Unit unit = allUnits.get(position);
            if(unit.getOwner().equals(player))
                result.put(position, unit);
        }
        return result;
    }

    /**
     * @brief Gets the current wealth of a specific player.
     *
     * @param gameCopy A copy of the game instance used for safe calculations
     * @param player The string identifier of the player
     *
     * @return The integer amount of wealth the player holds
     */
    private static int getPlayerWealth(Game gameCopy, String player){
        return gameCopy.getPlayerWealth().get(player);
    }

    /**
     * @brief Calculates the total projected income from a given set of buildings.
     *
     * @param buildings A map of buildings to calculate income from
     *
     * @return The total income integer value
     */
    private static int getIncome(Map<Position, Building> buildings){
        int income = 0;
        for(Building building : buildings.values()){
            if(building.isCity())
                income += 1000;
        }
        return income;
    }

    /**
     * @brief Scans the map and retrieves all terrain positions that are buildings but currently unowned.
     *
     * @param gameCopy A copy of the game instance used for safe calculations
     *
     * @return A map mapping positions to unowned building terrains
     */
    private static Map<Position, Terrain> getUnownedBuildings(Game gameCopy){
        Map<Position, Terrain> result = new HashMap<>();
        Map<Position, Building> ownedBuildings = gameCopy.getBuildings();

        for(int i = 0; i < gameCopy.getRows(); i++){
            for(int j = 0; j < gameCopy.getColumns(); j++) {
                Position position = new Position(i, j);
                if(Terrain.isBuilding(gameCopy.getTerrain(position)) && ownedBuildings.get(position) == null){
                    result.put(position, gameCopy.getTerrain(position));
                }
            }
        }

        return result;
    }

    /**
     * @brief Filters a map of buildings to return only the ones that are factories.
     *
     * @param buildings A map of positions to building objects
     *
     * @return A map containing only the factory buildings
     */
    private static Map<Position, Building> getFactories(Map<Position, Building> buildings){
        Map<Position, Building> factories = new HashMap<>();

        for(Position position : buildings.keySet()){
            Building building = buildings.get(position);
            if(building.isFactory()){
                factories.put(position, building);
            }
        }
        return factories;
    }

    /**
     * @brief Commands the game to physically move a unit to a target position.
     *
     * @param game The main game instance
     * @param unit The unit to be moved
     * @param position The destination position
     */
    private static void moveUnit(Game game, Unit unit, Position position){
        Platform.runLater(() -> {
            game.moveUnit(unit.getPosition(), position, false);
        });
    }

    /**
     * @brief Retrieves a faction-specific list of available unit types to buy.
     *
     * @param player The string identifier of the player ("P1" or "P2")
     *
     * @return A list of UnitType objects available to that player
     */
    private static List<UnitType> getAvailableUnits(String player){
        return new ArrayList<>(UnitType.getUnitsForPlayer(player));
    }

    /**
     * @brief Filters a list of available units, returning only those that the player can currently afford.
     *
     * @param playerWealth The current wealth of the player
     * @param availableUnits The list of all unit types available to the player's faction
     *
     * @return A list of UnitType objects that cost less than or equal to the player's wealth
     */
    private static List<UnitType> getAffordableUnits(int playerWealth, List<UnitType> availableUnits){
        List<UnitType> affordableUnits = new ArrayList<>();
        for(UnitType unitType : availableUnits){
            if(unitType.getPrice() <= playerWealth)
                affordableUnits.add(unitType);
        }
        return affordableUnits;
    }
}
