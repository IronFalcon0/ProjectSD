import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.Enumeration;

public class UDPHeartbeats extends Thread{
    private static int ownUDPPort;
    private static int otherUDPPort;
    private static String serverHost;
    private static final int maxfailedrounds = 3;
    private static final int timeout = 3000;
    private static final int bufsize = 4096;
    private static final int period = 2000;

    public UDPHeartbeats(String ip, int ownPort, int otherPort){
        ownUDPPort = ownPort;
        otherUDPPort = otherPort;
        serverHost = ip;
        this.start();
    }


    public void run() {
        //int countHeartBeats = 0;

        try (DatagramSocket aSocket = new DatagramSocket()) {
            //aSocket.setSoTimeout(timeout);
            System.out.println("sending heartbeats");
            int failedheartbeats = 0;

            while(failedheartbeats < maxfailedrounds) {
                try {

                    sendHeartbeat(aSocket, otherUDPPort);

                    System.out.println("Port used: " + otherUDPPort);

                    if (receiveRespondHB(aSocket)) {
                        failedheartbeats = 0;
                    } else {
                        failedheartbeats++;
                        System.out.println("Failed heartbeats: " + failedheartbeats);
                    }


                    Thread.sleep(period);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
            aSocket.close();
            System.out.println("no connection to main server");
            // turn server to main, init thread to listen to secondary servers
            new UDPConnectionListener(ownUDPPort);
            // ends current thread to allow the main process to accept connections from clients




        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        }
    }

    private boolean sendHeartbeat(DatagramSocket aSocket, int port) throws UnknownHostException {
        try {
            int heartbeat = 200;
            byte[] buf = ByteBuffer.allocate(4).putInt(heartbeat).array();

            InetAddress aHost = InetAddress.getByName(serverHost);
            DatagramPacket request = new DatagramPacket(buf, buf.length, aHost, port);
            aSocket.send(request);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean receiveRespondHB(DatagramSocket aSocket) {
        try {
            aSocket.setSoTimeout(timeout);

            byte[] buffer = new byte[bufsize];
            DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
            aSocket.receive(reply);

            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buffer, 0, reply.getLength()));
            int respond = dis.readInt();
            System.out.println("Recebeu: " + respond);

            if (respond == 200) {
                return true;
            }
            return false;

        } catch (SocketTimeoutException ste) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

}
