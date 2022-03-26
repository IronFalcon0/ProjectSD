import java.net.*;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Scanner;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Server {
    private static int serverTCPPort;
    private static int serverUDPPort = 5000;
    private static int serverFilePort;
    public static String bars = "\\";
    private static String usersInfoStr;
    private static String baseDirConf = "Content_files" + bars + "conf_file";
    public static String baseDirServer = "Content_files" + bars + "Server" + bars;
    public static volatile ArrayList<ClientInfo> clientsInfo = new ArrayList<ClientInfo>();


    public static void main(String args[]) {
        loadConfigurations();
        loadUserInfo(usersInfoStr);

        // init UDP threads
        new UDPConnection(serverUDPPort);

        // receive heartbeats
        // send hearts

        // if first
        // send heartbeats -> no response
        // papel principal e deixa de mandar heartbeats

        // senão
        // continua a mandar heatbeats


        try (ServerSocket listenSocketClient = new ServerSocket(serverTCPPort)) {
            while(true) {
                // creates a thread for each client
                Socket clientSocket = listenSocketClient.accept();
                new Connection(clientSocket);
            }

        } catch (IOException e) {
            System.out.println("Server listen for clients exception: " +e.getMessage());
        }
    }

    // load vars from configuration file
    private static void loadConfigurations() {
        try {
            File configurationFile = new File(baseDirConf);
            Scanner scannerFile = new Scanner(configurationFile);

            scannerFile.nextLine();
            String line = scannerFile.nextLine();
            String[] ucDriveServer = line.split(" ");
            serverTCPPort = Integer.parseInt(ucDriveServer[1]);

            line = scannerFile.nextLine();
            String[] usersPath = line.split(" ");
            usersInfoStr = usersPath[1];

            scannerFile.close();

        } catch (FileNotFoundException e) {
            System.out.println("Error loading usersInfo: " +e.getMessage());
        }
    }

    // load user info and last visited dirs from a file
    private static void loadUserInfo(String fileName) {
        try {
            File userInfoFile = new File(fileName);
            Scanner scannerFile = new Scanner(userInfoFile);

            while (scannerFile.hasNextLine()) {
                String line = scannerFile.nextLine();
                String[] user = line.split(" ");

                ClientInfo ci;
                if (user.length == 2) {
                    ci = new ClientInfo(user[0], user[1]);
                } else {
                    ci = new ClientInfo(user[0], user[1], user[2]);
                }
                //System.out.println(line);
                clientsInfo.add(ci);


                // verify if user folder already exists. If not, creates it
                Path path = Paths.get(baseDirServer + "Home");
                if (!Files.exists(path)) {
                    try {
                        Files.createDirectories(path);
                    } catch (IOException e) {
                        System.out.println("IO: " + e.getMessage());
                    }
                }
            }

            scannerFile.close();

        } catch (FileNotFoundException e) {
            System.out.println("Error loading usersInfo: " + e.getMessage());
            e.printStackTrace();
        }

    }

    // saves userInfo in a file, runs each time a client closes a connection
    public static void saveUserInfo(String userName) {
        try {
            FileWriter userInfoFile = new FileWriter(usersInfoStr);
            for (ClientInfo c: clientsInfo) {
                userInfoFile.write(c.name + " " + c.password + " " + c.directoryS + "\n");
            }
            userInfoFile.close();
        } catch (IOException e) {
            System.out.println("Error saving usersInfo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
