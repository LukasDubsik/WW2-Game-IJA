package model.unit;

import java.util.List;

import model.map.Terrain;

public enum UnitType {
    // List of the types and their names and values of the UnitType
    // Goes as:
    // DISPLAY_NAME, ASSET_PATH, MAX_HP, PRICE, MOVEMENT_TYPE, MOVEMENT,
    // MIN_ATTACK_RANGE, MAX_ATTACK_RANGE, SOFT_DAMAGE, HARD_DAMAGE, ARMAMENTS

    WEHRMACHT_RIFLE_SQUAD(
            "Wehrmacht Rifle Squad",
            "lib/assets/units/germany/infantry/base_infantry.png",
            100, 900, MovementType.INFANTRY, 3, 1, 1,
            18, 4,
            List.of(
                    ArmamentType.RIFLES,
                    ArmamentType.GRENADES
            )
    ),

    GRENADIER_SQUAD(
            "Grenadier Squad",
            "lib/assets/units/germany/infantry/grenadiers.png",
            100, 1100, MovementType.INFANTRY, 3, 1, 1,
            22, 6,
            List.of(
                    ArmamentType.RIFLES,
                    ArmamentType.GRENADES
            )
    ),

    MG42_TEAM(
            "MG 42 Team",
            "lib/assets/units/germany/infantry/mg_team.png",
            100, 1200, MovementType.INFANTRY, 2, 1, 1,
            30, 3,
            List.of(
                    ArmamentType.MG42,
                    ArmamentType.RIFLES
            )
    ),

    SDKFZ_251_HALFTRACK(
            "Sd.Kfz. 251/1 Half-track",
            "lib/assets/units/germany/vehicles/sdkfz_251.png",
            100, 3200, MovementType.VEHICLE, 6, 1, 1,
            16, 10,
            List.of(
                    ArmamentType.VEHICLE_MG_GERMAN
            )
    ),

    PANZER_IV_AUSF_J(
            "Panzer IV Ausf. J",
            "lib/assets/units/germany/vehicles/panzer_IV_J.png",
            100, 7000, MovementType.VEHICLE, 5, 1, 1,
            24, 34,
            List.of(
                    ArmamentType.KWK_40_L48_75MM,
                    ArmamentType.VEHICLE_MG_GERMAN,
                    ArmamentType.VEHICLE_MG_GERMAN
            )
    ),

    SDKFZ_234_2_PUMA(
            "Sd.Kfz. 234/2 Puma",
            "lib/assets/units/germany/vehicles/puma.png",
            100, 5200, MovementType.VEHICLE, 8, 1, 1,
            18, 24,
            List.of(
                    ArmamentType.KWK_39_L60_50MM,
                    ArmamentType.VEHICLE_MG_GERMAN
            )
    ),

    SOVIET_ASSAULT_SAPPER_SQUAD(
            "Soviet Assault Sapper Squad",
            "lib/assets/units/soviets/infantry/assult_infantry.png",
            100, 1200, MovementType.INFANTRY, 3, 1, 1,
            24, 8,
            List.of(
                    ArmamentType.PPSh41_SMGS,
                    ArmamentType.GRENADES
            )
    ),

    DP27_TEAM(
            "DP-27 Team",
            "lib/assets/units/soviets/infantry/mg.png",
            100, 1200, MovementType.INFANTRY, 2, 1, 1,
            28, 3,
            List.of(
                    ArmamentType.DP27,
                    ArmamentType.RIFLES
            )
    ),

    ZIS_3_FIELD_GUN(
            "ZiS-3 Field Gun",
            "lib/assets/units/soviets/guns/vehicle_gun.png",
            100, 3500, MovementType.VEHICLE, 2, 2, 3,
            20, 36,
            List.of(
                    ArmamentType.ZIS3_76MM
            )
    ),

    IS_1_HEAVY_TANK(
            "IS-1 Heavy Tank",
            "lib/assets/units/soviets/vehicles/IS1.png",
            100, 8000, MovementType.VEHICLE, 5, 1, 1,
            26, 40,
            List.of(
                    ArmamentType.D25T_85MM,
                    ArmamentType.VEHICLE_MG_SOVIET
            )
    ),

    M3_HALFTRACK(
            "M3 Half-track",
            "lib/assets/units/soviets/vehicles/halftrack.png",
            100, 3600, MovementType.VEHICLE, 7, 1, 1,
            15, 9,
            List.of(
                    ArmamentType.VEHICLE_MG_SOVIET
            )
    ),

    BA_64_ARMORED_CAR(
            "BA-64 Armored Car",
            "lib/assets/units/soviets/vehicles/armored_car.png",
            100, 4200, MovementType.VEHICLE, 8, 1, 1,
            14, 8,
            List.of(
                    ArmamentType.VEHICLE_MG_SOVIET
            )
    );

    private String name; ///< Units name
    private String asset_path; ///< Path to the unit image asset
    private int max_hp; ///< The maximum HP of the unit
    private int price; ///< The price of the unit to deploy
    private MovementType movement_type; ///< What type of movement is available
    private int movement; ///< The movement range of the unit
    private int min_attack_range; ///< The minimum attack range
    private int max_attack_range; ///< The maximum attack range
    private int soft_damage; ///< Provisional baked base damage against soft targets
    private int hard_damage; ///< Provisional baked base damage against hard targets
    private List<ArmamentType> armaments; ///< List of armaments carried by the unit type

    /**
     * @brief Constructor of the UnitType enum
     */
    UnitType(String name_, String asset_path_, int max_hp_, int price_,
             MovementType movement_type_, int movement_,
             int min_attack_range_, int max_attack_range_,
             int soft_damage_, int hard_damage_,
             List<ArmamentType> armaments_) {
        this.name = name_;
        this.asset_path = asset_path_;
        this.max_hp = max_hp_;
        this.price = price_;
        this.movement_type = movement_type_;
        this.movement = movement_;
        this.min_attack_range = min_attack_range_;
        this.max_attack_range = max_attack_range_;
        this.soft_damage = soft_damage_;
        this.hard_damage = hard_damage_;
        this.armaments = List.copyOf(armaments_);
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
     * @brief Get provisional baked damage against soft targets
     * 
     * @return Damage against infantry / gun crews
     */
    public int getSoftDamage() {
        return this.soft_damage;
    }

    /**
     * @brief Get provisional baked damage against hard targets
     * 
     * @return Damage against armored / vehicle targets
     */
    public int getHardDamage() {
        return this.hard_damage;
    }

    /**
     * @brief Get the list of armaments carried by the unit type
     * 
     * @return The armament list
     */
    public List<ArmamentType> getArmaments() {
        return this.armaments;
    }

    /**
     * @brief Get the movement cost of the unit for the given terrain type
     * 
     * @param terrain The terrain to analyze the movement for
     * @return The integer cost of the movement
     */
    public int getMovementCost(Terrain terrain) {
        return switch (this.movement_type) {
            case VEHICLE -> terrain.getVehicleMovementCost();
            case INFANTRY -> terrain.getInfantryMovementCost();
        };
    }

    /**
     * @brief Compute the total attack against soft targets from all armaments
     * 
     * @return The computed aggregate soft attack
     */
    public int getComputedSoftAttack() {
        return getComputedSoftAttackAtDistance(1);
    }

    /**
     * @brief Compute the total attack against hard targets from all armaments
     * 
     * @return The computed aggregate hard attack
     */
    public int getComputedHardAttack() {
        return getComputedHardAttackAtDistance(1);
    }

    /**
     * @brief Compute the total attack against soft targets at the given distance
     * 
     * @param distance The attack distance
     * @return The computed aggregate soft attack
     */
    public int getComputedSoftAttackAtDistance(int distance) {
        return getComputedAttackAtDistance(TargetClass.SOFT, distance);
    }

    /**
     * @brief Compute the total attack against hard targets at the given distance
     * 
     * @param distance The attack distance
     * @return The computed aggregate hard attack
     */
    public int getComputedHardAttackAtDistance(int distance) {
        return getComputedAttackAtDistance(TargetClass.HARD, distance);
    }

    /**
     * @brief Compute the total attack against the requested target class at the given distance
     * 
     * This stage stops summing all armaments blindly.
     * Only armaments valid for the given distance contribute.
     * 
     * @param target_class The target class against which to compute the attack
     * @param distance The attack distance
     * @return The computed aggregate attack
     */
    public int getComputedAttackAtDistance(TargetClass target_class, int distance) {
        int attack_sum = 0;

        // Sum only armaments that are valid at the given distance
        for (ArmamentType armament : this.armaments) {
            if (!armament.canFireAtDistance(distance)) {
                continue;
            }

            attack_sum += armament.getAttackAgainst(target_class);
        }

        return attack_sum;
    }

    /**
     * @brief Compute the direct-fire contribution against the given target class and distance
     * 
     * @param target_class The target class
     * @param distance The attack distance
     * @return Total direct-fire contribution
     */
    public int getDirectFireAttackAtDistance(TargetClass target_class, int distance) {
        int attack_sum = 0;

        for (ArmamentType armament : this.armaments) {
            if (!armament.contributesDirectAtDistance(distance)) {
                continue;
            }

            attack_sum += armament.getAttackAgainst(target_class);
        }

        return attack_sum;
    }

    /**
     * @brief Compute the indirect-fire contribution against the given target class and distance
     * 
     * @param target_class The target class
     * @param distance The attack distance
     * @return Total indirect-fire contribution
     */
    public int getIndirectFireAttackAtDistance(TargetClass target_class, int distance) {
        int attack_sum = 0;

        for (ArmamentType armament : this.armaments) {
            if (!armament.contributesIndirectAtDistance(distance)) {
                continue;
            }

            attack_sum += armament.getAttackAgainst(target_class);
        }

        return attack_sum;
    }

    /**
     * @brief Get provisional baked damage against another unit
     * 
     * This is still the old system and is intentionally left in place for this stage.
     * 
     * @param target The target unit type
     * @return The provisional baked base damage value
     */
    public int getDamageAgainst(UnitType target) {
        if (target == null) {
            throw new IllegalArgumentException("Target unit type cannot be null.");
        }

        return switch (target.getMovementType()) {
            case INFANTRY -> this.soft_damage;
            case VEHICLE -> this.hard_damage;
        };
    }

    /**
     * @brief Get computed aggregate damage against another unit from armaments at the given distance
     * 
     * This is the new armament-based total, but it is still not yet wired into the
     * real combat resolution logic in this stage.
     * 
     * @param target The target unit type
     * @param distance The attack distance
     * @return The computed armament-based damage value
     */
    public int getComputedDamageAgainst(UnitType target, int distance) {
        if (target == null) {
            throw new IllegalArgumentException("Target unit type cannot be null.");
        }

        return switch (target.getMovementType()) {
            case INFANTRY -> getComputedSoftAttackAtDistance(distance);
            case VEHICLE -> getComputedHardAttackAtDistance(distance);
        };
    }

    /**
     * @brief Check whether the unit type has any valid direct-fire contribution at the distance
     * 
     * @param distance The attack distance
     * @return True if any direct-fire armament contributes
     */
    public boolean hasDirectFireAtDistance(int distance) {
        return getDirectFireAttackAtDistance(TargetClass.SOFT, distance) > 0
                || getDirectFireAttackAtDistance(TargetClass.HARD, distance) > 0;
    }

    /**
     * @brief Check whether the unit type has any valid indirect-fire contribution at the distance
     * 
     * @param distance The attack distance
     * @return True if any indirect-fire armament contributes
     */
    public boolean hasIndirectFireAtDistance(int distance) {
        return getIndirectFireAttackAtDistance(TargetClass.SOFT, distance) > 0
                || getIndirectFireAttackAtDistance(TargetClass.HARD, distance) > 0;
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