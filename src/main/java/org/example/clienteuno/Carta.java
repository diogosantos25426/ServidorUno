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

	// Método atualizado para considerar a cor escolhida após um Coringa
	public boolean podeSerJogadaSobre(Carta outra, String corAtualEscolhida) {
		// Se esta carta é um coringa, sempre pode ser jogada
		if (this.cor.equals("CORINGA")) {
			return true;
		}

		// Se a carta de cima for um coringa e uma cor tiver sido escolhida
		if (outra.getCor().equals("CORINGA") && corAtualEscolhida != null) {
			return this.cor.equals(corAtualEscolhida) || this.valor == outra.valor;
		}

		// Regras normais: mesma cor ou mesmo valor
		return this.cor.equals(outra.cor) || this.valor == outra.valor;
	}


	@Override
	public String toString() {
		return nomeImagem;
	}
}