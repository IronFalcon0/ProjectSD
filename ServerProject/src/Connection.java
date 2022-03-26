import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// thread created for each client connected
class Connection extends Thread {
    private DataInputStream in;
    private DataOutputStream out;
    private Socket clientSocket;
    private ClientInfo ci;
    private static final int BLOCK_SIZE_FILE = 8192;

    public Connection (Socket aClientSocKet) {
        try {
            clientSocket = aClientSocKet;
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());
            this.start();

        } catch (IOException e) {
            System.out.println("Connection exception: " + e.getMessage());
        }
    }

    // verify user info
    private ClientInfo Login () throws IOException {
        try {
            out.writeUTF("login");

            String username = in.readUTF();

            out.writeUTF(username);

            String password = in.readUTF();

            if (username.isEmpty() || password.isEmpty()) {
                return null;
            }

            for (ClientInfo c : Server.clientsInfo) {
                if (c.name.equals(username) && c.password.equals(password))
                    return c;
            }
        }
        catch (EOFException e) {
            System.out.println("EOF:" + e.getMessage());
        }
        catch(IOException e){
            System.out.println("IO:" + e.getMessage());
        }
        return null;
    }

    public void run() {
        try {
            while(true) {
                String command = in.readUTF();

                if (command.equals("")) {
                    continue;
                }
                // trim command
                command = command.strip();

                String[] commandParts = command.split(":", 3);

                switch (commandParts[0]) {
                    case "LOGIN":
                        ci = Login();

                        if (ci == null) {
                            out.writeUTF("NOT_VALID");
                            System.out.println("Client tried to connect")   ;

                        } else {
                            System.out.println("Client connected");
                            out.writeUTF(ci.directoryS);
                        }
                        continue;

                    case "CLOSE_CONNECTION":                                                 // update file users_info and closes connection
                        Server.saveUserInfo(ci.name);
                        in.close();
                        out.close();
                        clientSocket.close();
                        return;

                    case "cp":
                        String newPassword = in.readUTF();
                        changePassword(newPassword);
                        Server.saveUserInfo("");
                        break;

                    case "ls":
                        if (commandParts[1].equals("server")) {
                            out.writeUTF("server:" + ci.directoryS + "> " + listFilesCurDir());
                        }
                        break;

                    case "cd":
                        if (commandParts[1].equals("server")) {
                            String resp = changeCurDir(command);
                            out.writeUTF(resp);
                        }
                        break;

                    case "GET":                                                                         // client get file from server current dir
                        String fileName = Server.baseDirServer + ci.directoryS + Server.bars + command.substring(command.indexOf(":") + 1, command.length());
                        sendFile(fileName);

                        break;

                    case "SEND":                                                                        // client send file to server to current dir
                        fileName = command.substring(command.indexOf(":") + 1, command.length());
                        getFile(fileName);
                        break;

                    default:
                        System.out.println("Received: " + command);
                        out.writeUTF("Command now found: " + command);

                }
            }
        } catch(EOFException e) {
            System.out.println("EOF:" + e);
        } catch(IOException e) {
            System.out.println("IO:" + e);
        }
    }

    private void changePassword (String password) {
        try {
            //saving new password

            ci.password = password;

            String respond = "Password changed with success";
            out.writeUTF(respond);
            return;

        }
        catch (Exception e){
            //System.out.println(e.getMessage());
        }
    }


    // changes the server dir
    private String changeCurDir(String command) {
        try {
            String newDir = command.substring(command.indexOf("server") + 7, command.length());

            if (newDir.equals("")) {
                return "Invalid path";
            }

            // go to parent folder
            if (newDir.contains("..")) {

                // verify if user already in base dir
                Path path = Paths.get(ci.directoryS);
                if (!path.toString().equals("") && path.toString().equals("Home")) {
                    return "server:" + path.toString() + ">";
                } else {
                    ci.directoryS = path.getParent().toString();
                    return "server:" + ci.directoryS + ">";
                }
            }

            // go to a sub-folder
            File folder = new File(Server.baseDirServer + ci.directoryS + Server.bars + newDir);
            if (folder.exists() && folder.isDirectory()) {
                ci.directoryS = ci.directoryS + Server.bars + newDir;

                return "server:" + ci.directoryS + ">";
            } else if (!folder.exists()) {
                if (folder.mkdir()) {
                    System.out.println(folder);
                    ci.directoryS = folder.toString();
                    return "server:" + ci.directoryS + ">";
                } else {
                    return "couldn't create folder";
                }
            }
            return "couldn't create folder";
        } catch (Exception e) {
            //System.out.println(e.getMessage());
        }
        return "Invalid path";
    }

    // list files and folder on the current server dir
    private String listFilesCurDir() {
        File[] files = new File(Server.baseDirServer + ci.directoryS).listFiles();
        String str = "";
        System.out.println("ls: " + Server.baseDirServer + ci.directoryS);

        if (files == null)
            return str;

        for (File file : files) {
            if (file.getName().contains(" ")) {
                str += "\"" + file.getName() + "\"  ";
            } else {
                str += file.getName() + "  ";
            }

        }
        return str;
    }

    // server sends a file to the client
    private void sendFile(String fileName) {
        try {
            File file = new File(fileName);
            if (!file.exists() || !file.isFile()) {
                out.writeUTF("server:" + ci.directoryS + "> " + "File not found");
                return;
            }

            ServerSocket fileS = new ServerSocket(0);
            out.writeUTF("CLIENT_CONNECT_GET:" + fileS.getLocalPort() + ":" + file.getName());     // pass client port to connect

            Socket fileSocket = fileS.accept();

            byte[] mybytearray = new byte[(int) file.length()];

            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            bis.read(mybytearray,0,mybytearray.length);


            OutputStream output = fileSocket.getOutputStream();
            DataOutputStream dout = new DataOutputStream(output);

            OutputStream os = fileSocket.getOutputStream();
            os.write(mybytearray,0,mybytearray.length);
            os.flush();

            // close stream and socket
            bis.close();
            os.close();
            fileSocket.close();

            System.out.println("File sent: " + file.getName());

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    // server receives file from client
    private void getFile(String fileName) {
        try {
            ServerSocket fileS = new ServerSocket(0);
            out.writeUTF("CLIENT_CONNECT_SEND:" + fileS.getLocalPort() + ":" + fileName);     // pass client port to connect

            Socket fileSocket = fileS.accept();

            byte [] buffer  = new byte [BLOCK_SIZE_FILE];
            InputStream is = fileSocket.getInputStream();

            FileOutputStream fos = new FileOutputStream(Server.baseDirServer + ci.directoryS + Server.bars + fileName);
            BufferedOutputStream bos = new BufferedOutputStream(fos);

            int bytesRead = 0;
            while (bytesRead != -1) {
                bos.write(buffer, 0, bytesRead);
                bos.flush();
                bytesRead = is.read(buffer, 0, buffer.length);

            }

            System.out.println("File received: " + fileName);
            // close streams
            fos.close();
            bos.close();
            fileS.close();


        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }

}
