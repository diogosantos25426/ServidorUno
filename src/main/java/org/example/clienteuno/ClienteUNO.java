package org.example.clienteuno;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.List;

public class ClienteUNO extends Application {
	@Override
	public void start(Stage stage) throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("cliente.fxml"));
		Scene scene = new Scene(fxmlLoader.load(), 600, 400);
		stage.setTitle("Cliente UNO");
		stage.setScene(scene);
		stage.show();
	}
	public static void main(String[] args) {
		launch(args);
	}
}
