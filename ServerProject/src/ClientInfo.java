
// class with the information of clients
class ClientInfo {
    String name;
    String password;
    String directoryS;

    public ClientInfo(String name, String password, String directoryS) {
        this.name = name;
        this.password = password;
        this.directoryS = directoryS;
    }

    public ClientInfo(String name, String password) {
        this.name = name;
        this.password = password;
        this.directoryS = "Home";
    }
}
