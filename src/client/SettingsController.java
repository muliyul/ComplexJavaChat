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
    private ClientController clientController;
    
    @FXML
    private Button settingsApply;

    @FXML
    private Button settingsCancel;

    @FXML
    private TextField hostnameField;

    @FXML
    private TextField portField;
    
    public SettingsController(ClientController cc) {
	this.clientController = cc;
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {

	
	settingsApply.setOnAction(new EventHandler<ActionEvent>() {
	    public void handle(ActionEvent event) {
		String lastHost = clientController.getHostname();
		int lastPort = clientController.getPort();
		try {
		    clientController.setHostname(hostnameField.getText());
		    clientController.setPort(Integer.parseInt(portField.getText()));
		} catch (IllegalArgumentException e) {
		    clientController.setHostname(lastHost);
		    clientController.setPort(lastPort);
		}
		clientController.closeSettings();
		hostnameField.setText(clientController.getHostname());
		portField.setText("" + clientController.getPort());
	    }
	});
	settingsCancel.setOnAction(new EventHandler<ActionEvent>() {
	    public void handle(ActionEvent event) {
		clientController.closeSettings();
		hostnameField.setText(clientController.getHostname());
		portField.setText("" + clientController.getPort());
		event.consume();
	    }
	});
    }

}
