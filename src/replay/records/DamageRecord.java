package replay.records;

import model.map.Serializable.Position;

import java.io.Serializable;

/**
 * @brief Immutable record of damage dealt.
 *
 * @param position position of the damaged unit
 * @param damage value of the damage dealt
 */
public record DamageRecord(Position position, int damage) implements Serializable {
}
