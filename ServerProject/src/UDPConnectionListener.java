import java.net.*;
//import java.io.*;

public class UDPConnectionListener extends Thread{
    private static int serverUDPPort;

    public UDPConnectionListener (int port) {
        serverUDPPort = port;
        this.start();
    }


    // thread that listen for heartbeats
    public void run() {

        try (DatagramSocket aSocket = new DatagramSocket(serverUDPPort)) {
            System.out.println("UDP socket up");

            while(true) {

            }




        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        }
    }
}


