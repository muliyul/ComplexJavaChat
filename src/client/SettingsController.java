package client;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class SettingsController implements Initializable{
    private ClientController cc;
    
    @FXML
    private Button settingsApply;

    @FXML
    private Button settingsCancel;

    @FXML
    private TextField hostnameField;

    @FXML
    private TextField portField;
    
    public SettingsController(ClientController cc) {
	this.cc = cc;
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {

	
	settingsApply.setOnAction(new EventHandler<ActionEvent>() {
	    public void handle(ActionEvent event) {
		String lastHost = cc.getHostname();
		int lastPort = cc.getPort();
		try {
		    cc.setHostname(hostnameField.getText());
		    cc.setPort(Integer.parseInt(portField.getText()));
		} catch (IllegalArgumentException e) {
		    cc.setHostname(lastHost);
		    cc.setPort(lastPort);
		}
		cc.closeSettings();
		hostnameField.setText(cc.getHostname());
		portField.setText("" + cc.getPort());
	    }
	});
	settingsCancel.setOnAction(new EventHandler<ActionEvent>() {
	    public void handle(ActionEvent event) {
		cc.closeSettings();
		hostnameField.setText(cc.getHostname());
		portField.setText("" + cc.getPort());
		event.consume();
	    }
	});
    }

}
