package com.example.sae3021;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class DataHandler {
    private final DatagramSocket socket;
    private final InetAddress serverAddr;
    private final int serverPort;
    private static final int BUFFER_SIZE = 1024;
    private static final String SERVER_IP = "192.168.27.66"; // IP fixe du serveur
    private static final int SERVER_PORT = 6010;

    public DataHandler() throws IOException {
        this.serverAddr = InetAddress.getByName(SERVER_IP);
        this.serverPort = SERVER_PORT;
        this.socket = new DatagramSocket();
        this.socket.setSoTimeout(5000); // Timeout de 5 secondes
    }

    /**
     * Envoie un message au serveur
     */
    public void sendMessage(String message) throws IOException {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length, serverAddr, serverPort);
        socket.send(packet);
        
        System.out.println("[LOG] Message envoyé: " + message);
    }

    /**
     * Reçoit un message du serveur
     */
    public String receiveMessage() throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        
        String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
        System.out.println("[LOG] Message reçu: " + message);
        
        return message;
    }

    public String sendAndReceive(String message) throws IOException {
        sendMessage(message);
        return receiveMessage();
    }

    /**
     * Parse un message reçu
     */
    public String[] parseMessage(String message) {
        return message.split(",");
    }

    /**
     * Ferme la connexion
     */
    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}