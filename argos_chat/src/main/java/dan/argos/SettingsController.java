package dan.argos;

/////////////////////////
//Author: Daniel Marquez
//Description: JavaFX FXML Controller class responsible for the connection settings window.
/////////////////////////

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

public class SettingsController {

    public ClientController controller;

    @FXML
    private Button bt_enterDetails;

    @FXML
    private Button bt_connect;

    @FXML
    private TextField fld_ip;

    @FXML
    private Text txt_err;

    @FXML
    private TextField fld_port;

    @FXML
    private TextField fld_username;

    @FXML
    private TextField fld_uuid;

    @FXML
    private TextField fld_pass;

    //Updates/saves settings. Closes settings window.
    @FXML
    void saveDetails(ActionEvent event) throws Exception{
        txt_err.setText("");
        if(fld_uuid.getText().length()<3) {
            String randomUUID = new ChatAES().genRandomString(10);
            fld_uuid.setText(randomUUID);
        }
        if(fld_username.getText().length()<3) {
            txt_err.setText("Username is too short.");
        }
        else if(fld_ip.getText().trim().isEmpty()) {
            txt_err.setText("IP address cannot be empty.");
        }
        else {
            if(fld_port.getText().isEmpty()) {
                controller.updateSettings(fld_username.getText(),fld_uuid.getText(),fld_ip.getText(),34040,fld_pass.getText(),false);
            }
            else if(fld_port.getText().matches("-?\\d+")) {
                controller.updateSettings(fld_username.getText(),fld_uuid.getText(),fld_ip.getText(),Integer.parseInt(fld_port.getText()),fld_pass.getText(),false);
            }
            else {
                txt_err.setText("Port invalid. Integers only.");
            }
        }
    }

    //To-Do: Find more elegant solution than to have two different methods for these two different bindings. I was tired when I wrote this so this was the best I could think of.

    //Connects to Server after updating/savings settings. Closes settings window.
    @FXML
    void connectToServer(ActionEvent event) throws Exception{
        txt_err.setText("");
        if(fld_uuid.getText().length()<3) {
            String randomUUID = new ChatAES().genRandomString(10);
            fld_uuid.setText(randomUUID);
        }
        if(fld_username.getText().length()<3) {
            txt_err.setText("Username is too short.");
        }
        else if(fld_ip.getText().trim().isEmpty()) {
            txt_err.setText("IP address cannot be empty.");
        }
        else {
            if(fld_port.getText().isEmpty()) {
                controller.updateSettings(fld_username.getText(),fld_uuid.getText(),fld_ip.getText(),34040,fld_pass.getText(),true);
            }
            else if(fld_port.getText().matches("-?\\d+")) {
                controller.updateSettings(fld_username.getText(),fld_uuid.getText(),fld_ip.getText(),Integer.parseInt(fld_port.getText()),fld_pass.getText(),true);
            }
            else {
                txt_err.setText("Port invalid. Integers only.");
            }
        }
    }

    //Updates fields with details read in from save file.
    public void updateDetails(String user, String uuid, String IP, String port, String pass) {
        fld_username.setText(user);
        fld_uuid.setText(uuid);
        fld_ip.setText(IP);
        fld_port.setText(port);
        fld_pass.setText(pass);
    }
}