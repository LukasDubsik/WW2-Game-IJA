package view.board;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;
import model.game.Game;
import model.map.Building;
import model.map.Serializable.Overlay;
import model.map.Serializable.Position;
import model.map.Serializable.Terrain;
import model.unit.Unit;

public final class GameCanvas extends Canvas {
    
    // Holders for the internal values
    private Game game; ///< The whole game keeping the internal state
    private final double tile_size_x; ///< The size of the tile x -> will be hexagons
    private final double tile_size_y; ///< the size of the tile y

    private Consumer<Position> tileClickHandler = position -> {}; ///< The handler for the mouse click

    private static final double INITIAL_ZOOM = 1.6;
    private static final double ZOOM_STEP = 1.03;
    private static final double MIN_ZOOM = 0.85;
    private static final double MAX_ZOOM = 2.4;

    private double zoom = INITIAL_ZOOM; ///< the size of the zoom

    private final boolean[][] tiles_selected; ///< The map of the selected tiles
    private final boolean[][] movement_map; ///< The map of tiles the unit can move to

    private final Map<Terrain, Image> terrain_image_cache = new HashMap<>(); ///< Holder of cached terrain images
    private final Map<Overlay, Image> overlay_image_cache = new HashMap<>(); ///< Holder of cached overlay icons
    private final Map<String, Image> unit_image_cache = new HashMap<>(); ///< Holder of cached unit images

    Position previous_position; ///< The previously selected position

    private boolean movement_animation_running = false; ///< Whether the unit is currently animating
    private Unit animated_unit = null; ///< The unit currently being animated
    private Position animation_origin = null; ///< The original tile of the animated unit
    private Position animation_draw_position = null; ///< The current tile at which the animated unit is drawn

    private boolean[][] attack_map; ///< Which tiles are currently attackable by the selected unit

    private Image static_map_cache = null; ///< Cached static terrain / overlay layer
    private double static_map_cache_zoom = -1.0; ///< Zoom of the cached terrain layer
    private double static_map_cache_width = -1.0; ///< Width of the cached terrain layer
    private double static_map_cache_height = -1.0; ///< Height of the cached terrain layer

    private final PauseTransition zoom_rebuild_timer = new PauseTransition(Duration.millis(140)); ///< Delay before rebuilding HQ cache after zoom stops
    private boolean draw_scheduled = false; ///< Whether a redraw is already queued into JavaFX

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

        attack_map = new boolean[game.getRows()][game.getColumns()];

        // Initialized the movement map so all are false
        movement_map = new boolean[game.getRows()][game.getColumns()];

        // Configure delayed cache rebuild after zooming stops
        zoom_rebuild_timer.setOnFinished(event -> {
            rebuildStaticMapCache();
            requestDraw();
        });

        // Draw the current game
        updateCanvasSize();

        Platform.runLater(() -> {
            rebuildStaticMapCache();
            draw();
        });
    }

    public void setGame(Game game){
        this.game = game;
        invalidateStaticMapCache();
        rebuildStaticMapCache();
        requestDraw();
    }

    /**
     * @brief Mark the cache as outdated but keep the old image for temporary zoom preview
     */
    private void markStaticMapCacheStale() {
        static_map_cache_zoom = -1.0;
        static_map_cache_width = -1.0;
        static_map_cache_height = -1.0;
    }

    /**
     * @brief Schedule one redraw into the next JavaFX pulse
     */
    private void requestDraw() {
        if (draw_scheduled) {
            return;
        }

        draw_scheduled = true;

        Platform.runLater(() -> {
            draw_scheduled = false;
            draw();
        });
    }

    /**
     * @brief Handle the click of the mouse on the canvas
     * 
     * @param x The x coordinate of the click
     * @param y The y coordinate of the click
     */
    public void handleClick(double x, double y) {
        // Ignore clicks while animation is running
        if (movement_animation_running) {
            return;
        }

        // Lock gameplay interaction after victory
        if (game.isFinished()) {
            return;
        }

        // Find the hexagon that has been clicked
        Position clicked = findHexAt(x, y);

        // Clicking outside of the map shouldn't crash the whole system
        if (clicked == null) {
            return;
        }

        // If the tile is an attack option, perform the attack
        if (attack_map[clicked.row()][clicked.column()]) {
            if (previous_position != null) {

                // Turn off replay mode
                if (game.isReplayMode()) {
                    game.setReplayMode(false);
                    game.getReplay().branchTimeline();
                }

                game.attackUnit(previous_position, clicked);
            }

            clearSelections();
            draw();
            return;
        }

        // If the tile is one of the movement options, animate movement and stop here
        if (movement_map[clicked.row()][clicked.column()]) {

            // Turn off replay mode
            if (game.isReplayMode()) {
                game.setReplayMode(false);
                game.getReplay().branchTimeline();
            }

            animateMovement(previous_position, clicked);

            return;
        }

        // Check whether we clicked the same active unit again -> temporary wait / end action
        Unit clicked_unit = game.getUnit(clicked);
        if (clicked_unit != null
                && previous_position != null
                && clicked.equals(previous_position)
                && clicked_unit.getOwner().equals(game.getCurrentPlayer())
                && !clicked_unit.hasAlreadyPlayed()) {

            game.finishUnitAction(clicked);
            clearSelections();
            draw();
            return;
        }

        // Reset all selections when a new normal click occurs
        resetSelections();

        // Run the event registered for it
        tileClickHandler.accept(clicked);

        // Based on whether there is a unit on the tile
        Unit unit = game.getUnit(clicked);

        // If there is a unit, and it belongs to the active player, show its legal actions
        if (unit != null
                && unit.getOwner().equals(game.getCurrentPlayer())
                && !unit.hasAlreadyPlayed()) {

            // Mark all reachable movement tiles
            List<Position> tiles_possible = game.getReachableTiles(clicked);
            for (Position tile : tiles_possible) {
                movement_map[tile.row()][tile.column()] = true;
            }

            // Mark all attackable enemy tiles
            List<Position> attackable_tiles = game.getAttackableTiles(clicked);
            for (Position tile : attackable_tiles) {
                attack_map[tile.row()][tile.column()] = true;
            }

            // Keep the unit tile visibly selected
            tiles_selected[clicked.row()][clicked.column()] = true;

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

        // Attempt to load the actual image asset
        Image image = loadUnitImage(unit);

        // Units that already acted are drawn dimmed, so turn state is visible on the map
        gc.save();
        if (unit.hasAlreadyPlayed()) {
            gc.setGlobalAlpha(0.48);
        }

        // If the image failed to load, fall back to the text label
        if (image == null) {
            gc.setFill(ownerColor(unit.getOwner()));
            gc.setFont(Font.font(18));
            gc.fillText(unitLabel(unit), x - getTileX() * 0.08, y + getTileY() * 0.06);
        } else {
            // Draw the unit image centered on the tile
            double draw_w = getTileX() * 0.63;
            double draw_h = getTileY() * 0.63;

            gc.drawImage(
                    image,
                    x - draw_w / 2.0,
                    y - draw_h / 2.0,
                    draw_w,
                    draw_h
            );
        }
        gc.restore();

        // Draw the hp bar background
        double hp_bar_w = getTileX() * 0.42;
        double hp_bar_h = getTileY() * 0.08;
        double hp_bar_x = x - hp_bar_w / 2.0;
        double hp_bar_y = y + getTileY() * 0.22;

        gc.save();

        gc.setFill(Color.color(0.0, 0.0, 0.0, 0.78));
        gc.fillRect(hp_bar_x, hp_bar_y, hp_bar_w, hp_bar_h);

        // Draw the hp fill
        double hp_ratio = (double) unit.getCurrentHp() / unit.getUnitType().getMaxHP();
        double hp_fill_w = hp_bar_w * hp_ratio;

        // Green -> yellow -> red feeling by rough thresholds
        if (hp_ratio > 0.66) {
            gc.setFill(Color.LIMEGREEN);
        } else if (hp_ratio > 0.33) {
            gc.setFill(Color.GOLD);
        } else {
            gc.setFill(Color.RED);
        }

        gc.fillRect(hp_bar_x, hp_bar_y, hp_fill_w, hp_bar_h);

        // Border around the hp bar
        gc.setStroke(Color.BLACK);
        gc.strokeRect(hp_bar_x, hp_bar_y, hp_bar_w, hp_bar_h);

        // Draw hp number
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font(10));
        gc.fillText(
                Integer.toString(unit.getCurrentHp()),
                x - getTileX() * 0.07,
                hp_bar_y - getTileY() * 0.02
        );

        // Draw a small check mark when the unit has no action left
        if (unit.hasAlreadyPlayed()) {
            double marker_r = getTileY() * 0.10;
            gc.setFill(Color.color(0.0, 0.0, 0.0, 0.70));
            gc.fillOval(x + getTileX() * 0.22, y - getTileY() * 0.34, marker_r * 2.0, marker_r * 2.0);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font(11));
            gc.fillText("✓", x + getTileX() * 0.255, y - getTileY() * 0.20);
        }

        gc.restore();
    }

    public void setOnTileClicked(Consumer<Position> handler) {
        this.tileClickHandler = handler != null ? handler : position -> {};
    }

    public void draw() {
        GraphicsContext gc = getGraphicsContext2D();

        gc.clearRect(0, 0, getWidth(), getHeight());
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, getWidth(), getHeight());

        // During normal operation draw the cached terrain layer.
        // If zoom changed and the cache is stale, temporarily stretch the old cache
        // so zoom remains smooth, then rebuild it shortly after zooming stops.
        if (isStaticMapCacheValid()) {
            gc.drawImage(static_map_cache, 0, 0, getWidth(), getHeight());
        } else if (static_map_cache != null) {
            gc.drawImage(static_map_cache, 0, 0, getWidth(), getHeight());
        } else {
            drawStaticMap(gc);
        }

        gc.setFont(Font.font(16));

        // Draw only dynamic content on top
        for (int row = 0; row < game.getRows(); row++) {
            for (int column = 0; column < game.getColumns(); column++) {
                Position pos = new Position(row, column);
                Unit unit = game.getUnit(pos);

                double x = getHexX(row, column);
                double y = getHexY(row);

                double[] x_points = getPointsX(x);
                double[] y_points = getPointsY(y);

                if (tiles_selected[row][column]) {
                    gc.save();
                    gc.setFill(Color.color(0, 0, 1.0, 0.22));
                    gc.fillPolygon(x_points, y_points, 6);
                    gc.restore();
                }

                if (movement_map[row][column]) {
                    gc.save();
                    gc.setFill(Color.color(0, 0, 1.0, 0.42));
                    gc.fillPolygon(x_points, y_points, 6);
                    gc.restore();
                }

                if (attack_map[row][column]) {
                    gc.save();
                    gc.setFill(Color.color(1.0, 0.0, 0.0, 0.42));
                    gc.fillPolygon(x_points, y_points, 6);
                    gc.restore();
                }

                drawBuildingState(gc, pos, x, y);

                if (movement_animation_running
                        && animated_unit != null
                        && animation_origin != null
                        && pos.equals(animation_origin)
                        && unit == animated_unit) {
                    unit = null;
                }

                if (unit != null) {
                    drawUnit(gc, unit, pos);
                }
            }
        }

        if (movement_animation_running
                && animated_unit != null
                && animation_draw_position != null) {
            drawUnit(gc, animated_unit, animation_draw_position);
        }
    }

    /**
     * @brief Draw owner and capture state for a building tile
     *
     * @param gc Graphics context
     * @param pos Tile position
     * @param x Tile center X
     * @param y Tile center Y
     */
    private void drawBuildingState(GraphicsContext gc, Position pos, double x, double y) {
        Building building = game.getBuilding(pos);
        if (building == null) {
            return;
        }

        double flag_w = getTileX() * 0.26;
        double flag_h = getTileY() * 0.06;
        double flag_x = x - flag_w / 2.0;
        double flag_y = y - getTileY() * 0.39;

        gc.save();

        // Owner marker
        gc.setFill(ownerColor(building.getOwner()));
        gc.fillRect(flag_x, flag_y, flag_w, flag_h);
        gc.setStroke(Color.BLACK);
        gc.strokeRect(flag_x, flag_y, flag_w, flag_h);

        // Capture / integrity marker when the building is being reduced
        if (!building.isFull()) {
            double bar_w = getTileX() * 0.34;
            double bar_h = getTileY() * 0.055;
            double bar_x = x - bar_w / 2.0;
            double bar_y = y + getTileY() * 0.36;
            double ratio = (double) building.getIntegrity() / (double) building.getMaxIntegrity();

            gc.setFill(Color.color(0.0, 0.0, 0.0, 0.78));
            gc.fillRect(bar_x, bar_y, bar_w, bar_h);
            gc.setFill(Color.ORANGE);
            gc.fillRect(bar_x, bar_y, bar_w * ratio, bar_h);
            gc.setStroke(Color.BLACK);
            gc.strokeRect(bar_x, bar_y, bar_w, bar_h);
        }

        gc.restore();
    }

    /**
     * Draw terrain, overlays and borders directly onto the main canvas.
     */
    private void drawStaticMap(GraphicsContext gc) {
        for (int row = 0; row < game.getRows(); row++) {
            for (int column = 0; column < game.getColumns(); column++) {
                Position pos = new Position(row, column);

                Terrain terrain = game.getTerrain(pos);
                Overlay overlay = game.getOverlay(pos);

                double x = getHexX(row, column);
                double y = getHexY(row);

                double[] x_points = getPointsX(x);
                double[] y_points = getPointsY(y);

                drawTerrainTile(gc, terrain, x, y, x_points, y_points);

                if (overlay != Overlay.NONE) {
                    drawOverlayIcon(gc, overlay, x, y);
                }

                gc.setStroke(Color.BLACK);
                gc.strokePolygon(x_points, y_points, 6);
            }
        }
    }

    /**
     * @brief Check whether the cached static terrain layer is still valid
     * 
     * @return True if the cache may be reused
     */
    private boolean isStaticMapCacheValid() {
        return static_map_cache != null
                && Double.compare(static_map_cache_zoom, zoom) == 0
                && Math.abs(static_map_cache_width - getWidth()) < 0.1
                && Math.abs(static_map_cache_height - getHeight()) < 0.1;
    }

    /**
     * @brief Invalidate the cached static terrain layer
     */
    private void invalidateStaticMapCache() {
        static_map_cache = null;
        static_map_cache_zoom = -1.0;
        static_map_cache_width = -1.0;
        static_map_cache_height = -1.0;
    }

    /**
     * @brief Rebuild the cached static terrain layer
     * 
     * This contains:
     * - terrain art
     * - overlay art
     * - hex borders
     * 
     * Dynamic highlights and units are still drawn every frame separately.
     */
    private void rebuildStaticMapCache() {
        Canvas layer = new Canvas(getWidth(), getHeight());
        GraphicsContext gc = layer.getGraphicsContext2D();

        gc.clearRect(0, 0, layer.getWidth(), layer.getHeight());
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, layer.getWidth(), layer.getHeight());

        for (int row = 0; row < game.getRows(); row++) {
            for (int column = 0; column < game.getColumns(); column++) {
                Position pos = new Position(row, column);

                Terrain terrain = game.getTerrain(pos);
                Overlay overlay = game.getOverlay(pos);

                double x = getHexX(row, column);
                double y = getHexY(row);

                double[] x_points = getPointsX(x);
                double[] y_points = getPointsY(y);

                drawTerrainTile(gc, terrain, x, y, x_points, y_points);

                if (overlay != Overlay.NONE) {
                    drawOverlayIcon(gc, overlay, x, y);
                }

                gc.setStroke(Color.BLACK);
                gc.strokePolygon(x_points, y_points, 6);
            }
        }

        WritableImage snapshot = new WritableImage(
                Math.max(1, (int) Math.ceil(getWidth())),
                Math.max(1, (int) Math.ceil(getHeight()))
        );

        static_map_cache = layer.snapshot(new SnapshotParameters(), snapshot);
        static_map_cache_zoom = zoom;
        static_map_cache_width = getWidth();
        static_map_cache_height = getHeight();
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
     * @brief Load the image for a unit type from disk
     * 
     * @param unit The unit for which to load the image
     * @return The loaded image, or null if loading failed
     */
    private Image loadUnitImage(Unit unit) {
        String path = unit.getUnitType().getAssetPath();

        // Return cached copy if present.
        // containsKey() is used because a missing/broken asset is cached as null.
        if (unit_image_cache.containsKey(path)) {
            return unit_image_cache.get(path);
        }

        try {
            File file = new File(path);
            if (!file.exists()) {
                System.err.println("Missing unit asset: " + path);
                unit_image_cache.put(path, null);
                return null;
            }

            Image image = new Image(file.toURI().toString());

            if (image.isError()) {
                System.err.println("Failed to load unit asset: " + path);
                if (image.getException() != null) {
                    image.getException().printStackTrace();
                }
                unit_image_cache.put(path, null);
                return null;
            }

            unit_image_cache.put(path, image);
            return image;
        } catch (Exception exception) {
            System.err.println("Error while loading unit asset: " + path);
            exception.printStackTrace();
            unit_image_cache.put(path, null);
            return null;
        }
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
        List<Position> path = game.getMovementPath(from, to);
        if (path == null || path.isEmpty()) {
            return;
        }

        Unit unit = game.getUnitAt(from);
        if (unit == null) {
            return;
        }

        movement_animation_running = true;
        animated_unit = unit;
        animation_origin = from;
        animation_draw_position = from;

        Timeline timeline = new Timeline();

        // Start at 1 so there is no artificial pause on the original tile
        for (int i = 1; i < path.size(); i++) {
            Position step = path.get(i);

            timeline.getKeyFrames().add(
                new KeyFrame(Duration.millis(85 * i), event -> {
                    animation_draw_position = step;
                    draw();
                })
            );
        }

        timeline.setOnFinished(event -> {
            boolean moved_ok = game.moveUnit(from, to, false);

            // If the logical move failed for any reason, just reset the UI safely
            if (!moved_ok) {
                movement_animation_running = false;
                animated_unit = null;
                animation_origin = null;
                animation_draw_position = null;

                clearSelections();
                draw();
                return;
            }

            movement_animation_running = false;
            animated_unit = null;
            animation_origin = null;
            animation_draw_position = null;

            // Reset the old visual state first
            resetSelections();

            // If the moved unit can still attack, show the attack options immediately
            Unit moved_unit = game.getUnitAt(to);
            if (moved_unit != null
                    && moved_unit.getOwner().equals(game.getCurrentPlayer())
                    && !moved_unit.hasAlreadyPlayed()) {

                List<Position> attackable_tiles = game.getAttackableTiles(to);

                // If there are attack options, keep the unit selected and show them
                if (!attackable_tiles.isEmpty()) {
                    tiles_selected[to.row()][to.column()] = true;

                    for (Position tile : attackable_tiles) {
                        attack_map[tile.row()][tile.column()] = true;
                    }

                    previous_position = to;
                    draw();
                    return;
                }
            }

            // Otherwise simply clear everything and redraw
            clearSelections();
            draw();
        });

        timeline.playFromStart();
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

    public void zoomIn() {
        zoomBy(ZOOM_STEP);
    }

    public void zoomOut() {
        zoomBy(1.0 / ZOOM_STEP);
    }

    /**
     * Change zoom by a multiplicative factor.
     *
     * @param factor value above 1 zooms in, value below 1 zooms out
     * @return true if zoom really changed
     */
    public boolean zoomBy(double factor) {
        if (factor <= 0.0 || Double.isNaN(factor) || Double.isInfinite(factor)) {
            return false;
        }

        double old_zoom = zoom;
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * factor));

        if (Math.abs(zoom - old_zoom) < 0.0001) {
            return false;
        }

        // Resize immediately so ScrollPane logic still works correctly
        updateCanvasSize();

        // Keep the previous cache image for a temporary stretched preview
        // and rebuild the expensive terrain layer only after zooming settles.
        markStaticMapCacheStale();

        // Fast preview redraw
        requestDraw();

        // Delay the expensive rebuild until the user stops zooming
        zoom_rebuild_timer.playFromStart();

        return true;
    }

    /**
     * @brief Change the canvas size based on the zoomed system
     */
    private void updateCanvasSize() {
        setWidth(canvasWidth(game.getColumns(), getTileX()));
        setHeight(canvasHeight(game.getRows(), getTileY()));
    }

    /**
     * @brief Reset all current tile selections and movement / attack highlights
     */
    private void resetSelections() {
        for (boolean[] row : tiles_selected) {
            Arrays.fill(row, false);
        }

        for (boolean[] row : movement_map) {
            Arrays.fill(row, false);
        }

        for (boolean[] row : attack_map) {
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
