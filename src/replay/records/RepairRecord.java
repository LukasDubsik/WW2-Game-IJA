/**
 * @file RepairRecord.java
 * @author xdubsil00, xbobekp00
 * @brief Source file RepairRecord.java for the IJA Advance-Wars-inspired game project.
 */
package replay.records;

import java.io.Serializable;

import model.map.Serializable.Position;

public record RepairRecord(Position position, int cost, int amount) implements Serializable {

    /**
     * @brief Backward-compatible constructor for older replay records
     */
    public RepairRecord(Position position, int cost) {
        this(position, cost, 20);
    }
}
