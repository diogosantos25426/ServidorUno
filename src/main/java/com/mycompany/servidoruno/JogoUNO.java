package com.mycompany.servidoruno;

import java.io.IOException;
import java.net.Socket;
import java.util.*;

public class JogoUNO {
	private final ClientHandler jogador1;
	private final ClientHandler jogador2;
	private final Map<ClientHandler, List<Carta>> maos = new HashMap<>();
	private final Baralho baralho = new Baralho();
	private Carta cartaAtual;
	private ClientHandler jogadorAtual;
	private String corEscolhida; // Para cartas coringa
	private boolean jogoAtivo = true; // Para controlar o estado do jogo
	private boolean sentidoHorario = true; // true para A -> B, false para B -> A (para mais de 2 jogadores, mas manter para consistência)

	public JogoUNO(ClientHandler jogador1, ClientHandler jogador2) {
		this.jogador1 = jogador1;
		this.jogador2 = jogador2;
		this.jogador1.definirJogo(this);
		this.jogador2.definirJogo(this);
		jogadorAtual = jogador1; // Começa com o jogador1
		sentidoHorario = true; // Inicia no sentido horário
		logServidor("JogoUNO criado para " + jogador1.getNome() + " e " + jogador2.getNome());
	}

	public void iniciarJogo() {
		try {
			baralho.embaralhar();

			maos.put(jogador1, new ArrayList<>());
			maos.put(jogador2, new ArrayList<>());

			for (int i = 0; i < 7; i++) {
				Carta carta1 = baralho.tirarCarta();
				Carta carta2 = baralho.tirarCarta();
				if (carta1 != null) maos.get(jogador1).add(carta1);
				if (carta2 != null) maos.get(jogador2).add(carta2);
			}
			logServidor("Cartas iniciais distribuídas: " + jogador1.getNome() + " (" + maos.get(jogador1).size() + ") e " + jogador2.getNome() + " (" + maos.get(jogador2).size() + ")");


			do {
				cartaAtual = baralho.tirarCarta();
				// Garante que a primeira carta não é uma carta de ação ou coringa
			} while (cartaAtual != null && (cartaAtual.getCor().equals("CORINGA") || cartaAtual.getValor() < 0));

			broadcast("MENSAGEM:Jogo iniciado!");
			logServidor("Primeira carta do meio: " + cartaAtual.getNomeImagem());

			enviarEstado(); // Envia o estado completo para ambos
			if (jogadorAtual != null) {
				jogadorAtual.enviar("SUA_VEZ");
				logServidor("Enviado SUA_VEZ para " + jogadorAtual.getNome());
				ClientHandler outroJogador = (jogadorAtual == jogador1) ? jogador2 : jogador1;
				outroJogador.enviar("AGUARDE");
				logServidor("Enviado AGUARDE para " + outroJogador.getNome());
			}
		} catch (IOException e) {
			logServidor("Erro no servidor ao iniciar o jogo: " + e.getMessage());
			encerrarJogo("Erro no servidor ao iniciar o jogo.");
		}
	}

	public synchronized void processarJogada(ClientHandler jogador, String mensagem) {
		logServidor("Processando mensagem de " + jogador.getNome() + ": " + mensagem);

		if (!jogoAtivo) {
			try {
				jogador.enviar("MENSAGEM:Jogo encerrado.");
			} catch (IOException e) {
				System.err.println("Erro ao notificar jogador: " + e.getMessage());
			}
			return;
		}

		if (mensagem.equals("REFRESH")) {
			logServidor(jogador.getNome() + " pediu REFRESH. Enviando estado.");
			enviarEstadoParaJogador(jogador);
			return;
		}

		if (jogador != jogadorAtual) {
			try {
				jogador.enviar("MENSAGEM:Não é a tua vez!");
				logServidor("ERRO: " + jogador.getNome() + " tentou jogar fora de sua vez.");
			} catch (IOException e) {
				System.err.println("Erro ao enviar mensagem: " + e.getMessage());
			}
			return;
		}

		try {
			if (mensagem.startsWith("JOGAR:")) {
				String nomeCarta = mensagem.replace("JOGAR:", "").trim();
				List<Carta> mao = maos.get(jogador);
				Carta cartaSelecionada = null;

				for (Carta c : mao) {
					if (c.getNomeImagem().equalsIgnoreCase(nomeCarta)) {
						cartaSelecionada = c;
						break;
					}
				}

				if (cartaSelecionada == null || !podeJogarCarta(cartaSelecionada, corEscolhida)) {
					jogador.enviar("MENSAGEM:Jogada inválida. Essa carta não pode ser jogada.");
					logServidor("Jogada inválida de " + jogador.getNome() + ": " + nomeCarta + ". Carta selecionada: " + (cartaSelecionada != null ? cartaSelecionada.toString() : "null") + ". Carta atual: " + cartaAtual.toString() + ". Cor escolhida: " + (corEscolhida != null ? corEscolhida : "N/A"));
					return;
				}

				mao.remove(cartaSelecionada);
				cartaAtual = cartaSelecionada;
				logServidor(jogador.getNome() + " jogou " + cartaAtual.getNomeImagem() + ". Cartas restantes: " + mao.size());

				// Resetar corEscolhida se a carta jogada não for um Coringa
				if (!cartaAtual.getCor().equals("CORINGA")) {
					corEscolhida = null;
				}

				boolean pulouVez = false;
				boolean comprouCartas = false;

				// Processamento de cartas especiais
				if (cartaAtual.getCor().equals("CORINGA")) {
					if (cartaAtual.getValor() == -5) { // MAIS4
						ClientHandler proximo = getProximoJogadorSemAlternar();
						for (int i = 0; i < 4; i++) {
							Carta extra = baralho.tirarCarta();
							if (extra != null) maos.get(proximo).add(extra);
						}
						proximo.enviar("MENSAGEM:Recebeste 4 cartas!");
						broadcast("MENSAGEM:" + proximo.getNome() + " comprou 4 cartas.");
						logServidor(proximo.getNome() + " recebeu 4 cartas. Total: " + maos.get(proximo).size());
						pulouVez = true; // O jogador que comprou 4 cartas perde a vez
						comprouCartas = true;
					}
					corEscolhida = null; // A cor deve ser escolhida após jogar o coringa
					jogador.enviar("MENSAGEM:Escolha uma cor.");
					logServidor(jogador.getNome() + " jogou CORINGA. Esperando escolha de cor.");
					enviarEstado(); // Envia o estado atualizado para todos para mostrar a carta coringa
					return; // Retorna para aguardar a escolha da cor
				} else { // Cartas não coringa
					if (cartaAtual.getValor() == -3) { // MAIS2
						ClientHandler proximo = getProximoJogadorSemAlternar();
						for (int i = 0; i < 2; i++) {
							Carta extra = baralho.tirarCarta();
							if (extra != null) maos.get(proximo).add(extra);
						}
						proximo.enviar("MENSAGEM:Recebeste 2 cartas!");
						broadcast("MENSAGEM:" + proximo.getNome() + " comprou 2 cartas.");
						logServidor(proximo.getNome() + " recebeu 2 cartas. Total: " + maos.get(proximo).size());
						pulouVez = true; // O jogador que comprou 2 cartas perde a vez
						comprouCartas = true;
					} else if (cartaAtual.getValor() == -1) { // PROIBIDO (Skip)
						broadcast("MENSAGEM:" + jogadorAtual.getNome() + " jogou carta PROIBIDO. Pulou a vez.");
						logServidor(jogadorAtual.getNome() + " jogou PROIBIDO. Próximo jogador será pulado.");
						pulouVez = true; // Pula a vez do próximo jogador
					} else if (cartaAtual.getValor() == -2) { // TROCA (Reverse)
						broadcast("MENSAGEM:" + jogadorAtual.getNome() + " jogou carta TROCA (Muda de Sentido).");
						sentidoHorario = !sentidoHorario; // Inverte o sentido
						logServidor("Sentido do jogo invertido para " + (sentidoHorario ? "Horário" : "Anti-horário"));
						// Para 2 jogadores, uma carta de reversão age como uma carta de pular/retornar a vez ao mesmo jogador.
						// Então, o jogador atual joga novamente.
						// O `alternarJogador()` abaixo vai lidar com isso se `getProximoJogador` for bem implementado para 2P.
					}
					broadcast("MENSAGEM:" + jogador.getNome() + " jogou: " + cartaAtual.getNomeImagem());
				}

				if (mao.isEmpty()) {
					broadcast("MENSAGEM:" + jogador.getNome() + " venceu o jogo!");
					logServidor(jogador.getNome() + " venceu o jogo.");
					jogoAtivo = false;
					return;
				}

				// Determinar quem é o próximo jogador
				ClientHandler proximoJogadorNoTurno = getProximoJogador(false); // Apenas pega o próximo, não alterna ainda

				if (comprouCartas || pulouVez) {
					// Se houve compra de cartas (+2, +4) ou pulou a vez, a vez passa para o jogador pulado/que comprou
					// E este jogador perde a vez. Então a vez vai para o jogador seguinte a ele.
					// Em 2 jogadores, é o mesmo que o jogador atual jogar de novo.
					jogadorAtual = proximoJogadorNoTurno; // O que era "próximo" agora é o "atual"
					alternarJogador(); // E este "atual" agora passa a vez novamente
					logServidor("Devido a carta de ação, vez de " + jogadorAtual.getNome());
				} else {
					alternarJogador(); // Para cartas normais e Reverse (em 2p, o Reverse faz a vez voltar para o mesmo)
					logServidor("Alternando para o próximo jogador: " + jogadorAtual.getNome());
				}

				enviarEstado(); // Envia o estado atualizado para todos os jogadores
			} else if (mensagem.startsWith("COMPRAR")) {
				Carta novaCarta = baralho.tirarCarta();
				if (novaCarta != null) {
					maos.get(jogador).add(novaCarta);
					logServidor(jogador.getNome() + " comprou carta: " + novaCarta.getNomeImagem() + ". Total: " + maos.get(jogador).size());
					boolean podeJogar = podeJogarCarta(novaCarta, corEscolhida);
					if (podeJogar) {
						jogador.enviar("MENSAGEM:Você comprou uma carta jogável. Clique para jogar.");
						// Não alterna o jogador, ele tem a chance de jogar a carta comprada
					} else {
						jogador.enviar("MENSAGEM:Você comprou uma carta. Não é jogável. Sua vez terminou.");
						alternarJogador(); // Alterna a vez se a carta não for jogável
					}
					enviarEstado(); // Envia o estado após a compra
				} else {
					jogador.enviar("MENSAGEM:Baralho vazio!");
					logServidor("Baralho vazio, " + jogador.getNome() + " tentou comprar.");
				}
			} else if (mensagem.startsWith("COR:")) {
				corEscolhida = mensagem.replace("COR:", "").trim();
				if (!corEscolhida.matches("[VGAB]")) {
					jogador.enviar("MENSAGEM:Cor inválida! Escolha novamente.");
					logServidor("ERRO: " + jogador.getNome() + " escolheu cor inválida: " + corEscolhida);
					return;
				}
				broadcast("MENSAGEM:" + jogador.getNome() + " escolheu a cor " + corEscolhida);
				logServidor(jogador.getNome() + " escolheu a cor " + corEscolhida);

				// Após escolher a cor, a vez deve alternar, a menos que seja um +4 Coringa
				// Se for um +4 Coringa, a vez já foi tratada no processamento do "JOGAR" (pulouVez = true)
				// então o alternarJogador já foi chamado.
				// Para um Coringa normal (-4), a vez deve alternar após a escolha da cor.
				if (cartaAtual.getValor() == -4) { // Se for Coringa normal
					alternarJogador();
					logServidor("Após escolha de cor para Coringa, alternando para o próximo jogador: " + jogadorAtual.getNome());
				} else if (cartaAtual.getValor() == -5) { // Se for +4 Coringa
					// A vez já foi tratada, o jogador pulou a vez
					// Nada a fazer aqui, a vez já está no jogador correto.
				}
				enviarEstado(); // Envia o estado após a escolha da cor
			}
		} catch (IOException e) {
			logServidor("Erro de IO com jogador " + jogador.getNome() + " durante a jogada: " + e.getMessage());
			encerrarJogo("Jogador " + jogador.getNome() + " desconectado.");
		} catch (Exception e) {
			logServidor("Erro inesperado ao processar jogada de " + jogador.getNome() + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

	// Adicionado um método para obter o próximo jogador com base no sentido do jogo (sem alternar a variável jogadorAtual)
	private ClientHandler getProximoJogador(boolean alternarAgora) {
		ClientHandler proximo = null;
		if (sentidoHorario) {
			proximo = (jogadorAtual == jogador1) ? jogador2 : jogador1;
		} else { // Sentido anti-horário
			proximo = (jogadorAtual == jogador1) ? jogador2 : jogador1; // Em 2 jogadores, é sempre o outro
		}
		if (alternarAgora) {
			jogadorAtual = proximo;
		}
		return proximo;
	}

	// Novo método para apenas obter o próximo jogador sem alternar
	private ClientHandler getProximoJogadorSemAlternar() {
		return (jogadorAtual == jogador1) ? jogador2 : jogador1;
	}


	// Sobrecarga de podeJogarCarta para usar a corEscolhida
	private boolean podeJogarCarta(Carta carta, String corAtualEscolhida) {
		// Se a carta que está sendo jogada é um CORINGA, sempre pode ser jogada
		if (carta.getCor().equals("CORINGA")) {
			return true;
		}
		// Se há uma cor escolhida (de um coringa anterior) e a carta do meio é um coringa
		// A carta deve corresponder à cor escolhida ou ser o mesmo valor
		if (corAtualEscolhida != null && cartaAtual.getCor().equals("CORINGA")) {
			return carta.getCor().equals(corAtualEscolhida) || carta.getValor() == cartaAtual.getValor();
		}
		// Regras normais: cor ou valor da carta do meio
		return carta.getValor() == cartaAtual.getValor() || carta.getCor().equals(cartaAtual.getCor());
	}


	private void alternarJogador() {
		try {
			ClientHandler proximo = getProximoJogador(true); // Altera o jogadorAtual
			if (jogoAtivo && proximo != null) {
				proximo.enviar("SUA_VEZ");
				logServidor("Enviado SUA_VEZ para " + proximo.getNome() + " (após alternância)");
				ClientHandler outroJogador = (proximo == jogador1) ? jogador2 : jogador1;
				outroJogador.enviar("AGUARDE");
				logServidor("Enviado AGUARDE para " + outroJogador.getNome() + " (após alternância)");
			}
		} catch (IOException e) {
			logServidor("Erro ao alternar jogador: " + e.getMessage());
			encerrarJogo("Erro ao alternar jogador.");
		}
	}

	public void enviarEstado() {
		if (!jogoAtivo) return;
		logServidor("Enviando estado para todos os jogadores.");
		for (ClientHandler ch : maos.keySet()) {
			enviarEstadoParaJogador(ch);
		}
	}

	// Novo método para enviar estado para um jogador específico (usado para REFRESH)
	private void enviarEstadoParaJogador(ClientHandler ch) {
		try {
			List<Carta> minhaMao = maos.get(ch);
			ClientHandler adversario = (ch == jogador1) ? jogador2 : jogador1;
			List<Carta> maoAdversario = maos.get(adversario);

			StringBuilder cartasStr = new StringBuilder();
			for (Carta c : minhaMao) {
				cartasStr.append(c.getNomeImagem()).append(":").append(c.getValor()).append(":").append(c.getCor()).append(";");
			}
			if (cartasStr.length() > 0) cartasStr.deleteCharAt(cartasStr.length() - 1);

			ch.enviar("CARTAS:" + cartasStr);
			logServidor("Enviado CARTAS para " + ch.getNome() + ": " + minhaMao.size() + " cartas.");
			// Inclui a cor escolhida na mensagem MEIO, se houver
			ch.enviar("MEIO:" + (cartaAtual != null ? cartaAtual.getNomeImagem() + ":" + cartaAtual.getValor() + ":" + (corEscolhida != null ? corEscolhida : cartaAtual.getCor()) : ""));
			logServidor("Enviado MEIO para " + ch.getNome() + ": " + (cartaAtual != null ? cartaAtual.getNomeImagem() : "N/A") + ". Cor escolhida: " + (corEscolhida != null ? corEscolhida : "N/A"));
			ch.enviar("OPONENTE:" + maoAdversario.size());
			logServidor("Enviado OPONENTE para " + ch.getNome() + ": " + maoAdversario.size() + " cartas.");

			// Enviar a vez atual para o jogador
			if (ch == jogadorAtual) {
				ch.enviar("SUA_VEZ");
				logServidor("Enviado SUA_VEZ para " + ch.getNome());
			} else {
				ch.enviar("AGUARDE");
				logServidor("Enviado AGUARDE para " + ch.getNome());
			}
		} catch (IOException e) {
			logServidor("ERRO CRÍTICO AO ENVIAR ESTADO PARA " + ch.getNome() + ": " + e.getMessage());
			// Tente notificar o controlador da UI do servidor, se houver
			if (jogador1.getController() != null) { // Assumindo que o controller é passado no ClientHandler
				jogador1.getController().log("ERRO DE IO COM " + ch.getNome() + ": " + e.getMessage());
			}
			encerrarJogo("Conexão perdida com " + ch.getNome()); // Pode ser agressivo, mas sinaliza a falha
		}
	}

	public void broadcast(String mensagem) {
		if (!jogoAtivo) return;
		logServidor("BROADCAST: " + mensagem);
		try {
			jogador1.enviar(mensagem);
		} catch (IOException e) {
			logServidor("Erro ao enviar broadcast para " + jogador1.getNome() + ": " + e.getMessage());
			encerrarJogo("Jogador " + jogador1.getNome() + " desconectado.");
		}
		try {
			jogador2.enviar(mensagem);
		} catch (IOException e) {
			logServidor("Erro ao enviar broadcast para " + jogador2.getNome() + ": " + e.getMessage());
			encerrarJogo("Jogador " + jogador2.getNome() + " desconectado.");
		}
	}

	public void encerrarJogo(String motivo) {
		if (!jogoAtivo) return;
		jogoAtivo = false;
		logServidor("ENCERRANDO JOGO: " + motivo);

		String mensagem = "MENSAGEM:Jogo encerrado: " + motivo;
		try {
			jogador1.enviar(mensagem);
		} catch (IOException e) {
			System.err.println("Erro ao notificar " + jogador1.getNome() + ": " + e.getMessage());
		}
		try {
			jogador2.enviar(mensagem);
		} catch (IOException e) {
			System.err.println("Erro ao notificar " + jogador2.getNome() + ": " + e.getMessage());
		}

		// Fechar sockets
		try {
			if (jogador1.getSocket() != null && !jogador1.getSocket().isClosed()) {
				jogador1.getSocket().close();
			}
			if (jogador2.getSocket() != null && !jogador2.getSocket().isClosed()) {
				jogador2.getSocket().close();
			}
		} catch (IOException e) {
			logServidor("Erro ao fechar sockets: " + e.getMessage());
		}

		System.out.println("Jogo encerrado: " + motivo);
	}

	// Adicione este método para ter acesso ao logger do ServidorController
	private void logServidor(String mensagem) {
		if (jogador1.getController() != null) { // Assumindo que o controller é passado no ClientHandler
			jogador1.getController().log("JogoUNO: " + mensagem);
		} else {
			System.out.println("JogoUNO: " + mensagem); // Fallback se não houver controller
		}
	}
}