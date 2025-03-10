import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class DAS {
    private static final int BUFFER_SIZE = 1024;
    private final int port;
    private final int number;
    private final List<Integer> receivedNumbers;

    public DAS(int port, int number) {
        this.port = port;
        this.number = number;
        this.receivedNumbers = new ArrayList<>();
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java DAS <port> <number>");
            return;
        }

        try {
            int port = Integer.parseInt(args[0]);

            double inputNumber = Double.parseDouble(args[1]);
            int number = (int) Math.floor(inputNumber);

            DAS das = new DAS(port, number);
            das.start();
        } catch (NumberFormatException e) {
            System.err.println("Invalid arguments. Port and number must be integers.");
        }
    }

    public void start() {
        try {
            DatagramSocket socket = new DatagramSocket(port);

            System.out.println("Master mode started on port: " + port);

            runMasterMode(socket);
        } catch (SocketException e) {
            System.out.println("Port already in use. Entering slave mode...");

            runSlaveMode();
        }
    }

    private void runMasterMode(DatagramSocket socket) {
        receivedNumbers.add(number);

        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, BUFFER_SIZE);

        try {
            InetAddress localIPAddress = InetAddress.getLocalHost();

            while (true) {
                socket.receive(packet);

                if (packet.getAddress().equals(localIPAddress)) {
                    continue;
                }

                String receivedData = new String(packet.getData(), 0, packet.getLength());
                int receivedValue = Integer.parseInt(receivedData);

                if (receivedValue == 0) {
                    int average = computeAverage();
                    System.out.println("Computed average: " + average);
                    sendBroadcast(socket, average);
                } else if (receivedValue == -1) {
                    System.out.println("Received termination signal (-1). Closing socket.");
                    sendBroadcast(socket, -1);

                    socket.close();
                    break;
                } else {
                    System.out.println("Received value: " + receivedValue);

                    receivedNumbers.add(receivedValue);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void runSlaveMode() {
        try {
            DatagramSocket socket = new DatagramSocket();
            byte[] data = String.valueOf(number).getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName("localhost"), port);
            socket.send(packet);

            System.out.println("Slave mode: Sent number " + number + " to port " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int computeAverage() {
        int sum = 0;
        int count = 0;

        for (int value : receivedNumbers) {
            if (value != 0) {
                sum += value;
                count++;
            }
        }

        return count == 0 ? 0 : sum / count;
    }

    private void sendBroadcast(DatagramSocket socket, int value) {
        try {
            InetAddress broadcastAddress = calculateBroadcastAddress();

            if (broadcastAddress == null) {
                System.out.println("Failed to find broadcast address. Message not sent.");
                return;
            }

            byte[] data = String.valueOf(value).getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, broadcastAddress, port);

            socket.setBroadcast(true);
            socket.send(packet);

            System.out.println("Broadcasted value: " + value + " to " + broadcastAddress);
        } catch (IOException e) {
            System.out.println("Broadcasting failed: " + e.getMessage());
        }
    }

    private InetAddress calculateBroadcastAddress() {
        try {
            //Retrieve the local IP address
            InetAddress localIPAddress = Inet4Address.getLocalHost();

            //Get the network interface associated with the local IP
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(localIPAddress);

            //Retrieve the prefix length (subnet mask)
            short prefixLength = -1;
            for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                if (address.getAddress() instanceof Inet4Address) {
                    prefixLength = address.getNetworkPrefixLength();
                    break;
                }
            }

            if (prefixLength == -1) {
                System.out.println("Couldn't find proper prefix length");
                return null;
            }

            //Perform bitwise calculation to get the broadcast address
            return bitwiseCalculation(localIPAddress, prefixLength);
        } catch (IOException e) {
            System.out.println("Failed to calculate the broadcast address: " + e.getMessage());
        }
        return null;
    }

    private InetAddress bitwiseCalculation(InetAddress IPAddress, short prefixLength) {
        try {
            //Get byte representations of IP address and subnet mask
            byte[] IPBytes = IPAddress.getAddress();
            int subnetMask = -(1 << (32 - prefixLength));
            byte[] maskBytes = {
                    (byte) ((subnetMask >> 24) & 0xFF),
                    (byte) ((subnetMask >> 16) & 0xFF),
                    (byte) ((subnetMask >> 8) & 0xFF),
                    (byte) (subnetMask & 0xFF)
            };

            //Compute the broadcast address
            byte[] broadcastBytes = new byte[4];
            for (int i = 0; i < 4; i++) {
                broadcastBytes[i] = (byte) (IPBytes[i] | ~maskBytes[i]);
            }

            return InetAddress.getByAddress(broadcastBytes);
        } catch (UnknownHostException e) {
            System.out.println("Couldn't calculate broadcast address");
        }
        return null;
    }
}