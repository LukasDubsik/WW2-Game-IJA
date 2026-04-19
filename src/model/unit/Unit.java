package model.unit;

import model.map.Position;

public class Unit {

    // Values of the class
    private UnitType unitType; ///< the type of the unit
    private String owner; ///< the owner of the unit
    private Position position; ///< Where the unit is currently located
    private int current_hp; ///< What is the current hp of the unit
    private boolean already_played; ///< Whether the unit has already used its turn action

    /**
     * @brief Constructor of the Unit class. The hp is initialised at maximum.
     * 
     * @param unitType The type of the unit
     * @param owner The owner of the unit
     * @param position Where the unt is located on the game map
     */
    public Unit(UnitType unitType, String owner, Position position) {
        this.unitType = unitType;
        this.owner = owner;
        this.position = position;
        this.current_hp = unitType.getMaxHP();
        this.already_played = false;
    }

    // Get teh values of the Unit

    /**
     * @brief Get the unit's type
     * 
     * @return The unit's type
     */
    public UnitType getUnitType() {
        return unitType;
    }

    /**
     * @brief Get the unit's owner
     * 
     * @return The unit's owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * @brief Get the unit's position
     * 
     * @return The unit's position
     */
    public Position getPosition() {
        return position;
    }

    /**
     * @brief Get the unit's current HP
     * 
     * @return The unit's current HP
     */
    public int getCurrentHp() {
        return current_hp;
    }

    /**
     * @brief Set the new unit's position
     * 
     * @param pos The position to change to
     */
    public void setPosition(Position pos) {
        this.position = pos;
    }

    /**
     * @brief Check whether the unit has already acted this turn
     * 
     * @return True if already acted, false otherwise
     */
    public boolean hasAlreadyPlayed() {
        return already_played;
    }

    /**
     * @brief Set whether the unit has already acted this turn
     * 
     * @param already_played_ The new value of the played state
     */
    public void setAlreadyPlayed(boolean already_played_) {
        this.already_played = already_played_;
    }

    /**
     * @brief Convert to String representation
     */
    @Override
    public String toString() {
        return "{" + unitType.getName() + position.toString() + "[" + current_hp + "]}";
    }
}
