package model.map;

import model.map.Serializable.Terrain;

public class Building {

    private final Terrain terrain; ///< Holder of the specific type of building

    private String owner; ///< Name of the owner
    private int integrity; ///< Current integrity of the building

    /**
     * @brief Constructor sets the initial attributes of a building
     *
     * @param owner The string of the player
     * @param terrain The specific type of building
     */
    public Building(String owner, Terrain terrain) {
        this.owner = owner;
        this.terrain = terrain;
        this.integrity = getMaxIntegrityForTerrain(terrain);
    }


    /**
     * @brief Copy constructor of the Building class
     *
     * @param other Building to copy
     */
    public Building(Building other) {
        this.owner = other.owner;
        this.terrain = other.terrain;
        this.integrity = other.integrity;
    }

    /**
     * @brief Check if the building is a city for a income
     *
     * @return True if the building is a CITY.
     */
    public boolean isCity() {
        return terrain == Terrain.CITY;
    }

    /**
     * @brief Check if the building is a factory
     *
     * @return True if the building is a FACTORY.
     */
    public boolean isFactory(){
        return terrain == Terrain.FACTORY;
    }

    /**
     * @brief Get the building current owner
     *
     * @return The string form of the owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * @brief Get the integrity of the building
     *
     * @return Current integrity of the building.
     */
    public int getIntegrity() {
        return integrity;
    }

    /**
     * @brief Change the owner of the building
     *
     * @param owner The new owner of the building.
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * @brief Change the value of integrity of the building
     *
     * @param integrity The new integrity value of the building.
     */
    public void setIntegrity(int integrity) {
        this.integrity = integrity;
    }

    /**
     * @brief Check if the building is on full integrity
     *
     * @return True if the building is at full integrity.
     */
    public boolean isFull(){
        return integrity >= getMaxIntegrity();
    }

    /**
     * @brief Find the maximum integrity of the building
     *
     * @return Integer of the maximum integrity.
     */
    public int getMaxIntegrity(){
        return getMaxIntegrityForTerrain(terrain);
    }

    /**
     * @brief Get the capture integrity for a building terrain
     *
     * Cities and factories are intentionally quick to capture so that the
     * ownership change is visible during normal play. HQ tiles are slower
     * because capturing them is the victory condition.
     *
     * @param terrain The building terrain
     * @return The maximum integrity of that building
     */
    private static int getMaxIntegrityForTerrain(Terrain terrain) {
        return terrain == Terrain.HQ ? 3 : 1;
    }

    /**
     * @brief Check if the building is a headquarters
     *
     * @return True if the building is a HQ.
     */
    public boolean isHQ(){
        return terrain == Terrain.HQ;
    }
}
