package model.unit;

import model.map.Terrain;

public enum UnitType {
    // List of the types and their names and values of the UnitType
    // Goes as:
    // DISPLAY_NAME, ASSET_PATH, MAX_HP, PRICE, MOVEMENT_TYPE, MOVEMENT, MIN_ATTACK_RANGE, MAX_ATTACK_RANGE
    // Damage is intentionally not modeled here yet. Weapons can own that later.

    WEHRMACHT_RIFLE_SQUAD(
            "Wehrmacht Rifle Squad",
            "lib/assets/units/germany/infantry/base_infantry.png",
            100, 900, MovementType.INFANTRY, 3, 1, 1
    ),

    GRENADIER_SQUAD(
            "Grenadier Squad",
            "lib/assets/units/germany/infantry/grenadiers.png",
            100, 1100, MovementType.INFANTRY, 3, 1, 1
    ),

    MG42_TEAM(
            "MG 42 Team",
            "lib/assets/units/germany/infantry/mg_team.png",
            100, 1200, MovementType.INFANTRY, 2, 1, 1
    ),

    SDKFZ_251_HALFTRACK(
            "Sd.Kfz. 251/1 Half-track",
            "lib/assets/units/germany/vehicles/sdkfz_251.png",
            100, 3200, MovementType.VEHICLE, 6, 1, 1
    ),

    PANZER_IV_AUSF_J(
            "Panzer IV Ausf. J",
            "lib/assets/units/germany/vehicles/panzer_IV_J.png",
            100, 7000, MovementType.VEHICLE, 5, 1, 1
    ),

    SDKFZ_234_2_PUMA(
            "Sd.Kfz. 234/2 Puma",
            "lib/assets/units/germany/vehicles/puma.png",
            100, 5200, MovementType.VEHICLE, 8, 1, 1
    ),

    SOVIET_ASSAULT_SAPPER_SQUAD(
            "Soviet Assault Sapper Squad",
            "lib/assets/units/soviets/infantry/assult_infantry.png",
            100, 1200, MovementType.INFANTRY, 3, 1, 1
    ),

    DP27_TEAM(
            "DP-27 Team",
            "lib/assets/units/soviets/infantry/mg.png",
            100, 1200, MovementType.INFANTRY, 2, 1, 1
    ),

    ZIS_3_FIELD_GUN(
            "ZiS-3 Field Gun",
            "lib/assets/units/soviets/guns/vehicle_gun.png",
            100, 3500, MovementType.VEHICLE, 1, 2, 3
    ),

    IS_1_HEAVY_TANK(
            "IS-1 Heavy Tank",
            "lib/assets/units/soviets/vehicles/IS1.png",
            100, 8000, MovementType.VEHICLE, 5, 1, 1
    ),

    M3_HALFTRACK(
            "M3 Half-track",
            "lib/assets/units/soviets/vehicles/halftrack.png",
            100, 3600, MovementType.VEHICLE, 7, 1, 1
    ),

    BA_64_ARMORED_CAR(
            "BA-64 Armored Car",
            "lib/assets/units/soviets/vehicles/armored_car.png",
            100, 4200, MovementType.VEHICLE, 8, 1, 1
    );

    private String name; ///< Units name
    private String asset_path; ///< Path to the unit image asset
    private int max_hp; ///< The maximum HP of the unit
    private int price; ///< The price of the unit to deploy
    private MovementType movement_type; ///< What type of movement is available
    private int movement; ///< The movement range of the unit
    private int min_attack_range; ///< The minimum attack range
    private int max_attack_range; ///< The maximum attack range

    /**
     * @brief Constructor of the UnitType enum
     */
    UnitType(String name_, String asset_path_, int max_hp_, int price_,
             MovementType movement_type_, int movement_,
             int min_attack_range_, int max_attack_range_) {
        this.name = name_;
        this.asset_path = asset_path_;
        this.max_hp = max_hp_;
        this.price = price_;
        this.movement_type = movement_type_;
        this.movement = movement_;
        this.min_attack_range = min_attack_range_;
        this.max_attack_range = max_attack_range_;
    }

    /**
     * @brief Get the Unit type name
     * 
     * @return The unit type name
     */
    public String getName() {
        return this.name;
    }

    /**
     * @brief Get the unit asset path
     * 
     * @return The path to the unit image asset
     */
    public String getAssetPath() {
        return this.asset_path;
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
     * @brief Get the movement cost of the unit for the given terrain type
     * 
     * @param terrain The terrain to analyze the movement for
     * 
     * @return The integer cost of the movement
     */
    public int getMovementCost(Terrain terrain) {
        return switch (this.movement_type) {
            case VEHICLE -> terrain.getVehicleMovementCost();
            case INFANTRY -> terrain.getInfantryMovementCost();
        };
    }

    /**
     * @brief Given enum in the String form, create from it the enum
     * 
     * @param type The string enum value.
     * @return The enum created from the String input.
     */
    public static UnitType convert(String type) {
        // Check that the input isn't null
        if (type == null || type.trim().length() == 0) {
            throw new IllegalArgumentException("Expected a nonempty input.");
        }

        // Attempt to normalize the string
        String type_proc = type.trim().toUpperCase();

        // Attempt to convert to the enum
        return switch (type_proc) {
            case "WEHRMACHT RIFLE SQUAD", "WEHRMACHT_RIFLE_SQUAD", "RIFLE SQUAD", "BASE_INFANTRY", "INFANTRY"
                    -> WEHRMACHT_RIFLE_SQUAD;

            case "GRENADIER SQUAD", "GRENADIER_SQUAD", "GRENADIERS"
                    -> GRENADIER_SQUAD;

            case "MG 42 TEAM", "MG42 TEAM", "MG_42_TEAM", "MG42_TEAM", "MG_TEAM"
                    -> MG42_TEAM;

            case "SDKFZ 251 HALFTRACK", "SDKFZ_251_HALFTRACK", "SDKFZ_251", "SD.KFZ. 251/1 HALF-TRACK", "SD.KFZ. 251/1 HALFTRACK"
                    -> SDKFZ_251_HALFTRACK;

            case "PANZER IV AUSF. J", "PANZER IV AUSF J", "PANZER_IV_AUSF_J", "PANZER IV J", "PANZER_IV_J", "TANK"
                    -> PANZER_IV_AUSF_J;

            case "SDKFZ 234/2 PUMA", "SDKFZ_234_2_PUMA", "PUMA", "SD.KFZ. 234/2 PUMA"
                    -> SDKFZ_234_2_PUMA;

            case "SOVIET ASSAULT SAPPER SQUAD", "SOVIET_ASSAULT_SAPPER_SQUAD", "ASSAULT SAPPER SQUAD", "ASSULT_INFANTRY", "ASSAULT_INFANTRY"
                    -> SOVIET_ASSAULT_SAPPER_SQUAD;

            case "DP-27 TEAM", "DP27 TEAM", "DP_27_TEAM", "DP27_TEAM", "SOVIET MG", "MG"
                    -> DP27_TEAM;

            case "ZIS-3 FIELD GUN", "ZIS 3 FIELD GUN", "ZIS_3_FIELD_GUN", "ZIS-3", "ZIS3", "ARTILLERY", "VEHICLE_GUN"
                    -> ZIS_3_FIELD_GUN;

            case "IS-1 HEAVY TANK", "IS 1 HEAVY TANK", "IS_1_HEAVY_TANK", "IS-1", "IS1"
                    -> IS_1_HEAVY_TANK;

            case "M3 HALF-TRACK", "M3 HALFTRACK", "M3_HALFTRACK", "HALFTRACK"
                    -> M3_HALFTRACK;

            case "BA-64 ARMORED CAR", "BA 64 ARMORED CAR", "BA_64_ARMORED_CAR", "BA-64", "BA64", "ARMORED_CAR"
                    -> BA_64_ARMORED_CAR;

            default -> throw new IllegalArgumentException("Unsupported unit type: " + type_proc);
        };
    }
}