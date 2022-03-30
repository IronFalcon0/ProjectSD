import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

public class UDPHeartbeats extends Thread{
    private static final int maxfailedrounds = 3;
    private static final int timeout = 1000;
    private static final int period = 5000;
    private static ArrayList<String> newFiles = new ArrayList<String>();

    public UDPHeartbeats(){
        this.start();
    }


    public void run() {

        try (DatagramSocket aSocket = new DatagramSocket()) {
            System.out.println("Server took on secondary server role");
            int failedheartbeats = 0;

            while(failedheartbeats < maxfailedrounds) {
                try {
                    // convert to byte array heartbeat code (200)
                    int heartbeat = 200;
                    byte[] buf = ByteBuffer.allocate(4).putInt(heartbeat).array();

                    // send heartbeat
                    InetAddress aHost = InetAddress.getByName(Server.serverHost);
                    DatagramPacket request = new DatagramPacket(buf, buf.length, aHost, Server.serverUDPPort);
                    aSocket.send(request);

                    aSocket.setSoTimeout(timeout);

                    // waits for respond from main server with timeout
                    byte[] buffer = new byte[Server.bufsize];
                    DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                    aSocket.receive(reply);

                    DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buffer, 0, reply.getLength()));
                    newFiles = new ArrayList<>();

                    // reads newFiles received in the heartbeat by main server
                    while (dis.available() > 0) {
                        String element = dis.readUTF();
                        newFiles.add(element);

                    }

                    System.out.println("Files to sync: " + newFiles);
                    failedheartbeats = 0;

                    if (!newFiles.isEmpty()) {
                        for (String fileName: newFiles) {
                            requestFile(fileName, aSocket);
                        }
                    }


                    Thread.sleep(period);


                } catch (SocketTimeoutException ste) {
                        failedheartbeats++;
                        System.out.println("Timeout heartbeats: " + timeout +  "ms, " + failedheartbeats);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            aSocket.close();
            // turn server to main, init thread to listen to secondary servers
            new UDPConnectionListener();
            // ends current thread to allow the main process to accept connections from clients




        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        }
    }

    // request file to main server by UDP
    private void requestFile(String fileName, DatagramSocket aSocket) {

        // convert to byte array requestFile code (101)
        int heartbeat = 101;
        byte[] buf = ByteBuffer.allocate(4).putInt(heartbeat).array();

        // send code to main server
        try {
            InetAddress aHost = InetAddress.getByName(Server.serverHost);
            DatagramPacket request = new DatagramPacket(buf, buf.length, aHost, Server.serverUDPPort);
            aSocket.send(request);

        } catch (IOException e) {
            e.printStackTrace();
        }

        DatagramSocket dsoc = null;
        try {
            // send fileName
            byte[] buffer;
            dsoc = new DatagramSocket();
            buffer = fileName.getBytes(StandardCharsets.UTF_8);
            DatagramPacket dp = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(Server.serverHost), Server.UDPFilesPortMain);
            dsoc.send(dp);

            byte[] fileBuf = new byte[4];

            // receive fileSize
            dsoc.setSoTimeout(timeout);
            DatagramPacket fileSizePacket = new DatagramPacket(fileBuf, fileBuf.length);
            dsoc.receive(fileSizePacket);
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(fileBuf, 0, fileSizePacket.getLength()));
            int fileSize = dis.readInt();


            fileBuf = new byte[Server.bufsize];
            int counter = 0;
            int div = 0;
            checkDirs(Server.baseDirServer + fileName);

            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(Server.baseDirServer + fileName));

            // receive packets with the file data
            while (counter < fileSize) {
                // receive file packet
                DatagramPacket filePacket = new DatagramPacket(fileBuf, fileBuf.length);
                dsoc.receive(filePacket);


                // write new packet in file
                counter += Server.bufsize;
                if (counter > fileSize)
                    div = counter - fileSize;

                bos.write(fileBuf, 0, fileBuf.length - div);
                bos.flush();
            }

            // close fileOutputStream
            bos.close();

            // calculate checksum and send result
            byte[] fileContent = Files.readAllBytes(Paths.get(Server.baseDirServer + fileName));
            Checksum checksum = new Adler32();
            checksum.update(fileContent, 0, fileContent.length);

            byte[] buff = ByteBuffer.allocate(8).putLong(checksum.getValue()).array();
            DatagramPacket checksumPacket = new DatagramPacket(buff, buff.length, dp.getAddress(), dp.getPort());
            dsoc.send(checksumPacket);


            dsoc.close();

            System.out.println("File received from main server");

        } catch (SocketTimeoutException ste) {
            dsoc.close();
            System.out.println("File sync timeout: " + timeout + "ms");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // verify if dir exists, if not then creates it
    private void checkDirs(String fileName) {
        File pathAsFile = new File(fileName).getParentFile();

        if (!Files.exists(Paths.get(fileName))) {
            pathAsFile.mkdirs();
        }
    }

}
