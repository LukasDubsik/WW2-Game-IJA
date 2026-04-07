package model.game;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import model.map.Terrain;

/**
 * @class GameFactory
 * @brief Class for construction of the Game class from the input map
 */
public class GameFactory {
    
    /**
     * @brief Private constructor - static "class", makes it imposible to create as a class
     */
    private GameFactory() {

    }  

    /**
     * @brief Create the game map from a file
     * 
     * @param mapFile The path to the map file
     * 
     * @return The newly created empty game
     */
    public static Game createGame(Path map) {
        // Check if the file is not empty
        if (map == null) {
            throw new IllegalArgumentException("Map file path cannot be empty!");
        }

        // Attempt to read all the map file lines
        try {
            List<String> lines = Files.readAllLines(map);
            return createGame(lines.toArray(String[]::new));
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read map file: " + map, e);
        }
    }

    /**
     * @brief Taking the input map, convert it into internal enum format and then init the Game class.
     * 
     * @param map The String map format (individual elems describing the terrain)
     * @return The created Game class from the input map in array String form
     */
    public static Game createGame(String[] map) {
        // Check that the input is valid
        if (map == null || map.length == 0) {
            throw new IllegalArgumentException("Map can't be null or have zero length!");
        }

        // How many columns are there
        int columns = -1;
        // Holder for the Terrain after conversion
        List<Terrain[]> rows = new ArrayList<>();

        // Iterate through the map and process the values
        for (String row : map) {
            if (row == null) {
                throw new IllegalArgumentException("Unknow row value (null) of the map.");
            }

            String row_proc = row.trim();
 
            if (row_proc.isEmpty()) {
                throw new IllegalArgumentException("Map can't have empty rows.");
            }

            // Convert into individual fields - Split by whitaspaces
            String[] tokens = row_proc.split("\\s+");

            // Check the columns
            if (columns == -1) {
                columns = tokens.length;
            } else if (columns != tokens.length) {
                throw new IllegalArgumentException("Map must be rectangular.");
            }

            // Create Terrain array for this row
            Terrain[] row_terrain = new Terrain[columns];

            for (int i = 0; i < tokens.length; i++) {
                row_terrain[i] = Terrain.convert(tokens[i]);
            }

            // Push back into the general List holder
            rows.add(row_terrain);
        }

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Map can't be empty.");
        }

        // Create the final array holder of the map
        Terrain[][] board_terrain = rows.toArray(Terrain[][]::new);

        // Return the create game from the map
        return new Game(board_terrain);
    }
}
