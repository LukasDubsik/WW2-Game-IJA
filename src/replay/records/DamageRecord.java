/**
 * @file DamageRecord.java
 * @author Team
 * @brief Source file DamageRecord.java for the IJA Advance-Wars-inspired game project.
 */
package replay.records;

import java.io.Serializable;

import model.map.Serializable.Position;

/**
 * @brief Immutable record of damage dealt.
 *
 * @param position position of the damaged unit
 * @param damage value of the damage dealt
 */
public record DamageRecord(Position position, int damage) implements Serializable {
}
