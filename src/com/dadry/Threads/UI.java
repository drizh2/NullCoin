package com.dadry.Threads;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class UI extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = null;
        try {
            root = FXMLLoader.load(getClass()
                    .getResource("../View/MainWindow.fxml"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        primaryStage.setTitle("NullCoin");
        primaryStage.setScene(new Scene(root, 900, 700));
        primaryStage.show();
    }
}
