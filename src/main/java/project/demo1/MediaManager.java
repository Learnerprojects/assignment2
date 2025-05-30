package project.demo3;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.*;
import javafx.scene.image.*;
import javafx.stage.FileChooser;
import javafx.scene.input.*;
import javafx.scene.paint.Color;
import javafx.scene.image.ImageView;
import java.io.File;

public class MediaManager
{

    private CanvasManager canvasManager;
    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private ImageView imageView;
    private Label mediaLabel;

    public MediaManager(CanvasManager canvasManager)
    {
        this.canvasManager = canvasManager;
        this.mediaLabel = new Label();
        this.mediaLabel.setTextFill(Color.BLACK); // Make text readable on light backgrounds
        this.mediaLabel.setStyle("-fx-font-size: 14px;");
    }

    // Create multimedia controls (video, music, image upload, and clear)
    public VBox createMultimediaControls()
    {
        VBox multimediaControls = new VBox(10);

        // Buttons for adding image, video, music, and clearing the canvas
        Button imageButton = new Button("Load Image");
        Button videoButton = new Button("Load Video");
        Button musicButton = new Button("Load Music");
        Button clearButton = new Button("Clear Canvas");

        // Image button functionality
        imageButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
            File file = fileChooser.showOpenDialog(null);
            if (file != null) {
                // Display selected image
                Image image = new Image(file.toURI().toString());
                imageView = new ImageView(image);
                imageView.setFitWidth(400); // Initially set width, can be resized later
                imageView.setFitHeight(300); // Initially set height, can be resized later
                imageView.setPreserveRatio(true); // Ensure the image aspect ratio is preserved

                // Remove previous media before adding new one
                canvasManager.getRootContainer().getChildren().clear();

                // Add the image to the canvas or any container
                canvasManager.getRootContainer().getChildren().add(imageView);  // Ensure this adds it to your layout
                canvasManager.getRootContainer().getChildren().add(mediaLabel);  // Add label with media name
                mediaLabel.setText("Image: " + file.getName());
                System.out.println("Image selected: " + file.getAbsolutePath());

                // Enable double-click resizing functionality
                addDoubleClickResize(imageView);
            }
        });

        // Video button functionality
        videoButton.setOnAction(e ->
        {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.avi", "*.mkv"));
            File file = fileChooser.showOpenDialog(null);
            if (file != null)
            {
                try
                {
                    // Load video into the media player
                    Media media = new Media(file.toURI().toString());
                    mediaPlayer = new MediaPlayer(media);
                    mediaView = new MediaView(mediaPlayer);

                    // Resize the video to fit within the canvas size
                    mediaView.setFitWidth(800); // Adjust according to your canvas size
                    mediaView.setFitHeight(600); // Adjust according to your canvas size
                    mediaView.setPreserveRatio(true); // Maintain aspect ratio

                    // Remove previous media before adding new one
                    canvasManager.getRootContainer().getChildren().clear();

                    // Add the MediaView to the canvas or layout
                    canvasManager.getRootContainer().getChildren().add(mediaView);  // Ensure this adds it to your layout
                    canvasManager.getRootContainer().getChildren().add(mediaLabel);  // Add label with media name
                    mediaLabel.setText("Video: " + file.getName());
                    System.out.println("Video selected: " + file.getAbsolutePath());

                    // Optionally, play the video automatically once selected
                    mediaPlayer.play();
                }
                catch (Exception ex)
                {
                    System.out.println("Error loading video: " + ex.getMessage());
                    mediaLabel.setText("Error: Unsupported video format.");
                }
            }
        });

        // Music button functionality
        musicButton.setOnAction(e ->
        {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Music Files", "*.mp3", "*.wav"));
            File file = fileChooser.showOpenDialog(null);
            if (file != null)
            {
                try 
                {
                    // Load music into MediaPlayer
                    Media media = new Media(file.toURI().toString());
                    mediaPlayer = new MediaPlayer(media);

                    // Remove previous media before adding new one
                    canvasManager.getRootContainer().getChildren().clear();

                    // Set to auto-play
                    mediaPlayer.play();
                    canvasManager.getRootContainer().getChildren().add(mediaLabel);  // Add label with media name
                    mediaLabel.setText("Music: " + file.getName());
                    System.out.println("Music selected: " + file.getAbsolutePath());
                }
                catch (Exception ex)
                {
                    System.out.println("Error loading music: " + ex.getMessage());
                    mediaLabel.setText("Error: Unsupported music format.");
                }
            }
        });

        // Clear button functionality
        clearButton.setOnAction(e ->
        {
            // Remove all media from the canvas
            canvasManager.getRootContainer().getChildren().clear();
            mediaLabel.setText("Media cleared.");
        });

        multimediaControls.getChildren().addAll(imageButton, videoButton, musicButton, clearButton);
        multimediaControls.setStyle("-fx-background-color: #f4f4f4; -fx-padding: 10;");
        return multimediaControls;
    }

    // Add double-click resizing to the image
    private void addDoubleClickResize(ImageView imageView)
    {
        imageView.setOnMouseClicked(event ->
        {
            if (event.getClickCount() == 2)
            {  // Detect double-click
                // Open a dialog to resize the image
                TextInputDialog widthDialog = new TextInputDialog(String.valueOf(imageView.getFitWidth()));
                widthDialog.setHeaderText("Enter new width:");
                widthDialog.showAndWait().ifPresent(width ->
                {
                    try
                    {
                        double newWidth = Double.parseDouble(width);
                        imageView.setFitWidth(newWidth);
                    }
                    catch (NumberFormatException ex)
                    {
                        System.out.println("Invalid width value");
                    }
                });

                TextInputDialog heightDialog = new TextInputDialog(String.valueOf(imageView.getFitHeight()));
                heightDialog.setHeaderText("Enter new height:");
                heightDialog.showAndWait().ifPresent(height ->
                {
                    try
                    {
                        double newHeight = Double.parseDouble(height);
                        imageView.setFitHeight(newHeight);
                    }
                    catch (NumberFormatException ex)
                    {
                        System.out.println("Invalid height value");
                    }
                });
            }
        });
    }

    // Create media controls (play/pause, volume control)
    public HBox createMediaControls()
    {
        HBox mediaControls = new HBox(10);

        Button playButton = new Button("Play");
        Button pauseButton = new Button("Pause");

        // Play button functionality
        playButton.setOnAction(e ->
        {
            if (mediaPlayer != null)
            {
                mediaPlayer.play();

            }
        });

        // Pause button functionality
        pauseButton.setOnAction(e ->
        {
            if (mediaPlayer != null)
            {
                mediaPlayer.pause();

            }
        });

        mediaControls.getChildren().addAll(playButton, pauseButton);
        mediaControls.setStyle("-fx-background-color: #eaeaea; -fx-padding: 10;");
        return mediaControls;
    }

    // Create music controls (e.g., volume control)
    public HBox createMusicControls()
    {
        HBox musicControls = new HBox(10);

        Label volumeLabel = new Label("Volume");
        Slider volumeSlider = new Slider(0, 100, 50);
        volumeSlider.setBlockIncrement(10);
        volumeSlider.setShowTickMarks(true);
        volumeSlider.setShowTickLabels(true);

        // Adjust media volume when slider is moved
        volumeSlider.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            if (mediaPlayer != null)
            {
                mediaPlayer.setVolume(newValue.doubleValue() / 100);
            }
        });

        Button prevTrackButton = new Button("Previous");
        Button nextTrackButton = new Button("Next");


        nextTrackButton.setOnAction(e -> 
        {
            // Action for next track (you can modify this to actually change tracks)
            System.out.println("Next track clicked!");
        });

        musicControls.getChildren().addAll(volumeLabel, volumeSlider, prevTrackButton, nextTrackButton);
        musicControls.setStyle("-fx-background-color: #dcdcdc; -fx-padding: 10;");
        return musicControls;
    }
}
