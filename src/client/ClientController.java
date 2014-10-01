package client;

import java.io.IOException;
import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Vector;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ClientController implements Initializable {
    private Client associatedClient;
    private String hostname;
    private int port;
    
    private Stage primaryStage;

    @FXML
    private VBox nickList;

    @FXML
    private TextArea chatArea;

    @FXML
    private TextField msgBar;

    @FXML
    private Button sendButton;

    @FXML
    private MenuItem connectDisconnect;

    @FXML
    private TabPane privateChatPane;

    @FXML
    private MenuItem settingsButton;

    @FXML
    private Stage settings;
    
    public ClientController(Stage primaryStage, Client c) {
	this.primaryStage = primaryStage;
	associatedClient = c;
	this.hostname = "localhost";
	this.port = 5050;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
	// INIT
	FXMLLoader loader =
		new FXMLLoader(getClass().getResource("Settings.fxml"));
	loader.setController(new SettingsController(this));
	settings = new Stage();
	settings.setTitle("Settings");
	try {
	    settings.setScene(new Scene(loader.load()));
	} catch (IOException e1) {
	    e1.printStackTrace();
	}
	msgBar.setOnKeyPressed(new EventHandler<KeyEvent>() {
	    public void handle(KeyEvent event) {
		if (event.getCode().equals(KeyCode.ENTER)) {
		    if (!msgBar.getText().equals("")
			    || msgBar.getText() != null)
			try {
			    associatedClient.sendMessage(msgBar.getText());
			} catch (IOException e) {
			}
		    event.consume();
		}
	    }
	});
	sendButton.setOnAction(new EventHandler<ActionEvent>() {
	    public void handle(ActionEvent event) {
		if (!msgBar.getText().equals("") || msgBar.getText() != null)
		    try {
			associatedClient.sendMessage(msgBar.getText());
		    } catch (IOException e) {
		    }
		event.consume();
	    }
	});
	connectDisconnect.setOnAction(new EventHandler<ActionEvent>() {
	    public void handle(ActionEvent event) {
		if (associatedClient.isConnected()) {
		    associatedClient.disconnect();
		    
		} else {
		    associatedClient.connect(hostname, port);
		}
		associatedClient.setConnected(!associatedClient.isConnected());
		event.consume();
	    }
	});
	settingsButton.setOnAction(new EventHandler<ActionEvent>() {
	    public void handle(ActionEvent event) {
		settings.show();
		event.consume();
	    }
	});
    }

    protected String getHostname() {
	return hostname;
    }

    protected int getPort() {
	return port;
    }

    protected void closeSettings() {
	settings.close();
    }

    public void setHostname(String text) {
	this.hostname = text;
    }

    public void setPort(int port) {
	this.port = port;
    }

    public void appendToChatArea(String str) {
	Platform.runLater(new Runnable() {
	    public void run() {
		chatArea.appendText("[ "
			+ LocalTime.now().format(
				DateTimeFormatter.ofPattern("HH:mm:ss"))
			+ " ] " + str + System.getProperty("line.separator"));
	    }
	});
    }

    public void updateNickList(Set<String> o) {
	Platform.runLater(new Runnable() {
	    public void run() {
		List<String> nicks;
		Collections.sort(nicks = new Vector<String>(o));
		nickList.getChildren().removeAll(nickList.getChildren());
		for (String nick : nicks) {
		    Label t;
		    nickList.getChildren().add(t = new Label(nick));
		    if (!nick.equals(associatedClient.getNick())) {
			t.setOnMouseClicked(new EventHandler<MouseEvent>() {
			    public void handle(MouseEvent event) {
				if (event.getClickCount() >= 2) {
				    associatedClient.openPrivateSession(nick,
					    true, 0);
				}
			    }
			});
		    } else {
			// TODO emphasize own nick
		    }
		}
	    }
	});
    }

    protected void clearMsgBar() {
	Platform.runLater(new Runnable() {
	    public void run() {
		msgBar.clear();
	    }
	});
    }

    protected void showSettingsAndWait() {
	Platform.runLater(new Runnable() {
	    public void run() {
		settings.showAndWait();
	    }
	});
    }

    protected void addPrivateChatTab(Tab privateChat) {
	Platform.runLater(new Runnable() {
	    public void run() {
		privateChatPane.getTabs().add(privateChat);
		privateChatPane.getSelectionModel().select(privateChat);
	    }
	});
    }

    public String getTitle() {
	return primaryStage.getTitle();
    }

    protected void setTitle(String string) {
	primaryStage.setTitle(string);
    }

}
