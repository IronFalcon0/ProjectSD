===================================================================================
    Departamento Eng. Informatica - FCTUC
    Sistemas Distribuídos - 2021/2022
    ................................................
    Rodrigo Francisco Ferreira \ nº2019220060
    Sofia Botelho Vieira Alves \ nº2019227240
    ................................................
===================================================================================

:::::::::::::::::::::::: ucDrive: Repositório de ficheiros na UC ::::::::::::::::::


-> Run the program
	To run the program currectly there should be 2 ucDrive.jar instances and at least one terminal.jar running

	To run the ucDrive.jar use the following command:

		java -jar ucDrive.jar *folderID*

	Where:
	folderID 	-> Identifies which folder the server can access. There will be created two folders to be used as storage space: "Server" and "Server2". To select the first one use 0, for the second use 1. The 2 servers need to have different values;

	The first time the ucDrive.jar is executed, it extracts the conf_file and users_info


	To run the terminal.jar use the following command:
	
		java -jar terminal.jar *serverIP1* *serverPort1* *serverIP2* *serverPort2*

    Where:
    serverIP1 		-> server1 IP;
    serverPort1 	-> server1 Port;
    serverIP2 		-> server2 IP;
    serverPort2 	-> server2 Port;

-> The default values are:
	serverIP1 	-> localhost
	serverPort1 	-> 6000
	serverIP2 	-> localhost
	serverPort2 	-> 6001

	This values can be changed in the conf_file after running the ucDrive.jar for the first time

===================================================================================


-> User info for login is the following:
	Username: user1		Password: 1234
	Username: user2		Password: ss11


-> Available commands of terminal.jar:

	ls client
		-> list files and folders in the current client dir

	ls server
		-> list files and folders in the current server dir

	cd client *dirName*
 		-> change the directory of the client
		-> '..' go to parent folder

	cd server *dirName*
 		-> change the directory of the server
		-> if folder doesn't exits creates it
		-> '..' go to parent folder

	exit()
		-> closes conection client-server

	cp *newPassword*
 		-> changes user's password

	get *fileName*
		-> get file from the current server dir to the current client dir

	send *fileName*
		-> send file from the current client dir to the current server dir


===================================================================================
