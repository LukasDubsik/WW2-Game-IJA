package model.map;

import java.util.Arrays;
import java.util.List;

/**
 * @class Map
 * @brief Holder for the whole with terrain and overlays
 */
public class Map {
    
    private final Terrain[][] terrain_map;
    private final List<FullOverlay> overlay_map;
    private final String map_name;

    /**
     * @brief Constructor of the Map class. Copies the terrain map.
     * 
     * @param terrain_map_ The map of teh terrain.
     * @param overlay_map_ The list of teh overlays and their possitions
     * @param map_name_ The name of the map
     */
    public Map(Terrain[][] terrain_map_, List<FullOverlay> overlay_map_, String map_name_) {

        // Keep the number of columns to check it is rectangle
        int columns = terrain_map_[0].length;
        // Create the map to copy into
        Terrain[][] map_copied = new Terrain[terrain_map_.length][];

        // Copy the terrain map
        for (int row = 0; row < terrain_map_.length; row++) {
            // Check not null
            if (terrain_map_[row] == null) {
                throw new IllegalArgumentException("Terrain row cannot be null.");  
            }

            // Acquire the row
            Terrain[] row_ = terrain_map_[row];

            // It must not be empty
            if (row_.length == 0) {
                throw new IllegalArgumentException("Terrain row cannot be empty.");
            }

            // If not rectangular
            if (columns != row_.length) {
                throw new IllegalArgumentException("Terrain grid must be rectangular.");
            }

            // Then copy that row
            map_copied[row] = Arrays.copyOf(row_, row_.length);
        }

        // Set the local values
        this.terrain_map = map_copied;
        this.map_name = map_name_;

        // If the overlays are empty
        if (overlay_map_ == null) {
            this.overlay_map = List.of();
        } else {
            this.overlay_map = overlay_map_;
        }
    }

    /**
     * @brief Get the map's name
     * 
     * @return The map's name
     */
    public String getName() {
        return this.map_name;
    }

    /**
     * @brief Get the list of the overlays for the map
     * 
     * @return The map's overlays
     */
    public List<FullOverlay> getOverlays() {
        return this.overlay_map;
    }

    /**
     * @brief Get the copy of the terrain map. This is necessary so no changes may happen.
     * 
     * @return The terrain map.
     */
    public Terrain[][] getTerrainMap() {
        Terrain[][] copy = new Terrain[terrain_map.length][];
        for (int i = 0; i < terrain_map.length; i++) {
            copy[i] = Arrays.copyOf(terrain_map[i], terrain_map[i].length);
        }
        return copy;
    }
}
