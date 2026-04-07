package game;

/**
 * @enum Terrain
 * @brief The individual Terrain types of the game
 */
public enum Terrain {
    // List of the types and their names of the Terrain and values
    // Goes as: NAME, DEFENCE_BONUS, INFANTRY_COST, VEHICLE_COST
    PLAIN("P", 1, 1, 1),
    FOREST("F", 2, 1, 2),
    MOUNTAIN("M", 4, 2, Integer.MAX_VALUE),
    WATER("W", 0, Integer.MAX_VALUE, Integer.MAX_VALUE),
    CITY("C", 3, 1, 1),
    FACTORY("T", 3, 1, 1),
    HQ("H", 4, 1, 1);

    private String val; ///< Holder for the String value of the enum
    private int defence_bonus; ///< Value for defence
    private int infantry_cost; ///< Value of cost for infantry to cross
    private int vehicle_cost;  ///< Value of cost for vehicle to cross

    /**
     * @brief Constructor of the Terrain enum
     * 
     * @param val_ The String value of the enum.
     */
    Terrain(String val_, int defence_bonus_, int infantry_cost_, int vehicle_cost_) {
        this.val = val_;
        this.defence_bonus = defence_bonus_;
        this.infantry_cost = infantry_cost_;
        this.vehicle_cost = vehicle_cost_;
    }

    /**
     * @brief Return the cost of infantry movement for that terrain
     * 
     * @return The cost of the infantry movement.
     */
    public int getInfantryCost() {
        return infantry_cost;
    }

    /**
     * @brief Return the cost of vehicle movement for that terrain
     * 
     * @return The cost of the vehicle movement.
     */
    public int getVehicleCost() {
        return vehicle_cost;
    }

    /**
     * @brief Given enum in teh String form, create from it the enum
     * 
     * @param type The string enum value.
     * @return The enum created from teh String input.
     */
    public static Terrain convert(String type) {
        // Check that the input isn't null
        if (type == null || type.trim().length() == 0) {
            throw new IllegalArgumentException("Expected a single Letter input.");
        }

        // Attempt to normalie the string
        String type_proc = type.trim().toUpperCase();

        // Attempt to convert to the enum
        return switch (type_proc) {
            case "P" -> PLAIN;
            case "F" -> FOREST;
            case "M" -> MOUNTAIN;
            case "W" -> WATER;
            case "C" -> CITY;
            case "T" -> FACTORY;
            case "H" -> HQ;
            default -> throw new IllegalArgumentException("Unsupported terrain value: " + type_proc);
        };
    }
}
