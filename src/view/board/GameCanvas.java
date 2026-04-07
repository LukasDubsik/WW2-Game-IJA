package view.board;

import java.util.function.Consumer;

import javafx.scene.canvas.Canvas;
import model.game.Game;
import model.map.Position;

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
}
