import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;

public class UDPHeartbeats extends Thread{
    private static final int maxfailedrounds = 3;
    private static final int timeout = 2000;
    private static final int period = 5000;
    private static ArrayList<String> newFiles = new ArrayList<String>();

    public UDPHeartbeats(){
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
                    // convert to byte array heartbeat code (200)
                    int heartbeat = 200;
                    byte[] buf = ByteBuffer.allocate(4).putInt(heartbeat).array();

                    InetAddress aHost = InetAddress.getByName(Server.serverHost);
                    DatagramPacket request = new DatagramPacket(buf, buf.length, aHost, Server.serverUDPPort);
                    aSocket.send(request);

                    aSocket.setSoTimeout(timeout);

                    byte[] buffer = new byte[Server.bufsize];
                    DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                    aSocket.receive(reply);

                    DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buffer, 0, reply.getLength()));
                    newFiles = new ArrayList<>();

                    while (dis.available() > 0) {
                        String element = dis.readUTF();
                        newFiles.add(element);

                    }

                    System.out.println("newFiles: " + newFiles);
                    //System.out.println("Recebeu: " + respond);
                    failedheartbeats = 0;

                    if (!newFiles.isEmpty()) {
                        for (String fileName: newFiles) {
                            requestFile(fileName, aSocket);
                        }
                    }


                    Thread.sleep(period);


                } catch (SocketTimeoutException ste) {
                        failedheartbeats++;
                        System.out.println("Failed heartbeats: " + failedheartbeats);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            aSocket.close();
            System.out.println("no connection to main server");
            // turn server to main, init thread to listen to secondary servers
            new UDPConnectionListener();
            // ends current thread to allow the main process to accept connections from clients




        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        }
    }

    private void requestFile(String fileName, DatagramSocket aSocket) {
        System.out.println("request file: " + fileName);
        // convert to byte array requestFile code (101)
        int heartbeat = 101;
        byte[] buf = ByteBuffer.allocate(4).putInt(heartbeat).array();

        // send code to main server
        try {
            InetAddress aHost = InetAddress.getByName(Server.serverHost);
            DatagramPacket request = new DatagramPacket(buf, buf.length, aHost, Server.serverUDPPort);
            aSocket.send(request);
            System.out.println("code 101 sended");

        } catch (IOException e) {
            e.printStackTrace();
        }
        //....
        try {
            // send fileName
            byte buffer[];
            DatagramSocket dsoc = new DatagramSocket();
            buffer = fileName.getBytes(StandardCharsets.UTF_8);
            DatagramPacket dp = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(Server.serverHost), Server.UDPFilesPortMain);
            dsoc.send(dp);
            System.out.println("filename sent");

            byte fileBuf[] = new byte[4];

            // receive fileSize
            dsoc.setSoTimeout(timeout);
            DatagramPacket fileSizePacket = new DatagramPacket(fileBuf, fileBuf.length);
            dsoc.receive(fileSizePacket);
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(fileBuf, 0, fileSizePacket.getLength()));
            int fileSize = dis.readInt();


            fileBuf = new byte[Server.bufsize];
            int counter = 0;
            int div = 0;
            BufferedOutputStream bos = new BufferedOutputStream( new FileOutputStream(Server.baseDirServer + fileName));

            // needs to send confirmation before receiving another packet, also timeout
            while (counter < fileSize) {
                // receive file packet
                DatagramPacket filePacket = new DatagramPacket(fileBuf, fileBuf.length);
                dsoc.receive(filePacket);
                System.out.println("package receive");


                // write new packet in file
                counter += Server.bufsize;
                if (counter > fileSize)
                    div = counter - fileSize;

                System.out.println(counter - div);
                bos.write(fileBuf, 0, counter - div);
                bos.flush();

                // send confirmation to server, can receive next packet
                byte[] bufConf = ByteBuffer.allocate(4).putInt(200).array();
                DatagramPacket confirmation = new DatagramPacket(bufConf, bufConf.length, dp.getAddress(), dp.getPort());
                dsoc.send(confirmation);
                System.out.println("confirmation sended");

            }
            System.out.println("ended");

            bos.close();
            dsoc.close();

        } catch (SocketTimeoutException ste) {
            System.out.println("File sync timeout: " + timeout + "ms");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
