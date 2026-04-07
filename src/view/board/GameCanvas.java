package view.board;

import java.util.function.Consumer;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import model.game.Game;
import model.map.Position;
import model.map.Terrain;
import model.unit.Unit;

public class GameCanvas extends Canvas {
    
    // Holders for the internal values
    private Game game; ///< The whole game keeping the internal state
    private double tile_size_x; ///< The size of the tile x -> will be hexagons
    private double tile_size_y; ///< the size of the tile y

    private Consumer<Position> tileClickHandler = position -> {}; ///< The handler for the mouse click

    /**
     * @brief The constructor of the caanvas
     */
    public GameCanvas(Game game_, double tile_size_x_, double tile_size_y_) {
        // Call the parent constructor and define the final size
        super(game_.getColumns() * tile_size_x_, game_.getRows() * tile_size_y_);

        this.game = game_;
        this.tile_size_x = tile_size_x_;
        this.tile_size_y = tile_size_y_;

        // Set up the evnt for mouse click
        setOnMouseClicked(event -> {
            // Get the true x/y position within the map
            int column = (int)(event.getX()/tile_size_x);
            int row = (int)(event.getY()/tile_size_y);

            // If we clicked within the map
            if (row >= 0 && row < game.getRows() && column >= 0 && column < game.getColumns()) {
                // Activate the handler for the click
                tileClickHandler.accept(new Position(row, column));
            }
        });

        // Draw the current game
        draw();
    }

    public void draw() {
        // Define a 2d rectangular canvas
        GraphicsContext gc = getGraphicsContext2D();

        // Set teh size of tey drawing part
        gc.clearRect(0, 0, tile_size_x, tile_size_y);
        // And the font size
        gc.setFont(Font.font(16));

        for (int row = 0; row < game.getRows(); row++) {
            for (int column = 0; column < game.getColumns(); column++) {
                // Set up the current position
                Position pos = new Position(row, column);

                Terrain terrain = game.getTerrain(pos);
                Unit unit = game.getUnit(pos);

                // Real position on canvas for the hex
                double x = getHexX(row, column);
                double y = getHexY(row);

                // The point of the hexagon
                double[] x_points = getPointsX(x);
                double[] y_points = getPointsY(y);

                // Set the hexagon backgroud color
                gc.setFill(terrainColor(terrain));
                gc.fillPolygon(x_points, y_points, 6);

                // Set the hexagon border color
                gc.setStroke(Color.BLACK);
                gc.strokePolygon(x_points, y_points, 6);

                // Add the unit marker
                if (unit != null) {
                    // Draw the color for the owner
                    gc.setFill(ownerColor(unit.getOwner()));
                    gc.setFont(Font.font(18));
                    gc.fillText(unitLabel(unit), x - tile_size_x*0.08, y + tile_size_y*0.06);
                }
            }
        }
    }
}
