/**
 * @file UnitPurchaseRecord.java
 * @author xdubsil00, xbobekp00
 * @brief Source file UnitPurchaseRecord.java for the IJA Advance-Wars-inspired game project.
 */
package replay.records;

import java.io.Serializable;

import model.map.Serializable.Position;
import model.unit.UnitType;

/**
 * @brief Immutable record of purchased units
 *
 * @param unitType Type of unit being purchased
 * @param position position of the factory
 */
public record UnitPurchaseRecord(UnitType unitType, Position position) implements Serializable {
}
