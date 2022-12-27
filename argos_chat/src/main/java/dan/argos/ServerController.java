package dan.argos;

/////////////////////////
//Author: Daniel Marquez
//Description: Terminal/Command-Prompt based server controller which creates as many threads/connections as possible and manages communication between clients.
//TO-DO: Make encryption end-to-end by having each client exchange RSA/AES keys with each other, rather than messages being decrypted then re-encrypted
//       once they reach the server.
/////////////////////////

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class ServerController extends Thread {
    //Initialize serverSocket
    private ServerSocket serverSocket;
    private int numThreads = Runtime.getRuntime().availableProcessors()*4; //Gets logical threads available to JVM. Currently multiplied by 4, in theory logical threads should be able to switch between threads.
    private List<ServerThread> chatThreads = new ArrayList<ServerThread>(); //Array of chat threads, default # of users is one per thread available.
    private String connectionKey = ""; //Optional password for connection, not encryption
    private ArrayList<String> profaneWords = new ArrayList<String>(); //Banned word list, imported from papaya.argos file
    private String RandomUUIDKey;
    
    public void main(String cK) {
        //Load banned words
        loadProfanity();
        //Load in UUID encrypter string (random for each server and stored in secretServerUUIDKey.argo)
        if(!readUUIDKey()) {
            genUUIDKey();
        }
        //Set desired port number and create new serverSocket
        System.out.println("(Info) " + numThreads + " connections available. (Number is 4x amount of logical threads available to server.)");
        connectionKey = cK; //Sets optional connection password
        try {
            serverSocket = new ServerSocket(34040);
            System.out.println("(Info) New server socket created on port 34040.");
            //serverSocket.setSoTimeout(100000); //Sets timeout time - Do not set/uncomment.
        }
        catch(IOException e) {
            System.out.println("(Error) Exception occurred when creating serverSocket");
        }
        
        //Every 2.5 seconds check how many threads are still alive using ping-pong system and update user-count. Very latent connections may switch between being
        //"alive" or not.
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                //Checks each thread for times and decides if alive
                int count = 0;
                for(ServerThread thread : chatThreads) {
                    if(thread.lastReceived > thread.pingTime-7500) {
                        count++;
                    }
                }
                broadcast("h&43jfkFargos:"+count); //Semi-random string anticipated by ClientThread to signal new usercount data
            }
        };
        timer.scheduleAtFixedRate(task,2500,2500);

        //Try to create new chat threads to communicate between clients until numThreads max is reached
        while(true) {
            try {
                //Wait for a client to connect, create new thread, start thread.
                if(chatThreads.size() < numThreads) {
                    System.out.println("Ready for connection at: " + serverSocket.getLocalSocketAddress());
                    chatThreads.add(new ServerThread(serverSocket.accept(), this,connectionKey));
                    chatThreads.get(chatThreads.size()-1).start();
                }
                else {
                    System.out.println("Threads full. No more room for connections. If the server is still performing well and you wish to expand capacity, edit line 25 of the ServerController.java file in the source and recompile. Or, report this issue to the developer so that we can expand capacity for the next release.");
                }
            }
            catch(SocketTimeoutException s) {
                System.out.println("(Warning) Socket timeout exception.");
            }
            catch(IOException e) {
                System.out.println("(Warning) IO Exception. Someone may have unsafely disconnected.");
            }
        }
    }
    
    //Broadcast messages from one client thread to others.
    public void broadcast(String message) {
        message = checkWords(message);
        for(ServerThread currentThread : chatThreads) {
            if(currentThread.finishedConnection) {
                currentThread.toClient(message);
            }
        }
    }

    public void removeThread(ServerThread remThread) {
        chatThreads.remove(remThread);
    }

    //Loads in papaya.argo file for banned words.
    private void loadProfanity() {
        try {
            String filepath = System.getProperty("user.dir") + File.separator + "papaya.argo";
            File swearFile = new File(filepath);
            Scanner fileReader = new Scanner(swearFile);
            while(fileReader.hasNextLine()) {
                profaneWords.add(fileReader.nextLine());
            }
            fileReader.close();
            System.out.println("Profanity file read.");
        }
        catch(Exception e) {
            System.out.println("(Warning) Optional profanity file not found. If you wish to filter certain words please download the papaya.argo file from the \"https://odyssey6.gitlab.io/argoschat/documentation/\" page and place it into the server directory.");
            System.out.println("(Warning) This warning may occur when running the server .jar when the terminal is not cd into the .jar's directory.");
        }
    }

    //Check entire message for banned words and replaces them with ****
    private String checkWords(String message) {
        String[] profSplit = message.split(" ");
        ArrayList<String> outWords = new ArrayList<String>();
        for(String word : profSplit) {
            if(profaneWords.contains(word)) {
                outWords.add("****");
            } else {
                outWords.add(word);
            }
        }
        message = "";
        for(String outWord : outWords) {
            message = message + outWord + " ";
        }
        message = message.trim();
        return message;
    }

    //Try to read in UUID file.
    private boolean readUUIDKey() {
        try {
            String filepath = System.getProperty("user.dir") + File.separator + "secretServerUUIDKey.argo";
            File keyFile = new File(filepath);
            Scanner fileReader = new Scanner(keyFile);
            RandomUUIDKey = fileReader.nextLine();
            fileReader.close();
        }
        catch(Exception e) {
            System.out.println("(Warning) Error with reading secret UUID key. Perfectly normal on the first run or if file was deleted.");
            RandomUUIDKey = "";
            return false;
        }
        return true;
    }

    //Creates new UUID file
    private void genUUIDKey() {
        System.out.println("Trying to generate secret UUID key...");
        try {
            Random rd = new Random();
            RandomUUIDKey = rd.ints(97,123).limit(32).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
            String filepath = System.getProperty("user.dir") + File.separator + "secretServerUUIDKey.argo";
            PrintWriter out = new PrintWriter(filepath);
            out.println(RandomUUIDKey);
            out.close();
        }
        catch(Exception e) {
            System.out.println("(Error) Could not generate secret UUID Key. Please report this to the issue tracker.");    
        }
    }

    public String getUUIDKey() {
        return RandomUUIDKey;
    }
}