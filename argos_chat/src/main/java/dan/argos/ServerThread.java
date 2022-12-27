package dan.argos;

/////////////////////////
//Author: Daniel Marquez
//Description: Threaded Server Connection Class. Passes information from Server to Client and vice-versa.
/////////////////////////

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;

public class ServerThread extends Thread {
    //ServerThread has a lot of global variables. Get over it.
    private Socket server;
    private ServerController secureServer;
    private String message = "";
    private DataInputStream in;
    private DataOutputStream out;
    private ChatRSA RSA = new ChatRSA();
    private ChatAES AES = new ChatAES();
    private String secretKey;
    private String connectionKey = "";
    public boolean finishedConnection = false;
    private String UUID = "";

    //Ping-pong system for checking if connection alive.
    public double lastReceived = System.currentTimeMillis();
    public double pingTime = System.currentTimeMillis();

    private double lastMessage = System.currentTimeMillis(); //Spam prevention
    
    //Constructor Method
    public ServerThread(Socket threadSocket, ServerController serverRef, String connectKey) {
        server = threadSocket;
        secureServer = serverRef;
        connectionKey = connectKey;
    }
    
    //Main method for relaying communication
    public void run() {
        try {
            System.out.println("Connection established with: " + server.getRemoteSocketAddress());
            
            //Define new data i/o streams
            in = new DataInputStream(server.getInputStream());
            out = new DataOutputStream(server.getOutputStream());
            
            //Receive RSA public key and send symmetric key
            String firstContact = in.readUTF();
            //If we don't expect a password
            if(connectionKey.isEmpty()) {
                if(firstContact.contains("serverkey:")) {
                    out.writeUTF("accepted"); //Even though we don't need a password, client still needs confirmation
                    RSA.foreignN = new BigInteger(in.readUTF()); //If the client sent a password, ignore it
                }
                else {
                    RSA.foreignN = new BigInteger(firstContact); //If the client sent a RSA key, use it
                }
            }
            //If we do expect a password
            else {
                if(firstContact.contains("serverkey:") && (firstContact.substring(firstContact.indexOf(":")+1).equals(connectionKey))) {
                    out.writeUTF("accepted");
                    RSA.foreignN = new BigInteger(in.readUTF());
                }
                else {
                    destroySelf();
                }
            }
            RSA.foreignE = new BigInteger(in.readUTF());

            //Generate symmetric key with random input
            secretKey = AES.genRandomString(64); //Length 64 will give us a 256-bit AES key.
            out.writeUTF(RSA.encrypt(secretKey)); //Send encrypted secret key
            
            //UUID exchange.
            UUID = AES.decrypt(in.readUTF(),secretKey);
            UUID = AES.encrypt(UUID,secureServer.getUUIDKey()); //The point of this is not really to "encrypt" the UUID, more just to obfuscate it and make it unique using classes already available to the project.
            out.writeUTF(AES.encrypt(UUID,secretKey));
            
            finishedConnection = true; //Used so that no messages are sent to this thread before keys and initial information are exchanged, which would generate an error.
            //This was an issue in earlier development versions of ArgosChat as the encryption key would be set to the amount of users on the server... Yikes.

            //If client has message, relay to server.
            while(!server.isClosed()) {
                if(in.available() > 0) {
                    message = AES.decrypt(in.readUTF(), secretKey);
                    //Client when disconnecting safely will send the below simple message.
                    if(message.equals("Closing session.")) {
                        destroySelf();
                        break;
                    }
                    //Ping-pong heartbeat system
                    else if(message.contains("pong")) {
                        lastReceived = System.currentTimeMillis();
                    }
                    else {
                        //Spam prevention check
                        if(Math.abs(System.currentTimeMillis()-lastMessage) > 250) {
                            //Temporary solution for ensuring that user is using the generated UUID. Attempts to prevent UUID spoofing.
                            if(message.contains(UUID)) {
                                secureServer.broadcast(message); //Only if all the above conditions are met will the message be sent.
                            }
                            else {
                                out.writeUTF(AES.encrypt("!!Are you trying to spoof your UUID? Shame on you!!", secretKey));
                                destroySelf();
                            }
                        }
                        else {
                            if(secretKey != null) {
                                out.writeUTF(AES.encrypt("!!Server Rate Limit Exceeded! If you did not try to modify your client please report this bug using the issue reporting form!!", secretKey));
                            }
                        }
                        lastMessage = System.currentTimeMillis();
                    }
                }
                //Refresh rate of heartbeat check is 2000, or 2 seconds
                if(System.currentTimeMillis() > pingTime+2000) {
                    pingTime = System.currentTimeMillis();
                    out.writeUTF(AES.encrypt("ping", secretKey));
                }
            }
        }
        //Catch exceptions
        catch(IOException e) {
            System.out.println("Broken IO Exception, User most likely disconnected.");
            destroySelf();
        }
        catch(Exception e) {
            System.out.println("Oh no... Our Generic Thread Exception... It's broken!");
            destroySelf();
        }
    }

    //Attempt to close thread and remove itself from its controller. Should be destroyed by GC shortly after.
    public void destroySelf() {
        try {
            secureServer.removeThread(this);
            server.close();
        }
        catch(IOException e) {
            System.out.println("Exception occurred while closing thread.");
        }
    }
    
    //Method for relaying messages from server to client.
    public void toClient(String message) {
        try {
            if(secretKey != null) {
                out.writeUTF(AES.encrypt(message, secretKey));
            }
            else {
                System.out.println("Sending unencrypted message");
                out.writeUTF(message);
            }
        }
        catch(IOException e) {
            System.out.println("(Error) Thread IOException. Disconnecting Thread.");
            destroySelf();
        }
    }

    //Returns secretKey to ServerController.
    public String getKey() {
        return secretKey;
    }
}