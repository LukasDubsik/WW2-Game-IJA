package app;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import model.game.Game;
import model.game.GameFactory;
import model.map.Building;
import model.map.Serializable.Overlay;
import model.map.Position;
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

        Label description_section; ///< Description section title
        InfoRow description_row; ///< Description row
    }

    /**
     * @brief the Main method to start the application as a whole
     */
    @Override
    public void start(Stage stage) {
        // Select the scenario to be loaded on application start
        // To switch scenario later, just change these two paths
        Path map_path = Path.of("lib/maps/balga_heiligenbeil_corridor_1945_large.map");
        Path units_path = Path.of("lib/maps/balga_heiligenbeil_corridor_1945_large.units");

        // Load the scenario -> map + starting units
        game = GameFactory.createGame(map_path, units_path);

        // Label holding the current wealth of each player
        economyLabel = new Label();
        economyLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 16px; -fx-font-weight: bold;");
        updateEconomyLabel(economyLabel, game);

        // Create the prettier side information panel
        info_panel = createInfoPanel();
        setFactoryButtonsEvents(info_panel, game, economyLabel);
        clearInfoPanel(info_panel);

        // Label holding the currently active turn/player
        turnLabel = new Label();
        turnLabel.setStyle("-fx-text-fill: #d0d0d0; -fx-font-size: 14px;");
        updateTurnLabel(turnLabel, game);

        // Label if the game is in replay mode
        replayLabel = new Label();
        replayLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
        updateReplayLabel(replayLabel, game);

        // Create the game canvas
        canvas = new GameCanvas(game, 80, 70);

        // What happens when tile is clicked by a mouse
        canvas.setOnTileClicked(position -> {
            updateInfoPanel(info_panel, game, position);
        });

        // Arrange the layout of the game
        BorderPane root = new BorderPane();
        root.setLeft(info_panel.root);

        // Create a group for the canvas
        // The group is important here because the ScrollPane operates on layoutBounds
        // and scaled content behaves more predictably this way
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
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // Ensure that the center pane resizes with the visible viewport
        scroller.viewportBoundsProperty().addListener((obs, old_bounds, new_bounds) -> {
            centered_pane.setMinSize(new_bounds.getWidth(), new_bounds.getHeight());
        });

        // Add handler for scrolling
        scroller.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() > 0) {
                zoomKeepingViewportCenter(canvas, scroller, true);
            } else if (event.getDeltaY() < 0) {
                zoomKeepingViewportCenter(canvas, scroller, false);
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

            // Ignore tiny hand jitter so a click is not lost
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
                updateReplayLabel(replayLabel, game);
            }

            dragging[0] = false;
            event.consume();
        });

        // Center the scroller in the center of the all
        root.setCenter(scroller);

        // Create a lower control bar for turn handling
        HBox bottomPanel = new HBox(12);
        bottomPanel.setPadding(new Insets(12));
        bottomPanel.setAlignment(Pos.CENTER_LEFT);
        bottomPanel.setStyle("-fx-background-color: #111111;");

        // Spacers so the button goes to the right corner
        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        Region rightSpacer = new Region();
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        // Checkbox for player 1 to be a bot
        CheckBox botP1CheckBox = new CheckBox("P1 bot");
        botP1CheckBox.setStyle(
                "-fx-text-fill: #f3f6fb;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 11px;" +
                        "-fx-padding: 4 0 4 0;" +
                        "-fx-mark-color: #4ac26b;" +
                        "-fx-focus-color: transparent;" +
                        "-fx-faint-focus-color: transparent;"
        );
        botP1CheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            game.setPlayerBot("P1", newValue);
        });

        // Checkbox for player 2 to be a bot
        CheckBox botP2CheckBox = new CheckBox("P2 bot");
        botP2CheckBox.setStyle(
                "-fx-text-fill: #f3f6fb;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 11px;" +
                        "-fx-padding: 4 0 4 0;" +
                        "-fx-mark-color: #4ac26b;" +
                        "-fx-focus-color: transparent;" +
                        "-fx-faint-focus-color: transparent;"
        );
        botP2CheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            game.setPlayerBot("P2", newValue);
        });

        // Button to open a new replay
        Button openReplayButton = new Button("Open replay");
        setDefaultButtonFont(openReplayButton);
        openReplayButton.setOnAction(event -> {
            CompletableFuture.runAsync(new Runnable() {
                @Override
                public void run() {
                    FileUtil.chooseFile("Open replay");

                    File file = FileUtil.file;
                    if(file == null)
                        return;

                    Replay replay = FileUtil.readFiletoReplay(file);
                    if(replay == null)
                        return;
                    String[] players = {"P1", "P2"};

                    game = new Game(players, replay);
                    canvas.setGame(game);

                    Platform.runLater(() -> {
                        canvas.draw();
                        updateReplayLabel(replayLabel, game);
                        updateTurnLabel(turnLabel, game);
                        updateEconomyLabel(economyLabel, game);
                    });
                }
            });
        });

        // Button to save the replay of the current game
        Button saveReplayButton = new Button("Save replay");
        setDefaultButtonFont(saveReplayButton);
        saveReplayButton.setOnAction(event -> {
            CompletableFuture.runAsync(new Runnable() {
                @Override
                public void run() {

                    FileUtil.createReplayFile("Save replay");
                    File file = FileUtil.file;
                    if(file == null)
                        return;

                    FileUtil.saveReplayToFile(game.getReplay(), file);
                }
            });
        });

        // Button to revert the previouse turn
        Button prevTurnButton = new Button("Prev turn");
        setDefaultButtonFont(prevTurnButton);
        prevTurnButton.setOnAction(event -> {
            game.prevTurn();

            // Remove all previous visual movement selections
            canvas.clearSelections();

            // Update the lower label
            updateTurnLabel(turnLabel, game);

            // Update replay label
            updateReplayLabel(replayLabel, game);

            // Update the economy label
            updateEconomyLabel(economyLabel, game);

            // Clear stale info from previous turn selection
            clearInfoPanel(info_panel);
        });

        // Button for shifting to the next turn
        Button nextTurnButton = new Button("Next turn");
        setDefaultButtonFont(nextTurnButton);
        nextTurnButton.setOnAction(event -> {
            // Move the game to the next turn
            game.nextTurn();

            // Remove all previous visual movement selections
            canvas.clearSelections();

            // Update the lower label
            updateTurnLabel(turnLabel, game);

            // Update replay label
            updateReplayLabel(replayLabel, game);

            // Update the economy label
            updateEconomyLabel(economyLabel, game);

            // Clear stale info from previous turn selection
            clearInfoPanel(info_panel);
        });

        // Put the controls together
        bottomPanel.getChildren().addAll(turnLabel, replayLabel, leftSpacer, economyLabel, rightSpacer, botP1CheckBox, botP2CheckBox, openReplayButton, saveReplayButton, prevTurnButton, nextTurnButton);

        // Place the panel at the bottom
        root.setBottom(bottomPanel);

        Scene scene = new Scene(root, 1500, 900);

        stage.setTitle("IJA game");
        stage.setScene(scene);
        stage.show();
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
        panel.description_row = createInfoRow("Summary");
        panel.description_row.value.setStyle("-fx-text-fill: #d5d9e0; -fx-font-size: 12px;");
        panel.description_row.value.setWrapText(true);

        // Create buttons for factory shop
        createFactoryButtons(panel);

        panel.content = new VBox(
                10,
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
     */
    private static void updateTurnLabel(Label turnLabel, Game game) {
        turnLabel.setText("Turn: " + game.getCurrentTurn() + " | Current player: " + game.getCurrentPlayer());
    }

    /**
     * @brief Update the reply label
     *
     * @param replayLabel The label to be updated
     * @param game The game from which to read the active turn
     */
    private static void updateReplayLabel(Label replayLabel, Game game){
        if(game.isReplayMode())
            replayLabel.setText("*Replay mode");
        else
            replayLabel.setText("");
    }

    /**
     * @brief Update the economy label text
     *
     * @param economyLabel The label to be updated
     * @param game The game from which to read the active turn
     */
    private static void updateEconomyLabel(Label economyLabel, Game game) {
        Map<String, Integer> playerWealth = game.getPlayerWealth();
        String p1 = (String) playerWealth.keySet().toArray()[0];
        String p2 = (String) playerWealth.keySet().toArray()[1];
        int p1Wealth = playerWealth.get(p1);
        int p2Wealth = playerWealth.get(p2);

        economyLabel.setText(p1Wealth + "$ : " + p1 + " | " + p2 + " : " + p2Wealth + "$");
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

        // P1 shop
        panel.factory_shop_grid_P1 = new GridPane();
        panel.factory_shop_grid_P1.setHgap(5);
        panel.factory_shop_grid_P1.setVgap(5);
        panel.factory_shop_grid_P1.add(panel.WEHRMACHT_RIFLE_SQUAD, 0, 0);
        panel.factory_shop_grid_P1.add(panel.GRENADIER_SQUAD, 1, 0);
        panel.factory_shop_grid_P1.add(panel.MG42_TEAM, 2, 0);
        panel.factory_shop_grid_P1.add(panel.SDKFZ_251_HALFTRACK, 0, 1);
        panel.factory_shop_grid_P1.add(panel.PANZER_IV_AUSF_J, 1, 1);
        panel.factory_shop_grid_P1.add(panel.SDKFZ_234_2_PUMA, 2, 1);
        panel.factory_shop_grid_P1.setVisible(false);
        panel.factory_shop_grid_P1.setManaged(false);

        // P2 shop
        panel.factory_shop_grid_P2 = new GridPane();
        panel.factory_shop_grid_P2.setHgap(5);
        panel.factory_shop_grid_P2.setVgap(5);
        panel.factory_shop_grid_P2.add(panel.SOVIET_ASSAULT_SAPPER_SQUAD, 0, 0);
        panel.factory_shop_grid_P2.add(panel.DP27_TEAM, 1, 0);
        panel.factory_shop_grid_P2.add(panel.ZIS_3_FIELD_GUN, 2, 0);
        panel.factory_shop_grid_P2.add(panel.IS_1_HEAVY_TANK, 0, 1);
        panel.factory_shop_grid_P2.add(panel.M3_HALFTRACK, 1, 1);
        panel.factory_shop_grid_P2.add(panel.BA_64_ARMORED_CAR, 2, 1);
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
        if(player.equals("P1")){
            setButtonFontAffordable(panel.WEHRMACHT_RIFLE_SQUAD, game.canAffor(player, UnitType.WEHRMACHT_RIFLE_SQUAD));
            setButtonFontAffordable(panel.GRENADIER_SQUAD, game.canAffor(player, UnitType.GRENADIER_SQUAD));
            setButtonFontAffordable(panel.MG42_TEAM, game.canAffor(player, UnitType.MG42_TEAM));
            setButtonFontAffordable(panel.SDKFZ_251_HALFTRACK, game.canAffor(player, UnitType.SDKFZ_251_HALFTRACK));
            setButtonFontAffordable(panel.PANZER_IV_AUSF_J, game.canAffor(player, UnitType.PANZER_IV_AUSF_J));
            setButtonFontAffordable(panel.SDKFZ_234_2_PUMA, game.canAffor(player, UnitType.SDKFZ_234_2_PUMA));
        }
        else{
            setButtonFontAffordable(panel.SOVIET_ASSAULT_SAPPER_SQUAD, game.canAffor(player, UnitType.SOVIET_ASSAULT_SAPPER_SQUAD));
            setButtonFontAffordable(panel.DP27_TEAM, game.canAffor(player, UnitType.DP27_TEAM));
            setButtonFontAffordable(panel.ZIS_3_FIELD_GUN, game.canAffor(player, UnitType.ZIS_3_FIELD_GUN));
            setButtonFontAffordable(panel.IS_1_HEAVY_TANK, game.canAffor(player, UnitType.IS_1_HEAVY_TANK));
            setButtonFontAffordable(panel.M3_HALFTRACK, game.canAffor(player, UnitType.M3_HALFTRACK));
            setButtonFontAffordable(panel.BA_64_ARMORED_CAR, game.canAffor(player, UnitType.BA_64_ARMORED_CAR));
        }
    }

    /**
     * @brief Update the current canvas screen
     *
     * @param game The current game
     */
    public static void updateScreen(Game game){
        canvas.clearSelections();
        updateTurnLabel(turnLabel, game);
        updateReplayLabel(replayLabel, game);
        updateEconomyLabel(economyLabel, game);
        clearInfoPanel(info_panel);
    }
}