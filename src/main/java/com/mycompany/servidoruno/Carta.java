package com.mycompany.servidoruno;

public class Carta {
	private final String nomeImagem;
	private final int valor;     // Pode ser número, -1 para PROIBIDO, -2 para TROCA, etc.
	private final String cor;    // V, G, A, B, ou CORINGA

	public Carta(String nomeImagem, int valor, String cor) {
		this.nomeImagem = nomeImagem;
		this.valor = valor;
		this.cor = cor;
	}

	public String getNomeImagem() {
		return nomeImagem;
	}

	public int getValor() {
		return valor;
	}

	public String getCor() {
		return cor;
	}

	// Define se esta carta pode ser jogada sobre outra
	public boolean podeSerJogadaSobre(Carta outra) {
		return this.cor.equals(outra.cor) || this.valor == outra.valor || this.cor.equals("CORINGA");
	}

	@Override
	public String toString() {
		return nomeImagem;  // isto será suficiente para exibir no jogo
	}
}
