package dan.argos;

/////////////////////////
//Author: Daniel Marquez
//Description: Launches ServerController. Sets Connection Key if given. (See Documentation for information)
/////////////////////////

public class ServerLauncher {
    public static void main(String[] args) {
        if(args.length > 0) {
            new ServerController().main(args[0]);
        }
        else {
            new ServerController().main("");
        }
    }
}
