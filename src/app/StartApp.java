package app;

import java.nio.file.Path;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.game.Game;
import model.game.GameFactory;
import model.map.Overlay;
import model.map.Terrain;
import model.unit.Unit;
import view.board.GameCanvas;

public class StartApp extends Application {

    /**
     * @brief the Main method to start the application as a whole
     */
    @Override
    public void start(Stage stage) {
        // Load one of the generated historical larger maps from file
        // To switch scenario, change the file name below.
        Path map_path = Path.of("lib/maps/balga_heiligenbeil_corridor_1945_large.map");

        // Load the map into a working game -> using the game factory
        Game game = GameFactory.createGame(map_path);

        // Preload some testing units directly in code for now
        // P1 = Soviets attacking from the west / south-west
        // P2 = Germans defending toward the coastal corridor

        // Soviet testing force
        game.createUnit("IS-1 Heavy Tank", "P1", 9, 0);
        game.createUnit("IS-1 Heavy Tank", "P1", 10, 1);
        game.createUnit("M3 Half-track", "P1", 8, 1);
        game.createUnit("ZiS-3 Field Gun", "P1", 8, 0);
        game.createUnit("Soviet Assault Sapper Squad", "P1", 9, 2);
        game.createUnit("DP-27 Team", "P1", 10, 2);
        game.createUnit("BA-64 Armored Car", "P1", 11, 0);

        // German testing force
        game.createUnit("Panzer IV Ausf. J", "P2", 3, 23);
        game.createUnit("Panzer IV Ausf. J", "P2", 5, 24);
        game.createUnit("Sd.Kfz. 251/1 Half-track", "P2", 2, 22);
        game.createUnit("Sd.Kfz. 234/2 Puma", "P2", 7, 24);
        game.createUnit("Wehrmacht Rifle Squad", "P2", 1, 21);
        game.createUnit("Grenadier Squad", "P2", 4, 22);
        game.createUnit("MG 42 Team", "P2", 0, 23);

        // Experimenting with some base labels
        Label infoLabel = new Label("Click a tile.");
        infoLabel.setWrapText(true); // Make text dynamic

        // Label holding the currently active turn/player
        Label turnLabel = new Label();
        updateTurnLabel(turnLabel, game);

        // Create the game canvas
        GameCanvas canvas = new GameCanvas(game, 80, 70);

        // What happens when tile is clicked by a mouse
        // TODO: This is currently a temporary method for debugging
        canvas.setOnTileClicked(position -> {
            // The values at the position in the game
            Terrain terrain = game.getTerrain(position);
            Unit unit = game.getUnit(position);
            Overlay overlay = game.getOverlay(position);

            // Construct the informations to be shown
            StringBuilder text = new StringBuilder();
            text.append("Tile: [")
                    .append(position.row())
                    .append(",")
                    .append(position.column())
                    .append("]\n");
            text.append("Terrain: ").append(terrain).append("\n");
            text.append("Overlay: ").append(overlay).append("\n");
            text.append("Defence bonus: ").append(game.getCombinedDefenceBonus(position)).append("\n");
            text.append("Move cost Infantry: ").append(formatMovementCost(game.getCombinedMovementInfantry(position))).append("\n");
            text.append("Move cost Vehicle: ").append(formatMovementCost(game.getCombinedMovementVehicle(position))).append("\n");

            if (unit == null) {
                text.append("Unit: none");
            } else {
                text.append("Unit: ").append(unit);
            }

            // Set the text label in the left corner
            infoLabel.setText(text.toString());
        });

        // Create a side panel for info display
        VBox sidePanel = new VBox(12, infoLabel); // Spacing of 12 px between future children
        sidePanel.setPadding(new Insets(12));
        sidePanel.setPrefWidth(220);

        // Arrange the layout of the game
        BorderPane root = new BorderPane();
        root.setLeft(sidePanel);

        // Create a group for the canvas
        // The group is important here because the ScrollPane operates on layoutBounds
        // and scaled content behaves more predictably this way
        Group canvas_group = new Group(canvas);

        // Center the starting position of the system
        StackPane centered_pane = new StackPane(canvas_group);
        centered_pane.setAlignment(Pos.CENTER);
        centered_pane.setStyle("-fx-background-color: black;");

        // Set the system for scrolling and moving around
        ScrollPane scroller = new ScrollPane(centered_pane);
        scroller.setPannable(false);
        scroller.setStyle("-fx-background: black; -fx-background-color: black;");

        // Ensure that the center pane for setting the scene in the center resizes with each
        // event upon the scroller
        scroller.viewportBoundsProperty().addListener((obs, old_bounds, new_bounds) -> {
            centered_pane.setMinSize(new_bounds.getWidth(), new_bounds.getHeight());
        });

        // Add handler for scrolling
        // Important: zoom around the current viewport center instead of letting the
        // content drift toward one weird place
        scroller.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() > 0) {
                zoomKeepingViewportCenter(canvas, scroller, true);
            } else if (event.getDeltaY() < 0) {
                zoomKeepingViewportCenter(canvas, scroller, false);
            }

            // Remove the event, it has been consumed
            event.consume();
        });

        // Store the previous mouse position
        final double[] mouse = new double[2];

        // Store whether the previous interaction was a real drag
        final boolean[] dragging = new boolean[] {false};

        // Threshold to distinguish a click from an accidental tiny drag
        final double drag_threshold = 6.0;

        // When the mouse is pressed, remember the starting position for the drag
        canvas_group.setOnMousePressed(event -> {
            if (event.isPrimaryButtonDown()) {
                mouse[0] = event.getSceneX();
                mouse[1] = event.getSceneY();
                dragging[0] = false;
            }
        });

        // The left mouse button is held and mouse is being dragged
        canvas_group.setOnMouseDragged(event -> {
            // Don't run when we are no longer holding the left button down
            if (!event.isPrimaryButtonDown()) {
                return;
            }

            // The current displacement from the start
            double dx = event.getSceneX() - mouse[0];
            double dy = event.getSceneY() - mouse[1];

            // Ignore extremely small hand jitter so a click is not lost
            if (!dragging[0] && Math.hypot(dx, dy) < drag_threshold) {
                return;
            }

            dragging[0] = true;

            // The new position where the mouse got
            mouse[0] = event.getSceneX();
            mouse[1] = event.getSceneY();

            // Instead of translating the content node itself, scroll the viewport
            double viewport_w = scroller.getViewportBounds().getWidth();
            double viewport_h = scroller.getViewportBounds().getHeight();

            double content_w = centered_pane.getBoundsInLocal().getWidth();
            double content_h = centered_pane.getBoundsInLocal().getHeight();

            double extra_w = Math.max(0.0, content_w - viewport_w);
            double extra_h = Math.max(0.0, content_h - viewport_h);

            if (extra_w > 0.0) {
                scroller.setHvalue(clamp(scroller.getHvalue() - dx / extra_w, 0.0, 1.0));
            }

            if (extra_h > 0.0) {
                scroller.setVvalue(clamp(scroller.getVvalue() - dy / extra_h, 0.0, 1.0));
            }

            // Consume the event
            event.consume();
        });

        // Treat release as a click only when there was no real drag
        centered_pane.setOnMouseReleased(event -> {
            if (!dragging[0]) {
                javafx.geometry.Point2D localPoint = canvas.sceneToLocal(event.getSceneX(), event.getSceneY());
                canvas.handleClick(localPoint.getX(), localPoint.getY());
            }

            dragging[0] = false;
            event.consume();
        });

        // Center the scroller on the center of the all
        root.setCenter(scroller);

        // Create a lower control bar for turn handling
        HBox bottomPanel = new HBox(12);
        bottomPanel.setPadding(new Insets(12));
        bottomPanel.setAlignment(Pos.CENTER_LEFT);
        bottomPanel.setStyle("-fx-background-color: #111111;");

        // Spacer so the button goes to the right corner
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Button for shifting to the next turn
        Button nextTurnButton = new Button("Next turn");

        nextTurnButton.setOnAction(event -> {
            // Move the game to the next turn
            game.nextTurn();

            // Remove all previous visual movement selections
            canvas.clearSelections();

            // Update the lower label
            updateTurnLabel(turnLabel, game);

            // Temporary debugging info
            infoLabel.setText("Turn: " + game.getCurrentTurn() + "\nCurrent player: " + game.getCurrentPlayer());
        });

        // Put the controls together
        bottomPanel.getChildren().addAll(turnLabel, spacer, nextTurnButton);

        // Place the panel at the bottom
        root.setBottom(bottomPanel);

        Scene scene = new Scene(root, 1500, 900);

        stage.setTitle("IJA game");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * @brief Update the lower turn label text
     * 
     * @param turnLabel The label to be updated
     * @param game The game from which to read the active turn
     */
    private static void updateTurnLabel(Label turnLabel, Game game) {
        turnLabel.setText("Turn: " + game.getCurrentTurn() + " | Current player: " + game.getCurrentPlayer());
    }

    /**
     * @brief Return the movement cost as a readable value
     * 
     * @param movement_cost The integer movement cost
     * @return Human-readable string version of the cost
     */
    private static String formatMovementCost(int movement_cost) {
        if (movement_cost == Integer.MAX_VALUE) {
            return "INF";
        }

        return Integer.toString(movement_cost);
    }

    /**
     * @brief Return the value if within limits, otherwise either min (if smaller)
     *        and max, if larger than the bounds given.
     * 
     * @param value The input value
     * @param min The minimum allowed value
     * @param max The maximum allowed value
     * 
     * @return The clamped value
     */
    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * @brief Zoom while preserving the current viewport center
     * 
     * @param canvas The game canvas being zoomed
     * @param scroller The scroll pane showing the canvas
     * @param zoom_in True if zooming in, false if zooming out
     */
    private static void zoomKeepingViewportCenter(GameCanvas canvas, ScrollPane scroller, boolean zoom_in) {
        double old_width = canvas.getWidth();
        double old_height = canvas.getHeight();

        double viewport_width = scroller.getViewportBounds().getWidth();
        double viewport_height = scroller.getViewportBounds().getHeight();

        double old_extra_width = Math.max(0.0, old_width - viewport_width);
        double old_extra_height = Math.max(0.0, old_height - viewport_height);

        double old_center_x = (old_extra_width > 0.0)
                ? scroller.getHvalue() * old_extra_width + viewport_width / 2.0
                : old_width / 2.0;

        double old_center_y = (old_extra_height > 0.0)
                ? scroller.getVvalue() * old_extra_height + viewport_height / 2.0
                : old_height / 2.0;

        if (zoom_in) {
            canvas.zoomIn();
        } else {
            canvas.zoomOut();
        }

        double new_width = canvas.getWidth();
        double new_height = canvas.getHeight();

        double scale_x = new_width / old_width;
        double scale_y = new_height / old_height;

        double new_center_x = old_center_x * scale_x;
        double new_center_y = old_center_y * scale_y;

        double new_extra_width = Math.max(0.0, new_width - viewport_width);
        double new_extra_height = Math.max(0.0, new_height - viewport_height);

        if (new_extra_width > 0.0) {
            scroller.setHvalue(clamp((new_center_x - viewport_width / 2.0) / new_extra_width, 0.0, 1.0));
        } else {
            scroller.setHvalue(0.0);
        }

        if (new_extra_height > 0.0) {
            scroller.setVvalue(clamp((new_center_y - viewport_height / 2.0) / new_extra_height, 0.0, 1.0));
        } else {
            scroller.setVvalue(0.0);
        }
    }
}