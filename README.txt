===================================================================================
    Departamento Eng. Informatica - FCTUC
    Sistemas Distribuídos - 2021/2022
    ................................................
    Rodrigo Francisco Ferreira \ nº2019220060
    Sofia Botelho Vieira Alves \ nº2019227240
    ................................................
===================================================================================

:::::::::::::::::::::::: ucDrive: Repositório de ficheiros na UC ::::::::::::::::::


-> Execução do programa

    A seguir seguem-se os comandos necessários para a execução do programa.

    - Servidor Primário 
    
        java -jar ucDrive.jar 0


    - Servidor Secundário

        java -jar ucDrive.jar 1

    Onde:
    * O último parâmetro (0 ou 1) identifica a pasta onde o servidor irá guardar todas as
    diretorias dos clientes.

    - clientes

    java -jar terminal.jar *mainServerIP* *mainServerPort* *secServerIP* *secServerPort*

    Onde: 
    * mainServerIP - Ip do servidor principal;
    * mainServerPort - Porto do servidor principal
    * secServerIP - Ip do servidor secundário;
    * secServerPort - Porto do servidor secundário;


-> Comandos 

    Neste projeto, foram definidos os seguintes comandos:


        Listagem de ficheiros na pasta do cliente
            
            ls client

        Listagem de ficheiros na pasta do servidor 
            
            ls server

        Alteração da diretoria do cliente 
        
            cd client *nome da diretoria*

        Alteração da diretoria do servidor
        
            cd server *nome do ficheiro*

        Encerrar o terminal
            
            exit()

        Alterar a palavra passe do utilizador
        
            cp *nova palavra passe*

        Obtenção de um ficheiro do servidor
        
            get *nome do ficheiro*

        Envio de um ficheiro para o servidor
        
            send *nome do ficheiro*

===================================================================================
