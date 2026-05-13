package app;

import javafx.application.Platform;
import javafx.stage.FileChooser;
import replay.Replay;

import java.io.*;
import java.util.concurrent.CompletableFuture;

public class FileUtil {

    public static File file; ///< Holder of the latest choosen file

    /**
     * @brief Open UI to choose a file
     *
     * @param title The title of the file chooser
     */
    public static void chooseFile(String title) {
        CompletableFuture<File> future = new CompletableFuture<>();

        Platform.runLater(() -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(title);

            File currentDir = new File(System.getProperty("user.dir"));
            if (currentDir.exists() && currentDir.isDirectory()) {
                fileChooser.setInitialDirectory(currentDir);
            }

            File selectedFile = fileChooser.showOpenDialog(null);

            future.complete(selectedFile);
        });

        try {
            file = future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            file = null;
        } catch (Exception e) {
            e.printStackTrace();
            file = null;
        }
    }

    /**
     * @brief Open UI to create a replay file
     *
     * @param title The title of the file chooser
     */
    public static void createReplayFile(String title) {
        CompletableFuture<File> future = new CompletableFuture<>();

        Platform.runLater(() -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(title);

            File currentDir = new File(System.getProperty("user.dir"));
            if (currentDir.exists() && currentDir.isDirectory()) {
                fileChooser.setInitialDirectory(currentDir);
            }

            FileChooser.ExtensionFilter extFilter =
                    new FileChooser.ExtensionFilter("Replay Files (*.replay)", "*.replay");
            fileChooser.getExtensionFilters().add(extFilter);

            fileChooser.setInitialFileName("IJAGame.replay");

            File selectedFile = fileChooser.showSaveDialog(null);

            future.complete(selectedFile);
        });

        try {
            file = future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            file = null;
        } catch (Exception e) {
            e.printStackTrace();
            file = null;
        }
    }

    /**
     * @brief Save the replay into a file
     *
     * @param replay Replay of the game
     * @param targetFile File where the replay will be saved
     */
    public static void saveReplayToFile(Replay replay, File targetFile) {
        if (replay == null || targetFile == null)
            return;

        try (FileOutputStream fos = new FileOutputStream(targetFile);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {

            oos.writeObject(replay);

        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @brief Read the replay from a file
     *
     * @param file File to read
     *
     * @return The loaded replay
     */
    public static Replay readFiletoReplay(File file){
        if(file == null)
            return null;

        try (FileInputStream fis = new FileInputStream(file); ObjectInputStream ois = new ObjectInputStream(fis)) {

            Object object = ois.readObject();
            return (Replay) object;
        }
        catch (IOException | ClassNotFoundException e){
            e.printStackTrace();
            return null;
        }
    }
}
