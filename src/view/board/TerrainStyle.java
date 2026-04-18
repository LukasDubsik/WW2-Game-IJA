package view.board;

import java.util.Objects;

import javafx.scene.paint.Color;

/**
 * @brief Helper holder for terrain rendering values
 */
public record TerrainStyle(String asset_path, Color fallback_color) {

    /**
     * @brief Constructor for the TerrainRenderStyle record
     */
    public TerrainStyle {
        // Check that there was a path given
        if (asset_path == null || asset_path.isBlank()) {
            throw new IllegalArgumentException("Asset path cannot be empty.");
        }

        // Define the fallback color
        fallback_color = Objects.requireNonNull(fallback_color, "Fallback color cannot be null.");
    }
}
