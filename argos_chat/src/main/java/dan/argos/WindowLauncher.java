package dan.argos;

/////////////////////////
//Author: Daniel Marquez
//Description: Opens new ArgosChat Client Window.
/////////////////////////

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class WindowLauncher extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/dan/argos/ChatClient.fxml"));
        Scene scene = new Scene(root);
        stage.setTitle("Argos v1.1.0");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("icon.png")));
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}