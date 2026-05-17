/**
 * @file MoveRecord.java
 * @author xdubsil00, xbobekp00
 * @brief Source file MoveRecord.java for the IJA Advance-Wars-inspired game project.
 */
package replay.records;

import java.io.Serializable;

import model.map.Serializable.Position;

/**
 * @brief Immutable record of positions
 *
 * @param pos1 Previous position
 * @param pos2 New position
 */
public record MoveRecord(Position pos1, Position pos2) implements Serializable {
}
