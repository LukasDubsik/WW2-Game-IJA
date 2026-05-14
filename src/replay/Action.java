package replay;

import model.map.Serializable.Position;
import model.unit.Unit;
import replay.records.DamageRecord;
import replay.records.MoveRecord;
import replay.records.UnitPurchaseRecord;

import java.io.Serializable;

/**
 * @class Action
 * @brief Hold the information for each actions
 */
public class Action implements Serializable {

    /**
     * @brief Type of action
     */
    public enum ActionEnum implements Serializable {
        MOVE, ///< Action move
        DAMAGE, ///< Action damage
        BUY, ///< Action buy
        DESTROY /// Action destroy
    }

    private final ActionEnum actionEnum; ///< Type of the action
    private final MoveRecord moveRecord; ///< Holder of the move record
    private final DamageRecord damageRecord; ///< Holder of the damage record
    private final UnitPurchaseRecord unitPurchaseRecord; ///< Holder of the buy record
    private final Unit unitDestroyed; ///< Holder of the destroy unit
    private final Position destroyPosition; ///< Holder of the destroy position

    /**
     * @brief Create a move record action
     *
     * @param actionEnum Type of action
     * @param moveRecord Move record
     */
    public Action(ActionEnum actionEnum, MoveRecord moveRecord) {
        this.actionEnum = actionEnum;
        this.moveRecord = moveRecord;
        this.damageRecord = null;
        this.unitPurchaseRecord = null;
        this.unitDestroyed = null;
        destroyPosition = null;
    }

    /**
     * @brief Create a damage record action
     *
     * @param actionEnum Type of action
     * @param damageRecord Damage record
     */
    public Action(ActionEnum actionEnum, DamageRecord damageRecord) {
        this.actionEnum = actionEnum;
        this.moveRecord = null;
        this.damageRecord = damageRecord;
        this.unitPurchaseRecord = null;
        this.unitDestroyed = null;
        destroyPosition = null;
    }

    /**
     * @brief Create a buy record action
     *
     * @param actionEnum Type of action
     * @param unitPurchaseRecord Buy record
     */
    public Action(ActionEnum actionEnum, UnitPurchaseRecord unitPurchaseRecord) {
        this.actionEnum = actionEnum;
        this.moveRecord = null;
        this.damageRecord = null;
        this.unitPurchaseRecord = unitPurchaseRecord;
        this.unitDestroyed = null;
        destroyPosition = null;
    }

    /**
     * @brief Create a destroyed unit record action
     *
     * @param actionEnum Type of action
     * @param unitDestroyed Destroyed unit record
     * @param destroyPosition Position of the destroyed unit
     */
    public Action(ActionEnum actionEnum, Unit unitDestroyed, Position destroyPosition) {
        this.actionEnum = actionEnum;
        this.moveRecord = null;
        this.damageRecord = null;
        this.unitPurchaseRecord = null;
        this.unitDestroyed = unitDestroyed;
        this.destroyPosition = destroyPosition;
    }

    /**
     * @brief Get the type of action
     *
     * @return Action type
     */
    public ActionEnum getActionEnum() {
        return actionEnum;
    }

    /**
     * @brief Get the move record
     *
     * @return Move record
     */
    public MoveRecord getMoveRecord() {
        return moveRecord;
    }

    /**
     * @brief Get the damage record
     *
     * @return Damage record
     */
    public DamageRecord getDamageRecord() {
        return damageRecord;
    }

    /**
     * @brief Get the unit purchased record
     *
     * @return Unit purchased record
     */
    public UnitPurchaseRecord getUnitPurchaseRecord() {
        return unitPurchaseRecord;
    }

    /**
     * @brief Get the unit destroyed
     *
     * @return Destroyed unit
     */
    public Unit getUnitDestroyed() {
        return unitDestroyed;
    }

    /**
     * @brief Get the position of a destroyed unit
     *
     * @return Position of a destroyed unit
     */
    public Position getDestroyPosition() {
        return destroyPosition;
    }
}
