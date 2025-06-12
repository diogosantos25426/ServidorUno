package org.example.clienteuno;

import java.util.List;

public interface ControladorDeJogo {
	void atualizarMaoJogador(List<Carta> cartas);
	void atualizarCartaMeio(Carta carta);
	void atualizarCartasOponente(int numCartas);
	void adicionarMensagem(String msg);
}
