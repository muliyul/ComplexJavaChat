package server;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class ServerGUIController {
    private Server server;

    @FXML
    private Button cd;

    @FXML
    private Label statusLabel;

    public ServerGUIController(Server server) {
	this.server = server;
    }

    protected void connectDisconnect(ActionEvent e) {
	Platform.runLater(new Runnable() {
	    public void run() {
		if (server.isRunning()) {
		    cd.setText("Connect");
		    statusLabel.setText("Server is not listening");
		    ServerGUIController.this.server.stop();
		} else {
		    cd.setText("Disconnect");
		    statusLabel.setText("Server running on port "
			    + server.getLocalPort());
		    ServerGUIController.this.server.restart();
		}
	    }
	});
    }

    protected void setLabel(String string) {
	Platform.runLater(new Runnable() {
	    public void run() {
		statusLabel.setText(string);
	    }
	});
    }
}
