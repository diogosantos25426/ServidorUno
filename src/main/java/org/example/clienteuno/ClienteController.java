package org.example.clienteuno;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClienteController implements ControladorDeJogo  {

	@FXML private TextField campoMensagem;
	@FXML private TextArea areaMensagens;
	@FXML private Button botaoEnviar;
	@FXML private Button botaoLigar;
	@FXML private TextField campoNome;
	@FXML private TextField campoIP;
	@FXML private TextField campoPorta;
	@FXML private AnchorPane root;
	@FXML private ImageView baralhoView;
	@FXML private ImageView cartaMeio;
	@FXML private HBox maoJogador;
	@FXML private HBox maoOponenteHBox;
	@FXML private Label contadorOponente;
	@FXML private Button botaoComprar;
	@FXML private ChoiceBox<String> escolhaCor;
	@FXML private Button botaoConfirmarCor;


	private DataOutputStream dos;
	private DataInputStream dis;
	private Socket socket;
	private List<Carta> cartasJogador = new ArrayList<>();
	private Carta cartaAtualMeio;
	private int numCartasOponente;
	private LeitorServidor leitorServidor;

	public void initialize() {
		if (campoMensagem != null) {
			campoMensagem.setDisable(true);
			botaoEnviar.setDisable(true);
		}
		if (baralhoView != null) {
			try {
				baralhoView.setImage(new Image(getClass().getResourceAsStream("/images/baralho.png")));
			} catch (NullPointerException e) {
				System.err.println("Erro: baralho.png não encontrado em /images/");
				adicionarMensagem("Erro: baralho.png não encontrado.");
			}
		}
		if (escolhaCor != null) {
			escolhaCor.getItems().addAll("Vermelho", "Verde", "Amarelo", "Azul");
			escolhaCor.setVisible(false);
			botaoConfirmarCor.setVisible(false);
		}
	}

	@FXML
	private void ligarServidor() {
		String nome = campoNome.getText().trim();
		if (nome.isEmpty()) {
			adicionarMensagem("Insere um nome.");
			return;
		}

		try {
			String ip = campoIP.getText().trim();
			String portaTexto = campoPorta.getText().trim();

			if (ip.isEmpty() || portaTexto.isEmpty()) {
				adicionarMensagem("Insere o IP e a Porta.");
				return;
			}

			int porta;
			try {
				porta = Integer.parseInt(portaTexto);
			} catch (NumberFormatException e) {
				adicionarMensagem("Porta inválida.");
				return;
			}

			socket = new Socket(ip, porta);
			adicionarMensagem("Conectado ao servidor. Aguardando início do jogo...");

			dis = new DataInputStream(socket.getInputStream());
			dos = new DataOutputStream(socket.getOutputStream());

			dos.writeUTF(nome);

			leitorServidor = new LeitorServidor(dis, this);
			Thread leitor = new Thread(leitorServidor);
			leitor.setDaemon(true);
			leitor.start();


			campoMensagem.setDisable(false);
			botaoEnviar.setDisable(false);
			botaoLigar.setDisable(true);
			campoNome.setDisable(true);

		} catch (IOException e) {
			adicionarMensagem("Erro ao conectar: " + e.getMessage());
		}
	}



	@FXML
	private void enviarMensagem() {
		if (dos == null) {
			adicionarMensagem("Erro: Não conectado ao servidor.");
			return;
		}
		String mensagem = campoMensagem.getText().trim();
		if (!mensagem.isEmpty()) {
			try {
				dos.writeUTF("MENSAGEM:" + mensagem);
				campoMensagem.clear();
			} catch (IOException e) {
				adicionarMensagem("Erro ao enviar: " + e.getMessage());
			}
		}
	}

	@FXML
	public void iniciarJogo() {
		Platform.runLater(() -> {
			try {
				FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/clienteuno/tabuleiro.fxml"));
				AnchorPane root = loader.load();

				TabuleiroController controller = loader.getController();
				controller.inicializarComDados(socket, dis, dos, campoNome.getText().trim());

				// Atualiza o controller do leitor
				leitorServidor.setController(controller);


				Stage stage = (Stage) botaoLigar.getScene().getWindow();
				stage.setTitle("UNO - Tabuleiro");
				stage.setScene(new Scene(root));
				stage.show();

			} catch (IOException e) {
				e.printStackTrace();
				adicionarMensagem("Erro ao abrir tabuleiro: " + e.getMessage());
			}
		});
	}


	public void adicionarMensagem(String mensagem) {
		Platform.runLater(() -> {
			if (areaMensagens != null) {
				areaMensagens.appendText(mensagem + "\n");
			}
		});
	}

	public void atualizarMaoJogador(List<Carta> cartas) {
		Platform.runLater(() -> {
			this.cartasJogador = new ArrayList<>(cartas);
			maoJogador.getChildren().clear();
			System.out.println("Atualizando mão do jogador: " + cartas);
			for (Carta carta : cartas) {
				try {
					ImageView cartaView = new ImageView(new Image(getClass().getResourceAsStream("/images/" + carta.getNomeImagem() + ".png")));
					cartaView.setFitWidth(80);
					cartaView.setFitHeight(120);
					cartaView.setOnMouseClicked(event -> jogarCarta(carta));
					maoJogador.getChildren().add(cartaView);
				} catch (NullPointerException e) {
					System.err.println("Erro: Imagem " + carta.getNomeImagem() + ".png não encontrada em /images/");
					adicionarMensagem("Erro: Carta " + carta.getNomeImagem() + " não encontrada.");
				}
			}
		});
	}

	public void atualizarCartaMeio(Carta carta) {
		Platform.runLater(() -> {
			this.cartaAtualMeio = carta;
			System.out.println("Atualizando carta do meio: " + carta);
			try {
				cartaMeio.setImage(new Image(getClass().getResourceAsStream("/images/" + carta.getNomeImagem() + ".png")));
			} catch (NullPointerException e) {
				System.err.println("Erro: Imagem " + carta.getNomeImagem() + ".png não encontrada em /images/");
				adicionarMensagem("Erro: Carta do meio " + carta.getNomeImagem() + " não encontrada.");
			}
		});
	}

	public void atualizarCartasOponente(int numCartas) {
		Platform.runLater(() -> {
			this.numCartasOponente = numCartas;
			contadorOponente.setText(String.valueOf(numCartas));
			maoOponenteHBox.getChildren().clear();
			System.out.println("Atualizando cartas do oponente: " + numCartas);
			try {
				for (int i = 0; i < numCartas; i++) {
					ImageView cartaView = new ImageView(new Image(getClass().getResourceAsStream("/images/cartavoltada.png")));
					cartaView.setFitWidth(80);
					cartaView.setFitHeight(120);
					maoOponenteHBox.getChildren().add(cartaView);
				}
			} catch (NullPointerException e) {
				System.err.println("Erro: cartavoltada.png não encontrado em /images/");
				adicionarMensagem("Erro: Imagem do oponente não encontrada.");
			}
		});
	}

	private void jogarCarta(Carta carta) {
		if (dos == null) {
			adicionarMensagem("Erro: Não conectado ao servidor.");
			return;
		}
		try {
			dos.writeUTF("JOGAR:" + carta.getNomeImagem());
			if (carta.getCor().equals("CORINGA")) {
				escolhaCor.setVisible(true);
				botaoConfirmarCor.setVisible(true);
			}
		} catch (IOException e) {
			adicionarMensagem("Erro ao jogar carta: " + e.getMessage());
		}
	}

	@FXML
	private void comprarCarta() {
		if (dos == null) {
			adicionarMensagem("Erro: Não conectado ao servidor.");
			return;
		}
		try {
			dos.writeUTF("COMPRAR");
		} catch (IOException e) {
			adicionarMensagem("Erro ao comprar carta: " + e.getMessage());
		}
	}

	@FXML
	private void confirmarCor() {
		if (dos == null) {
			adicionarMensagem("Erro: Não conectado ao servidor.");
			return;
		}
		String corEscolhida = escolhaCor.getValue();
		if (corEscolhida != null) {
			try {
				String cor = corEscolhida.toUpperCase().charAt(0) + "";
				dos.writeUTF("COR:" + cor);
				escolhaCor.setVisible(false);
				botaoConfirmarCor.setVisible(false);
			} catch (IOException e) {
				adicionarMensagem("Erro ao escolher cor: " + e.getMessage());
			}
		}
	}
}