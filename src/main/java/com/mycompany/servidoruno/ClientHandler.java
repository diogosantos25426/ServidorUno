package com.mycompany.servidoruno;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

	private final String nome;
	private final DataInputStream dis;
	private final DataOutputStream dos;
	private final Socket socket;
	private final ServidorController controller; // Adicionado para logs
	private boolean ativo = true;
	private JogoUNO jogo;

	public ClientHandler(Socket socket, String nome, DataInputStream dis, DataOutputStream dos, ServidorController controller) {
		this.nome = nome;
		this.dis = dis;
		this.dos = dos;
		this.socket = socket;
		this.controller = controller; // Inicializa o controller
	}

	public String getNome() {
		return nome;
	}

	public Socket getSocket() {
		return socket;
	}

	public ServidorController getController() { // Novo getter para o controller
		return controller;
	}

	public void definirJogo(JogoUNO jogo) {
		this.jogo = jogo;
	}

	public void enviar(String mensagem) throws IOException {
		dos.writeUTF(mensagem);
		dos.flush();
		controller.log("SERVER -> " + nome + ": " + mensagem); // Log de envio
	}

	@Override
	public void run() {
		try {
			while (ativo) {
				String recebido = dis.readUTF();
				controller.log(nome + " <- CLIENT: " + recebido); // Log de recebimento

				if (recebido.equalsIgnoreCase("logout")) {
					ativo = false;
					socket.close();
					controller.log(nome + " saiu.");
					if (jogo != null) {
						// Notificar o jogo que este jogador saiu
						jogo.encerrarJogo(nome + " desconectou.");
					}
					break;
				}

				if (jogo != null) {
					jogo.processarJogada(this, recebido);
				}
			}
		} catch (IOException e) {
			controller.log("Erro de IO com o cliente " + nome + ": " + e.getMessage());
			if (ativo) { // Se o handler ainda estava ativo quando a exceção ocorreu
				ativo = false;
				if (jogo != null) {
					jogo.encerrarJogo(nome + " desconectou inesperadamente.");
				}
			}
		} finally {
			try {
				if (!socket.isClosed()) socket.close();
			} catch (IOException e) {
				controller.log("Erro ao fechar socket de " + nome + ": " + e.getMessage());
			}
		}
	}
}