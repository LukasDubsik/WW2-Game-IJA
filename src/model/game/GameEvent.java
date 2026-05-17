/**
 * @file GameEvent.java
 * @author xdubsil00, xbobekp00
 * @brief Source file GameEvent.java for the IJA Advance-Wars-inspired game project.
 */
package model.game;

import model.map.Serializable.Position;
import model.unit.Unit;

/**
 * @class GameEvent
 * @brief Lightweight event object used by the observer hooks.
 */
public class GameEvent {

    /**
     * @enum Type
     * @brief Basic kinds of game changes.
     */
    public enum Type {
        GENERIC,
        UNIT_MOVED,
        UNIT_ATTACKED,
        UNIT_DESTROYED,
        UNIT_PURCHASED,
        BUILDING_CHANGED,
        BUILDING_CAPTURED,
        TURN_CHANGED,
        GAME_FINISHED
    }

    private final Type type; ///< Type of the event
    private final Unit unit; ///< Main unit related to the event, if any
    private final Position from; ///< Source position, if relevant
    private final Position to; ///< Target position, if relevant

    /**
     * @brief Create a generic event
     */
    public GameEvent() {
        this(Type.GENERIC, null, null, null);
    }

    /**
     * @brief Create a typed event
     *
     * @param type Event type
     */
    public GameEvent(Type type) {
        this(type, null, null, null);
    }

    /**
     * @brief Create a full event
     *
     * @param type Event type
     * @param unit Main unit
     * @param from Source position
     * @param to Target position
     */
    public GameEvent(Type type, Unit unit, Position from, Position to) {
        this.type = type == null ? Type.GENERIC : type;
        this.unit = unit;
        this.from = from;
        this.to = to;
    }

    /**
     * @brief Get event type
     */
    public Type getType() {
        return type;
    }

    /**
     * @brief Get event unit
     */
    public Unit getUnit() {
        return unit;
    }

    /**
     * @brief Get source position
     */
    public Position getFrom() {
        return from;
    }

    /**
     * @brief Get target position
     */
    public Position getTo() {
        return to;
    }
}
