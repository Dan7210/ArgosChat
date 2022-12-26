package dan.argos;

/////////////////////////
//Author: Daniel Marquez
//Description: Handles networking connection with server. Isn't actually threaded, but it can be converted easily to threaded if needed in the future.
/////////////////////////

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class ClientThread {
    //The thread has a lot of global variables. Get over it.
    private ClientController controller;
    private ChatRSA RSA = new ChatRSA();
    private ChatAES AES = new ChatAES();
    private Socket client;
    private DataOutputStream out;
    private DataInputStream in;
    private String User;
    private String UUID;
    private String IP;
    private int Port;
    private Timeline timeline;

    private String secretKey; //AES encryption key shared between server/client

    public void connectServer(ClientController newController, String newUser, String newUUID, String newIP, int newPort, String pass) {
        System.out.println("Connecting to server.");
        //Store connection variables
        controller = newController;
        User = newUser;
        UUID = newUUID;
        IP = newIP;
        Port = newPort;


        try {
            //Connect with server and create IO channels
            client = new Socket(IP,Port);
            controller.addSystemMessage("Connected to server.");
            out = new DataOutputStream(client.getOutputStream());
            in = new DataInputStream(client.getInputStream());

            RSA.genKeys(2048); //Generate 2048 bit RSA keys
            //Optionally used to connect to private servers. This is not part of the encryption, solely a weak password exchange to allow for private servers.
            //The server side handles/discards the password based on if a password has been set and will connect/disconnect the user accordingly.
            if(!pass.trim().isEmpty()) {
                out.writeUTF("serverkey:"+pass);
                if(!in.readUTF().equals("accepted")) {
                    controller.addSystemMessage("Incorrect Server Key. Check for typos.");
                    client.close();
                    client = null;
                    return;
                }
            }
            //Passes along public key integers to remote
            out.writeUTF(RSA.n.toString());
            out.writeUTF(RSA.e.toString());
            //Read in expected AES key using stored private key integers
            secretKey = RSA.readMessage(in.readUTF());

            //Exchanges UUID with server and receives new obfuscated UUID (see ServerController UUIDKey for more information).
            //UUID system can be improved and has some work to do before being completely secure.
            out.writeUTF(AES.encrypt(UUID,secretKey));
            UUID = AES.decrypt(in.readUTF(),secretKey);

            //Infinite loop for JavaFX to check for new messages.
            timeline = new Timeline(new KeyFrame(Duration.millis(10), ev -> {
                messageCheck();
                if(client==null) {
                    timeline.stop(); //May be redundant but oh well.
                }
            }));
            timeline.setCycleCount(Animation.INDEFINITE);
            timeline.play();
        }
        catch(Exception e) {
            e.printStackTrace();
            System.out.println("Oh no... Our Generic Exception... It's broken!");
            controller.addSystemMessage("Generic error has occurred. Chat server may be offline.");
        }
    }

    //Closes thread safely with server.
    public void closeThread() throws IOException {
        sendMessage(User + " has left the chat.");
        sendMessage("Closing session.");
        client.close();
        client = null;
        timeline.stop();
    }

    //Checks for messages from the in channel and responds accordingly.
    public void messageCheck() {
        try {
            if(in.available() > 0) {
                String message = AES.decrypt(in.readUTF(),secretKey);
                if(message!=null && message.equals("ping")) {
                    sendMessage("pong");
                }
                //Janky solution for receiving user update number.
                //TO-DO: Change to be a randomized key by the server to prevent users from spoofing just like UUID key.
                else if(message!=null && message.contains("h&43jfkFargos:")) {
                    controller.updateUserAmount(Integer.valueOf(message.substring(message.indexOf(":")+1)));
                }
                else {
                    controller.addMessage(message);
                }
            }
        }
        catch(IOException e) {
            e.printStackTrace();
            System.out.println("Oh no... Our IO Exception... It's broken!");
            controller.addSystemMessage("Connection error, disconnect/reconnect if issues appear.");
        }
    }

    //Send encrypted message to the out channel
    public void sendMessage(String message) {
        try {
            out.writeUTF(AES.encrypt(User + "@" + UUID + ": " + message,secretKey));
        }
        catch(IOException e) {
            e.printStackTrace();
            System.out.println("Oh no... Our IO Exception... It's broken!");
            controller.addSystemMessage("Connection error, disconnect/reconnect if issues appear.");
        }
    }
    
    //Checks if thread is alive
    public boolean threadCheck() {
        return client!=null;
    }
}
