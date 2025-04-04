package project.demo3;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.Objects;

public class WhiteBoard extends Application
{
    private CanvasManager canvasManager;
    private MediaManager mediaManager;
    private BorderPane root;

    @Override
    public void start(Stage primaryStage)
    {
        primaryStage.setTitle("Whiteboard");

        // Initialize managers
        canvasManager = new CanvasManager();
        mediaManager = new MediaManager(canvasManager);

        // Create main layout
        root = new BorderPane();
        Scene scene = new Scene(root, 1000, 700);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/Styles.css")).toExternalForm());

        // Setup UI components
        setupUI();

        // Set keyboard shortcuts
        setupKeyboardShortcuts(scene);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void setupUI()
    {
        // Create all UI components
        HBox toolbar = canvasManager.createToolbar();
        VBox multimediaControls = mediaManager.createMultimediaControls();
        HBox mediaControls = mediaManager.createMediaControls();
        HBox musicControls = mediaManager.createMusicControls();
        VBox shapeControls = canvasManager.createShapeControls();
        VBox layerControls = canvasManager.createLayerControls();
        HBox stickyControls = canvasManager.createStickyControls();
        HBox collaborationControls = canvasManager.createCollaborationControls();
        HBox zoomControls = canvasManager.createZoomControls();
        HBox textControls = canvasManager.createTextControls();

        // Layout setup
        VBox leftPanel = new VBox(10, shapeControls, layerControls, stickyControls, textControls);
        leftPanel.getStyleClass().add("left-panel");

        VBox rightPanel = new VBox(10, multimediaControls, zoomControls);
        rightPanel.getStyleClass().add("right-panel");

        root.setTop(new VBox(toolbar, collaborationControls));
        root.setLeft(leftPanel);
        root.setRight(rightPanel);

        // Combine media and music controls at bottom
        VBox bottomControls = new VBox(5, mediaControls, musicControls);
        bottomControls.getStyleClass().add("bottom-controls");
        root.setBottom(bottomControls);

        root.setCenter(canvasManager.getRootContainer());
    }

    private void setupKeyboardShortcuts(Scene scene)
    {
        scene.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, event ->
        {
            if (event.isControlDown())
            {
                if (event.getCode() == javafx.scene.input.KeyCode.Z)
                {
                    canvasManager.undo();
                }
                else if (event.getCode() == javafx.scene.input.KeyCode.Y)
                {
                    canvasManager.redo();
                }
                else if (event.getCode() == javafx.scene.input.KeyCode.S)
                {
                    canvasManager.saveCanvasToFile();
                }
            }
        });
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
