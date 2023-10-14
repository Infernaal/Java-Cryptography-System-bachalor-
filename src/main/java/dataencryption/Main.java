package sample.dataencryption;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/sample/dataencryption/application.fxml"));
        AnchorPane root = fxmlLoader.load();
        Scene scene = new Scene(root, 450, 736);
        stage.setTitle("SecureCryptDevice");
        Image icon = new Image("D:\\JavaCryptographySystem\\src\\image\\icon.png");
        stage.getIcons().add(icon);

        stage.setOnShown(event -> {
            stage.setMinWidth(stage.getWidth());
            stage.setMinHeight(stage.getHeight());
            stage.setMaxWidth(stage.getWidth());
            stage.setMaxHeight(stage.getHeight());
        });

        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
