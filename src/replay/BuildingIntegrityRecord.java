package replay;

import model.map.Position;

import java.io.Serializable;

/**
 * @brief Immutable record of building integrity change and owner change.
 *
 * @param buildingPos position of the building
 * @param change change in building integrity
 * @param prevOwner record of the owner at that specific turn
 */
public record BuildingIntegrityRecord(Position buildingPos, int change, String prevOwner) implements Serializable {
}
