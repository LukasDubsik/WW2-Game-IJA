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

        // Load one of the generated historical maps from file
        // To switch scenario, change the file name below.
        Path map_path = Path.of("lib/maps/heiligenbeil_pocket_1945.map");

        // Load the map into a working game -> using the game factory
        Game game = GameFactory.createGame(map_path);

        // Preload some testing units directly in code for now
        // Scenario setup:
        // P1 = Soviets attacking from the west / south-west
        // P2 = Germans defending the coastal corridor

        // Soviet testing force
        game.createUnit("IS-1 Heavy Tank", "P1", 6, 0);
        game.createUnit("M3 Half-track", "P1", 6, 1);
        game.createUnit("ZiS-3 Field Gun", "P1", 5, 0);
        game.createUnit("Soviet Assault Sapper Squad", "P1", 6, 2);
        game.createUnit("DP-27 Team", "P1", 7, 1);
        game.createUnit("BA-64 Armored Car", "P1", 7, 0);

        // German testing force
        game.createUnit("Panzer IV Ausf. J", "P2", 0, 10);
        game.createUnit("Sd.Kfz. 251/1 Half-track", "P2", 2, 9);
        game.createUnit("Sd.Kfz. 234/2 Puma", "P2", 5, 10);
        game.createUnit("Wehrmacht Rifle Squad", "P2", 1, 9);
        game.createUnit("Grenadier Squad", "P2", 4, 9);
        game.createUnit("MG 42 Team", "P2", 0, 9);

        // Experimenting with some base labels
        Label infoLabel = new Label("Click a tile.");
        infoLabel.setWrapText(true); //Make text dynamic

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

        // Ensure that the center pane for setting the scene in the center resizes with each
        // event upon the scroller
        scroller.viewportBoundsProperty().addListener((obs, old_bounds, new_bounds) -> {
            centered_pane.setMinSize(new_bounds.getWidth(), new_bounds.getHeight());
        });

        // Add handler for scrolling
        scroller.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() > 0) {
                canvas.zoomIn();
            } else if (event.getDeltaY() < 0) {
                canvas.zoomOut();
            }

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
            if (!event.isPrimaryButtonDown()) {
                return;
            }

            // The current displacement from the start
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
            game.nextTurn();
            canvas.clearSelections();
            updateTurnLabel(turnLabel, game);
            infoLabel.setText("Turn: " + game.getCurrentTurn() + "\nCurrent player: " + game.getCurrentPlayer());
        });

        bottomPanel.getChildren().addAll(turnLabel, spacer, nextTurnButton);
        root.setBottom(bottomPanel);

        Scene scene = new Scene(root);

        stage.setTitle("IJA game");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * @brief Return the value if within limits, otherwise either min (if smaller)
     *        and max, if larger than the bounds given.
     */
    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * @brief Update the lower turn label text
     */
    private static void updateTurnLabel(Label turnLabel, Game game) {
        turnLabel.setText("Turn: " + game.getCurrentTurn() + " | Current player: " + game.getCurrentPlayer());
    }

    /**
     * @brief Return the movement cost as a readable value
     */
    private static String formatMovementCost(int movement_cost) {
        if (movement_cost == Integer.MAX_VALUE) {
            return "INF";
        }

        return Integer.toString(movement_cost);
    }
}