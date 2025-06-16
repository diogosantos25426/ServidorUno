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
import java.util.ArrayList;
import java.util.List;

public class TabuleiroController implements ControladorDeJogo {

	@FXML private HBox maoJogador;
	@FXML private HBox maoOponente;
	@FXML private ImageView cartaMeio;
	@FXML private TextArea areaMensagens;
	@FXML private Button botaoComprar;
	@FXML private Button botaoSincronizar;
	@FXML private Label contadorOponente;
	@FXML private HBox boxCores;

	private Socket socket;
	private DataInputStream dis;
	private DataOutputStream dos;
	private String nomeJogador;
	private boolean aguardarEscolhaCor = false;
	private boolean minhaVez = false;
	private boolean comprouCarta = false;
	private Carta cartaAtualMeio;
	private List<Carta> cartasJogador = new ArrayList<>();
	private String corEscolhida;
	private LeitorServidor leitorServidor;

	public void inicializarComDados(Socket socket, DataInputStream dis, DataOutputStream dos, String nomeJogador, LeitorServidor leitorServidor) {
		this.socket = socket;
		this.dis = dis;
		this.dos = dos;
		this.nomeJogador = nomeJogador;
		this.leitorServidor = leitorServidor;
		System.out.println("DEBUG TABULEIRO CTRL: TabuleiroController inicializado com dados. Jogador: " + nomeJogador);

		boxCores.setVisible(true); // Se quiser que seja visível
		desativarEscolhaCor(); // Mas desativado por padrão

		// O LeitorServidor já estará a correr e direcionado para este controlador
		// Não precisa iniciar uma nova thread aqui.
	}

	public void atualizarMaoJogador(List<Carta> cartas) {
		Platform.runLater(() -> {
			this.cartasJogador = new ArrayList<>(cartas);
			maoJogador.getChildren().clear();
			System.out.println("DEBUG TABULEIRO CTRL: Atualizando mão do jogador. Cartas: " + cartas.size());
			for (Carta carta : cartas) {
				try {
					ImageView cartaView = new ImageView(new Image(getClass().getResourceAsStream("/images/" + carta.getNomeImagem() + ".png")));
					cartaView.setFitWidth(80);
					cartaView.setFitHeight(120);
					cartaView.setOnMouseClicked(event -> jogarCarta(carta));
					maoJogador.getChildren().add(cartaView);
				} catch (NullPointerException e) {
					System.err.println("Erro: Imagem " + carta.getNomeImagem() + ".png não encontrada em /images/. " + e.getMessage());
					adicionarMensagem("Erro: Carta " + carta.getNomeImagem() + " não encontrada.");
				}
			}
		});
	}

	public void atualizarCartaMeio(Carta carta) {
		Platform.runLater(() -> {
			this.cartaAtualMeio = carta;
			System.out.println("DEBUG TABULEIRO CTRL: Atualizando carta do meio para: " + carta.getNomeImagem() + " (Cor: " + carta.getCor() + ")");
			if (carta.getNomeImagem() != null && !carta.getNomeImagem().isEmpty()) {
				try {
					Image img = new Image(getClass().getResourceAsStream("/images/" + carta.getNomeImagem() + ".png"));
					cartaMeio.setImage(img);
				} catch (NullPointerException e) {
					System.err.println("Erro: Imagem " + carta.getNomeImagem() + ".png não encontrada em /images/. " + e.getMessage());
					adicionarMensagem("Erro: Carta do meio " + carta.getNomeImagem() + " não encontrada.");
				}
			}
		});
	}

	public void atualizarCartasOponente(int numCartas) {
		Platform.runLater(() -> {
			maoOponente.getChildren().clear();
			contadorOponente.setText(String.valueOf(numCartas));
			System.out.println("DEBUG TABULEIRO CTRL: Atualizando cartas do oponente: " + numCartas);
			try {
				for (int i = 0; i < numCartas; i++) {
					ImageView cartaView = new ImageView(new Image(getClass().getResourceAsStream("/images/cartavoltada.png")));
					cartaView.setFitWidth(80);
					cartaView.setFitHeight(120);
					maoOponente.getChildren().add(cartaView);
				}
			} catch (NullPointerException e) {
				System.err.println("Erro: cartavoltada.png não encontrado em /images/. " + e.getMessage());
				adicionarMensagem("Erro: Imagem do oponente não encontrada.");
			}
		});
	}

	public void adicionarMensagem(String msg) {
		Platform.runLater(() -> {
			areaMensagens.appendText(msg + "\n");
			System.out.println("DEBUG TABULEIRO CTRL: Mensagem adicionada: " + msg);
			if (msg.contains("Escolha uma cor")) {
				aguardarEscolhaCor = true;
				ativarEscolhaCor();
				System.out.println("DEBUG TABULEIRO CTRL: Ativando escolha de cor.");
			}
		});
	}

	private void jogarCarta(Carta carta) {
		System.out.println("DEBUG TABULEIRO CTRL: Tentativa de jogar carta: " + carta.getNomeImagem() + ". Minha vez: " + minhaVez + ", Comprou carta: " + comprouCarta);
		if (dos == null) {
			adicionarMensagem("Erro: Não conectado ao servidor.");
			return;
		}
		if (leitorServidor.isBloqueioTemporario()) { // Agora sempre false
			adicionarMensagem("Aguarda a atualização do estado do jogo.");
			return;
		}
		if (!minhaVez) {
			adicionarMensagem("Não é a tua vez! Tenta sincronizar o estado.");
			System.out.println("DEBUG TABULEIRO CTRL: Jogada negada. Não é a minha vez.");
			return;
		}
		if (aguardarEscolhaCor) {
			adicionarMensagem("Tens de escolher uma cor antes de jogar.");
			System.out.println("DEBUG TABULEIRO CTRL: Jogada negada. Escolha de cor pendente.");
			return;
		}
		if (cartaAtualMeio == null) {
			adicionarMensagem("Erro: Carta do meio não definida. Tenta sincronizar.");
			System.err.println("DEBUG TABULEIRO CTRL: Jogada negada. Carta do meio é null.");
			return;
		}
		if (!cartasJogador.contains(carta)) {
			adicionarMensagem("Erro: Carta não está na tua mão.");
			System.err.println("DEBUG TABULEIRO CTRL: Jogada negada. Carta " + carta.getNomeImagem() + " não encontrada na mão.");
			return;
		}
		if (!carta.podeSerJogadaSobre(cartaAtualMeio, corEscolhida)) { // Modified call
			adicionarMensagem("Essa carta não pode ser jogada.");
			System.out.println("DEBUG TABULEIRO CTRL: Jogada negada. Carta " + carta.getNomeImagem() + " não pode ser jogada sobre " + cartaAtualMeio.getNomeImagem() + " (Cor escolhida: " + (corEscolhida != null ? corEscolhida : "N/A") + ")");
			return;
		}
		try {
			dos.writeUTF("JOGAR:" + carta.getNomeImagem());
			System.out.println("DEBUG TABULEIRO CTRL: Enviado JOGAR:" + carta.getNomeImagem());
			if (carta.getCor().equals("CORINGA")) { // Se a carta jogada for CORINGA
				aguardarEscolhaCor = true;
				ativarEscolhaCor();
				System.out.println("DEBUG TABULEIRO CTRL: Coringa jogado. Ativando escolha de cor.");
			}
			comprouCarta = false;
			dos.writeUTF("REFRESH"); // Added refresh after playing
			System.out.println("DEBUG TABULEIRO CTRL: Enviado REFRESH após jogar carta.");
		} catch (IOException e) {
			adicionarMensagem("Erro ao jogar carta: " + e.getMessage());
			System.err.println("DEBUG TABULEIRO CTRL: Erro de IO ao jogar carta: " + e.getMessage());
		}
	}

	@FXML
	private void comprarCarta() {
		System.out.println("DEBUG TABULEIRO CTRL: Tentativa de comprar carta. Minha vez: " + minhaVez + ", Comprou carta: " + comprouCarta);
		if (dos == null) {
			adicionarMensagem("Erro: Não conectado ao servidor.");
			return;
		}
		if (leitorServidor.isBloqueioTemporario()) { // Agora sempre false
			adicionarMensagem("Aguarda a atualização do estado do jogo.");
			return;
		}
		if (!minhaVez) {
			adicionarMensagem("Não é a tua vez! Tenta sincronizar o estado.");
			System.out.println("DEBUG TABULEIRO CTRL: Compra negada. Não é a minha vez.");
			return;
		}
		if (aguardarEscolhaCor) {
			adicionarMensagem("Tens de escolher uma cor antes de comprar.");
			System.out.println("DEBUG TABULEIRO CTRL: Compra negada. Escolha de cor pendente.");
			return;
		}
		if (comprouCarta) {
			adicionarMensagem("Já compraste uma carta nesta rodada!");
			System.out.println("DEBUG TABULEIRO CTRL: Compra negada. Já comprei nesta rodada.");
			return;
		}
		try {
			dos.writeUTF("COMPRAR");
			System.out.println("DEBUG TABULEIRO CTRL: Enviado COMPRAR.");
			comprouCarta = true;
			dos.writeUTF("REFRESH"); // Added refresh after buying
			System.out.println("DEBUG TABULEIRO CTRL: Enviado REFRESH após comprar carta.");
		} catch (IOException e) {
			adicionarMensagem("Erro ao comprar carta: " + e.getMessage());
			System.err.println("DEBUG TABULEIRO CTRL: Erro de IO ao comprar carta: " + e.getMessage());
		}
	}

	@FXML
	private void refreshEstado() {
		try {
			dos.writeUTF("REFRESH");
			adicionarMensagem("A atualizar estado...");
			System.out.println("DEBUG TABULEIRO CTRL: Enviado REFRESH manual.");
		} catch (IOException e) {
			adicionarMensagem("Erro ao pedir sincronização: " + e.getMessage());
			System.err.println("DEBUG TABULEIRO CTRL: Erro de IO ao pedir sincronização: " + e.getMessage());
		}
	}

	@FXML private void escolherVermelho() { enviarCor("V"); }
	@FXML private void escolherVerde()    { enviarCor("G"); }
	@FXML private void escolherAmarelo()  { enviarCor("A"); }
	@FXML private void escolherAzul()     { enviarCor("B"); }

	private void enviarCor(String cor) {
		System.out.println("DEBUG TABULEIRO CTRL: Tentativa de enviar cor: " + cor);
		if (!cor.matches("[VGAB]")) {
			adicionarMensagem("Cor inválida! Escolhe Vermelho, Verde, Amarelo ou Azul.");
			System.out.println("DEBUG TABULEIRO CTRL: Escolha de cor negada. Cor inválida: " + cor);
			return;
		}
		try {
			dos.writeUTF("COR:" + cor);
			System.out.println("DEBUG TABULEIRO CTRL: Enviado COR:" + cor);
			corEscolhida = cor;
			adicionarMensagem("Cor escolhida: " + cor);
			aguardarEscolhaCor = false;
			desativarEscolhaCor();
			System.out.println("DEBUG TABULEIRO CTRL: Cor " + cor + " confirmada. Desativando escolha de cor.");
			dos.writeUTF("REFRESH"); // Added refresh after sending color
			System.out.println("DEBUG TABULEIRO CTRL: Enviado REFRESH após enviar cor.");
		} catch (IOException e) {
			adicionarMensagem("Erro ao enviar cor: " + e.getMessage());
			System.err.println("DEBUG TABULEIRO CTRL: Erro de IO ao enviar cor: " + e.getMessage());
		}
	}

	private void ativarEscolhaCor() {
		Platform.runLater(() -> {
			boxCores.setDisable(false);
		});
	}

	private void desativarEscolhaCor() {
		Platform.runLater(() -> {
			boxCores.setDisable(true);
		});
	}

	public void setMinhaVez(boolean minhaVez) {
		this.minhaVez = minhaVez;
		this.comprouCarta = false; // Resetar o flag de compra quando a vez muda
		Platform.runLater(() -> {
			adicionarMensagem(minhaVez ? "É a tua vez!" : "Aguarda a tua vez.");
			System.out.println("DEBUG TABULEIRO CTRL: setMinhaVez para: " + minhaVez + ". comprouCarta resetado.");
		});
	}

	public void setCorEscolhida(String cor) {
		this.corEscolhida = cor;
		System.out.println("DEBUG TABULEIRO CTRL: Cor escolhida definida para: " + cor);
	}
}