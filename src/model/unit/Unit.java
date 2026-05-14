package model.unit;

import model.map.Serializable.Position;

import java.io.Serializable;

public class Unit implements Serializable {

    // Values of the class
    private UnitType unitType; ///< the type of the unit
    private String owner; ///< the owner of the unit
    private Position position; ///< Where the unit is currently located
    private int current_hp; ///< What is the current hp of the unit
    private boolean already_played; ///< Whether the unit has already fully finished its turn action
    private boolean moved_this_turn; ///< Whether the unit has already moved in the current turn

    /**
     * @brief Constructor of the Unit class. The hp is initialised at maximum.
     * 
     * @param unitType The type of the unit
     * @param owner The owner of the unit
     * @param position Where the unit is located on the game map
     */
    public Unit(UnitType unitType, String owner, Position position) {
        this.unitType = unitType;
        this.owner = owner;
        this.position = position;
        this.current_hp = unitType.getMaxHP();
        this.already_played = false;
        this.moved_this_turn = false;
    }

    // Get the values of the Unit

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
     * @brief Change the current position of the unit
     * 
     * @param position_ The new position of the unit
     */
    public void setPosition(Position position_) {
        this.position = position_;
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
     * @brief Check whether the unit has already fully acted this turn
     * 
     * @return True if already acted, false otherwise
     */
    public boolean hasAlreadyPlayed() {
        return already_played;
    }

    /**
     * @brief Set whether the unit has already fully acted this turn
     * 
     * @param already_played_ The new value of the played state
     */
    public void setAlreadyPlayed(boolean already_played_) {
        this.already_played = already_played_;
    }

    /**
     * @brief Check whether the unit has already moved this turn
     * 
     * @return True if the unit already moved, false otherwise
     */
    public boolean hasMovedThisTurn() {
        return moved_this_turn;
    }

    /**
     * @brief Set whether the unit has already moved this turn
     * 
     * @param moved_this_turn_ The new moved state
     */
    public void setMovedThisTurn(boolean moved_this_turn_) {
        this.moved_this_turn = moved_this_turn_;
    }

    /**
     * @brief Deal damage to the unit
     * 
     * @param damage_ The amount of damage to be applied
     */
    public void takeDamage(int damage_) {
        // Negative damage makes no sense here
        if (damage_ < 0) {
            throw new IllegalArgumentException("Damage cannot be negative.");
        }

        // Subtract the damage from the current hp
        this.current_hp -= damage_;

        // Prevent hp from going below zero
        if (this.current_hp < 0) {
            this.current_hp = 0;
        }
    }

    /**
     * @brief Check whether the unit has been destroyed
     * 
     * @return True if the unit has zero hp, false otherwise
     */
    public boolean isDestroyed() {
        return this.current_hp <= 0;
    }

    /**
     * @brief Set the current hp directly
     * 
     * @param hp_ The new hp value
     */
    public void setCurrentHp(int hp_) {
        // Clamp the hp to the valid interval
        if (hp_ < 0) {
            this.current_hp = 0;
        } else if (hp_ > this.unitType.getMaxHP()) {
            this.current_hp = this.unitType.getMaxHP();
        } else {
            this.current_hp = hp_;
        }
    }

    /**
     * @brief Convert to String representation
     */
    @Override
    public String toString() {
        return "{" + unitType.getName() + position.toString() + "[" + current_hp + "]}";
    }
}