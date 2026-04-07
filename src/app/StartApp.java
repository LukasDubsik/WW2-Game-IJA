package app;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.game.Game;
import model.game.GameFactory;
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
        GameCanvas canvas = new GameCanvas();

        // Create a side panel for info display
        VBox sidePanel = new VBox(12, infoLabel); // Spacing of 12 px between future children
        sidePanel.setPadding(new Insets(12)); // 
        sidePanel.setPrefWidth(220);

        // Arrange the layout of the game
        BorderPane root = new BorderPane();
        // The game is the center stage, info is on the left
        root.setCenter(canvas);
        root.setLeft(sidePanel);

        // Create the scene
        Scene scene = new Scene(root);

        // Set some last game properties
        stage.setTitle("IJA game");
        stage.setScene(scene);

        // Show the game
        stage.show();
    }
}
