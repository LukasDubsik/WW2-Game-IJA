package ija.ija2025.homework2.common;

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
