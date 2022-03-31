import java.io.File;

// class with the information of clients
class ClientInfo {
    String name;
    String password;
    String folderName;
    String directoryS;

    public ClientInfo(String name, String password, String folderName, String directoryS) {
        this.name = name;
        this.password = password;
        this.folderName = folderName;
        this.directoryS = directoryS;
    }

    public ClientInfo(String name, String password, String folderName) {
        this.name = name;
        this.password = password;
        this.folderName = folderName;
        this.directoryS = "Home";
    }
}
