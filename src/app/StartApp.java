/**
 * @file StartApp.java
 * @author xdubsil00, xbobekp00
 * @brief Source file StartApp.java for the IJA Advance-Wars-inspired game project.
 */
package app;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.game.Game;
import model.game.GameFactory;
import model.map.Building;
import model.map.Serializable.Overlay;
import model.map.Serializable.Position;
import model.map.Serializable.Terrain;
import model.unit.ArmamentType;
import model.unit.Unit;
import model.unit.UnitType;
import replay.Replay;
import view.board.GameCanvas;

public class StartApp extends Application {

    private static Game game; ///< Global game
    private static GameCanvas canvas; ///< Global game canvas
    private static Label turnLabel; ///< Global turn label
    private static Label replayLabel; ///< Global replay label
    private static Label economyLabel; ///< Global economy label
    private static InfoPanelWidgets info_panel; ///< Global info panel
    private static String chosen_player = "P1"; ///< Faction chosen in the startup menu

    /**
     * @brief Small helper structure for one field row in the side panel
     */
    private static class InfoRow {
        VBox box; ///< Full row container
        Label label; ///< Field label
        Label value; ///< Field value
    }

    /**
     * @brief Small helper structure holding the whole info panel widgets together
     */
    private static class InfoPanelWidgets {
        ScrollPane root; ///< Scrollable outer wrapper
        VBox content; ///< Inner content

        Image player1Victory; ///< Victory image for player 1
        Image player2Victory; ///< Victory image for player 2
        ImageView imageViewP1; ///< Image view for player 1 victory screen
        ImageView imageViewP2; ///< Image view for player 2 victory screen

        Label title_label; ///< Main title
        Label subtitle_label; ///< Subtitle / secondary text

        Label tile_section; ///< Tile section title
        InfoRow terrain_row; ///< Terrain row
        InfoRow overlay_row; ///< Overlay row
        InfoRow defence_row; ///< Defence row
        InfoRow move_inf_row; ///< Infantry move cost row
        InfoRow move_veh_row; ///< Vehicle move cost row

        Label building_section; ///< Building section title
        InfoRow building_owner; ///< Owner of the building
        InfoRow building_integrity; ///< Integrity of building

        Label buy_section; ///< Section for shop

        // Grid panes for each player
        GridPane factory_shop_grid_P1;
        GridPane factory_shop_grid_P2;

        // Buttons for P1 factory shop
        Button WEHRMACHT_RIFLE_SQUAD;
        Button GRENADIER_SQUAD;
        Button MG42_TEAM;
        Button SDKFZ_251_HALFTRACK;
        Button PANZER_IV_AUSF_J;
        Button SDKFZ_234_2_PUMA;

        // Buttons for P2 factory shop
        Button SOVIET_ASSAULT_SAPPER_SQUAD;
        Button ZIS_3_FIELD_GUN;
        Button DP27_TEAM;
        Button IS_1_HEAVY_TANK;
        Button M3_HALFTRACK;
        Button BA_64_ARMORED_CAR;

        Label unit_section; ///< Unit section title
        InfoRow owner_row; ///< Unit owner row
        InfoRow status_row; ///< Unit state row
        VBox hp_box; ///< HP block
        Label hp_label; ///< HP title
        Label hp_value; ///< HP text
        ProgressBar hp_bar; ///< HP bar
        InfoRow movement_row; ///< Movement value row
        InfoRow price_row; ///< Price row
        InfoRow armaments_row; ///< Armament list row

        Label action_section; ///< Action helper section title
        InfoRow action_row; ///< Current possible actions
        InfoRow attack_preview_row; ///< Attack preview row
        HBox action_button_box; ///< Box with explicit action buttons
        Button wait_button; ///< Button to wait / end unit action
        Button capture_button; ///< Button to capture a building

        Label description_section; ///< Description section title
        InfoRow description_row; ///< Description row
    }

        /**
     * @brief Entry point of the application
     */
    @Override
    public void start(Stage stage) {
        stage.setTitle("IJA game");
        stage.setScene(createStartupScene(stage));
        stage.show();
    }

        /**
     * @brief Create the basic startup menu scene
     * 
     * @param stage The main application stage
     * @return The created startup menu scene
     */
    private Scene createStartupScene(Stage stage) {
        // Holder of all available scenarios
        List<ScenarioDefinition> scenarios = getScenarioDefinitions();

        // Title
        Label title = new Label("East Prussia 1945");
        title.setStyle(
                "-fx-text-fill: white;"
                        + "-fx-font-size: 28px;"
                        + "-fx-font-weight: bold;"
        );

        Label subtitle = new Label("Choose the scenario and the faction you want to play.");
        subtitle.setStyle("-fx-text-fill: #c8d0db; -fx-font-size: 13px;");

        // Scenario selection
        Label scenario_label = new Label("Scenario");
        scenario_label.setStyle("-fx-text-fill: #8f9db2; -fx-font-size: 12px; -fx-font-weight: bold;");

        ComboBox<ScenarioDefinition> scenario_box = new ComboBox<>();
        scenario_box.getItems().addAll(scenarios);
        scenario_box.getSelectionModel().selectFirst();
        scenario_box.setPrefWidth(420);

        // Faction selection
        Label faction_label = new Label("Faction");
        faction_label.setStyle("-fx-text-fill: #8f9db2; -fx-font-size: 12px; -fx-font-weight: bold;");

        ToggleGroup faction_group = new ToggleGroup();

        RadioButton soviets_button = new RadioButton("P1 – Soviets");
        soviets_button.setToggleGroup(faction_group);
        soviets_button.setSelected(true);
        soviets_button.setStyle("-fx-text-fill: white;");

        RadioButton germans_button = new RadioButton("P2 – Germans");
        germans_button.setToggleGroup(faction_group);
        germans_button.setStyle("-fx-text-fill: white;");

        RadioButton bot_vs_bot_button = new RadioButton("Bot vs Bot");
        bot_vs_bot_button.setToggleGroup(faction_group);
        bot_vs_bot_button.setStyle("-fx-text-fill: white;");

        // Start button
        Button start_button = new Button("Start scenario");
        start_button.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #4d8df0, #2d63ba);"
                        + "-fx-text-fill: white;"
                        + "-fx-font-weight: bold;"
                        + "-fx-background-radius: 8;"
                        + "-fx-padding: 10 18 10 18;"
        );

        start_button.setOnAction(event -> {
            ScenarioDefinition chosen_scenario = scenario_box.getValue();

            String chosen_player = "P1";
            boolean bot_vs_bot = faction_group.getSelectedToggle() == bot_vs_bot_button;

            if (bot_vs_bot) {
                chosen_player = "BOT";
            } else if (faction_group.getSelectedToggle() == germans_button) {
                chosen_player = "P2";
            }

            // Open the selected scenario
            openScenario(stage, chosen_scenario, chosen_player, bot_vs_bot);
        });

        VBox menu_box = new VBox(
                12,
                title,
                subtitle,
                scenario_label,
                scenario_box,
                faction_label,
                soviets_button,
                germans_button,
                bot_vs_bot_button,
                start_button
        );

        menu_box.setAlignment(Pos.CENTER_LEFT);
        menu_box.setPadding(new Insets(24));
        menu_box.setMaxWidth(460);
        menu_box.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #14181f, #1c2330);"
                        + "-fx-border-color: #2c3442;"
                        + "-fx-border-width: 1;"
                        + "-fx-background-radius: 10;"
                        + "-fx-border-radius: 10;"
        );

        StackPane root = new StackPane(menu_box);
        root.setStyle("-fx-background-color: #0c0f14;");

        return new Scene(root, 1100, 700);
    }

        private void openScenario(Stage stage, ScenarioDefinition scenario, String chosen_player, boolean bot_vs_bot) {
        // Check that the scenario exists
        if (scenario == null) {
            throw new IllegalArgumentException("Scenario cannot be null.");
        }

        // Create the fully initialized scenario from map + unit placement + building ownership files
        Game loaded_game = GameFactory.createGame(scenario.map_path, scenario.units_path, scenario.buildings_path);

        // Let the chosen faction start. In Bot vs Bot mode P1 starts.
        loaded_game.setCurrentPlayer(bot_vs_bot ? "P1" : chosen_player);

        // The selected faction is controlled by the human player.
        // In Bot vs Bot mode both sides are controlled by bots.
        if (bot_vs_bot) {
            loaded_game.setPlayerBot("P1", true);
            loaded_game.setPlayerBot("P2", true);
        } else {
            loaded_game.setPlayerBot("P1", !"P1".equals(chosen_player));
            loaded_game.setPlayerBot("P2", !"P2".equals(chosen_player));
        }

        // Store globally for the rest of the application
        StartApp.game = loaded_game;
        StartApp.chosen_player = chosen_player;

        // Build the real game scene
        Scene scene = createGameScene(stage, loaded_game, chosen_player, scenario.name);

        stage.setTitle("IJA game - " + scenario.name);
        stage.setScene(scene);

        // Start the automatic bot loop if both sides are bots.
        if (bot_vs_bot) {
            loaded_game.playCurrentBotTurnIfActive();
        }
    }

    /**
     * @brief Get the list of all scenarios available in the startup menu
     * 
     * @return The list of scenarios
     */
    private List<ScenarioDefinition> getScenarioDefinitions() {
        return List.of(
                new ScenarioDefinition(
                        "Balga / Heiligenbeil corridor",
                        Path.of("lib/maps/balga_heiligenbeil_corridor_1945_large.map"),
                        Path.of("lib/maps/balga_heiligenbeil_corridor_1945_large.units"),
                        Path.of("lib/maps/balga_heiligenbeil_corridor_1945_large.buildings")
                ),
                new ScenarioDefinition(
                        "Allenstein lakes approaches",
                        Path.of("lib/maps/allenstein_lakes_approaches_1945_large.map"),
                        Path.of("lib/maps/allenstein_lakes_approaches_1945_large.units"),
                        Path.of("lib/maps/allenstein_lakes_approaches_1945_large.buildings")
                )
        );
    }

    /**
     * @brief Create the real game scene after the startup menu selection
     * 
     * @param stage The application stage
     * @param game The prepared game
     * @param chosen_player Which faction the user chose
     * @param scenario_name The scenario display name
     * @return The created scene
     */
    private Scene createGameScene(Stage stage, Game game, String chosen_player, String scenario_name) {
        // Create the prettier side information panel
        StartApp.info_panel = createInfoPanel();
        InfoPanelWidgets info_panel = StartApp.info_panel;
        clearInfoPanel(info_panel);

        // Label holding the currently active turn/player
        StartApp.turnLabel = new Label();
        Label turnLabel = StartApp.turnLabel;
        turnLabel.setStyle("-fx-text-fill: #d0d0d0; -fx-font-size: 14px;");
        updateTurnLabel(turnLabel, game, chosen_player);

        // Replay label
        StartApp.replayLabel = new Label();
        Label replayLabel = StartApp.replayLabel;
        replayLabel.setStyle("-fx-text-fill: #f0c674; -fx-font-size: 13px;");

        // Economy label
        StartApp.economyLabel = new Label();
        Label economyLabel = StartApp.economyLabel;
        economyLabel.setStyle("-fx-text-fill: #c8d0db; -fx-font-size: 13px;");

        // Create the game canvas
        StartApp.canvas = new GameCanvas(game, 80, 70);
        GameCanvas canvas = StartApp.canvas;

        // Keep the globally active game as well
        StartApp.game = game;

        // Initialize the extra labels immediately
        updateReplayLabel(replayLabel, game);
        updateEconomyLabel(economyLabel, game);

        // Reconnect the shop buttons to the current game instance
        setFactoryButtonsEvents(info_panel, game, economyLabel);

        // Store the current active UI objects globally as well
        StartApp.info_panel = info_panel;
        StartApp.turnLabel = turnLabel;
        StartApp.canvas = canvas;
        StartApp.game = game;

        // What happens when tile is clicked by a mouse
        canvas.setOnTileClicked(position -> {
            updateInfoPanel(info_panel, game, position);
        });

        // Arrange the layout of the game
        BorderPane root = new BorderPane();
        root.setLeft(info_panel.root);

        // Create a group for the canvas
        Group canvas_group = new Group(canvas);
        canvas_group.setCache(false);

        // Center the starting position of the system
        StackPane centered_pane = new StackPane(canvas_group);
        centered_pane.setAlignment(Pos.CENTER);
        centered_pane.setStyle("-fx-background-color: black;");
        centered_pane.setCache(false);

        // Set the system for scrolling and moving around
        ScrollPane scroller = new ScrollPane(centered_pane);
        scroller.setPannable(false);
        scroller.setStyle("-fx-background: black; -fx-background-color: black;");

        // Ensure that the center pane resizes with the visible viewport
        scroller.viewportBoundsProperty().addListener((obs, old_bounds, new_bounds) -> {
            centered_pane.setMinSize(new_bounds.getWidth(), new_bounds.getHeight());
        });

        // Add handler for scrolling. Use a continuous zoom factor instead of
        // one fixed zoom step per wheel event.
        scroller.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (Math.abs(event.getDeltaY()) > 0.01) {
                double zoom_factor = Math.pow(1.0015, event.getDeltaY());
                zoomKeepingViewportCenter(canvas, scroller, zoom_factor);
            }

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

        // Spacer so buttons go to the right corner
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Button for stepping one replay turn backwards
        Button prevTurnButton = new Button("Prev turn");
        prevTurnButton.setStyle(
                "-fx-background-color: #2b313d;"
                        + "-fx-text-fill: white;"
                        + "-fx-font-weight: bold;"
                        + "-fx-background-radius: 8;"
                        + "-fx-padding: 8 16 8 16;"
        );

        prevTurnButton.setOnAction(event -> {
            game.prevTurn();
            canvas.clearSelections();
            canvas.draw();
            updateTurnLabel(turnLabel, game, chosen_player);
            updateReplayLabel(replayLabel, game);
            updateEconomyLabel(economyLabel, game);
            clearInfoPanel(info_panel);
            updateVictoryScreen(info_panel);
        });

        // Button for saving the replay
        Button saveReplayButton = new Button("Save replay");
        saveReplayButton.setStyle(
                "-fx-background-color: #2b313d;"
                        + "-fx-text-fill: white;"
                        + "-fx-font-weight: bold;"
                        + "-fx-background-radius: 8;"
                        + "-fx-padding: 8 16 8 16;"
        );

        saveReplayButton.setOnAction(event -> {
            FileUtil.createReplayFile("Save replay");
            FileUtil.saveReplayToFile(game.getReplay(), FileUtil.file);
            updateReplayLabel(replayLabel, game);
        });

        // Button for loading a replay from disk
        Button loadReplayButton = new Button("Load replay");
        loadReplayButton.setStyle(
                "-fx-background-color: #2b313d;"
                        + "-fx-text-fill: white;"
                        + "-fx-font-weight: bold;"
                        + "-fx-background-radius: 8;"
                        + "-fx-padding: 8 16 8 16;"
        );

        loadReplayButton.setOnAction(event -> {
            FileUtil.chooseFile("Load replay");
            Replay loaded_replay = FileUtil.readFiletoReplay(FileUtil.file);

            if (loaded_replay != null) {
                Game loaded_game = new Game(new String[] {"P1", "P2"}, loaded_replay);
                loaded_game.setPlayerBot("P1", false);
                loaded_game.setPlayerBot("P2", false);

                StartApp.game = loaded_game;
                StartApp.chosen_player = "P1";

                Scene loaded_scene = createGameScene(stage, loaded_game, "P1", "Loaded replay - press Next replay turn");
                stage.setTitle("IJA game - Loaded replay");
                stage.setScene(loaded_scene);
            }
        });

        // Button for resuming normal play from the current replay state
        Button resumePlayButton = new Button("Resume play");
        resumePlayButton.setStyle(
                "-fx-background-color: #2b313d;"
                        + "-fx-text-fill: white;"
                        + "-fx-font-weight: bold;"
                        + "-fx-background-radius: 8;"
                        + "-fx-padding: 8 16 8 16;"
        );

        resumePlayButton.setOnAction(event -> {
            if (game.isReplayMode()) {
                game.setReplayMode(false);
                if (game.getReplay() != null) {
                    game.getReplay().branchTimeline();
                }
            }

            updateReplayLabel(replayLabel, game);
            updateTurnLabel(turnLabel, game, chosen_player);
        });

        // Button for shifting to the next turn
        Button nextTurnButton = new Button(game.isReplayMode() ? "Next replay turn" : "Next turn");
        nextTurnButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #4d8df0, #2d63ba);"
                        + "-fx-text-fill: white;"
                        + "-fx-font-weight: bold;"
                        + "-fx-background-radius: 8;"
                        + "-fx-padding: 8 16 8 16;"
        );

        nextTurnButton.setOnAction(event -> {
            // Move the game to the next turn
            game.nextTurn();

            // Remove all previous visual movement selections
            canvas.clearSelections();

            // Update the lower labels
            updateTurnLabel(turnLabel, game, chosen_player);
            updateReplayLabel(replayLabel, game);
            updateEconomyLabel(economyLabel, game);

            nextTurnButton.setText(game.isReplayMode() ? "Next replay turn" : "Next turn");

            // Clear stale info from previous turn selection
            clearInfoPanel(info_panel);

            // Update victory screen if needed
            updateVictoryScreen(info_panel);
        });

        // Small button for returning to the startup menu
        Button menuButton = new Button("Menu");
        menuButton.setStyle(
                "-fx-background-color: #2b313d;"
                        + "-fx-text-fill: white;"
                        + "-fx-font-weight: bold;"
                        + "-fx-background-radius: 8;"
                        + "-fx-padding: 8 16 8 16;"
        );

        menuButton.setOnAction(event -> {
            stage.setTitle("IJA game");
            stage.setScene(createStartupScene(stage));
        });

        // Put the controls together
        bottomPanel.getChildren().addAll(turnLabel, replayLabel, economyLabel, spacer, saveReplayButton, loadReplayButton, resumePlayButton, prevTurnButton, menuButton, nextTurnButton);

        // Place the panel at the bottom
        root.setBottom(bottomPanel);

        return new Scene(root, 1500, 900);
    }

    /**
     * @brief Create one reusable info row
     * 
     * @param label_text The row label text
     * @return The created row
     */
    private static InfoRow createInfoRow(String label_text) {
        InfoRow row = new InfoRow();

        row.label = new Label(label_text);
        row.label.setStyle(
                "-fx-text-fill: #8f9db2;"
                        + "-fx-font-size: 11px;"
                        + "-fx-font-weight: bold;"
        );

        row.value = new Label();
        row.value.setWrapText(true);
        row.value.setStyle("-fx-text-fill: #f3f6fb; -fx-font-size: 12px;");

        row.box = new VBox(2, row.label, row.value);
        row.box.setFillWidth(true);

        return row;
    }

    /**
     * @brief Create the full prettier side information panel
     * 
     * @return The created info panel widget bundle
     */
    private static InfoPanelWidgets createInfoPanel() {
        InfoPanelWidgets panel = new InfoPanelWidgets();

        // Getting the victory images
        File p1VictoryFile = new File("lib/assets/victoryScreens/p1VictoryScreen.png");
        File p2VictoryFile = new File("lib/assets/victoryScreens/p2VictoryScreen.png");
        panel.player1Victory = new Image(p1VictoryFile.toURI().toString());
        panel.player2Victory = new Image(p2VictoryFile.toURI().toString());
        panel.imageViewP1 = new ImageView(panel.player1Victory);
        panel.imageViewP2 = new ImageView(panel.player2Victory);
        panel.imageViewP1.setVisible(false);
        panel.imageViewP2.setVisible(false);
        panel.imageViewP1.setManaged(false);
        panel.imageViewP2.setManaged(false);

        // Main title and subtitle
        panel.title_label = new Label();
        panel.title_label.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        panel.title_label.setWrapText(true);

        panel.subtitle_label = new Label();
        panel.subtitle_label.setStyle("-fx-text-fill: #b9c2cf; -fx-font-size: 12px;");
        panel.subtitle_label.setWrapText(true);

        // Section titles
        panel.tile_section = makeSectionLabel("Tile");
        panel.unit_section = makeSectionLabel("Unit");
        panel.building_section = makeSectionLabel("Building");
        panel.action_section = makeSectionLabel("Actions");
        panel.description_section = makeSectionLabel("Description");

        // Tile rows
        panel.terrain_row = createInfoRow("Terrain");
        panel.overlay_row = createInfoRow("Overlay");
        panel.defence_row = createInfoRow("Defence bonus");
        panel.move_inf_row = createInfoRow("Move cost (infantry)");
        panel.move_veh_row = createInfoRow("Move cost (vehicle)");

        //Building rows
        panel.building_owner = createInfoRow("Owner");
        panel.building_integrity = createInfoRow("Integrity");

        // Unit rows
        panel.owner_row = createInfoRow("Owner");
        panel.status_row = createInfoRow("Status");
        panel.movement_row = createInfoRow("Movement");
        panel.price_row = createInfoRow("Price");
        panel.armaments_row = createInfoRow("Armaments");
        panel.armaments_row.value.setWrapText(true);

        // Action rows
        panel.action_row = createInfoRow("Available actions");
        panel.attack_preview_row = createInfoRow("Attack preview");
        panel.wait_button = new Button("Wait");
        panel.capture_button = new Button("Capture");
        setDefaultSmallActionButton(panel.wait_button);
        setDefaultSmallActionButton(panel.capture_button);
        panel.action_button_box = new HBox(8, panel.wait_button, panel.capture_button);
        panel.action_button_box.setManaged(false);
        panel.action_button_box.setVisible(false);

        // HP block
        panel.hp_label = new Label("Hit points");
        panel.hp_label.setStyle(
                "-fx-text-fill: #8f9db2;"
                        + "-fx-font-size: 11px;"
                        + "-fx-font-weight: bold;"
        );

        panel.hp_value = new Label();
        panel.hp_value.setStyle("-fx-text-fill: #f3f6fb; -fx-font-size: 12px;");

        panel.hp_bar = new ProgressBar(0.0);
        panel.hp_bar.setPrefWidth(Double.MAX_VALUE);
        panel.hp_bar.setStyle("-fx-accent: #4ac26b;");

        panel.hp_box = new VBox(2, panel.hp_label, panel.hp_value, panel.hp_bar);

        // Description row
        panel.description_row = createInfoRow("Historical note");
        panel.description_row.value.setStyle("-fx-text-fill: #d5d9e0; -fx-font-size: 12px;");
        panel.description_row.value.setWrapText(true);

        // Create buttons for factory shop
        createFactoryButtons(panel);
        panel.content = new VBox(
                10,
                panel.imageViewP1,
                panel.imageViewP2,
                panel.title_label,
                panel.subtitle_label,
                panel.tile_section,
                panel.terrain_row.box,
                panel.overlay_row.box,
                panel.defence_row.box,
                panel.move_inf_row.box,
                panel.move_veh_row.box,
                panel.building_section,
                panel.building_owner.box,
                panel.building_integrity.box,
                panel.buy_section,
                panel.factory_shop_grid_P1,
                panel.factory_shop_grid_P2,
                panel.unit_section,
                panel.owner_row.box,
                panel.status_row.box,
                panel.hp_box,
                panel.movement_row.box,
                panel.price_row.box,
                panel.armaments_row.box,
                panel.action_section,
                panel.action_row.box,
                panel.attack_preview_row.box,
                panel.action_button_box,
                panel.description_section,
                panel.description_row.box
        );

        panel.content.setPadding(new Insets(16));
        panel.content.setPrefWidth(290);
        panel.content.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #14181f, #1c2330);"
                        + "-fx-border-color: #2c3442;"
                        + "-fx-border-width: 0 1 0 0;"
        );

        panel.root = new ScrollPane(panel.content);
        panel.root.setFitToWidth(true);
        panel.root.setPrefWidth(300);
        panel.root.setMinWidth(300);
        panel.root.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        panel.root.setStyle("-fx-background: #14181f; -fx-background-color: #14181f;");

        return panel;
    }

    /**
     * @brief Create a label used for section titles in the info panel
     */
    private static Label makeSectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle(
                "-fx-text-fill: #7eb6ff;"
                        + "-fx-font-size: 13px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-padding: 8 0 0 0;"
        );
        return label;
    }

    /**
     * @brief Set the default style for small side-panel action buttons
     *
     * @param button Button to style
     */
    private static void setDefaultSmallActionButton(Button button) {
        button.setStyle(
                "-fx-background-color: #2b313d;"
                        + "-fx-text-fill: white;"
                        + "-fx-font-weight: bold;"
                        + "-fx-background-radius: 6;"
                        + "-fx-padding: 6 12 6 12;"
        );
    }

    /**
     * @brief Show or hide one info row depending on whether it has content
     * 
     * @param row The row to update
     * @param value The value to display
     */
    private static void setRowValue(InfoRow row, String value) {
        boolean show = value != null && !value.isBlank();

        row.box.setManaged(show);
        row.box.setVisible(show);

        if (show) {
            row.value.setText(value);
        } else {
            row.value.setText("");
        }
    }

    /**
     * @brief Show or hide a section label
     * 
     * @param section_label The section label
     * @param show Whether to show it
     */
    private static void setSectionVisible(Label section_label, boolean show) {
        section_label.setManaged(show);
        section_label.setVisible(show);
    }

    /**
     * @brief Check whether any of the given rows are currently visible
     * 
     * @param rows The rows to test
     * @return True if any row is visible
     */
    private static boolean anyVisible(InfoRow... rows) {
        for (InfoRow row : rows) {
            if (row.box.isManaged()) {
                return true;
            }
        }

        return false;
    }

    /**
     * @brief Clear the panel to the neutral unselected state
     */
    private static void clearInfoPanel(InfoPanelWidgets panel) {
        panel.title_label.setText("No selection");
        panel.subtitle_label.setText("Click a tile or a unit to inspect it.");

        setSectionVisible(panel.tile_section, false);
        setSectionVisible(panel.unit_section, false);
        setSectionVisible(panel.building_section, false);
        setSectionVisible(panel.buy_section, false);
        setSectionVisible(panel.action_section, false);
        setSectionVisible(panel.description_section, true);
        setRowValue(panel.terrain_row, null);
        setRowValue(panel.overlay_row, null);
        setRowValue(panel.defence_row, null);
        setRowValue(panel.move_inf_row, null);
        setRowValue(panel.move_veh_row, null);
        setRowValue(panel.owner_row, null);
        setRowValue(panel.building_owner, null);
        setRowValue(panel.building_integrity, null);
        setRowValue(panel.status_row, null);
        setRowValue(panel.movement_row, null);
        setRowValue(panel.price_row, null);
        setRowValue(panel.armaments_row, null);
        setRowValue(panel.action_row, null);
        setRowValue(panel.attack_preview_row, null);
        panel.action_button_box.setVisible(false);
        panel.action_button_box.setManaged(false);
        panel.wait_button.setVisible(false);
        panel.wait_button.setManaged(false);
        panel.capture_button.setVisible(false);
        panel.capture_button.setManaged(false);

        panel.factory_shop_grid_P1.setVisible(false);
        panel.factory_shop_grid_P1.setManaged(false);
        panel.factory_shop_grid_P2.setVisible(false);
        panel.factory_shop_grid_P2.setManaged(false);

        panel.hp_box.setManaged(false);
        panel.hp_box.setVisible(false);

        setRowValue(panel.description_row, "Terrain and unit information will appear here.");
    }

    /**
     * @brief Update the full side panel from the clicked tile
     * 
     * @param panel The UI info panel
     * @param game The current game
     * @param position The inspected position
     */
    private static void updateInfoPanel(InfoPanelWidgets panel, Game game, Position position) {
        Terrain terrain = game.getTerrain(position);
        Overlay overlay = game.getOverlay(position);
        Unit unit = game.getUnit(position);

        // Fill tile information first
        setRowValue(panel.terrain_row, prettyTerrainName(terrain));

        if (!overlay.toString().equalsIgnoreCase("NONE")) {
            setRowValue(panel.overlay_row, prettyOverlayName(overlay));
        } else {
            setRowValue(panel.overlay_row, null);
        }

        setRowValue(panel.defence_row, Integer.toString(game.getCombinedDefenceBonus(position)));
        setRowValue(panel.move_inf_row, formatMovementCost(game.getCombinedMovementInfantry(position)));
        setRowValue(panel.move_veh_row, formatMovementCost(game.getCombinedMovementVehicle(position)));

        setSectionVisible(
                panel.tile_section,
                anyVisible(panel.terrain_row, panel.overlay_row, panel.defence_row, panel.move_inf_row, panel.move_veh_row)
        );

        //If the terrain is a building show building information
        if(Terrain.isBuilding(terrain)) {
            setSectionVisible(panel.building_section, true);

            //If factory then show the shop
            if(terrain == Terrain.FACTORY) {
                Building building = game.getBuilding(position);

                if(building != null && building.getOwner().equals("P1") && game.getCurrentPlayer().equals("P1")){
                    setSectionVisible(panel.buy_section, true);
                    setButtonsAffodability(panel, game, "P1");
                    panel.factory_shop_grid_P1.setVisible(true);
                    panel.factory_shop_grid_P1.setManaged(true);
                    game.setSelectedFactory(position);
                }
                else if (building != null && building.getOwner().equals("P2") && game.getCurrentPlayer().equals("P2")){
                    setSectionVisible(panel.buy_section, true);
                    setButtonsAffodability(panel, game, "P2");
                    panel.factory_shop_grid_P2.setVisible(true);
                    panel.factory_shop_grid_P2.setManaged(true);
                    game.setSelectedFactory(position);
                }

            }
            else {
                setSectionVisible(panel.buy_section, false);
                panel.factory_shop_grid_P1.setVisible(false);
                panel.factory_shop_grid_P1.setManaged(false);
                panel.factory_shop_grid_P2.setVisible(false);
                panel.factory_shop_grid_P2.setManaged(false);
            }

            //If building is owned by a player show the owner and integrity
            if(game.getBuilding(position) != null){
                Building building = game.getBuilding(position);

                setRowValue(panel.building_owner, building.getOwner());
                setRowValue(panel.building_integrity, building.getIntegrity() + " / " + building.getMaxIntegrity());
            }
            else{
                setRowValue(panel.building_owner, "None");
            }
        }
        else {
            setSectionVisible(panel.building_section, false);
            setSectionVisible(panel.buy_section, false);
            panel.factory_shop_grid_P1.setVisible(false);
            panel.factory_shop_grid_P1.setManaged(false);
            panel.factory_shop_grid_P2.setVisible(false);
            panel.factory_shop_grid_P2.setManaged(false);
            setRowValue(panel.building_owner, null);
            setRowValue(panel.building_integrity, null);
        }

        // If there is no unit at the tile, show tile-only information
        if (unit == null) {
            panel.title_label.setText(prettyTerrainName(terrain));
            panel.subtitle_label.setText("Terrain sector");

            setSectionVisible(panel.unit_section, false);
            setRowValue(panel.owner_row, null);
            setRowValue(panel.status_row, null);
            setRowValue(panel.movement_row, null);
            setRowValue(panel.price_row, null);
            setRowValue(panel.armaments_row, null);
            setSectionVisible(panel.action_section, false);
            setRowValue(panel.action_row, null);
            setRowValue(panel.attack_preview_row, null);
            panel.action_button_box.setVisible(false);
            panel.action_button_box.setManaged(false);
            panel.wait_button.setVisible(false);
            panel.wait_button.setManaged(false);
            panel.capture_button.setVisible(false);
            panel.capture_button.setManaged(false);
            panel.hp_box.setManaged(false);
            panel.hp_box.setVisible(false);

            setSectionVisible(panel.description_section, true);
            setRowValue(panel.description_row, buildTileDescription(terrain, overlay, game, position));
            return;
        }

        // Otherwise show the unit details
        panel.title_label.setText(unit.getUnitType().getName());
        panel.subtitle_label.setText("Selected unit on " + prettyTerrainName(terrain));

        setRowValue(panel.owner_row, unit.getOwner());
        setRowValue(panel.status_row, buildUnitStatus(unit));
        setRowValue(panel.movement_row, Integer.toString(unit.getUnitType().getMovement()));
        setRowValue(panel.price_row, Integer.toString(unit.getUnitType().getPrice()));
        setRowValue(panel.armaments_row, formatArmaments(unit));

        int current_hp = unit.getCurrentHp();
        int max_hp = unit.getUnitType().getMaxHP();
        double hp_ratio = (double) current_hp / (double) max_hp;

        panel.hp_value.setText(current_hp + " / " + max_hp);
        panel.hp_bar.setProgress(hp_ratio);
        panel.hp_bar.setStyle("-fx-accent: " + hpBarColor(hp_ratio) + ";");
        panel.hp_box.setManaged(true);
        panel.hp_box.setVisible(true);

        setSectionVisible(
                panel.unit_section,
                panel.hp_box.isManaged()
                        || anyVisible(panel.owner_row, panel.status_row, panel.movement_row, panel.price_row, panel.armaments_row)
        );

        String action_text = buildActionText(game, position, unit);
        String attack_preview = buildAttackPreviewText(game, position, unit);
        setRowValue(panel.action_row, action_text);
        setRowValue(panel.attack_preview_row, attack_preview);

        boolean own_active_unit = unit.getOwner().equals(game.getCurrentPlayer()) && !unit.hasAlreadyPlayed();
        boolean can_capture = game.canCaptureBuilding(position);

        panel.wait_button.setVisible(own_active_unit);
        panel.wait_button.setManaged(own_active_unit);
        panel.capture_button.setVisible(can_capture);
        panel.capture_button.setManaged(can_capture);
        panel.action_button_box.setVisible(own_active_unit || can_capture);
        panel.action_button_box.setManaged(own_active_unit || can_capture);

        panel.wait_button.setOnAction(event -> {
            if (game.finishUnitAction(position)) {
                canvas.clearSelections();
                canvas.draw();
                updateInfoPanel(panel, game, position);
                updateTurnLabel(turnLabel, game);
                updateReplayLabel(replayLabel, game);
                updateEconomyLabel(economyLabel, game);
                updateVictoryScreen(panel);
            }
        });

        panel.capture_button.setOnAction(event -> {
            if (game.captureBuilding(position)) {
                canvas.clearSelections();
                canvas.draw();
                updateInfoPanel(panel, game, position);
                updateTurnLabel(turnLabel, game);
                updateReplayLabel(replayLabel, game);
                updateEconomyLabel(economyLabel, game);
                updateVictoryScreen(panel);
            }
        });

        setSectionVisible(panel.action_section, anyVisible(panel.action_row, panel.attack_preview_row) || own_active_unit || can_capture);

        setSectionVisible(panel.description_section, true);
        setRowValue(panel.description_row, buildUnitDescription(unit, terrain, overlay, game, position));
    }

    /**
     * @brief Format the armament list in a readable way, including the stats of each weapon
     */
    private static String formatArmaments(Unit unit) {
        if (unit == null || unit.getUnitType().getArmaments() == null || unit.getUnitType().getArmaments().isEmpty()) {
            return "";
        }

        // Group repeated armaments while preserving order
        LinkedHashMap<ArmamentType, Integer> grouped = new LinkedHashMap<>();

        for (ArmamentType armament : unit.getUnitType().getArmaments()) {
            grouped.put(armament, grouped.getOrDefault(armament, 0) + 1);
        }

        StringBuilder text = new StringBuilder();
        boolean first = true;

        for (Map.Entry<ArmamentType, Integer> entry : grouped.entrySet()) {
            ArmamentType armament = entry.getKey();
            int count = entry.getValue();

            if (!first) {
                text.append("\n\n");
            }

            if (count > 1) {
                text.append(armament.getName()).append(" x").append(count);
            } else {
                text.append(armament.getName());
            }

            text.append("\n");
            text.append("Soft ").append(armament.getSoftAttack());
            text.append(" | Hard ").append(armament.getHardAttack());
            text.append(" | Range ").append(armament.getMinRange()).append("-").append(armament.getMaxRange());

            if (armament.isIndirectFire()) {
                text.append(" | Indirect");
            } else if (armament.isDirectFire()) {
                text.append(" | Direct");
            }

            first = false;
        }

        return text.toString();
    }

    /**
     * @brief Build a readable current status string for the unit
     */
    private static String buildUnitStatus(Unit unit) {
        if (unit.hasAlreadyPlayed()) {
            return "Turn finished";
        }

        if (unit.hasMovedThisTurn()) {
            return "Moved this turn";
        }

        return "Ready";
    }


    /**
     * @brief Build a short helper text for available unit actions
     *
     * @param game The current game
     * @param position The selected unit position
     * @param unit The selected unit
     * @return Text describing the available actions
     */
    private static String buildActionText(Game game, Position position, Unit unit) {
        if (unit == null) {
            return "";
        }

        if (!unit.getOwner().equals(game.getCurrentPlayer())) {
            return "Enemy unit. It can be attacked only during your active turn if one of your units has it in range.";
        }

        if (unit.hasAlreadyPlayed()) {
            return "This unit has already finished its action this turn.";
        }

        int move_count = game.getReachableTiles(position).size();
        int attack_count = game.getAttackableTiles(position).size();

        StringBuilder text = new StringBuilder();
        text.append("Move: ").append(move_count).append(" reachable tiles.");
        text.append("\nAttack: ").append(attack_count).append(" available targets.");
        if (game.canCaptureBuilding(position)) {
            text.append("\nCapture: this infantry can capture the current building.");
        }
        text.append("\nWait: click the selected unit again or use the Wait button to end its action.");

        if (unit.hasMovedThisTurn()) {
            text.append("\nThis unit has already moved, so only remaining legal attacks are shown.");
        }

        return text.toString();
    }

    /**
     * @brief Build attack preview for all targets visible to the selected unit
     *
     * @param game The current game
     * @param position The selected unit position
     * @param unit The selected unit
     * @return Attack preview text
     */
    private static String buildAttackPreviewText(Game game, Position position, Unit unit) {
        if (unit == null || !unit.getOwner().equals(game.getCurrentPlayer()) || unit.hasAlreadyPlayed()) {
            return "";
        }

        List<Position> attackable_tiles = game.getAttackableTiles(position);
        if (attackable_tiles.isEmpty()) {
            return "";
        }

        StringBuilder text = new StringBuilder();
        boolean first = true;

        for (Position target_position : attackable_tiles) {
            Unit target = game.getUnit(target_position);
            if (target == null) {
                continue;
            }

            int damage = game.previewAttackDamage(position, target_position);
            int counter_damage = game.previewCounterAttackDamage(position, target_position);
            int target_after = Math.max(0, target.getCurrentHp() - damage);
            int attacker_after = Math.max(0, unit.getCurrentHp() - counter_damage);
            int distance = game.getTileDistanceForDisplay(position, target_position);

            if (!first) {
                text.append("\n\n");
            }

            text.append(target.getUnitType().getName());
            text.append(" at [").append(target_position.row()).append(",").append(target_position.column()).append("]");
            text.append("\nRange: ").append(distance);
            text.append(" | Damage: ").append(damage);
            text.append(" | Target HP: ").append(target.getCurrentHp()).append(" -> ").append(target_after);

            if (counter_damage > 0) {
                text.append("\nCounter: ").append(counter_damage);
                text.append(" | Own HP: ").append(unit.getCurrentHp()).append(" -> ").append(attacker_after);
            } else {
                text.append("\nCounter: none");
            }

            first = false;
        }

        return text.toString();
    }

    /**
     * @brief Return the terrain as a prettier display name
     */
    private static String prettyTerrainName(Terrain terrain) {
        String value = terrain.toString().toUpperCase();

        return switch (value) {
            case "PLAIN" -> "Snow plain";
            case "FOREST" -> "Forest";
            case "MOUNTAIN" -> "Ridge / mountain";
            case "WATER" -> "Water / frozen edge";
            case "CITY" -> "Urban sector";
            case "FACTORY" -> "Factory district";
            case "HQ" -> "Headquarters";
            case "TOWN" -> "Town";
            default -> terrain.toString();
        };
    }

    /**
     * @brief Return the overlay as a prettier display name
     */
    private static String prettyOverlayName(Overlay overlay) {
        String value = overlay.toString().toUpperCase();

        return switch (value) {
            case "NONE" -> "None";
            case "TRENCH" -> "Trench";
            case "CRATER" -> "Crater";
            case "BARBED_WIRE" -> "Barbed wire";
            case "RUBBLE" -> "Rubble";
            case "SMOKE" -> "Smoke";
            default -> overlay.toString();
        };
    }

    /**
     * @brief Build the descriptive text for an empty / terrain tile
     * 
     * This is meant as flavor text, not gameplay summary.
     */
    private static String buildTileDescription(Terrain terrain, Overlay overlay, Game game, Position position) {
        StringBuilder text = new StringBuilder();

        // Base terrain atmosphere
        switch (terrain.toString().toUpperCase()) {
            case "PLAIN" -> text.append(
                    "A broad snow-covered plain, exposed to wind, visibility, and long fields of fire. "
                            + "In the winter fighting of East Prussia, such ground often left infantry and vehicles "
                            + "painfully visible once they left woods, villages, or prepared cover."
            );

            case "FOREST" -> text.append(
                    "Dark winter woodland, offering concealment, broken sightlines, and a feeling of close, uncertain fighting. "
                            + "Forest sectors in East Prussia often served as temporary shelter, ambush ground, "
                            + "and cover for battered formations falling back from the open country."
            );

            case "MOUNTAIN" -> text.append(
                    "Broken upland ground and steep ridges, difficult to traverse and naturally suited for delay and defence. "
                            + "Such terrain channels movement, disrupts formation cohesion, and turns even a short advance "
                            + "into a hard local struggle."
            );

            case "WATER" -> text.append(
                    "A cold water barrier or frozen shoreline edge, shaping the battlefield more by restriction than by cover. "
                            + "In the East Prussian winter campaign, water, lagoons, and coastal edges were tied to retreat routes, "
                            + "bottlenecks, and desperate evacuation corridors."
            );

            case "CITY" -> text.append(
                    "A built-up urban sector of streets, masonry, courtyards, and shattered cover. "
                            + "Towns in late-war East Prussia were frequently turned into defended strongpoints, "
                            + "where short-range fighting and stubborn resistance replaced open maneuver."
            );

            case "FACTORY" -> text.append(
                    "An industrial district of workshops, brick halls, storage yards, and heavy structures. "
                            + "Such ground provides strong cover and often feels less like open battlefield terrain "
                            + "and more like a fortified knot inside the wider retreat."
            );

            case "HQ" -> text.append(
                    "A command-related locality, suggestive of communications, staff traffic, and local operational importance. "
                            + "In collapsing fronts, such positions often became improvised anchors of control amid confusion."
            );

            case "TOWN" -> text.append(
                    "A settlement sector of roads, houses, barns, and enclosed spaces. "
                            + "Across East Prussia in early 1945, villages and towns repeatedly became temporary defensive nodes "
                            + "that slowed the Soviet advance and sheltered withdrawing German forces."
            );

            default -> text.append(
                    "A battlefield sector inside the winter East Prussian setting, shaped by exposure, exhaustion, "
                            + "and the pressure of retreat and assault."
            );
        }

        // Add overlay flavor only if it exists
        if (!overlay.toString().equalsIgnoreCase("NONE")) {
            text.append(" ");

            switch (overlay.toString().toUpperCase()) {
                case "TRENCH" -> text.append(
                        "Fresh or reused field entrenchments cut into the frozen ground, showing that troops expected to hold here under pressure."
                );

                case "CRATER" -> text.append(
                        "Shell craters scar the ground, evidence of recent bombardment and the steady destruction of the local battlefield."
                );

                case "BARBED_WIRE" -> text.append(
                        "Barbed wire obstructs the approach, a sign of hurried but deliberate preparation for close defensive fighting."
                );

                case "RUBBLE" -> text.append(
                        "Collapsed structures and debris suggest artillery damage, burning, and the gradual reduction of the area into ruins."
                );

                case "SMOKE" -> text.append(
                        "Smoke hangs over the ground, blurring sightlines and giving the impression of fresh impact, fires, or deliberate concealment."
                );

                default -> text.append(
                        "Additional battlefield disturbance is visible on the tile."
                );
            }
        }

        return text.toString();
    }

    /**
     * @brief Build the descriptive text for a unit card
     * 
     * This is meant as historical / flavor text, not gameplay summary.
     */
    private static String buildUnitDescription(Unit unit, Terrain terrain, Overlay overlay, Game game, Position position) {
        String unit_name = unit.getUnitType().getName();

        return switch (unit_name) {
            case "Wehrmacht Rifle Squad" -> "A standard late-war German infantry element, representative of the rifle-heavy formations "
                    + "that formed the backbone of local defence even as the front in East Prussia collapsed. "
                    + "Such squads were expected to hold villages, woods, and improvised strongpoints under worsening conditions.";

            case "Grenadier Squad" -> "A German grenadier formation intended for aggressive local defence and short-range battlefield work. "
                    + "In the final East Prussian fighting, grenadier units often fought in fragmented groups, "
                    + "reinforcing threatened sectors and delaying advances around settlements and road junctions.";

            case "MG 42 Team" -> "A machine-gun detachment centered on the MG 42, one of the defining support weapons of late-war German infantry tactics. "
                    + "Properly positioned, such teams could dominate open approaches and make even snow-covered ground dangerous to cross.";

            case "Sd.Kfz. 251/1 Half-track" -> "A German armored personnel carrier used to move infantry, weapons, and ammunition under partial protection. "
                    + "By 1945, surviving half-tracks were valuable mobile assets, often committed wherever local mobility or rapid reinforcement still mattered.";

            case "Panzer IV Ausf. J" -> "One of the main late-war German medium tank variants, still widely used in the final campaigns of the war. "
                    + "In East Prussia, machines like this were often committed piecemeal in defensive counterstrokes, roadblock fighting, and local fire support.";

            case "Sd.Kfz. 234/2 Puma" -> "A fast German armored car designed for reconnaissance and mobile combat. "
                    + "In the closing battles, vehicles of this class were prized for rapid reaction, flank security, and exploiting whatever mobility the winter roads still allowed.";

            case "Soviet Assault Sapper Squad" -> "A Soviet assault-oriented infantry formation suited to close combat, strongpoint reduction, and difficult urban or fortified fighting. "
                    + "Units of this character were well suited to the brutal final reduction of defended localities in East Prussia.";

            case "DP-27 Team" -> "A Soviet light machine-gun team built around the DP-27, providing sustained infantry fire support during the advance. "
                    + "Such detachments helped Soviet infantry keep pressure on defended villages, woods, and road sectors during the winter offensives.";

            case "ZiS-3 Field Gun" -> "The ZiS-3 was one of the most widespread Soviet field guns of the war, valued for its versatility, simplicity, and heavy battlefield presence. "
                    + "Weapons of this kind supported the offensive with direct fire, anti-tank work, and local artillery support against stubborn resistance.";

            case "IS-1 Heavy Tank" -> "A Soviet heavy tank of the breakthrough type, associated with concentrated firepower and armored shock effect. "
                    + "In the East Prussian fighting, heavy tanks were especially useful when strong German positions had to be forced rather than bypassed.";

            case "M3 Half-track" -> "An American-designed half-track used by Soviet forces through Allied supply, combining troop mobility with battlefield flexibility. "
                    + "Vehicles of this type helped keep the offensive moving across long winter approaches and broken road networks.";

            case "BA-64 Armored Car" -> "A light Soviet armored car intended for reconnaissance, screening, and light support tasks. "
                    + "Though lightly protected, it fit the fast-moving and fragmented battlefield conditions of late-war pursuit and local exploitation.";

            default -> "A field formation taking part in the winter fighting of East Prussia in 1945, where local actions were shaped by cold, fatigue, "
                    + "broken communications, and the steady pressure of offensive and retreat.";
        };
    }

    /**
     * @brief Return the movement cost as a readable value
     */
    private static String formatMovementCost(int movement_cost) {
        if (movement_cost == Integer.MAX_VALUE) {
            return "Blocked";
        }

        return Integer.toString(movement_cost);
    }

    /**
     * @brief Return the HP bar accent color based on HP ratio
     */
    private static String hpBarColor(double hp_ratio) {
        if (hp_ratio > 0.66) {
            return "#43c463";
        }

        if (hp_ratio > 0.33) {
            return "#d7b739";
        }

        return "#d34f4f";
    }

    /**
     * @brief Update the lower turn label text
     * 
     * @param turnLabel The label to be updated
     * @param game The game from which to read the active turn
     * @param chosen_player Which faction the human chose in the startup menu
     */
    private static void updateTurnLabel(Label turnLabel, Game game, String chosen_player) {
        String chosen_player_name;
        if ("BOT".equals(chosen_player)) {
            chosen_player_name = "Bot vs Bot";
        } else {
            chosen_player_name = "P1".equals(chosen_player) ? "Soviets (P1)" : "Germans (P2)";
        }

        String winner = game.getWinner();
        if (winner != null) {
            String winner_name = "P1".equals(winner) ? "Soviets (P1)" : "Germans (P2)";
            turnLabel.setText(
                    "Scenario player: " + chosen_player_name
                            + " | Game over | Winner: " + winner_name
            );
            return;
        }

        turnLabel.setText(
                "Scenario player: " + chosen_player_name
                        + " | Turn: " + game.getCurrentTurn()
                        + " | Current player: " + game.getCurrentPlayer()
        );
    }

    /**
     * @brief Compatibility wrapper for older code paths still calling the old form
     * 
     * @param turnLabel The label to be updated
     * @param game The game from which to read the active turn
     */
    private static void updateTurnLabel(Label turnLabel, Game game) {
        updateTurnLabel(turnLabel, game, StartApp.chosen_player);
    }

    /**
     * @brief Update the replay label
     *
     * @param replayLabel The label to be updated
     * @param game The game from which to read replay state
     */
    private static void updateReplayLabel(Label replayLabel, Game game){
        if (game.isReplayMode()) {
            replayLabel.setText("*Replay mode: use Next replay turn / Prev turn");
        } else {
            replayLabel.setText("");
        }
    }

    /**
     * @brief Update the economy label text
     *
     * @param economyLabel The label to be updated
     * @param game The game from which to read the active turn
     */
    private static void updateEconomyLabel(Label economyLabel, Game game) {
        Map<String, Integer> playerWealth = game.getPlayerWealth();
        int p1Wealth = playerWealth.getOrDefault("P1", 0);
        int p2Wealth = playerWealth.getOrDefault("P2", 0);

        economyLabel.setText("P1: " + p1Wealth + "$ | P2: " + p2Wealth + "$");
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
    private static void zoomKeepingViewportCenter(GameCanvas canvas, ScrollPane scroller, double zoom_factor) {
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

        boolean zoom_changed = canvas.zoomBy(zoom_factor);
        if (!zoom_changed) {
            return;
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

    /**
     * @brief Getter for the current game
     *
     * @return The current game
     */
    public Game getGame() {
        return game;
    }

    /**
     * @brief Set the font for a default button
     *
     * @param button Button to be changed
     */
    private void setDefaultButtonFont(Button button){
        button.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #4d8df0, #2d63ba);"
                        + "-fx-text-fill: white;"
                        + "-fx-font-weight: bold;"
                        + "-fx-background-radius: 8;"
                        + "-fx-padding: 8 16 8 16;"
        );
    }

    /**
     * @brief Set the initial font of a shop button
     *
     * @param button Button to be changed
     */
    private static void setInitialFactoryButtonFont(Button button){

        setButtonFontAffordable(button, false);

        button.setPrefWidth(85);
        button.setPrefHeight(40);

        button.setWrapText(true);
        button.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
    }

    /**
     * @brief Set the shop button to grey if not affordable or to blue if affordable
     *
     * @param button Button to be changed
     * @param affordable boolean if the unit is affordable
     */
    private static void setButtonFontAffordable(Button button, boolean affordable){
        if(affordable){
            button.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #395c8c, #1f3657);"
                            + "-fx-text-fill: #e8f0fe;"
                            + "-fx-font-weight: bold;"
                            + "-fx-font-size: 9px;"
                            + "-fx-background-radius: 6;"
                            + "-fx-padding: 4 2 4 2;"
                            + "-fx-border-color: #5c85c2;"
                            + "-fx-border-width: 1;"
                            + "-fx-border-radius: 5;"
            );
        }
        else{
            button.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #596575, #363f4a);"
                            + "-fx-text-fill: #9aa6b8;"
                            + "-fx-font-weight: bold;"
                            + "-fx-font-size: 9px;"
                            + "-fx-background-radius: 6;"
                            + "-fx-padding: 4 2 4 2;"
            );
        }

    }

    /**
     * @brief Create buttons for factory shop
     *
     * @param panel Side panel of all widgets
     */
    private static void createFactoryButtons(InfoPanelWidgets panel){
        panel.buy_section = makeSectionLabel("Buy units");

        panel.WEHRMACHT_RIFLE_SQUAD = new Button("Wehrmacht rifle squad");
        panel.GRENADIER_SQUAD = new Button("Grenadier squad");
        panel.MG42_TEAM = new Button("MG42 team");
        panel.SDKFZ_251_HALFTRACK = new Button("SDKFZ 251 halftrack");
        panel.PANZER_IV_AUSF_J = new Button("Panzer IV AUSF J");
        panel.SDKFZ_234_2_PUMA = new Button("SDKFZ 234 2 puma");
        panel.SOVIET_ASSAULT_SAPPER_SQUAD = new Button("Soviet assault sapper squad");
        panel.ZIS_3_FIELD_GUN = new Button("ZIS 3 field gun");
        panel.DP27_TEAM = new Button("DP27 team");
        panel.IS_1_HEAVY_TANK = new Button("IS 1 heavy tank");
        panel.M3_HALFTRACK = new Button("M3 halftrack");
        panel.BA_64_ARMORED_CAR = new Button("BA 64 armored car");

        setInitialFactoryButtonFont(panel.WEHRMACHT_RIFLE_SQUAD);
        setInitialFactoryButtonFont(panel.GRENADIER_SQUAD);
        setInitialFactoryButtonFont(panel.MG42_TEAM);
        setInitialFactoryButtonFont(panel.SDKFZ_251_HALFTRACK);
        setInitialFactoryButtonFont(panel.PANZER_IV_AUSF_J);
        setInitialFactoryButtonFont(panel.SDKFZ_234_2_PUMA);
        setInitialFactoryButtonFont(panel.SOVIET_ASSAULT_SAPPER_SQUAD);
        setInitialFactoryButtonFont(panel.DP27_TEAM);
        setInitialFactoryButtonFont(panel.ZIS_3_FIELD_GUN);
        setInitialFactoryButtonFont(panel.IS_1_HEAVY_TANK);
        setInitialFactoryButtonFont(panel.M3_HALFTRACK);
        setInitialFactoryButtonFont(panel.BA_64_ARMORED_CAR);

        // P1 shop = Soviets
        panel.factory_shop_grid_P1 = new GridPane();
        panel.factory_shop_grid_P1.setHgap(5);
        panel.factory_shop_grid_P1.setVgap(5);
        panel.factory_shop_grid_P1.add(panel.SOVIET_ASSAULT_SAPPER_SQUAD, 0, 0);
        panel.factory_shop_grid_P1.add(panel.DP27_TEAM, 1, 0);
        panel.factory_shop_grid_P1.add(panel.ZIS_3_FIELD_GUN, 2, 0);
        panel.factory_shop_grid_P1.add(panel.IS_1_HEAVY_TANK, 0, 1);
        panel.factory_shop_grid_P1.add(panel.M3_HALFTRACK, 1, 1);
        panel.factory_shop_grid_P1.add(panel.BA_64_ARMORED_CAR, 2, 1);
        panel.factory_shop_grid_P1.setVisible(false);
        panel.factory_shop_grid_P1.setManaged(false);

        // P2 shop = Germans
        panel.factory_shop_grid_P2 = new GridPane();
        panel.factory_shop_grid_P2.setHgap(5);
        panel.factory_shop_grid_P2.setVgap(5);
        panel.factory_shop_grid_P2.add(panel.WEHRMACHT_RIFLE_SQUAD, 0, 0);
        panel.factory_shop_grid_P2.add(panel.GRENADIER_SQUAD, 1, 0);
        panel.factory_shop_grid_P2.add(panel.MG42_TEAM, 2, 0);
        panel.factory_shop_grid_P2.add(panel.SDKFZ_251_HALFTRACK, 0, 1);
        panel.factory_shop_grid_P2.add(panel.PANZER_IV_AUSF_J, 1, 1);
        panel.factory_shop_grid_P2.add(panel.SDKFZ_234_2_PUMA, 2, 1);
        panel.factory_shop_grid_P2.setVisible(false);
        panel.factory_shop_grid_P2.setManaged(false);
    }

    /**
     * @brief Set the action events for all factory shop buttons and update the economy label
     *
     * @param panel Side panel of all widgets
     * @param game The current game
     * @param economyLabel Label to be updated
     */
    private void setFactoryButtonsEvents(InfoPanelWidgets panel, Game game, Label economyLabel){
        panel.WEHRMACHT_RIFLE_SQUAD.setOnAction(event ->{
            game.buyUnit(UnitType.WEHRMACHT_RIFLE_SQUAD);
            canvas.draw();
            updateEconomyLabel(economyLabel, game);
        });
        panel.GRENADIER_SQUAD.setOnAction(event ->{
            game.buyUnit(UnitType.GRENADIER_SQUAD);
            canvas.draw();
            updateEconomyLabel(economyLabel, game);
        });
        panel.MG42_TEAM.setOnAction(event ->{
            game.buyUnit(UnitType.MG42_TEAM);
            canvas.draw();
            updateEconomyLabel(economyLabel, game);
        });
        panel.SDKFZ_251_HALFTRACK.setOnAction(event ->{
            game.buyUnit(UnitType.SDKFZ_251_HALFTRACK);
            canvas.draw();
            updateEconomyLabel(economyLabel, game);
        });
        panel.PANZER_IV_AUSF_J.setOnAction(event ->{
            game.buyUnit(UnitType.PANZER_IV_AUSF_J);
            canvas.draw();
            updateEconomyLabel(economyLabel, game);
        });
        panel.SDKFZ_234_2_PUMA.setOnAction(event ->{
            game.buyUnit(UnitType.SDKFZ_234_2_PUMA);
            canvas.draw();
            updateEconomyLabel(economyLabel, game);
        });
        panel.SOVIET_ASSAULT_SAPPER_SQUAD.setOnAction(event ->{
            game.buyUnit(UnitType.SOVIET_ASSAULT_SAPPER_SQUAD);
            canvas.draw();
            updateEconomyLabel(economyLabel, game);
        });
        panel.ZIS_3_FIELD_GUN.setOnAction(event ->{
            game.buyUnit(UnitType.ZIS_3_FIELD_GUN);
            canvas.draw();
            updateEconomyLabel(economyLabel, game);
        });
        panel.DP27_TEAM.setOnAction(event ->{
            game.buyUnit(UnitType.DP27_TEAM);
            canvas.draw();
            updateEconomyLabel(economyLabel, game);
        });
        panel.IS_1_HEAVY_TANK.setOnAction(event ->{
            game.buyUnit(UnitType.IS_1_HEAVY_TANK);
            canvas.draw();
            updateEconomyLabel(economyLabel, game);
        });
        panel.M3_HALFTRACK.setOnAction(event ->{
            game.buyUnit(UnitType.M3_HALFTRACK);
            canvas.draw();
            updateEconomyLabel(economyLabel, game);
        });
        panel.BA_64_ARMORED_CAR.setOnAction(event ->{
            game.buyUnit(UnitType.BA_64_ARMORED_CAR);
            canvas.draw();
            updateEconomyLabel(economyLabel, game);
        });
    }

    /**
     * @brief Set the font of affordability for each factory shop button
     *
     * @param panel Side panel of all widgets
     * @param game The current game
     * @param player The current player
     */
    private static void setButtonsAffodability(InfoPanelWidgets panel, Game game, String player){
        if (player.equals("P1")) {
            setButtonFontAffordable(panel.SOVIET_ASSAULT_SAPPER_SQUAD, game.canAffor(player, UnitType.SOVIET_ASSAULT_SAPPER_SQUAD));
            setButtonFontAffordable(panel.DP27_TEAM, game.canAffor(player, UnitType.DP27_TEAM));
            setButtonFontAffordable(panel.ZIS_3_FIELD_GUN, game.canAffor(player, UnitType.ZIS_3_FIELD_GUN));
            setButtonFontAffordable(panel.IS_1_HEAVY_TANK, game.canAffor(player, UnitType.IS_1_HEAVY_TANK));
            setButtonFontAffordable(panel.M3_HALFTRACK, game.canAffor(player, UnitType.M3_HALFTRACK));
            setButtonFontAffordable(panel.BA_64_ARMORED_CAR, game.canAffor(player, UnitType.BA_64_ARMORED_CAR));
        } else {
            setButtonFontAffordable(panel.WEHRMACHT_RIFLE_SQUAD, game.canAffor(player, UnitType.WEHRMACHT_RIFLE_SQUAD));
            setButtonFontAffordable(panel.GRENADIER_SQUAD, game.canAffor(player, UnitType.GRENADIER_SQUAD));
            setButtonFontAffordable(panel.MG42_TEAM, game.canAffor(player, UnitType.MG42_TEAM));
            setButtonFontAffordable(panel.SDKFZ_251_HALFTRACK, game.canAffor(player, UnitType.SDKFZ_251_HALFTRACK));
            setButtonFontAffordable(panel.PANZER_IV_AUSF_J, game.canAffor(player, UnitType.PANZER_IV_AUSF_J));
            setButtonFontAffordable(panel.SDKFZ_234_2_PUMA, game.canAffor(player, UnitType.SDKFZ_234_2_PUMA));
        }
    }

    /**
     * @brief Update the victory screen
     *
     * @param panel Side panel of all widgets
     */
    private static void updateVictoryScreen(InfoPanelWidgets panel){
        String winner = game.getWinner();
        if (winner != null){
            if(winner.equals("P1")) {
                panel.imageViewP1.setVisible(true);
                panel.imageViewP1.setManaged(true);
            }
            else {
                panel.imageViewP2.setVisible(true);
                panel.imageViewP2.setManaged(true);
            }
        }
        else{
            panel.imageViewP1.setVisible(false);
            panel.imageViewP1.setManaged(false);
            panel.imageViewP2.setVisible(false);
            panel.imageViewP2.setManaged(false);
        }
    }

    /**
     * @brief Update the current canvas screen
     *
     * @param game The current game
     */
    public static void updateScreen(Game game){
        if (canvas != null) {
            canvas.clearSelections();
        }

        if (turnLabel != null) {
            updateTurnLabel(turnLabel, game);
        }

        if (replayLabel != null) {
            updateReplayLabel(replayLabel, game);
        }

        if (economyLabel != null) {
            updateEconomyLabel(economyLabel, game);
        }

        if (info_panel != null) {
            clearInfoPanel(info_panel);
            updateVictoryScreen(info_panel);
        }
    }

    /**
     * @brief Small helper structure describing one startable scenario
     */
    private static class ScenarioDefinition {
        String name; ///< Display name of the scenario
        Path map_path; ///< The map file path
        Path units_path; ///< The unit placement file path
        Path buildings_path; ///< The building ownership file path

        /**
         * @brief Constructor of the scenario definition
         * 
         * @param name_ Display name of the scenario
         * @param map_path_ Path to the map file
         * @param units_path_ Path to the unit placement file
         * @param buildings_path_ Path to the building ownership file
         */
        ScenarioDefinition(String name_, Path map_path_, Path units_path_, Path buildings_path_) {
            this.name = name_;
            this.map_path = map_path_;
            this.units_path = units_path_;
            this.buildings_path = buildings_path_;
        }

        /**
         * @brief Show the scenario nicely inside ComboBox
         */
        @Override
        public String toString() {
            return this.name;
        }
    }
}