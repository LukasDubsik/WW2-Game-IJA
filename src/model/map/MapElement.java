package model.map;

public class MapElement {

    private final Overlay overlay; ///< Holder of the Overlay enum
    private final Terrain terrain; ///< Holder of the Terrain enum

    /**
     * @brief Constructor of the MapElemeent class. Converts the string form of the 
     *        overlay/terrain into its enum form.
     * 
     * @param overlay_ The string code for the overlay
     * @param terrain_ The string form of the terrain
     */
    public MapElement(String overlay_, String terrain_) {
        // If conversion fails, the enums have their own error haandlers
        this.terrain = Terrain.convert(terrain_);
        this.overlay = Overlay.convert(overlay_);
    }

    public MapElement(String elem) {
        // Strip the laast and first elem -> the [...]
        String inside = elem.substring(1, elem.length() - 1);

        // Check that in the middle (second position) lays ","
        if (!",".equals(inside.substring(2))) {
            throw new IllegalArgumentException("The map is elem is not of the form [X,Y]");
        }

        // Take the first and last -> the X,Y
        // First should be a terrain, the second the overlay
        String terrain_ = inside.substring(1);
        String overlay_ = inside.substring(inside.length() - 1);

        // Convert to the enums
        this.terrain = Terrain.convert(terrain_);
        this.overlay = Overlay.convert(overlay_);
    }
    
    
}
