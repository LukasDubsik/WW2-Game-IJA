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

        Label infoLabel = new Label("Click a tile.");
        infoLabel.setWrapText(true);

        Label turnLabel = new Label();
        updateTurnLabel(turnLabel, game);

        GameCanvas canvas = new GameCanvas(game, 80, 70);

        canvas.setOnTileClicked(position -> {
            Terrain terrain = game.getTerrain(position);
            Unit unit = game.getUnit(position);
            Overlay overlay = game.getOverlay(position);

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

            infoLabel.setText(text.toString());
        });

        VBox sidePanel = new VBox(12, infoLabel);
        sidePanel.setPadding(new Insets(12));
        sidePanel.setPrefWidth(220);

        BorderPane root = new BorderPane();
        root.setLeft(sidePanel);

        Group canvas_group = new Group(canvas);

        StackPane centered_pane = new StackPane(canvas_group);
        centered_pane.setAlignment(Pos.CENTER);
        centered_pane.setStyle("-fx-background-color: black;");

        centered_pane.setOnMouseClicked(event -> {
            javafx.geometry.Point2D localPoint = canvas.sceneToLocal(event.getSceneX(), event.getSceneY());
            canvas.handleClick(localPoint.getX(), localPoint.getY());
            event.consume();
        });

        ScrollPane scroller = new ScrollPane(centered_pane);
        scroller.setPannable(false);
        scroller.setStyle("-fx-background: black; -fx-background-color: black;");

        scroller.viewportBoundsProperty().addListener((obs, old_bounds, new_bounds) -> {
            centered_pane.setMinSize(new_bounds.getWidth(), new_bounds.getHeight());
        });

        scroller.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() > 0) {
                canvas.zoomIn();
            } else if (event.getDeltaY() < 0) {
                canvas.zoomOut();
            }
            event.consume();
        });

        final double[] mouse = new double[2];

        canvas_group.setOnMousePressed(event -> {
            if (event.isPrimaryButtonDown()) {
                mouse[0] = event.getSceneX();
                mouse[1] = event.getSceneY();
            }
        });

        canvas_group.setOnMouseDragged(event -> {
            if (!event.isPrimaryButtonDown()) {
                return;
            }

            double dx = event.getSceneX() - mouse[0];
            double dy = event.getSceneY() - mouse[1];

            mouse[0] = event.getSceneX();
            mouse[1] = event.getSceneY();

            double max_x = scroller.getViewportBounds().getWidth()/2.0;
            double max_y = scroller.getViewportBounds().getHeight()/2.0;

            double x = clamp(canvas_group.getTranslateX() + dx, -max_x, max_x);
            double y = clamp(canvas_group.getTranslateY() + dy, -max_y, max_y);

            canvas_group.setTranslateX(x);
            canvas_group.setTranslateY(y);

            event.consume();
        });

        root.setCenter(scroller);

        HBox bottomPanel = new HBox(12);
        bottomPanel.setPadding(new Insets(12));
        bottomPanel.setAlignment(Pos.CENTER_LEFT);
        bottomPanel.setStyle("-fx-background-color: #111111;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button nextTurnButton = new Button("Next turn");

        nextTurnButton.setOnAction(event -> {
            game.nextTurn();
            canvas.clearSelections();
            updateTurnLabel(turnLabel, game);
            infoLabel.setText("Turn: " + game.getCurrentTurn() + "\nCurrent player: " + game.getCurrentPlayer());
        });

        bottomPanel.getChildren().addAll(turnLabel, spacer, nextTurnButton);
        root.setBottom(bottomPanel);

        Scene scene = new Scene(root, 1500, 900);

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