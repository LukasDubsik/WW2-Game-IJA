package replay;

import model.map.Position;
import model.unit.Unit;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * @class TurnRecord
 * @brief Holder of information of each turn in a replay
 */
public class TurnRecord implements Serializable {

    private final ArrayList<MoveRecord> moves = new ArrayList<>(); ///< List of all moves made in that turn
    private final ArrayList<BuildingIntegrityRecord> birList = new ArrayList<>(); ///< List of all building integrity and owner changes
    private final ArrayList<DamageRecord> damageList = new ArrayList<>(); ///< List of all damages dealt that round
    private final ArrayList<Unit> unitsDestroyed = new ArrayList<>(); ///< List of all destroyed units in that turn

    private int income = 0; ///< Record of the income the current player got for that turn

    public TurnRecord() {
    }

    /**
     * @brief Copy the turn record of a replay
     *
     * @param turnRecord Turn record in the previous replay
     */
    public TurnRecord(TurnRecord turnRecord){
        moves.addAll(turnRecord.moves);
        birList.addAll(turnRecord.birList);
        damageList.addAll(turnRecord.damageList);
        unitsDestroyed.addAll(turnRecord.unitsDestroyed);
        this.income = turnRecord.income;
    }

    /**
     * @brief Adds a new move to the turn record
     *
     * @param pos1 The previous position of a unit
     * @param pos2 The new position of a unit
     */
    public void addMove(Position pos1, Position pos2){
        moves.add(new MoveRecord(pos1, pos2));
    }

    /**
     * @brief Get the list of moves in that turn
     *
     * @return List of all moves
     */
    public ArrayList<MoveRecord> getMoves() {
        return moves;
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
        damageList.add(new DamageRecord(pos, damage));
    }

    /**
     * @brief Get the list of damages dealt in that turn
     *
     * @return List of all damages dealt
     */
    public ArrayList<DamageRecord> getDamageList() {
        return damageList;
    }

    /**
     * @brief Adds a new record of a destroyed unit
     *
     * @param unit Destroyed unit
     */
    public void addDestroyedUnit(Unit unit){
        unitsDestroyed.add(unit);
    }

    /**
     * @brief Get the list of destroyed units in that turn
     *
     * @return List of all destroyed units
     */
    public ArrayList<Unit> getUnitsDestroyed() {
        return unitsDestroyed;
    }

    /**
     * @brief Get the income the player has gained in that turn
     *
     * @return The income gained in that turn
     */
    public int getIncome() {
        return income;
    }

    /**
     * @brief Set the new value of the income
     *
     * @param income new value of the income
     */
    public void setIncome(int income) {
        this.income = income;
    }

    /**
     * @brief Clears all recorded actions for this turn.
     *
     */
    public void clearRecords() {
        moves.clear();
        birList.clear();
        damageList.clear();
        unitsDestroyed.clear();
    }
}
