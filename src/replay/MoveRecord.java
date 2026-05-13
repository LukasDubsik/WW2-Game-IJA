package replay;

import model.map.Position;

import java.io.Serializable;

/**
 * @brief Immutable record of positions
 *
 * @param pos1 Previous position
 * @param pos2 New position
 */
public record MoveRecord(Position pos1, Position pos2) implements Serializable {
}
