package project.demo1;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class WhiteBoard extends Application
{

    // Application state
    private double fontSize = 24;
    private MediaPlayer mediaPlayer;
    private MediaPlayer musicPlayer;
    private MediaView mediaView;
    private Stack<Runnable> undoStack = new Stack<>();
    private Stack<Runnable> redoStack = new Stack<>();
    private String currentTool = "PEN";
    private String currentShape = "RECTANGLE";
    private double startX, startY;
    private Color currentColor = Color.BLACK;
    private double currentBrushSize = 2;
    private Image currentImage;
    private double imageX = 0, imageY = 0;
    private double imageWidth, imageHeight;
    private boolean isDragging = false;
    private boolean isResizing = false;
    private boolean isImageSelected = false;
    private double mouseOffsetX, mouseOffsetY;
    private List<Canvas> layers = new ArrayList<>();
    private Canvas activeCanvas;
    private Pane canvasPane;
    private ListView<String> layerListView;
    private int layerCounter = 1;
    private double zoomFactor = 1.0;
    private Label nowPlayingLabel;
    private Label nowPlayingMusicLabel;
    private static final double RESIZE_HANDLE_SIZE = 10;
    private StackPane rootContainer;
    private TextField textInputField;
    private boolean isTextInputActive = false;
    private double resizeStartX, resizeStartY;

    // Media control buttons
    private Button playBtn;
    private Button pauseBtn;
    private Button stopBtn;
    private Button playMusicBtn;
    private Button pauseMusicBtn;
    private Button stopMusicBtn;

    @Override
    public void start(Stage primaryStage)
    {
        primaryStage.setTitle("Whiteboard with Media Controls");
        BorderPane root = new BorderPane();
        Scene scene = new Scene(root, 1000, 700);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

        // Main container that will hold both canvas and video
        rootContainer = new StackPane();
        rootContainer.getStyleClass().add("root-container");

        // Initialize canvas with smaller size
        canvasPane = new Pane();
        canvasPane.getStyleClass().add("canvas-pane");
        canvasPane.setPrefSize(700, 500);

        activeCanvas = createNewCanvasLayer();
        layers.add(activeCanvas);
        canvasPane.getChildren().add(activeCanvas);
        initializeDrawing(activeCanvas);

        // Add canvas first (will be behind video)
        rootContainer.getChildren().add(canvasPane);

        // Create ALL UI components
        HBox toolbar = createToolbar();
        VBox multimediaControls = createMultimediaControls();
        HBox mediaControls = createMediaControls();
        HBox musicControls = createMusicControls();
        VBox shapeControls = createShapeControls();
        VBox layerControls = createLayerControls();
        HBox stickyControls = createStickyControls();
        HBox collaborationControls = createCollaborationControls();
        HBox zoomControls = createZoomControls();
        HBox textControls = createTextControls();

        // Layout setup - organizing all components
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

        root.setCenter(rootContainer);

        // Keyboard shortcuts
        scene.addEventHandler(KeyEvent.KEY_PRESSED, event ->
        {
            if (event.isControlDown()) {
                if (event.getCode() == KeyCode.Z) undo();
                else if (event.getCode() == KeyCode.Y) redo();
                else if (event.getCode() == KeyCode.S) saveCanvasToFile(activeCanvas);
            }
        });

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Canvas createNewCanvasLayer()
    {
        Canvas canvas = new Canvas(700, 500);
        canvas.setStyle("-fx-background-color: transparent;");
        return canvas;
    }

    private void initializeDrawing(Canvas canvas)
    {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e ->
        {
            if (isTextInputActive) {
                if (textInputField != null && !textInputField.getText().isEmpty())
                {
                    gc.setFill(currentColor);
                    gc.setFont(Font.font("Arial", FontWeight.NORMAL, fontSize));
                    gc.fillText(textInputField.getText(), e.getX(), e.getY());
                    textInputField.clear();
                }
                isTextInputActive = false;
                if (canvasPane.getChildren().contains(textInputField))
                {
                    canvasPane.getChildren().remove(textInputField);
                }
                return;
            }

            startX = e.getX();
            startY = e.getY();

            // Check if we're clicking on the image to move/resize it
            if (currentImage != null && isPointInImage(e.getX(), e.getY()))
            {
                isImageSelected = true;
                mouseOffsetX = e.getX() - imageX;
                mouseOffsetY = e.getY() - imageY;

                // Check if click is near the corner for resizing
                if (isPointNearCorner(e.getX(), e.getY()))
                {
                    isResizing = true;
                    resizeStartX = e.getX();
                    resizeStartY = e.getY();
                }
                else
                {
                    isDragging = true;
                }
                return;
            }

            if (currentTool.equals("PEN"))
            {
                gc.setStroke(currentColor);
                gc.setLineWidth(currentBrushSize);
                gc.beginPath();
                gc.lineTo(startX, startY);
                gc.stroke();
            } else if (currentTool.equals("ERASER"))
            {
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(currentBrushSize);
                gc.beginPath();
                gc.lineTo(startX, startY);
                gc.stroke();
            }
        });

        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e ->
        {
            if (isTextInputActive) return;

            if (isDragging && currentImage != null)
            {
                imageX = e.getX() - mouseOffsetX;
                imageY = e.getY() - mouseOffsetY;
                redrawCanvas();
                return;
            }

            if (isResizing && currentImage != null)
            {
                double deltaX = e.getX() - resizeStartX;
                double deltaY = e.getY() - resizeStartY;
                imageWidth += deltaX;
                imageHeight += deltaY;
                resizeStartX = e.getX();
                resizeStartY = e.getY();
                redrawCanvas();
                return;
            }

            double x = e.getX();
            double y = e.getY();

            if (currentTool.equals("PEN"))
            {
                gc.setStroke(currentColor);
                gc.setLineWidth(currentBrushSize);
                gc.lineTo(x, y);
                gc.stroke();
            } else if (currentTool.equals("ERASER"))
            {
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(currentBrushSize);
                gc.lineTo(x, y);
                gc.stroke();
            }
        });

        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, e ->
        {
            if (isTextInputActive) return;

            double endX = e.getX();
            double endY = e.getY();

            if (currentTool.equals("SHAPE"))
            {
                drawShape(gc, startX, startY, endX, endY);
            }

            isDragging = false;
            isResizing = false;
            isImageSelected = false;
        });
    }

    private boolean isPointInImage(double x, double y)
    {
        return x >= imageX && x <= imageX + imageWidth &&
                y >= imageY && y <= imageY + imageHeight;
    }

    private boolean isPointNearCorner(double x, double y)
    {
        double cornerX = imageX + imageWidth;
        double cornerY = imageY + imageHeight;
        double distance = Math.sqrt(Math.pow(x - cornerX, 2) + Math.pow(y - cornerY, 2));
        return distance < 20;
    }

    private void drawShape(GraphicsContext gc, double startX, double startY, double endX, double endY)
    {
        gc.setStroke(currentColor);
        gc.setLineWidth(currentBrushSize);

        double width = endX - startX;
        double height = endY - startY;

        switch (currentShape)
        {
            case "RECTANGLE":
                gc.strokeRect(startX, startY, width, height);
                break;
            case "CIRCLE":
                double radius = Math.sqrt(width * width + height * height);
                gc.strokeOval(startX, startY, radius, radius);
                break;
            case "LINE":
                gc.strokeLine(startX, startY, endX, endY);
                break;
        }
    }

    private HBox createToolbar()
    {
        HBox toolbar = new HBox(10);
        toolbar.getStyleClass().add("toolbar");

        Button penBtn = new Button("Pen");
        penBtn.getStyleClass().add("tool-button");
        penBtn.setOnAction(e -> currentTool = "PEN");

        Button eraserBtn = new Button("Eraser");
        eraserBtn.getStyleClass().add("tool-button");
        eraserBtn.setOnAction(e -> currentTool = "ERASER");

        Button shapeBtn = new Button("Shape");
        shapeBtn.getStyleClass().add("tool-button");
        shapeBtn.setOnAction(e -> currentTool = "SHAPE");

        Button clearBtn = new Button("Clear");
        clearBtn.getStyleClass().add("tool-button");
        clearBtn.setOnAction(e -> clearCanvas());

        Button saveBtn = new Button("Save");
        saveBtn.getStyleClass().add("tool-button");
        saveBtn.setOnAction(e -> saveCanvasToFile(activeCanvas));

        ColorPicker colorPicker = new ColorPicker(currentColor);
        colorPicker.getStyleClass().add("color-picker");
        colorPicker.setOnAction(e -> currentColor = colorPicker.getValue());

        Slider sizeSlider = new Slider(1, 50, currentBrushSize);
        sizeSlider.getStyleClass().add("size-slider");
        sizeSlider.setShowTickLabels(true);
        sizeSlider.valueProperty().addListener((obs, old, val) -> currentBrushSize = val.doubleValue());

        toolbar.getChildren().addAll(
                penBtn, eraserBtn, shapeBtn, clearBtn, saveBtn,
                new Label("Color:"), colorPicker,
                new Label("Size:"), sizeSlider
        );

        return toolbar;
    }

    private void clearCanvas()
    {
        // Clear all drawings from all canvases
        for (Canvas canvas : layers)
        {
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.setFill(Color.WHITE);
            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        }

        // Clear the current image
        currentImage = null;
        isImageSelected = false;

        // Clear all sticky notes
        canvasPane.getChildren().removeIf(node ->
                node instanceof VBox && node.getStyleClass().contains("sticky-container"));

        // Clear undo/redo stacks
        undoStack.clear();
        redoStack.clear();

        // Reset text input state
        isTextInputActive = false;
        if (textInputField != null && canvasPane.getChildren().contains(textInputField))
        {
            canvasPane.getChildren().remove(textInputField);
        }

        // Redraw to ensure clean state
        redrawCanvas();
    }

    private VBox createMultimediaControls()
    {
        VBox controls = new VBox(10);
        controls.getStyleClass().add("multimedia-controls");

        Label title = new Label("Multimedia Controls");
        title.getStyleClass().add("control-title");

        HBox videoControls = new HBox(10);
        Button loadVideoBtn = new Button("Load Video");
        loadVideoBtn.getStyleClass().add("media-button");
        loadVideoBtn.setOnAction(e -> loadVideo());

        Button loadImageBtn = new Button("Load Image");
        loadImageBtn.getStyleClass().add("media-button");
        loadImageBtn.setOnAction(e -> loadImage());

        videoControls.getChildren().addAll(loadVideoBtn, loadImageBtn);

        nowPlayingLabel = new Label("No media loaded");
        nowPlayingLabel.getStyleClass().add("status-label");

        controls.getChildren().addAll(title, videoControls, nowPlayingLabel);
        return controls;
    }

    private HBox createMediaControls()
    {
        HBox controls = new HBox(10);
        controls.getStyleClass().add("media-controls");

        playBtn = new Button("Play");
        playBtn.getStyleClass().add("media-control-button");
        playBtn.setOnAction(e -> playMedia());

        pauseBtn = new Button("Pause");
        pauseBtn.getStyleClass().add("media-control-button");
        pauseBtn.setDisable(true);
        pauseBtn.setOnAction(e -> pauseMedia());

        stopBtn = new Button("Stop");
        stopBtn.getStyleClass().add("media-control-button");
        stopBtn.setDisable(true);
        stopBtn.setOnAction(e -> stopMedia());

        CheckBox muteVideoBtn = new CheckBox("Mute");
        muteVideoBtn.setSelected(false);
        muteVideoBtn.setOnAction(e ->
        {
            if (mediaPlayer != null)
            {
                mediaPlayer.setMute(muteVideoBtn.isSelected());
            }
        });

        Slider volumeSlider = new Slider(0, 1, 0.8);
        volumeSlider.getStyleClass().add("volume-slider");
        volumeSlider.setShowTickLabels(true);
        volumeSlider.valueProperty().addListener((obs, old, val) ->
        {
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(val.doubleValue());
                muteVideoBtn.setSelected(val.doubleValue() == 0);
            }
        });

        controls.getChildren().addAll(
                playBtn, pauseBtn, stopBtn, muteVideoBtn,
                new Label("Volume:"), volumeSlider
        );

        return controls;
    }

    private HBox createMusicControls()
    {
        HBox controls = new HBox(10);
        controls.getStyleClass().add("music-controls");

        Button loadMusicBtn = new Button("Load Music");
        loadMusicBtn.getStyleClass().add("music-button");
        loadMusicBtn.setOnAction(e -> loadMusic());

        playMusicBtn = new Button("Play");
        playMusicBtn.getStyleClass().add("music-button");
        playMusicBtn.setOnAction(e -> playMusic());

        pauseMusicBtn = new Button("Pause");
        pauseMusicBtn.getStyleClass().add("music-button");
        pauseMusicBtn.setDisable(true);
        pauseMusicBtn.setOnAction(e -> pauseMusic());

        stopMusicBtn = new Button("Stop");
        stopMusicBtn.getStyleClass().add("music-button");
        stopMusicBtn.setDisable(true);
        stopMusicBtn.setOnAction(e -> stopMusic());

        CheckBox muteMusicBtn = new CheckBox("Mute");
        muteMusicBtn.setSelected(false);
        muteMusicBtn.setOnAction(e ->
        {
            if (musicPlayer != null)
            {
                musicPlayer.setMute(muteMusicBtn.isSelected());
            }
        });

        Slider musicVolumeSlider = new Slider(0, 1, 0.8);
        musicVolumeSlider.getStyleClass().add("volume-slider");
        musicVolumeSlider.setShowTickLabels(true);
        musicVolumeSlider.valueProperty().addListener((obs, old, val) ->
        {
            if (musicPlayer != null)
            {
                musicPlayer.setVolume(val.doubleValue());
                muteMusicBtn.setSelected(val.doubleValue() == 0);
            }
        });

        nowPlayingMusicLabel = new Label("No music loaded");
        nowPlayingMusicLabel.getStyleClass().add("status-label");

        controls.getChildren().addAll(
                loadMusicBtn, playMusicBtn, pauseMusicBtn, stopMusicBtn, muteMusicBtn,
                new Label("Volume:"), musicVolumeSlider, nowPlayingMusicLabel
        );

        return controls;
    }

    private VBox createShapeControls()
    {
        VBox controls = new VBox(10);
        controls.getStyleClass().add("shape-controls");

        Label title = new Label("Shape Tools");
        title.getStyleClass().add("control-title");

        ToggleGroup shapeGroup = new ToggleGroup();

        RadioButton rectBtn = new RadioButton("Rectangle");
        rectBtn.setToggleGroup(shapeGroup);
        rectBtn.setSelected(true);
        rectBtn.getStyleClass().add("shape-radio");
        rectBtn.setOnAction(e -> currentShape = "RECTANGLE");

        RadioButton circleBtn = new RadioButton("Circle");
        circleBtn.setToggleGroup(shapeGroup);
        circleBtn.getStyleClass().add("shape-radio");
        circleBtn.setOnAction(e -> currentShape = "CIRCLE");

        RadioButton lineBtn = new RadioButton("Line");
        lineBtn.setToggleGroup(shapeGroup);
        lineBtn.getStyleClass().add("shape-radio");
        lineBtn.setOnAction(e -> currentShape = "LINE");

        controls.getChildren().addAll(title, rectBtn, circleBtn, lineBtn);
        return controls;
    }

    private VBox createLayerControls()
    {
        VBox controls = new VBox(10);
        controls.getStyleClass().add("layer-controls");

        Label title = new Label("Layer Management");
        title.getStyleClass().add("control-title");

        layerListView = new ListView<>();
        layerListView.getItems().add("Layer 1");
        layerListView.getSelectionModel().select(0);
        layerListView.getStyleClass().add("layer-list");

        layerListView.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) ->
        {
            int selectedIndex = layerListView.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0 && selectedIndex < layers.size())
            {
                activeCanvas = layers.get(selectedIndex);
            }
        });

        HBox layerButtons = new HBox(10);
        Button addLayerBtn = new Button("Add Layer");
        addLayerBtn.getStyleClass().add("layer-button");
        addLayerBtn.setOnAction(e -> addLayer());

        Button removeLayerBtn = new Button("Remove Layer");
        removeLayerBtn.getStyleClass().add("layer-button");
        removeLayerBtn.setOnAction(e -> removeLayer());

        layerButtons.getChildren().addAll(addLayerBtn, removeLayerBtn);
        controls.getChildren().addAll(title, layerListView, layerButtons);
        return controls;
    }

    private HBox createStickyControls()
    {
        HBox controls = new HBox(10);
        controls.getStyleClass().add("sticky-controls");

        Button addNoteBtn = new Button("Add Sticky Note");
        addNoteBtn.getStyleClass().add("sticky-button");
        addNoteBtn.setOnAction(e -> addStickyNote());

        controls.getChildren().add(addNoteBtn);
        return controls;
    }

    private void addStickyNote()
    {
        TextArea sticky = new TextArea();
        sticky.setWrapText(true);
        sticky.setPrefSize(150, 100);
        sticky.setLayoutX(100);
        sticky.setLayoutY(100);
        sticky.getStyleClass().add("sticky-note");

        final Delta dragDelta = new Delta();
        sticky.setOnMousePressed(e ->
        {
            dragDelta.x = sticky.getLayoutX() - e.getSceneX();
            dragDelta.y = sticky.getLayoutY() - e.getSceneY();
            sticky.setCursor(javafx.scene.Cursor.MOVE);
        });

        sticky.setOnMouseReleased(e -> sticky.setCursor(javafx.scene.Cursor.DEFAULT));
        sticky.setOnMouseDragged(e ->
        {
            sticky.setLayoutX(e.getSceneX() + dragDelta.x);
            sticky.setLayoutY(e.getSceneY() + dragDelta.y);
        });

        Button deleteBtn = new Button("X");
        deleteBtn.getStyleClass().add("delete-button");
        deleteBtn.setOnAction(e -> canvasPane.getChildren().remove(sticky));

        HBox header = new HBox(deleteBtn);
        header.setAlignment(Pos.CENTER_RIGHT);

        VBox container = new VBox(header, sticky);
        container.getStyleClass().add("sticky-container");
        container.setLayoutX(100);
        container.setLayoutY(100);

        canvasPane.getChildren().add(container);
    }

    private HBox createCollaborationControls()
    {
        HBox controls = new HBox(10);
        controls.getStyleClass().add("collab-controls");

        Button hostBtn = new Button("Host Session");
        hostBtn.getStyleClass().add("collab-button");
        hostBtn.setOnAction(e -> showAlert("Collaboration", "Hosting session at localhost:8080"));

        Button joinBtn = new Button("Join Session");
        joinBtn.getStyleClass().add("collab-button");
        joinBtn.setOnAction(e ->
        {
            TextInputDialog dialog = new TextInputDialog("localhost:8080");
            dialog.setTitle("Join Collaboration");
            dialog.setHeaderText("Enter server address:");
            dialog.showAndWait().ifPresent(host ->
            {
                showAlert("Collaboration", "Connected to " + host);
            });
        });

        controls.getChildren().addAll(hostBtn, joinBtn);
        return controls;
    }

    private HBox createZoomControls()
    {
        HBox controls = new HBox(10);
        controls.getStyleClass().add("zoom-controls");

        Slider zoomSlider = new Slider(0.1, 3.0, 1.0);
        zoomSlider.getStyleClass().add("zoom-slider");
        zoomSlider.setShowTickLabels(true);
        zoomSlider.valueProperty().addListener((obs, old, val) ->
        {
            zoomFactor = val.doubleValue();
            canvasPane.setScaleX(zoomFactor);
            canvasPane.setScaleY(zoomFactor);
        });

        Button resetZoomBtn = new Button("Reset Zoom");
        resetZoomBtn.getStyleClass().add("zoom-button");
        resetZoomBtn.setOnAction(e -> zoomSlider.setValue(1.0));

        controls.getChildren().addAll(
                new Label("Zoom:"), zoomSlider, resetZoomBtn
        );
        return controls;
    }

    private HBox createTextControls()
    {
        HBox controls = new HBox(10);
        controls.getStyleClass().add("text-controls");

        Button textBtn = new Button("Add Text");
        textBtn.getStyleClass().add("text-button");
        textBtn.setOnAction(e -> addText());

        Slider fontSizeSlider = new Slider(8, 72, fontSize);
        fontSizeSlider.getStyleClass().add("font-slider");
        fontSizeSlider.setShowTickLabels(true);
        fontSizeSlider.valueProperty().addListener((obs, old, val) ->
        {
            fontSize = val.doubleValue();
        });

        controls.getChildren().addAll(
                textBtn, new Label("Font Size:"), fontSizeSlider
        );
        return controls;
    }

    private void loadVideo()
    {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Video File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.avi", "*.mov", "*.flv")
        );

        File file = fileChooser.showOpenDialog(null);
        if (file != null)
        {
            try
            {
                if (mediaView != null)
                {
                    rootContainer.getChildren().remove(mediaView);
                    if (mediaPlayer != null)
                    {
                        mediaPlayer.stop();
                        mediaPlayer.dispose();
                    }
                }

                Media media = new Media(file.toURI().toString());
                mediaPlayer = new MediaPlayer(media);
                mediaPlayer.setVolume(0.8);
                mediaView = new MediaView(mediaPlayer);

                mediaView.setPreserveRatio(true);
                mediaView.setFitWidth(300);
                mediaView.setLayoutX(20);
                mediaView.setLayoutY(20);

                rootContainer.getChildren().add(mediaView);
                mediaView.toFront();

                nowPlayingLabel.setText("Now playing: " + file.getName());
                updateMediaButtonStates(true);
                mediaPlayer.play();

                mediaView.setOnMouseClicked(e -> mediaView.toFront());

                mediaPlayer.setOnEndOfMedia(() -> {
                    nowPlayingLabel.setText("Playback finished: " + file.getName());
                    updateMediaButtonStates(false);
                });

                mediaPlayer.setOnError(() ->
                {
                    nowPlayingLabel.setText("Error playing: " + file.getName());
                    updateMediaButtonStates(false);
                });

            }
            catch (Exception e)
            {
                showAlert("Error", "Could not load video: " + e.getMessage());
            }
        }
    }

    private void loadMusic()
    {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Music File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav", "*.aac", "*.m4a")
        );

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                if (musicPlayer != null)
                {
                    musicPlayer.stop();
                    musicPlayer.dispose();
                }

                Media media = new Media(file.toURI().toString());
                musicPlayer = new MediaPlayer(media);
                musicPlayer.setVolume(0.8);

                nowPlayingMusicLabel.setText("Now playing: " + file.getName());
                updateMusicButtonStates(true);
                musicPlayer.play();

                musicPlayer.setOnEndOfMedia(() ->
                {
                    nowPlayingMusicLabel.setText("Playback finished: " + file.getName());
                    updateMusicButtonStates(false);
                });

                musicPlayer.setOnError(() ->
                {
                    nowPlayingMusicLabel.setText("Error playing: " + file.getName());
                    updateMusicButtonStates(false);
                });

            }
            catch (Exception e)
            {
                showAlert("Error", "Could not load music: " + e.getMessage());
            }
        }
    }

    private void loadImage()
    {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Image File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File file = fileChooser.showOpenDialog(null);
        if (file != null)
        {
            try
            {
                currentImage = new Image(file.toURI().toString());

                double canvasWidth = activeCanvas.getWidth();
                double canvasHeight = activeCanvas.getHeight();
                double imgWidth = currentImage.getWidth();
                double imgHeight = currentImage.getHeight();

                double scaleFactor = Math.min(
                        canvasWidth * 0.8 / imgWidth,
                        canvasHeight * 0.8 / imgHeight
                );

                imageWidth = imgWidth * scaleFactor;
                imageHeight = imgHeight * scaleFactor;

                imageX = (canvasWidth - imageWidth) / 2;
                imageY = (canvasHeight - imageHeight) / 2;

                redrawCanvas();
                nowPlayingLabel.setText("Loaded image: " + file.getName());

            }
            catch (Exception e)
            {
                showAlert("Error", "Could not load image: " + e.getMessage());
            }
        }
    }

    private void saveCanvasToFile(Canvas canvas)
    {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG files (*.png)", "*.png")
        );

        File file = fileChooser.showSaveDialog(null);
        if (file != null)
        {
            try {
                WritableImage writableImage = new WritableImage((int)canvas.getWidth(), (int)canvas.getHeight());
                canvas.snapshot(null, writableImage);

                String filename = file.getName();
                if (!filename.endsWith(".png"))
                {
                    file = new File(file.getParentFile(), filename + ".png");
                }

                ImageIO.write(SwingFXUtils.fromFXImage(writableImage, null), "png", file);
            }
            catch (IOException ex)
            {
                showAlert("Error", "Failed to save image: " + ex.getMessage());
            }
        }
    }

    private void undo()
    {
        if (!undoStack.isEmpty())
        {
            Runnable action = undoStack.pop();
            redoStack.push(action);
            redrawCanvas();
        }
    }

    private void redo()
    {
        if (!redoStack.isEmpty())
        {
            Runnable action = redoStack.pop();
            undoStack.push(action);
            redrawCanvas();
        }
    }

    private void redrawCanvas()
    {
        GraphicsContext gc = activeCanvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, activeCanvas.getWidth(), activeCanvas.getHeight());

        for (Runnable action : undoStack)
        {
            action.run();
        }

        if (currentImage != null)
        {
            gc.drawImage(currentImage, imageX, imageY, imageWidth, imageHeight);

            if (isImageSelected)
            {
                gc.setStroke(Color.BLUE);
                gc.setLineWidth(2);
                gc.strokeOval(imageX + imageWidth - 5, imageY + imageHeight - 5, 10, 10);
            }
        }
    }

    private void showAlert(String title, String message)
    {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void updateMediaButtonStates(boolean isPlaying)
    {
        playBtn.setDisable(isPlaying);
        pauseBtn.setDisable(!isPlaying);
        stopBtn.setDisable(!isPlaying);
    }

    private void updateMusicButtonStates(boolean isPlaying)
    {
        playMusicBtn.setDisable(isPlaying);
        pauseMusicBtn.setDisable(!isPlaying);
        stopMusicBtn.setDisable(!isPlaying);
    }

    private String getMediaName(String source)
    {
        try
        {
            return new File(new URL(source).getFile()).getName();
        }
        catch (Exception e)
        {
            return source;
        }
    }

    private void playMedia()
    {
        if (mediaPlayer != null)
        {
            mediaPlayer.play();
            nowPlayingLabel.setText("Playing: " + getMediaName(mediaPlayer.getMedia().getSource()));
            updateMediaButtonStates(true);
        }
    }

    private void pauseMedia()
    {
        if (mediaPlayer != null)
        {
            mediaPlayer.pause();
            nowPlayingLabel.setText("Paused: " + getMediaName(mediaPlayer.getMedia().getSource()));
            updateMediaButtonStates(false);
        }
    }

    private void stopMedia()
    {
        if (mediaPlayer != null)
        {
            mediaPlayer.stop();
            nowPlayingLabel.setText("Stopped: " + getMediaName(mediaPlayer.getMedia().getSource()));
            updateMediaButtonStates(false);
        }
    }

    private void playMusic()
    {
        if (musicPlayer != null)
        {
            musicPlayer.play();
            nowPlayingMusicLabel.setText("Playing: " + getMediaName(musicPlayer.getMedia().getSource()));
            updateMusicButtonStates(true);
        }
    }

    private void pauseMusic()
    {
        if (musicPlayer != null)
        {
            musicPlayer.pause();
            nowPlayingMusicLabel.setText("Paused: " + getMediaName(musicPlayer.getMedia().getSource()));
            updateMusicButtonStates(false);
        }
    }

    private void stopMusic()
    {
        if (musicPlayer != null)
        {
            musicPlayer.stop();
            nowPlayingMusicLabel.setText("Stopped: " + getMediaName(musicPlayer.getMedia().getSource()));
            updateMusicButtonStates(false);
        }
    }

    private void addLayer()
    {
        Canvas newLayer = createNewCanvasLayer();
        layers.add(newLayer);
        canvasPane.getChildren().add(newLayer);
        initializeDrawing(newLayer);
        layerCounter++;
        layerListView.getItems().add("Layer " + layerCounter);
        layerListView.getSelectionModel().select(layers.size() - 1);
    }

    private void removeLayer()
    {
        int selectedIndex = layerListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < layers.size())
        {
            if (layers.size() > 1)
            {
                canvasPane.getChildren().remove(layers.get(selectedIndex));
                layers.remove(selectedIndex);
                layerListView.getItems().remove(selectedIndex);
                layerListView.getSelectionModel().select(Math.min(selectedIndex, layers.size() - 1));
            }
            else
            {
                showAlert("Layer Error", "Cannot remove the last layer");
            }
        }
    }

    private void addText()
    {
        isTextInputActive = true;
        textInputField = new TextField();
        textInputField.setPromptText("Enter text here...");
        textInputField.setLayoutX(50);
        textInputField.setLayoutY(50);
        canvasPane.getChildren().add(textInputField);
        textInputField.requestFocus();
    }

    private class Delta
    {
        double x, y;
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}