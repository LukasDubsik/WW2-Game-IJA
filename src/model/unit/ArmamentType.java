package model.unit;

import java.util.EnumSet;

/**
 * @brief Static definition of all supported armament types
 * 
 * This stage adds helper methods for evaluating whether the armament can
 * contribute at a given distance and whether it is direct or indirect fire.
 */
public enum ArmamentType {
    // German armaments
    RIFLES(
            "Rifles",
            8, 1,
            1, 1,
            EnumSet.of(WeaponTag.DIRECT_FIRE)
    ),

    GRENADES(
            "Grenades",
            6, 2,
            1, 1,
            EnumSet.of(WeaponTag.HE, WeaponTag.DIRECT_FIRE)
    ),

    MG42(
            "MG 42",
            16, 2,
            1, 2,
            EnumSet.of(WeaponTag.MG, WeaponTag.DIRECT_FIRE)
    ),

    KWK_40_L48_75MM(
            "7.5 cm KwK 40 L/48",
            18, 26,
            1, 2,
            EnumSet.of(WeaponTag.ANTI_TANK, WeaponTag.HE, WeaponTag.DIRECT_FIRE)
    ),

    KWK_39_L60_50MM(
            "5 cm KwK 39 L/60",
            12, 18,
            1, 2,
            EnumSet.of(WeaponTag.ANTI_TANK, WeaponTag.DIRECT_FIRE)
    ),

    VEHICLE_MG_GERMAN(
            "Vehicle MG",
            8, 1,
            1, 2,
            EnumSet.of(WeaponTag.MG, WeaponTag.DIRECT_FIRE)
    ),

    // Soviet armaments
    PPSh41_SMGS(
            "PPSh-41 SMGs",
            12, 2,
            1, 1,
            EnumSet.of(WeaponTag.DIRECT_FIRE)
    ),

    DP27(
            "DP-27",
            14, 2,
            1, 2,
            EnumSet.of(WeaponTag.MG, WeaponTag.DIRECT_FIRE)
    ),

    D25T_85MM(
            "85 mm D-5T",
            18, 30,
            1, 2,
            EnumSet.of(WeaponTag.ANTI_TANK, WeaponTag.HE, WeaponTag.DIRECT_FIRE)
    ),

    ZIS3_76MM(
            "76 mm ZiS-3",
            16, 24,
            2, 3,
            EnumSet.of(WeaponTag.ANTI_TANK, WeaponTag.HE, WeaponTag.INDIRECT_FIRE)
    ),

    VEHICLE_MG_SOVIET(
            "Vehicle MG",
            8, 1,
            1, 2,
            EnumSet.of(WeaponTag.MG, WeaponTag.DIRECT_FIRE)
    );

    private String name; ///< Display name of the armament
    private int soft_attack; ///< Attack value against soft targets
    private int hard_attack; ///< Attack value against hard targets
    private int min_range; ///< Minimum valid firing range
    private int max_range; ///< Maximum valid firing range
    private EnumSet<WeaponTag> tags; ///< Set of qualitative weapon tags

    /**
     * @brief Constructor of the ArmamentType enum
     * 
     * @param name_ Display name of the weapon
     * @param soft_attack_ Attack value against soft targets
     * @param hard_attack_ Attack value against hard targets
     * @param min_range_ Minimum valid firing range
     * @param max_range_ Maximum valid firing range
     * @param tags_ Qualitative set of tags describing the weapon
     */
    ArmamentType(String name_,
                 int soft_attack_,
                 int hard_attack_,
                 int min_range_,
                 int max_range_,
                 EnumSet<WeaponTag> tags_) {
        this.name = name_;
        this.soft_attack = soft_attack_;
        this.hard_attack = hard_attack_;
        this.min_range = min_range_;
        this.max_range = max_range_;
        this.tags = tags_;
    }

    /**
     * @brief Get the display name of the armament
     * 
     * @return The display name
     */
    public String getName() {
        return this.name;
    }

    /**
     * @brief Get the attack value against soft targets
     * 
     * @return The soft attack value
     */
    public int getSoftAttack() {
        return this.soft_attack;
    }

    /**
     * @brief Get the attack value against hard targets
     * 
     * @return The hard attack value
     */
    public int getHardAttack() {
        return this.hard_attack;
    }

    /**
     * @brief Get the minimum valid firing range
     * 
     * @return The minimum firing range
     */
    public int getMinRange() {
        return this.min_range;
    }

    /**
     * @brief Get the maximum valid firing range
     * 
     * @return The maximum firing range
     */
    public int getMaxRange() {
        return this.max_range;
    }

    /**
     * @brief Get the full set of weapon tags
     * 
     * @return The weapon tag set
     */
    public EnumSet<WeaponTag> getTags() {
        return EnumSet.copyOf(this.tags);
    }

    /**
     * @brief Check whether the armament has the given tag
     * 
     * @param tag The tag to test
     * @return True if the tag is present, false otherwise
     */
    public boolean hasTag(WeaponTag tag) {
        return this.tags.contains(tag);
    }

    /**
     * @brief Check whether the weapon is direct-fire
     * 
     * @return True if the weapon is direct-fire, false otherwise
     */
    public boolean isDirectFire() {
        return this.tags.contains(WeaponTag.DIRECT_FIRE);
    }

    /**
     * @brief Check whether the weapon is indirect-fire
     * 
     * @return True if the weapon is indirect-fire, false otherwise
     */
    public boolean isIndirectFire() {
        return this.tags.contains(WeaponTag.INDIRECT_FIRE);
    }

    /**
     * @brief Get the attack value against the requested target class
     * 
     * @param target_class The target class
     * @return The matching attack value
     */
    public int getAttackAgainst(TargetClass target_class) {
        return switch (target_class) {
            case SOFT -> this.soft_attack;
            case HARD -> this.hard_attack;
        };
    }

    /**
     * @brief Check whether the armament may fire at the given distance
     * 
     * @param distance The attack distance
     * @return True if the distance lies inside the weapon interval
     */
    public boolean canFireAtDistance(int distance) {
        return distance >= this.min_range && distance <= this.max_range;
    }

    /**
     * @brief Check whether the armament contributes as direct fire at the distance
     * 
     * @param distance The attack distance
     * @return True if it is a valid direct-fire contribution
     */
    public boolean contributesDirectAtDistance(int distance) {
        return isDirectFire() && canFireAtDistance(distance);
    }

    /**
     * @brief Check whether the armament contributes as indirect fire at the distance
     * 
     * @param distance The attack distance
     * @return True if it is a valid indirect-fire contribution
     */
    public boolean contributesIndirectAtDistance(int distance) {
        return isIndirectFire() && canFireAtDistance(distance);
    }
}