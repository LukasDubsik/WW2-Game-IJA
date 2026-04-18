package view.board;

import javafx.scene.paint.Color;
import model.map.Terrain;

/**
 * @class TerrainStyleCatalog
 * @brief Holder mapping the logical terrain to its visual style.
 */
public final class TerrainCatalog {

    /**
     * @brief Private constructor - static helper class only
     */
    private TerrainCatalog() {
    }

    /**
     * @brief Resolve the terrain into its visual representation
     * 
     * @param terrain The terrain type to resolve
     * 
     * @return The rendering style for the given terrain
     */
    public static TerrainStyle resolve(Terrain terrain) {
        return switch (terrain) {
            case PLAIN -> new TerrainStyle("lib/assets/terrain/plain/snow_plain.png", Color.BEIGE);
            case FOREST -> new TerrainStyle("lib/assets/terrain/forest/winter_forest.png", Color.DARKSEAGREEN);
            case MOUNTAIN -> new TerrainStyle("lib/assets/terrain/mountain/snow_ridge.png", Color.LIGHTGRAY);
            case WATER -> new TerrainStyle("lib/assets/terrain/water/winter_water.png", Color.LIGHTSKYBLUE);
            case CITY -> new TerrainStyle("lib/assets/terrain/city/winter_city.png", Color.KHAKI);
            case FACTORY -> new TerrainStyle("lib/assets/terrain/factory/winter_factory.png", Color.LIGHTCORAL);
            case HQ -> new TerrainStyle("lib/assets/terrain/hq/winter_hq.png", Color.PLUM);
        };
    }
}