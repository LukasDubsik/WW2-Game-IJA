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

        // Setup a group for the caanvas
        Group canvas_group = new Group(canvas);

        canvas.setOnTileClicked(position -> {
            Terrain terrain = game.getTerrain(position);
            Unit unit = game.getUnit(position);

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

            infoLabel.setText(text.toString());
        });

        // Create a side panel for info display
        VBox sidePanel = new VBox(12, infoLabel); // Spacing of 12 px between future children
        sidePanel.setPadding(new Insets(12)); // 
        sidePanel.setPrefWidth(220);

        // Arrange the layout of the game
        BorderPane root = new BorderPane();
        // The game is the center stage, info is on the left
        //root.setCenter(canvas);
        root.setLeft(sidePanel);

        Group canvasGroup = new Group(canvas);

        StackPane centeredPane = new StackPane(canvasGroup);
        centeredPane.setAlignment(Pos.CENTER);
        centeredPane.setStyle("-fx-background-color: black;");

        ScrollPane scroller = new ScrollPane(centeredPane);
        scroller.setPannable(false);
        scroller.setStyle("-fx-background: black; -fx-background-color: black;");

        scroller.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            centeredPane.setMinSize(newBounds.getWidth(), newBounds.getHeight());
        });

        scroller.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() > 0) {
                canvas.zoomIn();
            } else if (event.getDeltaY() < 0) {
                canvas.zoomOut();
            }

            event.consume();
        });

        final double[] lastMouse = new double[2];

        canvasGroup.setOnMousePressed(event -> {
            if (event.isPrimaryButtonDown()) {
                lastMouse[0] = event.getSceneX();
                lastMouse[1] = event.getSceneY();
            }
        });

        canvasGroup.setOnMouseDragged(event -> {
            if (!event.isPrimaryButtonDown()) {
                return;
            }

            double dx = event.getSceneX() - lastMouse[0];
            double dy = event.getSceneY() - lastMouse[1];

            lastMouse[0] = event.getSceneX();
            lastMouse[1] = event.getSceneY();

            double maxX = scroller.getViewportBounds().getWidth() / 2.0;
            double maxY = scroller.getViewportBounds().getHeight() / 2.0;

            double newX = clamp(canvasGroup.getTranslateX() + dx, -maxX, maxX);
            double newY = clamp(canvasGroup.getTranslateY() + dy, -maxY, maxY);

            canvasGroup.setTranslateX(newX);
            canvasGroup.setTranslateY(newY);

            event.consume();
        });

        root.setCenter(scroller);

        // Create the scene
        Scene scene = new Scene(root);

        // Set some last game properties
        stage.setTitle("IJA game");
        stage.setScene(scene);

        // Show the game
        stage.show();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
