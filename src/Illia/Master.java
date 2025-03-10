package Illia;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Master implements Runnable {
    private static final int BUFFER_SIZE = 256;

    private final List<Integer> numbers = new ArrayList<>();
    private final byte[] buffer = new byte[BUFFER_SIZE];
    private final DatagramSocket socket;

    Master(DatagramSocket socket, int number) {
        this.socket = socket;
        numbers.add(number);
    }

    private void broadcast(int number) throws IOException {
        byte[] buffer = String.valueOf(number).getBytes();

        socket.setBroadcast(true);

//        Searches for not null interface broadcast address in all network interfaces
//        And send after it finds it
        Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
                .flatMap(inet -> inet.getInterfaceAddresses().stream())
                .map(InterfaceAddress::getBroadcast)
                .filter(Objects::nonNull)
                .limit(1)
                .map(address -> new DatagramPacket(
                        buffer,
                        buffer.length,
                        address,
                        socket.getLocalPort()
                ))
                .forEach(packet -> {
                    try {
                        socket.send(packet);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Override
    public void run() {
        System.out.println("Master started");

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            try {
                socket.receive(packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


            int num;
            try {
                num = Integer.parseInt(new String(packet.getData(), 0, packet.getLength()));

            } catch (NumberFormatException e) {
                System.out.println("Malformed packet");
                break;
            }

            if (num == -1) {
                System.out.println("Closing Socket");
                try {
                    broadcast(num);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                break;
            } else if (num == 0) {
                int sum = numbers.stream().filter(n -> n != 0).reduce(0, Integer::sum);
                int count = (int) numbers.stream().filter(n -> n != 0).count();

                System.out.println("AVG: " + sum / count);
                try {
                    broadcast(sum / count);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {

                numbers.add(num);
                System.out.println("Num: " + num);
            }
        }
    }
}