import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

public class UDPConnectionListener extends Thread{
    public static ArrayList<String> newFiles = new ArrayList<String>();
    private static final int timeout = 1000;
    private static int nPackets;

    public UDPConnectionListener () {
        System.out.println("array newFiles: " + newFiles);

        // this value is calculated to estimate the max number of packets that can be sent to the secondary server without overflow of the buffer and the give some time for the secondary server to read them
        nPackets = (int) Math.floor(65536 / Server.bufsize) - 1;
        if (nPackets <= 0)
            nPackets = 1;
        this.start();
    }


    // thread that listen for heartbeats
    public void run() {

        try (DatagramSocket aSocket = new DatagramSocket(Server.serverUDPPort)) {
            System.out.println("Server took on main server role");

            while(true) {
                // receive heartbeat
                byte[] buffer = new byte[Server.bufsize];
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(request);
                DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buffer, 0, request.getLength()));
                int heartbeat = dis.readInt();

                // secondary server is waiting for a file
                if (heartbeat == 101) {
                    sendFileUDP();
                    continue;
                }
                System.out.println("heartbeat received: " + heartbeat);

                // send respond, send array with new files
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(baos);

                for (String element : newFiles) {
                    out.writeUTF(element);
                }
                byte[] bytes = baos.toByteArray();

                DatagramPacket reply = new DatagramPacket(bytes, bytes.length, request.getAddress(), request.getPort());
                aSocket.send(reply);
            }

        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        }
    }

    // sends file to secondary server by UDP
    private void sendFileUDP () {
        // ask first for filename to send
        byte[] buffer = new byte[Server.bufsize];

        DatagramSocket dsoc = null;
        try {
            dsoc = new DatagramSocket(Server.UDPFilesPortMain);
            dsoc.setSoTimeout(timeout);

            // receive package, fileName
            DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
            dsoc.receive(dp);
            String fileName = new String(dp.getData(), 0, dp.getLength(), StandardCharsets.UTF_8);

            // convert file to byte array
            File fileSend = new File(Server.baseDirServer + fileName);
            byte[] fileContent = Files.readAllBytes(fileSend.toPath());

            byte[] intBuf = ByteBuffer.allocate(4).putInt(fileContent.length).array();


            // send fileSize
            DatagramPacket fileSizePacket = new DatagramPacket(intBuf, intBuf.length, dp.getAddress(), dp.getPort());
            dsoc.send(fileSizePacket);


            int counter = 0;
            int limit;
            int sleepCount = 0;

            // send files to seconday server by blocks
            while (counter < fileContent.length) {

                if (counter + Server.bufsize > fileContent.length)
                    limit = fileContent.length;
                else
                    limit = counter + Server.bufsize;
                byte[] packetData = Arrays.copyOfRange(fileContent, counter, limit);
                DatagramPacket filePacket = new DatagramPacket(packetData, 0, packetData.length, dp.getAddress(), dp.getPort());
                dsoc.send(filePacket);

                // to avoid overwrite of the buffer
                if (sleepCount == nPackets) {
                    Thread.sleep(1);
                    sleepCount = 0;
                }

                counter += Server.bufsize;
                sleepCount += 1;

            }

            // do checksum
            Checksum checksumMain = new Adler32();
            checksumMain.update(fileContent, 0, fileContent.length);


            // receive checksum from secondary server
            byte[] checksumBuf = new byte[8];
            DatagramPacket filePacket = new DatagramPacket(checksumBuf, checksumBuf.length);
            dsoc.receive(filePacket);
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(checksumBuf, 0, filePacket.getLength()));
            long checksum = dis.readLong();

            // remove fileName from the newFiles arrayList is both checksums are equal
            if (checksum == checksumMain.getValue()) {
                newFiles.remove(fileName);
                System.out.println("File sent to backup server");

            } else {
                System.out.println("Error sending file to backup server");
            }
            dsoc.close();

        } catch (SocketTimeoutException ste) {
            dsoc.close();
            System.out.println("File sync timeout: " + timeout + "ms");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}


