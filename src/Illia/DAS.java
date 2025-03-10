package Illia;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


public class DAS {
    public static void main(String[] args) throws SocketException {
        if (args.length < 2) {
            System.out.println("Usage: java DAS.java <port> <number>");

            System.exit(1);
            return;
        }

        int port = 0;
        int number = 0;

        try {
            port = Integer.parseInt(args[0]);
            number = Integer.parseInt(args[1]);

        } catch (Exception e) {
            System.out.println("There was an parsing error");

            System.out.println(e.getMessage());

            System.exit(1);
            return;
        }


        try(DatagramSocket socket = new DatagramSocket(port)) {
            new Master(socket, number).run();
        } catch (SocketException exception) {
            try (DatagramSocket socket = new DatagramSocket()) {
                new Slave(socket, number, port).run();
            }
        }
    }
}

