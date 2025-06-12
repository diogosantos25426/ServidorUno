package org.example.clienteuno;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class LeitorServidor implements Runnable {
	private final DataInputStream dis;
	private ControladorDeJogo controller;
	private final Queue<String> mensagensPendentes = new LinkedList<>();

	public LeitorServidor(DataInputStream dis, ControladorDeJogo controller) {
		this.dis = dis;
		this.controller = controller;
	}

	public void setController(ControladorDeJogo novoController) {
		this.controller = novoController;
		while (!mensagensPendentes.isEmpty()) {
			processarMensagem(mensagensPendentes.poll());
		}
	}

	@Override
	public void run() {
		try {
			while (true) {
				String mensagem = dis.readUTF();
				System.out.println("Mensagem recebida do servidor: " + mensagem);
				if (controller == null || (controller instanceof ClienteController)) {
					if (mensagem.equals("INICIAR_JOGO") && controller instanceof ClienteController cliente) {
						cliente.iniciarJogo(); // transição de janela
					} else {
						mensagensPendentes.add(mensagem);
					}
				} else {
					processarMensagem(mensagem);
				}
			}
		} catch (IOException e) {
			if (controller != null) controller.adicionarMensagem("Conexão perdida: " + e.getMessage());
		}
	}

	private void processarMensagem(String mensagem) {
		try {
			if (mensagem.startsWith("CARTAS:")) {
				String[] partes = mensagem.replace("CARTAS:", "").split(",");
				List<Carta> cartas = new ArrayList<>();
				for (String parte : partes) {
					if (parte.trim().isEmpty()) continue;
					String cor = parte.contains("CORINGA") ? "CORINGA" : parte.replaceAll("[^VGAB]", "");
					int valor = parte.matches("\\d+[VGAB]") ? Integer.parseInt(parte.replaceAll("[^0-9]", "")) :
							parte.contains("PROIBIDO") ? -1 :
									parte.contains("TROCA") ? -2 :
											parte.contains("MAIS2") ? -3 :
													parte.contains("TROCACOR") ? -4 : -5;
					cartas.add(new Carta(parte.trim(), valor, cor));
				}
				controller.atualizarMaoJogador(cartas);
			} else if (mensagem.startsWith("MEIO:")) {
				String nomeCarta = mensagem.replace("MEIO:", "").trim();
				if (nomeCarta.isEmpty()) {
					controller.adicionarMensagem("Erro: Carta do meio inválida.");
					return;
				}
				String cor = nomeCarta.contains("CORINGA") ? "CORINGA" : nomeCarta.replaceAll("[^VGAB]", "");
				int valor = nomeCarta.matches("\\d+[VGAB]") ? Integer.parseInt(nomeCarta.replaceAll("[^0-9]", "")) :
						nomeCarta.contains("PROIBIDO") ? -1 :
								nomeCarta.contains("TROCA") ? -2 :
										nomeCarta.contains("MAIS2") ? -3 :
												nomeCarta.contains("TROCACOR") ? -4 : -5;
				controller.atualizarCartaMeio(new Carta(nomeCarta, valor, cor));
			} else if (mensagem.startsWith("OPONENTE:")) {
				int numCartas = Integer.parseInt(mensagem.replace("OPONENTE:", "").trim());
				controller.atualizarCartasOponente(numCartas);
			} else if (mensagem.startsWith("MENSAGEM:")) {
				controller.adicionarMensagem(mensagem.replace("MENSAGEM:", ""));
			} else {
				System.out.println("Mensagem desconhecida: " + mensagem);
				controller.adicionarMensagem(mensagem);
			}
		} catch (Exception e) {
			controller.adicionarMensagem("Erro ao processar mensagem: " + e.getMessage());
		}
	}
}
