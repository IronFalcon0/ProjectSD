/*
Departamento Eng. Informatica - FCTUC
    Sistemas Distribuídos - 2021/22
    ................................................ Rodrigo Francisco Ferreira \ nº2019220060
    ................................................ Sofia Botelho Vieira Alves \ nº2019227240
 */

import java.net.*;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Scanner;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Server {
    private static int serverTCPPort;
    private static int serverTCPPort1;
    private static int serverTCPPort2;
    protected static int serverUDPPort;
    protected static String serverHost;
    protected static int UDPFilesPortMain;
    protected static final int bufsize = 1024;

    private static String usersInfoStr;
    private static String baseDirConf = "conf_file";
    public static String baseDirServer;
    public static String baseDirServer1 = "Server" + File.separator;
    public static String baseDirServer2 = "Server2" + File.separator;
    public static volatile ArrayList<ClientInfo> clientsInfo = new ArrayList<ClientInfo>();


    public static void main(String args[]) throws InterruptedException {

        if (args.length != 1) {
            System.out.println("Wrong Syntax: java -jar ucDrive.jar *folder (0|1)*");
            System.exit(0);
        }

        // check if file was already taken from the jar
        File tempFile = new File(baseDirConf);
        if (!tempFile.exists())
            loadJarToFile(baseDirConf);

        loadConfigurations();

        tempFile = new File(usersInfoStr);
        if (!tempFile.exists())
            loadJarToFile(usersInfoStr);

        loadUserInfo(usersInfoStr);

        try {
            int folder = Integer.parseInt(args[0]);
            if (folder == 0) {
                baseDirServer = baseDirServer1;
                serverTCPPort = serverTCPPort1;

            } else {
                baseDirServer = baseDirServer2;
                serverTCPPort = serverTCPPort2;
            }

        } catch (NumberFormatException e) {
            e.printStackTrace();
            System.exit(0);
        }



        // init UDP threads
        // send heartbeats looking for main server
        Thread secondServer = new UDPHeartbeats();


        // wait to be main server to accept connections
        secondServer.join();



        try (ServerSocket listenSocketClient = new ServerSocket(serverTCPPort)) {
            while(true) {
                // creates a thread for each client
                Socket clientSocket = listenSocketClient.accept();
                new Connection(clientSocket);
            }

        } catch (IOException e) {
            System.out.println("Server listen for clients exception: " +e.getMessage());
        }
        System.exit(0);
    }

    // load vars from configuration file
    private static void loadConfigurations() {
        try {
            File configurationFile = new File(baseDirConf);
            Scanner scannerFile = new Scanner(configurationFile);

            String line = scannerFile.nextLine();
            String[] strParts = line.split(" ");
            serverHost = strParts[1];

            line = scannerFile.nextLine();
            String[] ucDriveServer = line.split(" ");
            serverTCPPort1 = Integer.parseInt(ucDriveServer[1]);

            line = scannerFile.nextLine();
            strParts = line.split(" ");
            serverTCPPort2 = Integer.parseInt(strParts[1]);

            line = scannerFile.nextLine();
            strParts = line.split(" ");
            UDPFilesPortMain = Integer.parseInt(strParts[1]);

            line = scannerFile.nextLine();
            strParts = line.split(" ");
            serverUDPPort = Integer.parseInt(strParts[1]);

            line = scannerFile.nextLine();
            String[] usersPath = line.split(" ");
            usersInfoStr = usersPath[1];

            scannerFile.close();

        } catch (FileNotFoundException e) {
            System.out.println("Error loading usersInfo: " +e.getMessage());
        }
    }

    private static void loadJarToFile(String fileName) {
        System.out.println("extracting...");
        try (InputStream in = Server.class.getResourceAsStream(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))){

            FileWriter infoFile = new FileWriter(fileName);
            String line;

            while ((line = reader.readLine()) != null) {
                infoFile.write(line + "\n");
            }

            infoFile.flush();
            infoFile.close();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // load user info and last visited dirs from a file
    private static void loadUserInfo(String fileName) {
        try {
            File userInfoFile = new File(fileName);
            Scanner scannerFile = new Scanner(userInfoFile);
            scannerFile.nextLine();

            while (scannerFile.hasNextLine()) {
                String line = scannerFile.nextLine();
                String[] user = line.split(" ");

                ClientInfo ci;
                if (user.length == 3) {
                    ci = new ClientInfo(user[0], user[1], user[2]);
                } else {
                    ci = new ClientInfo(user[0], user[1], user[2], user[3]);
                }

                clientsInfo.add(ci);


                // verify if user folder already exists. If not, creates it
                Path path = Paths.get(baseDirServer1 + File.separator + ci.folderName + File.separator + "Home");
                if (!Files.exists(path)) {
                    try {
                        Files.createDirectories(path);
                    } catch (IOException e) {
                        System.out.println("IO: " + e.getMessage());
                    }
                }

                // do the same for the secondary server
                path = Paths.get(baseDirServer2 + File.separator + ci.folderName + File.separator + "Home");
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
    public static void saveUserInfo() {
        try {
            FileWriter userInfoFile = new FileWriter(usersInfoStr);
            userInfoFile.write("Username: Password: FolderName: LastDir:\n");
            for (ClientInfo c: clientsInfo) {
                userInfoFile.write(c.name + " " + c.password + " " + c.folderName + " " + c.directoryS + "\n");
            }
            userInfoFile.close();
        } catch (IOException e) {
            System.out.println("Error saving usersInfo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
