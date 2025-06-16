package com.mycompany.servidoruno;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Baralho {

	private final List<Carta> cartas;

	public Baralho() {
		cartas = new ArrayList<>();
		// Inicializa o baralho: exemplo para 0 a 9 cores e cartas especiais
		String[] cores = {"V", "G", "A", "B"}; // Vermelho, Verde, Amarelo, Azul
		// Números de 0 a 9
		for (String cor : cores) {
			for (int i = 0; i <= 9; i++) {
				cartas.add(new Carta(i + cor, i, cor));
			}
			// Cartas especiais, exemplo:
			cartas.add(new Carta("PROIBIDO" + cor, -1, cor));
			cartas.add(new Carta("TROCA" + cor, -2, cor));
			cartas.add(new Carta("MAIS2" + cor, -3, cor));
		}
		// Cartas coringa (sem cor)
		cartas.add(new Carta("TROCACOR1", -4, "CORINGA"));
		cartas.add(new Carta("TROCACOR2", -4, "CORINGA"));
		cartas.add(new Carta("TROCACOR3", -4, "CORINGA"));
		cartas.add(new Carta("TROCACOR4", -4, "CORINGA"));
		cartas.add(new Carta("MAIS41", -5, "CORINGA"));
		cartas.add(new Carta("MAIS42", -5, "CORINGA"));
		cartas.add(new Carta("MAIS43", -5, "CORINGA"));
		cartas.add(new Carta("MAIS44", -5, "CORINGA"));
		// etc...
	}

	public void embaralhar() {
		Collections.shuffle(cartas);
	}

	public List<Carta> distribuir(int quantidade) {
		List<Carta> mao = new ArrayList<>();
		for (int i = 0; i < quantidade; i++) {
			if (!cartas.isEmpty()) {
				mao.add(cartas.remove(0));
			}
		}
		return mao;
	}

	public boolean estaVazio() {
		return cartas.isEmpty();
	}



	public Carta tirarCarta() {
		if (!cartas.isEmpty()) {
			return cartas.remove(0);
		}
		return null;
	}
}
