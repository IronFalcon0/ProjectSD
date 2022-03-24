
// class with the information of clients
class ClientInfo {
    String name;
    String password;
    String directoryS;
    String directoryC;

    public ClientInfo(String name, String password, String directoryC) {
        this.name = name;
        this.password = password;
        this.directoryS = "Home";
        this.directoryC = directoryC;
    }

    public ClientInfo(String name, String password) {
        this.name = name;
        this.password = password;
        this.directoryS = "Home";
        this.directoryC = "Home";
    }
}
