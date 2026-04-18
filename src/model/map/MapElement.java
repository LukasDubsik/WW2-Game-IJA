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
    
}
