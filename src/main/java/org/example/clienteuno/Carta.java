package org.example.clienteuno;

public class Carta {
	private final String nomeImagem; // Ex.: "1V", "PROIBIDOV", "TROCACOR1"
	private final int valor;        // Número (0-9), -1 (PROIBIDO), -2 (TROCA), -3 (MAIS2), -4 (TROCACOR), -5 (MAIS4)
	private final String cor;       // "V", "G", "A", "B", ou "CORINGA"

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

	public boolean podeSerJogadaSobre(Carta outra) {
		return this.cor.equals(outra.cor) || this.valor == outra.valor || this.cor.equals("CORINGA");
	}

	@Override
	public String toString() {
		return nomeImagem;
	}
}