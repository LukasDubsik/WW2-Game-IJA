package model.map;

/**
 * @enum TileOverlay
 * @brief Additional overlay layer above terrain.
 *
 *        IMPORTANT:
 *        These are modifiers on top of the base terrain, not replacement terrain types.
 */
public enum Overlay {
    NONE("None", "N", 0, 0, 0),
    TRENCH("Trench", "TR", 2, 1, 2),
    BARBED_WIRE("Barbed Wire", "BW", 0, 1, 2),
    CRATER("Crater", "CR", 1, 1, 1),
    RUBBLE("Rubble", "RB", 1, 1, 2),
    SNOW_DRIFT("Snow Drift", "SD", 0, 1, 1);

    private final String display_name; ///< The name by which the overlay is displayed
    private final String short_label; ///< The label for identification
    private final int defence_bonus_modifier; ///< The bonus for defense
    private final int infantry_cost_modifier; ///< The additional cost for infantry movement
    private final int vehicle_cost_modifier; ///< The additional cost for vehicle movement

    /**
     * @brief The constructor of the Overlay class
     * 
     * @param display_name_ The name by which the overlay is displayed
     * @param short_label_ The label for identification
     * @param defence_bonus_modifier_ The bonus for defense
     * @param infantry_cost_modifier_ The additional cost for infantry movement
     * @param vehicle_cost_modifier_ The additional cost for vehicle movement
     */
    Overlay(String display_name_, String short_label_, int defence_bonus_modifier_, int infantry_cost_modifier_, int vehicle_cost_modifier_) {
        this.display_name = display_name_;
        this.short_label = short_label_;
        this.defence_bonus_modifier = defence_bonus_modifier_;
        this.infantry_cost_modifier = infantry_cost_modifier_;
        this.vehicle_cost_modifier = vehicle_cost_modifier_;
    }

    /**
     * @brief Get teh display name
     * 
     * @return The {@link #display_name}
     */
    public String getDisplayName() {
        return display_name;
    }

    /**
     * @brief Get the short label
     * 
     * @return The {@link #short_label}
     */
    public String getShortLabel() {
        return short_label;
    }

    /**
     * @brief Get the defence modifier
     * 
     * @return The {@link #defence_bonus_modifier}
     */
    public int getDefenceBonusModifier() {
        return defence_bonus_modifier;
    }

    /**
     * @brief Get the infantry movement cost modifier
     * 
     * @return The {@link #infantry_cost_modifier}
     */
    public int getInfantryCostModifier() {
        return infantry_cost_modifier;
    }

    /**
     * @brief Get the vehicle movement cost modifier
     * 
     * @return The {@link #vehicle_cost_modifier}
     */
    public int getVehicleCostModifier() {
        return vehicle_cost_modifier;
    }
}