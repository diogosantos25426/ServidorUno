package com.mycompany.servidoruno;

import java.io.*;
import java.net.*;
import java.util.Vector;

public class ServidorTCP implements Runnable {

    private final ServidorController controller;
    private ServerSocket serverSocket;
    public static Vector<ClientHandler> clientesAtivos = new Vector<>();
    public static int contadorClientes = 0;

    public ServidorTCP(ServidorController controller) {
        this.controller = controller;
    }

    @Override
    public void run() {
        try {
            controller.log("Servidor UNO a escutar na porta 1111...");
            serverSocket = new ServerSocket(1111);

            while (true) {
                Socket socket = serverSocket.accept();
                String nome = "Jogador" + contadorClientes;

                controller.log("Novo cliente conectado: " + nome);
                controller.adicionarJogador(nome);

                DataInputStream dis = new DataInputStream(socket.getInputStream());
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

                ClientHandler cliente = new ClientHandler(socket, nome, dis, dos, controller);
                Thread t = new Thread(cliente);

                clientesAtivos.add(cliente);
                t.start();
                contadorClientes++;

                if (clientesAtivos.size() == 2) {
                    controller.log("Dois jogadores conectados. Jogo pronto para começar.");
                }
            }

        } catch (IOException e) {
            controller.log("Erro no servidor: " + e.getMessage());
        }
    }

    static class ClientHandler implements Runnable {
        private final String nome;
        private final DataInputStream dis;
        private final DataOutputStream dos;
        private final Socket socket;
        private final ServidorController controller;
        private boolean ativo;

        public ClientHandler(Socket socket, String nome, DataInputStream dis, DataOutputStream dos, ServidorController controller) {
            this.nome = nome;
            this.dis = dis;
            this.dos = dos;
            this.socket = socket;
            this.controller = controller;
            this.ativo = true;
        }

        @Override
        public void run() {
            String recebido;
            try {
                while (ativo) {
                    recebido = dis.readUTF();
                    controller.log(nome + ": " + recebido);

                    if (recebido.equals("logout")) {
                        this.ativo = false;
                        this.socket.close();
                        controller.log(nome + " saiu.");
                        break;
                    }

                    for (ClientHandler ch : ServidorTCP.clientesAtivos) {
                        if (!ch.nome.equals(this.nome)) {
                            ch.dos.writeUTF(nome + ": " + recebido);
                        }
                    }
                }
            } catch (IOException e) {
                controller.log("Erro com o cliente " + nome + ": " + e.getMessage());
            }
        }
    }
}
