/*
Departamento Eng. Informatica - FCTUC
    Sistemas Distribuídos - 2021/22
    ................................................ Rodrigo Francisco Ferreira \ nº2019220060
    ................................................ Sofia Botelho Vieira Alves \ nº2019227240
 */

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
