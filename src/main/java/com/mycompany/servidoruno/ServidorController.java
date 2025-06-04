package com.mycompany.servidoruno;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

public class ServidorController {

    @FXML
    private ListView<String> listaJogadores;

    @FXML
    private TextArea logServidor;

    @FXML
    private Button botaoIniciar;

    private boolean servidorAtivo = false;

    @FXML
    private void iniciarJogo() {
        if (!servidorAtivo) {
            servidorAtivo = true;
            botaoIniciar.setDisable(true);

            Thread servidorThread = new Thread(new ServidorTCP(this));
            servidorThread.setDaemon(true);
            servidorThread.start();

            log("Servidor iniciado.");
        } else {
            log("Servidor já está a correr.");
        }
    }

    public void log(String mensagem) {
        Platform.runLater(() -> logServidor.appendText(mensagem + "\n"));
    }

    public void adicionarJogador(String nome) {
        Platform.runLater(() -> listaJogadores.getItems().add(nome));
    }
}
