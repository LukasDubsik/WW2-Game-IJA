/**
 * @file BuildingIntegrityRecord.java
 * @author Team
 * @brief Source file BuildingIntegrityRecord.java for the IJA Advance-Wars-inspired game project.
 */
package replay.records;

import java.io.Serializable;

import model.map.Serializable.Position;

/**
 * @brief Immutable record of building integrity change and owner change.
 *
 * @param buildingPos position of the building
 * @param change change in building integrity
 * @param prevOwner record of the owner at that specific turn
 */
public record BuildingIntegrityRecord(Position buildingPos, int change, String prevOwner) implements Serializable {
}
