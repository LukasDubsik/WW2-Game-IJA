package model.map;

/**
 * @class FullOverlay
 * @brief Holds the position and the overlay upon it
 */
public class FullOverlay {
    
    private Position position; ///< The position on the map
    private Overlay overlay; ///< The overlay on that position

    /**
     * @brief The constructor of the class
     * 
     * @param position_ The position of the overlay
     * @param overlay_ The overlay enum
     */
    public FullOverlay(Position position_, Overlay overlay_) {
        this.position = position_;
        this.overlay = overlay_;
    }

    /**
     * @brief Get the position of the overlay
     * 
     * @return The {@link #position}
     */
    public Position getPosition() {
        return position;
    }

    /**
     * @brief Get the overlay enum
     * 
     * @return The {@link #overlay}
     */
    public Overlay getOverlay() {
        return overlay;
    }
}
