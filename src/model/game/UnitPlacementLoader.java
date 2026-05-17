/**
 * @file UnitPlacementLoader.java
 * @author xdubsil00, xbobekp00
 * @brief Source file UnitPlacementLoader.java for the IJA Advance-Wars-inspired game project.
 */
package model.game;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * @class UnitPlacementLoader
 * @brief Static helper class for reading unit placement scenario files
 */
public class UnitPlacementLoader {

    /**
     * @brief Private constructor - static helper class, should not be instantiated
     */
    private UnitPlacementLoader() {

    }

    /**
     * @brief Load starting unit placements from a file into an already created Game
     * 
     * The expected format of each non-empty, non-comment line is:
     * OWNER ; UNIT_TYPE ; ROW ; COLUMN
     * 
     * Example:
     * P1 ; IS-1 Heavy Tank ; 9 ; 0
     * 
     * @param game The game into which the units are to be loaded
     * @param unit_file The path to the unit placement file
     */
    public static void loadUnits(Game game, Path unit_file) {
        // Check the inputs
        if (game == null) {
            throw new IllegalArgumentException("Game cannot be null.");
        }

        if (unit_file == null) {
            throw new IllegalArgumentException("Unit placement file path cannot be null.");
        }

        // Holder for the file lines
        List<String> lines;

        // Attempt to read the file
        try {
            lines = Files.readAllLines(unit_file);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read unit placement file: " + unit_file, e);
        }

        // Process line by line
        for (int line_index = 0; line_index < lines.size(); line_index++) {
            String raw_line = lines.get(line_index);

            // Null line should not normally happen, but protect against it anyway
            if (raw_line == null) {
                throw new IllegalArgumentException("Null line encountered in unit file at line " + (line_index + 1) + ".");
            }

            String line = raw_line.trim();

            // Skip empty lines
            if (line.isEmpty()) {
                continue;
            }

            // Skip comment lines
            if (line.startsWith("#")) {
                continue;
            }

            // Split by semicolon with optional surrounding whitespace
            String[] tokens = line.split("\\s*;\\s*");

            // Check the number of fields
            if (tokens.length != 4) {
                throw new IllegalArgumentException(
                        "Invalid unit placement format at line " + (line_index + 1)
                                + ". Expected 4 fields: OWNER ; UNIT_TYPE ; ROW ; COLUMN"
                );
            }

            String owner = tokens[0].trim();
            String unit_type = tokens[1].trim();

            int row;
            int column;

            // Parse the numeric position
            try {
                row = Integer.parseInt(tokens[2].trim());
                column = Integer.parseInt(tokens[3].trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Invalid row/column number in unit placement file at line " + (line_index + 1) + ".",
                        e
                );
            }

            // Create the unit inside the already existing game
            try {
                game.createUnit(unit_type, owner, row, column);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Invalid unit placement at line " + (line_index + 1) + ": " + e.getMessage(),
                        e
                );
            }
        }
    }
}