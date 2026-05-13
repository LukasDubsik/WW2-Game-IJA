package model.map.Serializable;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @class Map
 * @brief Holder for the whole with terrain and overlays
 */
public class GameMap implements Serializable {

    private final Terrain[][] terrainsMap; ///< The map of the game -> in terms of terrain
    private final Overlay[][] overlayMap; ///< The map of the game -> in terms of overlay

    private final int rows; ///< Number of rows of the map
    private final int columns; ///< Number of ciolumns of the map

    /**
     * @brief The constructor of the GameMap class
     *
     * @param terrainsMap The array map - already converted to the Terrain enum
     * @param overlayMap The map of all overlays
     */
    public GameMap(Terrain[][] terrainsMap, Overlay[][] overlayMap) {
        this.terrainsMap = copyInputMap(terrainsMap);
        this.rows = terrainsMap.length;
        this.columns = terrainsMap[0].length;
        this.overlayMap = copyOverlayMap(overlayMap, this.rows, this.columns);
    }

    /**
     * @brief Getter for the terrain map
     *
     * @return The terrain map
     */
    public Terrain[][] getTerrainsMap() {
        return terrainsMap;
    }

    /**
     * @brief Getter for the overlay map
     *
     * @return The overlay map
     */
    public Overlay[][] getOverlayMap() {
        return overlayMap;
    }

    /**
     * @brief Getter for the number of rows
     *
     * @return Number of rows
     */
    public int getRows() {
        return rows;
    }

    /**
     * @brief Getter for the number of columns
     *
     * @return Number of columns
     */
    public int getColumns() {
        return columns;
    }

    /**
     * @brief Copies the incoming terrain map or creates an empty one if null
     *
     * @param map_in map to copy
     */
    private static Terrain[][] copyInputMap(Terrain[][] map_in) {
        // Create an empty holder of the same size
        Terrain[][] copy = new Terrain[map_in.length][];

        // Copy element by element
        for (int i = 0; i < map_in.length; i++) {
            copy[i] = Arrays.copyOf(map_in[i], map_in[i].length);
        }

        // Return the copied form
        return copy;
    }

    /**
     * @brief Copies the incoming overlay map or creates an empty one if null
     *
     * @param overlay Overlay to copy
     * @param rows Number of rows
     * @param columns Number of columns
     */
    private static Overlay[][] copyOverlayMap(Overlay[][] overlay, int rows, int columns) {
        // CXreate the copy map
        Overlay[][] copy = new Overlay[rows][columns];

        // Fill the map with NONE as a base
        for (int row = 0; row < rows; row++) {
            Arrays.fill(copy[row], Overlay.NONE);
        }

        // If nothing was provided, no overlays are present
        if (overlay == null) {
            return copy;
        }

        // Check that dimensions match
        if (overlay.length != rows) {
            throw new IllegalArgumentException("Overlay row count must match terrain row count.");
        }

        // Then, finally, start copying
        for (int row = 0; row < rows; row++) {
            // If dimensions don't match a rectangle
            if (overlay[row] == null || overlay[row].length != columns) {
                throw new IllegalArgumentException("Overlay map must have same rectangular shape as terrain map.");
            }

            // If okay, copy
            for (int column = 0; column < columns; column++) {
                copy[row][column] = overlay[row][column] == null ? Overlay.NONE : overlay[row][column];
            }
        }

        return copy;
    }
}
