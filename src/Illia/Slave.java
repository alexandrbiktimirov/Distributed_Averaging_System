package Illia;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Slave implements Runnable {
    private final DatagramSocket socket;
    private final int number;
    private final int masterPort;

    Slave(DatagramSocket socket, int number, int masterPort) {
        this.socket = socket;
        this.number = number;
        this.masterPort = masterPort;
    }


    @Override
    public void run() {
        byte[] buffer = String.valueOf(number).getBytes();
        DatagramPacket packet = null;
        try {
            packet = new DatagramPacket(buffer, buffer.length, InetAddress.getLocalHost(), masterPort);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        try {
            socket.send(packet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
