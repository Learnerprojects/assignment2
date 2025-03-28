module project.demo1 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.desktop;
    requires javafx.swing;

    opens project.demo1 to javafx.fxml;
    exports project.demo1;
}