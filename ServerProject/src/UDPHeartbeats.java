import java.net.*;
import java.io.*;

public class UDPHeartbeats extends Thread{
    private static int serverUDPPort;
    private static final int maxfailedrounds = 5;
    private static final int timeout = 5000;
    private static final int bufsize = 4096;
    private static final int period = 10000;

    public UDPHeartbeats(int port){
        serverUDPPort = port;
        this.start();
    }


    public void run() {
        //int countHeartBeats = 0;

        InetAddress ia = InetAddress.getByName("localhost");
        try (DatagramSocket aSocket = new DatagramSocket(serverUDPPort)) {
            aSocket.setSoTimeout(timeout);
            System.out.println("UDP socket up");
            int failedHeartBeats = 0;

            while(failedHeartBeats < maxfailedrounds) {

            }




        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        }
    }

}
