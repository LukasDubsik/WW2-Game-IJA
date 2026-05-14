package replay;

import model.map.Position;
import model.unit.Unit;
import model.unit.UnitType;
import replay.records.*;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * @class TurnRecord
 * @brief Holder of information of each turn in a replay
 */
public class TurnRecord implements Serializable {

    private final ArrayList<BuildingIntegrityRecord> birList = new ArrayList<>(); ///< List of all building integrity and owner changes
    private final ArrayList<RepairRecord> repairList = new ArrayList<>(); ///< List of all repaired units
    private final ArrayList<Action> actionList = new ArrayList<>(); ///< List of all actions the player has made this current turn
    private IncomeRecord incomeRecord; ///< Record of the income the current player got for that turn

    public TurnRecord() {
    }

    /**
     * @brief Copy the turn record of a replay
     *
     * @param turnRecord Turn record in the previous replay
     */
    public TurnRecord(TurnRecord turnRecord){
        birList.addAll(turnRecord.birList);
        this.incomeRecord = turnRecord.incomeRecord;
        this.actionList.addAll(turnRecord.actionList);
        this.repairList.addAll(turnRecord.repairList);
    }

    /**
     * @brief Adds a new move to the turn record
     *
     * @param pos1 The previous position of a unit
     * @param pos2 The new position of a unit
     */
    public void addMove(Position pos1, Position pos2){
        actionList.add(new Action(Action.ActionEnum.MOVE, new MoveRecord(pos1, pos2)));
    }

    /**
     * @brief Adds a new record of building change in integrity or owner
     *
     * @param buildingPos Position of the building
     * @param change value of the change
     * @param prevOwner record of the owner in the current turn
     */
    public void addBir(Position buildingPos, int change, String prevOwner){
        birList.add(new BuildingIntegrityRecord(buildingPos, change, prevOwner));
    }

    /**
     * @brief Get the list of building changes in that turn
     *
     * @return List of all building changes
     */
    public ArrayList<BuildingIntegrityRecord> getBirList() {
        return birList;
    }

    /**
     * @brief Adds a new record of damage dealt
     *
     * @param pos Position of the damage receiver
     * @param damage Damage dealt
     */
    public void addDamageRecord(Position pos, int damage){
        actionList.add(new Action(Action.ActionEnum.DAMAGE, new DamageRecord(pos, damage)));
    }

    /**
     * @brief Adds a new record of a destroyed unit
     *
     * @param unit Destroyed unit
     */
    public void addDestroyedUnit(Unit unit, Position destroPosition){
        actionList.add(new Action(Action.ActionEnum.DESTROY, unit, destroPosition));
    }

    /**
     * @brief Get the income the player has gained in that turn
     *
     * @return The income gained in that turn
     */
    public IncomeRecord getIncomeRecord() {
        return incomeRecord;
    }

    /**
     * @brief Set the new value of the income
     *
     * @param income new value of the income
     */
    public void setIncomeRecord(String player, int income) {
        this.incomeRecord = new IncomeRecord(player, income);
    }

    /**
     * @brief Clears all recorded actions for this turn.
     *
     */
    public void clearRecords() {
        birList.clear();
        actionList.clear();
        repairList.clear();
    }

    /**
     * @brief Adds a new record of a purchased unit
     *
     * @param unitType Purchased type of unit
     * @param position Position of the factory
     */
    public void addPurchasedUnit(UnitType unitType, Position position){
        actionList.add(new Action(Action.ActionEnum.BUY, new UnitPurchaseRecord(unitType, position)));
    }

    /**
     * @brief Get the list of actions the player has made that turn
     *
     * @return The list of actions
     */
    public ArrayList<Action> getActionList() {
        return actionList;
    }

    /**
     * @brief Get the list of all repaired units
     *
     * @return The list of repaired units
     */
    public ArrayList<RepairRecord> getRepairList() {
        return repairList;
    }

    /**
     * @brief Adds a new record of a purchased unit
     *
     * @param position Position of the factory
     * @param cost Cost of the repair
     */
    public void addRepairedUnit(Position position, int cost){
        repairList.add(new RepairRecord(position, cost));
    }
}
