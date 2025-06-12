package org.example.clienteuno;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.List;

public class JanelaVerdeController {

	private HBox cartasJogadorBox = new HBox(10);
	private HBox cartasAdversarioBox = new HBox(10);
	private VBox root = new VBox(30, cartasAdversarioBox, cartasJogadorBox);

	public JanelaVerdeController(Stage stage) {
		cartasJogadorBox.setStyle("-fx-padding: 20; -fx-background-color: #228B22;");
		cartasAdversarioBox.setStyle("-fx-padding: 20; -fx-background-color: #006400;");

		// Criar 7 cartas viradas para o adversário
		InputStream viradaInput = getClass().getResourceAsStream("/imagens/CARTAVIRADA.PNG");
		if (viradaInput != null) {
			Image imgVirada = new Image(viradaInput);
			for (int i = 0; i < 7; i++) {
				ImageView imgView = new ImageView(imgVirada);
				imgView.setFitWidth(70);
				imgView.setFitHeight(100);
				cartasAdversarioBox.getChildren().add(imgView);
			}
		} else {
			System.out.println("Imagem CARTAVIRADA.PNG não encontrada!");
		}

		// Criar janela
		Scene scene = new Scene(root, 800, 400);
		stage.setTitle("Mesa de Jogo UNO");
		stage.setScene(scene);
		stage.show();
	}

	// Método que podes chamar para atualizar as cartas do jogador
	public void mostrarCartasJogador(List<String> cartas) {
		cartasJogadorBox.getChildren().clear();
		for (String carta : cartas) {
			InputStream input = getClass().getResourceAsStream("/imagens/" + carta);
			if (input == null) {
				System.out.println("Carta não encontrada: " + carta);
				continue;
			}
			Image img = new Image(input);
			ImageView imgView = new ImageView(img);
			imgView.setFitWidth(70);
			imgView.setFitHeight(100);
			cartasJogadorBox.getChildren().add(imgView);
		}
	}
}
