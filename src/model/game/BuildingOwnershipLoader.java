/**
 * @file BuildingOwnershipLoader.java
 * @author xdubsil00, xbobekp00
 * @brief Source file BuildingOwnershipLoader.java for the IJA Advance-Wars-inspired game project.
 */
package model.game;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * @class BuildingOwnershipLoader
 * @brief Static helper class for reading initial building ownership files
 */
public class BuildingOwnershipLoader {

    /**
     * @brief Private constructor - static helper class, should not be instantiated
     */
    private BuildingOwnershipLoader() {

    }

    /**
     * @brief Load initial building ownership into an already created game
     *
     * The expected format of each non-empty, non-comment line is:
     * OWNER ; ROW ; COLUMN
     *
     * Example:
     * P1 ; 4 ; 0
     *
     * @param game The game into which building ownership is loaded
     * @param building_file The building ownership file
     */
    public static void loadBuildings(Game game, Path building_file) {
        if (game == null) {
            throw new IllegalArgumentException("Game cannot be null.");
        }

        if (building_file == null) {
            throw new IllegalArgumentException("Building ownership file path cannot be null.");
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(building_file);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read building ownership file: " + building_file, e);
        }

        for (int line_index = 0; line_index < lines.size(); line_index++) {
            String raw_line = lines.get(line_index);

            if (raw_line == null) {
                throw new IllegalArgumentException("Null line encountered in building file at line " + (line_index + 1) + ".");
            }

            String line = raw_line.trim();

            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] tokens = line.split("\\s*;\\s*");
            if (tokens.length != 3) {
                throw new IllegalArgumentException(
                        "Invalid building ownership format at line " + (line_index + 1)
                                + ". Expected 3 fields: OWNER ; ROW ; COLUMN"
                );
            }

            String owner = tokens[0].trim();
            int row;
            int column;

            try {
                row = Integer.parseInt(tokens[1].trim());
                column = Integer.parseInt(tokens[2].trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Invalid row/column number in building ownership file at line " + (line_index + 1) + ".",
                        e
                );
            }

            game.setBuildingOwnership(owner, row, column);
        }
    }
}
