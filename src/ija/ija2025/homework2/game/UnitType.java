package ija.ija2025.homework2.game;

public enum UnitType {
    // List of the types and their names and values of the UnitType
    // Goes as: NAME, MAX_HP, PRICE, MOVEMENT_TYPE, MOVEMENT, MIN_ATTACK_RANGE, MAX_ATTACK_RANGE
    // TO DO: Special abilities are not yet added/considered for now
    INFANTRY("Infantry", 100, 1000, MovementType.INFANTRY, 3, 1, 1),
    TANK("Tank", 100, 7000, MovementType.VEHICLE, 6, 1, 1),
    ARTILLERY("Artillery", 100, 6000, MovementType.VEHICLE, 5, 2, 3);

    private String name; ///< Units name
    private int max_hp; ///< The maximum HP of the unit
    private int price; ///< The price of the unit to deploy
    private MovementType movement_type; ///< What type of movement is available
    private int movement; ///< The movement range of the unit
    private int min_attack_range; ///< The minimum attack range
    private int max_attack_range; /// The maximum attack range

    /**
     * @brief Constructor of the UnitType enum
     * 
     * @param val_ 
     * @param defence_bonus_
     * @param infantry_cost_
     * @param vehicle_cost_
     */
    UnitType(String name_, int max_hp_, int price_, MovementType movement_type_, int movement_, int min_attack_range_, int max_attack_range_) {
        this.name = name_;
        this.max_hp = max_hp_;
        this.price = price_;
        this.movement_type = movement_type_;
        this.movement = movement_;
        this.min_attack_range = min_attack_range_;
        this.max_attack_range = max_attack_range_;
    }

    // Functions to acces the values of the class

    /**
     * @brief Get the Unit type name
     * 
     * @return The unit type name
     */
    public String getName() {
        return this.name;
    }

    /**
     * @brief Get the Unit maximum hp
     * 
     * @return The unit maximum hp
     */
    public int getMaxHP() {
        return this.max_hp;
    }

    /**
     * @brief Get the Unit price
     * 
     * @return The unit price
     */
    public int getPrice() {
        return this.price;
    }

    /**
     * @brief Get the Unit movement type
     * 
     * @return The unit movement type
     */
    public MovementType getMovementType() {
        return this.movement_type;
    }

    /**
     * @brief Get the Unit movement value
     * 
     * @return The unit movement value
     */
    public int getMovement() {
        return this.movement;
    }

    /**
     * @brief Get the Unit minimum attack range
     * 
     * @return The unit minimum attack range
     */
    public int getMinAttackRange() {
        return this.min_attack_range;
    }

    /**
     * @brief Get the Unit maximum attack range
     * 
     * @return The unit maximum attack range
     */
    public int getMaxAttackRange() {
        return this.max_attack_range;
    }

    /**
     * @brif Get the movement cost of the unit for the given terrain type
     * 
     * @param terrain The terrain to analyze the movement for
     * 
     * @return The integer cost of the movement
     */
    public int getMovementCost(Terrain terrain) {
        return switch (this.movement_type) {
            case VEHICLE -> terrain.getVehicleCost();
            case INFANTRY -> terrain.getInfantryCost();
        };
    }

    /**
     * @brief Given enum in teh String form, create from it the enum
     * 
     * @param type The string enum value.
     * @return The enum created from teh String input.
     */
    public static UnitType convert(String type) {
        // Check that the input isn't null
        if (type == null || type.trim().length() == 0) {
            throw new IllegalArgumentException("Expected a nonempty input.");
        }

        // Attempt to normalie the string
        String type_proc = type.trim().toUpperCase();

        // Attempt to convert to the enum
        return switch (type_proc) {
            case "INFANTRY" -> INFANTRY;
            case "TANK" -> TANK;
            case "ARTILLERY" -> ARTILLERY;
            default -> throw new IllegalArgumentException("Unsupported unit type: " + type_proc);
        };
    }
}
