package app;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.game.Game;
import model.game.GameFactory;
import model.map.Terrain;
import model.unit.Unit;
import view.board.GameCanvas;

public class StartApp extends Application {

    /**
     * @brief the Main method to start the application as a whole
     */
    @Override
    public void start(Stage stage) {

        // Temporary data for testing the app starts and load
        String[] map_test = {
            "P P F C H",
            "P W F P P",
            "M P P F P",
            "P F P P P",
            "H P P C P"
        };

        // Load the map into a working game -> using the game factory
        Game game = GameFactory.createGame(map_test);

        // Preload some testing units -> This will be removed later, for testing
        game.createUnit("Infantry", "P1", 0, 0);
        game.createUnit("Tank", "P1", 2, 1);
        game.createUnit("Artillery", "P2", 4, 4);

        // Experimenting with some base labels
        Label infoLabel = new Label("Click a tile.");
        infoLabel.setWrapText(true); //Make text dynamic
        
        // Create the game canvas
        GameCanvas canvas = new GameCanvas(game, 80, 70);

        // What happens when tile is clicked by a mouse
        // TODO: This is currently a temporary method for debugging
        canvas.setOnTileClicked(position -> {
            // The values at the position in the game
            Terrain terrain = game.getTerrain(position);
            Unit unit = game.getUnit(position);

            // Construct the informations to be shown
            StringBuilder text = new StringBuilder();
            text.append("Tile: [")
                    .append(position.row())
                    .append(",")
                    .append(position.column())
                    .append("]\n");
            text.append("Terrain: ").append(terrain).append("\n");

            if (unit == null) {
                text.append("Unit: none");
            } else {
                text.append("Unit: ").append(unit);
            }

            // Set the text label in teh left corner
            infoLabel.setText(text.toString());
        });

        // Create a side panel for info display
        VBox sidePanel = new VBox(12, infoLabel); // Spacing of 12 px between future children
        sidePanel.setPadding(new Insets(12)); // 
        sidePanel.setPrefWidth(220);

        // Arrange the layout of the game
        BorderPane root = new BorderPane();
        // The infor is on the left
        root.setLeft(sidePanel);

        // Create a group for the canvas
        Group canvas_group = new Group(canvas);

        // Center the starting position of the system
        StackPane centered_pane = new StackPane(canvas_group);
        centered_pane.setAlignment(Pos.CENTER);
        centered_pane.setStyle("-fx-background-color: black;");

        // Add event when clicked onto
        centered_pane.setOnMouseClicked(event -> {
            javafx.geometry.Point2D localPoint = canvas.sceneToLocal(event.getSceneX(), event.getSceneY());

            canvas.handleClick(localPoint.getX(), localPoint.getY());

            event.consume();
        });

        // Set the system for scrolling and moving around
        ScrollPane scroller = new ScrollPane(centered_pane);
        scroller.setPannable(false);
        scroller.setStyle("-fx-background: black; -fx-background-color: black;");

        // Ensure that teh center pane for setting the scene in teh center resizes with each 
        // event upon the scroller
        scroller.viewportBoundsProperty().addListener((obs, old_bounds, new_bounds) -> {
            centered_pane.setMinSize(new_bounds.getWidth(), new_bounds.getHeight());
        });

        // Add handler for scrolling 
        scroller.addEventFilter(ScrollEvent.SCROLL, event -> {
            // Decide if we are zooming in or out
            if (event.getDeltaY() > 0) {
                canvas.zoomIn();
            } else if (event.getDeltaY() < 0) {
                canvas.zoomOut();
            }

            // Remove the event, it has been consumed
            event.consume();
        });

        // Store the previous mouse position
        final double[] mouse = new double[2];

        // When the mouse is pressed, remember the starting position for the drag
        canvas_group.setOnMousePressed(event -> {
            if (event.isPrimaryButtonDown()) {
                mouse[0] = event.getSceneX();
                mouse[1] = event.getSceneY();
            }
        });

        // The left mouse button is held and mouse is being dragged
        canvas_group.setOnMouseDragged(event -> {
            // Don't run when we are no longer holding the left button down
            if (!event.isPrimaryButtonDown()) {
                return;
            }

            // The current displacement from teh start
            double dx = event.getSceneX() - mouse[0];
            double dy = event.getSceneY() - mouse[1];

            // The new position where the mouse got
            mouse[0] = event.getSceneX();
            mouse[1] = event.getSceneY();

            // Make sure the map can be shifted only by half of the current map size
            double max_x = scroller.getViewportBounds().getWidth()/2.0;
            double max_y = scroller.getViewportBounds().getHeight()/2.0;

            // Find the true new position displacement
            double x = clamp(canvas_group.getTranslateX() + dx, -max_x, max_x);
            double y = clamp(canvas_group.getTranslateY() + dy, -max_y, max_y);

            canvas_group.setTranslateX(x);
            canvas_group.setTranslateY(y);

            // Consume the event
            event.consume();
        });

        // Center teh scroller on teh center of the all
        root.setCenter(scroller);

        // Create the scene
        Scene scene = new Scene(root);

        // Set some last game properties
        stage.setTitle("IJA game");
        stage.setScene(scene);

        // Show the game
        stage.show();
    }

    /**
     * @brief Return the value if within limits, otherwise either min (if smaller)
     *        and max, if larger than the bounds given.
     * 
     * @param value The value to clamp
     * @param min The lower bound
     * @param max The upper bound
     * 
     * @return The value, if within min/max or the min or max value.
     */
    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
