package com.mycompany.servidoruno;

import java.net.*;
import java.io.*;
import java.util.*;

public class ServidorUNO {

    static Vector<ClientHandler> clientesAtivos = new Vector<>();
    static int contadorClientes = 0;

    public static void main(String[] args) {
        try {
            System.out.println("Servidor UNO a escutar na porta 1111...");
            ServerSocket serverSocket = new ServerSocket(1111);
            Socket socket;

            while (true) {
                socket = serverSocket.accept();
                System.out.println("Novo cliente conectado: " + socket);

                DataInputStream dis = new DataInputStream(socket.getInputStream());
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

                ClientHandler cliente = new ClientHandler(socket, "Jogador" + contadorClientes, dis, dos);
                Thread t = new Thread(cliente);

                clientesAtivos.add(cliente);
                t.start();

                contadorClientes++;
                if (clientesAtivos.size() == 2) {
                    System.out.println("Dois jogadores conectados. Jogo pronto para começar.");
                }
            }

        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
        }
    }

    static class ClientHandler implements Runnable {
        private final String nome;
        private final DataInputStream dis;
        private final DataOutputStream dos;
        private final Socket socket;
        private boolean ativo;

        public ClientHandler(Socket socket, String nome, DataInputStream dis, DataOutputStream dos) {
            this.nome = nome;
            this.dis = dis;
            this.dos = dos;
            this.socket = socket;
            this.ativo = true;
        }

        @Override
        public void run() {
            String recebido;
            try {
                while (ativo) {
                    recebido = dis.readUTF();
                    System.out.println("Recebido de " + nome + ": " + recebido);

                    if (recebido.equals("logout")) {
                        this.ativo = false;
                        this.socket.close();
                        System.out.println(nome + " saiu.");
                        break;
                    }

                    for (ClientHandler ch : ServidorUNO.clientesAtivos) {
                        if (!ch.nome.equals(this.nome)) {
                            ch.dos.writeUTF(nome + ": " + recebido);
                            ch.dos.flush();
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Erro com o cliente " + nome + ": " + e.getMessage());
            }
        }
    }
}
