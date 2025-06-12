package org.example.clienteuno;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class TabuleiroController implements ControladorDeJogo {

	@FXML private HBox maoJogador;
	@FXML private HBox maoOponente;
	@FXML private ImageView cartaMeio;
	@FXML private TextArea areaMensagens;
	@FXML private Button botaoComprar;
	@FXML private Label contadorOponente;
	@FXML private HBox boxCores; // Contém os 4 botões de cor

	private Socket socket;
	private DataInputStream dis;
	private DataOutputStream dos;
	private String nomeJogador;

	public void inicializarComDados(Socket socket, DataInputStream dis, DataOutputStream dos, String nomeJogador) {
		this.socket = socket;
		this.dis = dis;
		this.dos = dos;
		this.nomeJogador = nomeJogador;


		boxCores.setVisible(true); // Esconde os botões de cor inicialmente

		// Inicia leitura do servidor
		if (this.dis != null) {
			Thread leitor = new Thread(new LeitorServidor(this.dis, this));
			leitor.setDaemon(true);
			leitor.start();
		}
	}

	public void atualizarMaoJogador(List<Carta> cartas) {
		Platform.runLater(() -> {
			maoJogador.getChildren().clear();
			for (Carta carta : cartas) {
				Image img = new Image(getClass().getResourceAsStream("/images/" + carta.getNomeImagem() + ".png"));
				ImageView view = new ImageView(img);
				view.setFitWidth(80);
				view.setFitHeight(120);
				view.setOnMouseClicked(e -> jogarCarta(carta));
				maoJogador.getChildren().add(view);
			}
		});
	}

	public void atualizarCartaMeio(Carta carta) {
		Platform.runLater(() -> {
			Image img = new Image(getClass().getResourceAsStream("/images/" + carta.getNomeImagem() + ".png"));
			cartaMeio.setImage(img);
		});
	}

	public void atualizarCartasOponente(int numCartas) {
		Platform.runLater(() -> {
			maoOponente.getChildren().clear();
			contadorOponente.setText(String.valueOf(numCartas));
			for (int i = 0; i < numCartas; i++) {
				Image img = new Image(getClass().getResourceAsStream("/images/cartavoltada.png"));
				ImageView view = new ImageView(img);
				view.setFitWidth(80);
				view.setFitHeight(120);
				maoOponente.getChildren().add(view);
			}
		});
	}

	public void adicionarMensagem(String msg) {
		Platform.runLater(() -> areaMensagens.appendText(msg + "\n"));
	}

	private void jogarCarta(Carta carta) {

		try {
			dos.writeUTF("JOGAR:" + carta.getNomeImagem());

		} catch (IOException e) {
			adicionarMensagem("Erro ao jogar carta.");
		}
	}

	@FXML
	private void comprarCarta() {
		try {
			dos.writeUTF("COMPRAR");
		} catch (IOException e) {
			adicionarMensagem("Erro ao comprar carta.");
		}
	}

	@FXML
	private void comecarJogo() {
		botaoComprar.setVisible(true);
		try {
			dos.writeUTF("COMECOU");
		} catch (IOException e) {
			adicionarMensagem("Erro ao iniciar o jogo.");
		}
	}

	// Métodos para os botões de cor
	@FXML private void escolherVermelho() { enviarCor("V"); }
	@FXML private void escolherVerde()    { enviarCor("G"); }
	@FXML private void escolherAmarelo()  { enviarCor("A"); }
	@FXML private void escolherAzul()     { enviarCor("B"); }

	private void enviarCor(String cor) {
		try {
			dos.writeUTF("COR:" + cor);
			boxCores.setVisible(false); // Esconde após enviar
		} catch (IOException e) {
			adicionarMensagem("Erro ao enviar cor.");
		}
	}
}
