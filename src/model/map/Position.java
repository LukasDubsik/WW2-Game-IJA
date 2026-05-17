/**
 * @file Position.java
 * @author xdubsil00, xbobekp00
 * @brief Source file Position.java for the IJA Advance-Wars-inspired game project.
 */
package model.map;

/**
 * @brief Immutable board position addressed as [row, column].
 *
 * @param row zero-based row index
 * @param column zero-based column index
 */
public record Position(int row, int column) {
    /**
     * @brief Convert teh position to expected string format
     */
    @Override
    public String toString() {
        return ("[" + row + "," + column + "]");
    }
}
