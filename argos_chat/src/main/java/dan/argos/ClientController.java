package dan.argos;

/////////////////////////
//Author: SceneBuilder/Daniel Marquez
//Description: JavaFX FXML Controller class responsible for main client window. Also controls most of the client logic and interactions with other classes.
/////////////////////////

import java.io.File;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Scanner;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.stage.Stage;

import java.awt.*;

public class ClientController {

    ////Create global variables
    private String username = "";
    private String UUID;
    private String IP = "localhost";
    private int port = 34040;
    private String pass = "";
    private Stage popup;
    private ClientThread thread;
    private long lastTime = System.currentTimeMillis();
    private long averageTime = 5000;

    //Called on Window Created
    @FXML
    public void initialize() {
        readSettings();
        //Update message listcell factory
        updateCellFactory();
    }

    ////Initialize all the FXML Objects
    @FXML
    private MenuItem bt_about;
    @FXML
    private MenuItem bt_connect;
    @FXML
    private MenuItem bt_settings;
    @FXML
    private MenuItem bt_clearchat;
    @FXML
    private MenuItem bt_disconnect;
    @FXML
    private MenuItem bt_report;
    @FXML
    private MenuItem bt_exit;
    @FXML
    private Button bt_sendmessage;
    @FXML
    private ListView<String> chat_scrollchat;
    @FXML
    private TextField chat_textbox;
    @FXML
    private Text txt_ipport;
    @FXML
    private Text txt_uuid;
    @FXML
    private Text txt_user;
    @FXML
    private Text txt_userbox;
    @FXML
    private Color x21;
    @FXML
    private Font x3;
    @FXML
    private Color x4;

    ////FXML Bound Methods
    @FXML
    private void closeProgram(ActionEvent event) {
        System.exit(0);
    }
    //Open About Window Popup
    @FXML
    private void openAbout(ActionEvent event) throws Exception {
        if(popup!=null) {
            popup.close();
        }
        Stage stage = new Stage();
        Parent root = FXMLLoader.load(getClass().getResource("ClientAbout.fxml"));
        Scene scene = new Scene(root);
        stage.setTitle("About Argos");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("icon.png")));
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
        popup = stage;
    }
    //Open Connection Settings Window Popup
    @FXML
    private void openSettings(ActionEvent event) throws Exception {
        if(popup!=null) {
            popup.close();
        }
        Stage stage = new Stage();
        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("ClientSettings.fxml"));
        SettingsController testControl = new SettingsController();
        testControl.controller = this;
        loader.setController(testControl);
        loader.setLocation(this.getClass().getResource("ClientSettings.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Argos Connection Settings");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("icon.png")));
        stage.setResizable(false);
        stage.show();
        popup = stage;
        testControl.updateDetails(username, UUID, IP, String.valueOf(port),pass);
    }
    //Calls connect method if values have been set
    @FXML
    private void connect(ActionEvent event) throws Exception {
        if(!username.isEmpty() & !UUID.isEmpty()) {
            createConnection();
        }
    }
    //Sends method if client isn't spamming their chat and their message isn't empty.
    //Note: This is paired with rate limiting on the server-side as well.
    @FXML
    private void sendMessage(ActionEvent event) throws Exception {
        if(System.currentTimeMillis()-lastTime > Math.max(Math.min(((2000 * (1-(averageTime/5000)))),5000),250)) {
            bt_sendmessage.setText("Send Message");
            String message = chat_textbox.getText();
            chat_textbox.setText("");
            if(!message.trim().isEmpty()) {
                thread.sendMessage(message);
            }
            averageTime = ((averageTime + Math.min((System.currentTimeMillis()-lastTime),2500))/2);
            lastTime = System.currentTimeMillis();
        }
        else {
            bt_sendmessage.setText("Slow Down!");
        }
    }
    //Disconnects from the server.
    @FXML
    private void disconnect(ActionEvent event) throws Exception {
        if(thread!=null && thread.threadCheck()) {
            thread.closeThread();
            addSystemMessage("Disconnected from server.");
            thread = null;
        }
        updateUserAmount(0);
    }
    //Opens issue reporting form URL in client's browser.
    @FXML
    private void reportIssue(ActionEvent event) throws Exception {
        Desktop desk = Desktop.getDesktop();
        desk.browse(new URI("https://forms.gle/anzKJRqA6iaJzXv66"));
    }
    //Clears chat message list
    @FXML
    private void clearChat(ActionEvent event) throws Exception {
        chat_scrollchat.getItems().clear();
    }

    ////Update FXML Element Methods
    //Add message to message list, splitting large messages up, playing sound if username tagged, and scrolling to the bottom.
    public void addMessage(String message) {
        if(message.contains("@"+username)) {
            playNotifSound();
        }
        String[] lineSplit = message.split("(?<=\\G.{" + 120 + "})");
        for(String split : lineSplit) {
            chat_scrollchat.getItems().add(split);
        }
        chat_scrollchat.refresh();
        chat_scrollchat.scrollTo(chat_scrollchat.getItems().size());
    }
    //Simple system message method, mostly for connected/disconnected/error messages
    public void addSystemMessage(String message) {
        chat_scrollchat.getItems().add("\\---- " + message + " -----/");
        chat_scrollchat.refresh();
    }
    public void updateUserAmount(int amount) {
        txt_userbox.setText("Users: " + amount);
    }
    //Plays notif.wav
    public void playNotifSound() {
        try {
            Clip clip = AudioSystem.getClip();
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(
            Main.class.getResourceAsStream("notif.wav"));
            clip.open(inputStream);
            clip.start();
        }
        catch(Exception e) {
            addSystemMessage("Error with playing client sound");
        }
    }

    //Update client connection settings
    public void updateSettings(String newUser, String newUUID, String newIP, int newPort, String newPass, boolean connect) throws Exception{
        disconnect(null);
        username = newUser;
        UUID = newUUID;
        if(newIP.toLowerCase().equals("localhost")) {
            IP = "127.0.0.1";
        }
        else {
            IP = newIP;
        }
        port = newPort;
        pass = newPass;
        popup.close();

        txt_user.setText("Username: " + username);
        txt_ipport.setText("Address: " + IP + ":" + port);
        txt_uuid.setText("UUID: " + UUID);

        saveSettings(); //Writes to file in AppData folder
        updateCellFactory(); //Updates what messages are highlighted/tagged if username changes

        if(connect) {
            if(!username.isEmpty() & !UUID.isEmpty()) {
                createConnection();
            }
        }
    }
    //Essentially the default CellFactory with an if statement to highlight text lines if the username is tagged
    public void updateCellFactory() {
        //Set cell factory
        chat_scrollchat.setCellFactory(lv -> new ListCell<String>() {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if(item==null || empty) {
                    setText(null);
                    setGraphic(null);
                    setBackground(Background.EMPTY);
                } else {
                    if(item.contains("@" + username)) {
                        setText(item);
                        setBackground(new Background(new BackgroundFill(Color.LIGHTGREEN, CornerRadii.EMPTY, null)));
                    } else {
                        setText(item);
                        setBackground(Background.EMPTY);
                    }
                }
            }
        });
    }

    //Create ClientThread instance and attempt connection using given settings
    public void createConnection() {
        thread = new ClientThread();
        thread.connectServer(this,username,UUID,IP,port,pass);
    }

    //Write settings to file
    public void saveSettings() {
        try {
            String filePath = System.getenv("APPDATA") + File.separator + "Argos"; //Creates filepath, File.separator is OS agnostic
            new File(filePath).mkdirs();
            PrintWriter out = new PrintWriter(filePath+File.separator+"options.cfg");
            out.println(username + "," + UUID + "," + IP + "," + port + "," + pass);
            out.close();
        }
        catch(Exception e) {
            e.printStackTrace();
            addSystemMessage("An error has occurred with saving options data.");
        }
    }

    //Read settings from file
    public void readSettings() {
        try {
            String filepath = System.getenv("APPDATA") + File.separator + "Argos" + File.separator + "options.cfg";
            File configFile = new File(filepath);
            Scanner fileReader = new Scanner(configFile);
            String configLine = fileReader.nextLine();
            if(!configLine.isEmpty()) {
                String[] newArgs = configLine.split(",");
                username = newArgs[0];
                UUID = newArgs[1];
                IP = newArgs[2];
                port = Integer.valueOf(newArgs[3]);
                if(newArgs.length > 4) {
                    pass = newArgs[4];
                }
                System.out.println("Values read.");

                txt_user.setText("Username: " + username);
                txt_ipport.setText("Connection Address: " + IP + ":" + port);
                txt_uuid.setText("UUID: " + UUID);
            }
            fileReader.close();
        }
        catch(Exception e) {
            e.printStackTrace();
            addSystemMessage("Welcome to ArgosChat. Enjoy your stay.");
        }
    }
}
