package view.board;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;
import model.game.Game;
import model.map.Overlay;
import model.map.Position;
import model.map.Terrain;
import model.unit.Unit;

public final class GameCanvas extends Canvas {
    
    // Holders for the internal values
    private final Game game; ///< The whole game keeping the internal state
    private final double tile_size_x; ///< The size of the tile x -> will be hexagons
    private final double tile_size_y; ///< the size of the tile y

    private Consumer<Position> tileClickHandler = position -> {}; ///< The handler for the mouse click

    private static final double INITIAL_ZOOM = 1.6;
    private static final double ZOOM_STEP = 1.05;
    private static final double MIN_ZOOM = 0.85;
    private static final double MAX_ZOOM = 2.4;

    private double zoom = INITIAL_ZOOM; ///< the size of the zoom

    private final boolean[][] tiles_selected; ///< The map of the selected tiles
    private final boolean[][] movement_map; ///< The map of tiles the unit can move to

    private final Map<Terrain, Image> terrain_image_cache = new HashMap<>(); ///< Holder of cached terrain images
    private final Map<Overlay, Image> overlay_image_cache = new HashMap<>(); ///< Holder of cached overlay icons

    Position previous_position; ///< The previously selected position

    private boolean movement_animation_running = false; ///< Whether the unit is currently animating
    private Unit animated_unit = null; ///< The unit currently being animated
    private Position animation_origin = null; ///< The original tile of the animated unit
    private Position animation_draw_position = null; ///< The current tile at which the animated unit is drawn

    /**
     * @brief The constructor of the caanvas
     */
    public GameCanvas(Game game_, double tile_size_x_, double tile_size_y_) {
        // Call the parent constructor and define the final size
        super(canvasWidth(game_.getColumns(), tile_size_x_), canvasHeight(game_.getRows(), tile_size_y_));

        this.game = game_;
        this.tile_size_x = tile_size_x_;
        this.tile_size_y = tile_size_y_;

        // Initialized the selected tiles so all are false
        tiles_selected = new boolean[game.getRows()][game.getColumns()];

        // Initialized the movement map so all are false
        movement_map = new boolean[game.getRows()][game.getColumns()];

        // Draw the current game
        updateCanvasSize();
        draw();
    }

        public void handleClick(double x, double y) {
        // Ignore clicks while animation is running
        if (movement_animation_running) {
            return;
        }

        // Find the hexagon that has been clicked
        Position clicked = findHexAt(x, y);

        // Clicking outside of the map shouldn't crash the whole system
        if (clicked == null) {
            return;
        }

        // If the tile is one of the movement options, animate movement and stop here
        if (movement_map[clicked.row()][clicked.column()]) {
            animateMovement(previous_position, clicked);
            return;
        }

        // Reset all to false when new click occurs
        resetSelections();

        // Run the event registered for it
        tileClickHandler.accept(clicked);

        // Based if there is unit on the tile
        Unit unit = game.getUnit(clicked);

        // If there is an unit, set the movement tiles
        if (unit != null) {
            List<Position> tiles_possible = game.getReachableTiles(clicked);

            // For each position that is reachable, mark it in the map
            for (Position tile : tiles_possible) {
                movement_map[tile.row()][tile.column()] = true;
            }
        } else {
            // Otherwise normal tile was clicked so register it
            tiles_selected[clicked.row()][clicked.column()] = true;
        }

        // Set this position as the previous one for next click
        previous_position = clicked;

        // Redraw the canvas
        draw();
    }

    /**
     * @brief Draw one unit at the given tile position
     * 
     * @param gc The graphics context
     * @param unit The unit to draw
     * @param pos The tile position where to draw it
     */
    private void drawUnit(GraphicsContext gc, Unit unit, Position pos) {
        // Real position on canvas for the hex
        double x = getHexX(pos.row(), pos.column());
        double y = getHexY(pos.row());

        // Draw the color for the owner
        gc.setFill(ownerColor(unit.getOwner()));
        gc.setFont(Font.font(18));
        gc.fillText(unitLabel(unit), x - getTileX()*0.08, y + getTileY()*0.06);
    }

    public void setOnTileClicked(Consumer<Position> handler) {
        this.tileClickHandler = handler != null ? handler : position -> {};
    }

    public void draw() {
        // Define a 2d rectangular canvas
        GraphicsContext gc = getGraphicsContext2D();

        gc.clearRect(0, 0, getWidth(), getHeight());
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, getWidth(), getHeight());

        // And the font size
        gc.setFont(Font.font(16));

        for (int row = 0; row < game.getRows(); row++) {
            for (int column = 0; column < game.getColumns(); column++) {
                // Set up the current position
                Position pos = new Position(row, column);

                Terrain terrain = game.getTerrain(pos);
                Unit unit = game.getUnit(pos);
                Overlay overlay = game.getOverlay(pos);

                // Real position on canvas for the hex
                double x = getHexX(row, column);
                double y = getHexY(row);

                // The point of the hexagon
                double[] x_points = getPointsX(x);
                double[] y_points = getPointsY(y);

                // Draw either the loaded terrain asset or the fallback color for it
                drawTerrainTile(gc, terrain, x, y, x_points, y_points);

                // Draw the overlay icon if an overlay exists on the tile
                if (overlay != Overlay.NONE) {
                    drawOverlayIcon(gc, overlay, x, y);
                }

                // If this tile is clicked and doesn't contain an unit
                if (tiles_selected[row][column]) {
                    // Add a blue tint to it
                    gc.save();
                    gc.setFill(Color.color(0, 0, 1.0, 0.22));
                    gc.fillPolygon(x_points, y_points, 6);
                    gc.restore();
                }

                // If the clicked tile included unit and this tile is a possible movement option
                if (movement_map[row][column]) {
                    // Add a white tint to it
                    gc.save();
                    gc.setFill(Color.color(0, 0, 1.0, 0.42));
                    gc.fillPolygon(x_points, y_points, 6);
                    gc.restore();
                }

                // Set the hexagon border color
                gc.setStroke(Color.BLACK);
                gc.strokePolygon(x_points, y_points, 6);

                // If the unit is currently being animated, do not draw it on its original tile
                if (movement_animation_running
                        && animated_unit != null
                        && animation_origin != null
                        && pos.equals(animation_origin)
                        && unit == animated_unit) {
                    unit = null;
                }

                // Add the unit marker
                if (unit != null) {
                    drawUnit(gc, unit, pos);
                }
            }
        }

        // Draw the animated unit on its temporary animation tile
        if (movement_animation_running
                && animated_unit != null
                && animation_draw_position != null) {
            drawUnit(gc, animated_unit, animation_draw_position);
        }
    }

    /**
     * @brief Find the hexagon at this position 
     * 
     * @param mouseX The position x whgere mouse clicked
     * @param mouseY The position y where mouse clicked
     * 
     * @return The Position within the game map
     */
    private Position findHexAt(double mouseX, double mouseY) {
        // Iterate through the map
        for (int row = 0; row < game.getRows(); row++) {
            for (int column = 0; column < game.getColumns(); column++) {
                // Acquire the hexagon residing at that point in the map
                double centerX = getHexX(row, column);
                double centerY = getHexY(row);

                double[] xPoints = getPointsX(centerX);
                double[] yPoints = getPointsY(centerY);

                // Determine if the clicked point lies in that hexagon
                if (pointInPolygon(mouseX, mouseY, xPoints, yPoints)) {
                    return new Position(row, column);
                }
            }
        }
        return null;
    }

    /**
     * @brief Determine if the point is inside the polygon points
     * 
     * @param px The point we are searching for -> x coordinate
     * @param py The point we are searching for -> y coordinate
     * @param xPoints The points of the polygon -> x positions
     * @param yPoints The points of the polygon -> y positions
     * 
     * @return If the point is inside the polygon
     */
    private boolean pointInPolygon(double px, double py, double[] xPoints, double[] yPoints) {
        boolean inside = false;
        int n = xPoints.length;

        // For every point of the polygon
        for (int i = 0, j = n - 1; i < n; j = i++) {
            // Does it intersect?
            boolean intersects = ((yPoints[i] > py) != (yPoints[j] > py)) && (px < (xPoints[j] - xPoints[i]) * (py - yPoints[i])/(yPoints[j] - yPoints[i]) + xPoints[i]);

            // If so, change the lie inside status
            if (intersects) {
                inside = !inside;
            }
        }

        return inside;
    }

    // Function for hex computation

    /**
     * @brief Get the x position of the hexagon
     * 
     * @param row The row position in the map
     * @param column The column position in the map
     * 
     * @return The center of the hexagon x
     */
    private double getHexX(int row, int column) {
        double row_offset = (row%2 == 1) ? getTileX()/2.0 : 0.0;
        return column*getTileX() + row_offset + getTileX()/2.0 + 20;
    }

    /**
     * @brief Get the y position of the hexagon
     * 
     * @param row The row position in the map
     * 
     * @return The center of the hexagon y
     */
    private double getHexY(int row) {
        return row*getTileY()*0.75 + getTileY()/2.0 + 30;
    }

    /**
     * @brief Get the positions of the hexagon points X
     * 
     * @param x The center of the hexagon x
     * 
     * @return The x positions of the hexagon
     */
    private double[] getPointsX(double x) {
        double half = getTileX()/2.0;

        // Return all the positions of the hexagon x
        return new double[] {x, x + half, x + half, x, x - half, x - half};
    }

    /**
     * @brief Get the positions of the hexagon points Y
     * 
     * @param y The center of the hexagon y
     * 
     * @return The y positions of the hexagon
     */
    private double[] getPointsY(double y) {
        double half = getTileY()/2.0;
        double quarter = getTileY()/4.0;

        // Return all the positions of the hexagon y
        return new double[] {y - half, y - quarter, y + quarter, y + half, y + quarter, y - quarter};
    }

    /**
     * @brief Determine the x size of the canvas
     * 
     * @param columns The number of columns of the canvas
     * @param tile_x Size of one tile in the x direction
     * 
     * @return The width of the canvas
     */
    private static double canvasWidth(int columns, double tile_x) {
        return columns*tile_x + tile_x/2.0 + 60;
    }

    /**
     * @brief Determine the y size of the canvas
     * 
     * @param rows The number of rows of the canvas
     * @param tile_y Size of one tile in the y direction
     * 
     * @return The height of the canvas
     */
    private static double canvasHeight(int rows, double tile_y) {
        return (rows - 1)*(tile_y*0.75) + tile_y + 60;
    }

    /**
     * @brief Draw the terrain tile either from asset or from fallback color
     * 
     * @param gc The graphics context used for drawing
     * @param terrain The terrain type being drawn
     * @param center_x The center x of the tile
     * @param center_y The center y of the tile
     * @param x_points The x coordinates of the tile polygon
     * @param y_points The y coordinates of the tile polygon
     */
    private void drawTerrainTile(GraphicsContext gc, Terrain terrain, double center_x, double center_y, double[] x_points, double[] y_points) {
        TerrainStyle style = TerrainCatalog.resolve(terrain);
        Image terrain_image = loadTerrainImage(terrain, style);

        // If the terrain image exists, clip the drawing into the hexagon and draw it
        if (terrain_image != null) {
            // Move to teh origin
            gc.save();
            gc.beginPath();
            gc.moveTo(x_points[0], y_points[0]);

            // Prepare teh omage lines
            for (int i = 1; i < x_points.length; i++) {
                gc.lineTo(x_points[i], y_points[i]);
            }

            // Draw the image
            gc.closePath();
            gc.clip();
            gc.drawImage(terrain_image, center_x - getTileX()/2.0, center_y - getTileY()/2.0, getTileX(), getTileY());
            gc.restore();
            return;
        }

        // Otherwise fall back to color rendering so the game remains functional
        gc.setFill(style.fallback_color());
        gc.fillPolygon(x_points, y_points, 6);
    }

    /**
     * @brief Load the terrain image into cache, if available on disk
     * 
     * @param terrain The terrain type whose image should be loaded
     * @param style The style describing where the asset is located
     * 
     * @return The loaded image or null if no valid image exists yet
     */
    private Image loadTerrainImage(Terrain terrain, TerrainStyle style) {
        // If already known, return the cached value
        if (terrain_image_cache.containsKey(terrain)) {
            return terrain_image_cache.get(terrain);
        }

        File asset_file = new File(style.asset_path());

        // If the asset isn't there yet, remember it and fall back to color rendering
        if (!asset_file.isFile()) {
            terrain_image_cache.put(terrain, null);
            return null;
        }

        try {
            Image loaded = new Image(asset_file.toURI().toString(), false);

            if (loaded.isError()) {
                terrain_image_cache.put(terrain, null);
                return null;
            }

            terrain_image_cache.put(terrain, loaded);
            return loaded;
        } catch (Exception e) {
            terrain_image_cache.put(terrain, null);
            return null;
        }
    }

    /**
     * @brief Draw the overlay icon into the upper-right corner of the tile
     * 
     * @param gc The graphics context used for drawing
     * @param overlay The overlay which should be drawn
     * @param center_x The x center of the tile
     * @param center_y The y center of the tile
     */
    private void drawOverlayIcon(GraphicsContext gc, Overlay overlay, double center_x, double center_y) {
        Image overlay_image = loadOverlayImage(overlay);

        // If the image doesn't exist, fall back to the short text marker
        if (overlay_image == null) {
            gc.save();
            gc.setFill(Color.color(1.0, 1.0, 1.0, 0.95));
            gc.setFont(Font.font(10));
            gc.fillText(
                    overlay.getShortLabel(),
                    center_x + getTileX()*0.18,
                    center_y - getTileY()*0.18
            );
            gc.restore();
            return;
        }

        // Size and placement of the icon in the top-right corner
        double icon_size = Math.min(getTileX(), getTileY()) * 0.30;
        double icon_x = center_x + getTileX()*0.17 - icon_size/2.0;
        double icon_y = center_y - getTileY()*0.19 - icon_size/2.0;

        // Draw the transparent overlay icon itself
        gc.save();
        gc.drawImage(overlay_image, icon_x, icon_y, icon_size, icon_size);
        gc.restore();
    }

    /**
     * @brief Animate the movement of a unit step by step over the path tiles
     * 
     * @param from Starting position
     * @param to Target position
     */
    private void animateMovement(Position from, Position to) {
        // Ask the game for the exact movement path
        List<Position> path = game.getMovementPath(from, to);
        if (path.isEmpty()) {
            return;
        }

        // Get the unit to animate
        Unit unit = game.getUnitAt(from);
        if (unit == null) {
            return;
        }

        // Prepare animation state
        movement_animation_running = true;
        animated_unit = unit;
        animation_origin = from;
        animation_draw_position = from;

        // Create the timeline
        Timeline timeline = new Timeline();

        // Add one frame per path tile
        for (int i = 0; i < path.size(); i++) {
            Position step = path.get(i);

            timeline.getKeyFrames().add(
                new KeyFrame(Duration.millis(120 * i), event -> {
                    animation_draw_position = step;
                    draw();
                })
            );
        }

        // Once animation finishes, perform the real move in the game state
        timeline.setOnFinished(event -> {
            movement_animation_running = false;
            animated_unit = null;
            animation_origin = null;
            animation_draw_position = null;

            // Perform the final logical move
            game.moveUnit(from, to);

            // Clear visual selections after moving
            clearSelections();

            // Redraw final state
            draw();
        });

        timeline.play();
    }

    /**
     * @brief Load the overlay icon into cache, if available on disk
     * 
     * @param overlay The overlay type whose icon should be loaded
     * 
     * @return The loaded image or null if no valid icon exists yet
     */
    private Image loadOverlayImage(Overlay overlay) {
        // If already known, return the cached value
        if (overlay_image_cache.containsKey(overlay)) {
            return overlay_image_cache.get(overlay);
        }

        File asset_file = new File(overlayAssetPath(overlay));

        // If the asset isn't there yet, remember it and fall back to text rendering
        if (!asset_file.isFile()) {
            overlay_image_cache.put(overlay, null);
            return null;
        }

        try {
            Image loaded = new Image(asset_file.toURI().toString(), false);

            if (loaded.isError()) {
                overlay_image_cache.put(overlay, null);
                return null;
            }

            overlay_image_cache.put(overlay, loaded);
            return loaded;
        } catch (Exception e) {
            overlay_image_cache.put(overlay, null);
            return null;
        }
    }

    /**
     * @brief Return the path of the icon belonging to the overlay type
     * 
     * @param overlay The overlay enum value
     * 
     * @return The asset path to the correct overlay icon
     */
    private String overlayAssetPath(Overlay overlay) {
        return switch (overlay) {
            case NONE -> "lib/assets/overlays/none.png";
            case BARBED_WIRE -> "lib/assets/overlays/barbed_wire.png";
            case TRENCH -> "lib/assets/overlays/trench.png";
            case CRATER -> "lib/assets/overlays/crater.png";
            case RUBBLE -> "lib/assets/overlays/rubble.png";
            case SNOW_DRIFT -> "lib/assets/overlays/snow_drift.png";
        };
    }

    /**
     * @brief Temporary class for owner unit color
     * @param owner
     * @return
     */
    private Color ownerColor(String owner) {
        if (Objects.equals(owner, "P1")) {
            return Color.DARKBLUE;
        }
        if (Objects.equals(owner, "P2")) {
            return Color.DARKRED;
        }
        return Color.BLACK;
    }

    /**
     * @brief Temporary return for unit symbols, using the first letter
     * 
     * @param unit The unit class containing the unit's info
     * 
     * @return The first letter of the unit's type name
     */
    private String unitLabel(Unit unit) {
        String type = unit.getUnitType().getName().toUpperCase();

        return type.substring(0, 1);
    }

    /**
     * @brief Get the right position of the tile x coordinate relative to zoom
     * 
     * @return The tile's correct position
     */
    private int getTileX() {
        return (int)(tile_size_x*zoom);
    }

    /**
     * @brief Get the right position of the tile y coordinate relative to zoom
     * 
     * @return The tile's correct position
     */
    private int getTileY() {
        return (int)(tile_size_y*zoom);
    }

    /**
     * @brief Change the zoom when zooming in (called from outside teh canvas)
     */
    public void zoomIn() {
        zoom *= ZOOM_STEP;
        zoom = Math.min(zoom, MAX_ZOOM);

        updateCanvasSize();
        draw();
    }

    /**
     * @brief Change the zoom when zooming out (called from outside teh canvas)
     */
    public void zoomOut() {
        zoom /= ZOOM_STEP;
        zoom = Math.max(zoom, MIN_ZOOM);

        updateCanvasSize();
        draw();
    }

    /**
     * @brief Change the canvas size based on the zoomed system
     */
    private void updateCanvasSize() {
        setWidth(canvasWidth(game.getColumns(), getTileX()));
        setHeight(canvasHeight(game.getRows(), getTileY()));
    }

    /**
     * @brief Reset all current tile highlights/selections
     */
    private void resetSelections() {
        for (boolean[] row : tiles_selected) {
            Arrays.fill(row, false);
        }

        for (boolean[] row : movement_map) {
            Arrays.fill(row, false);
        }

        previous_position = null;
    }

    /**
     * @brief Public helper to clear all selections and redraw the canvas
     */
    public void clearSelections() {
        resetSelections();
        draw();
    }
}
