package replay;

import model.map.Position;
import model.unit.UnitType;

import java.io.Serializable;

/**
 * @brief Immutable record of purchased units
 *
 * @param unitType Type of unit being purchased
 * @param position position of the factory
 */
public record UnitPurchaseRecord(UnitType unitType, Position position) implements Serializable {
}
