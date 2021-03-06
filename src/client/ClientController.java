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
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
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
    protected Client associatedClient;
    private String hostname;
    private int port;

    private Stage primaryStage;
    private String title;

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
    private List<PrivateChat> privateChatSessions;

    public ClientController(String title, Stage primaryStage, Client c) {
	this.primaryStage = primaryStage;
	primaryStage.setTitle(this.title = title);
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
			    associatedClient.send(msgBar.getText());
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
			associatedClient.send(msgBar.getText());
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

    protected void setHostname(String text) {
	this.hostname = text;
    }

    protected void setPort(int port) {
	this.port = port;
    }

    protected void appendToChatArea(String str) {
	Platform.runLater(new Runnable() {
	    public void run() {
		chatArea.appendText("[ "
			+ LocalTime.now().format(
				DateTimeFormatter.ofPattern("HH:mm:ss"))
			+ " ] " + str + System.getProperty("line.separator"));
	    }
	});
    }

    protected void updateNickList(Set<String> o) {
	Platform.runLater(new Runnable() {
	    public void run() {
		List<String> nicks;
		Collections.sort(nicks = new Vector<String>(o));
		nickList.getChildren().removeAll(nickList.getChildren());
		for (String nick : nicks) {
		    Label t;
		    nickList.getChildren().add(t = new Label(nick));
		    if (!nick.equals(associatedClient.getNickname())) {
			t.setOnMouseClicked(new EventHandler<MouseEvent>() {
			    public void handle(MouseEvent event) {
				if (event.getClickCount() >= 2) {
				    associatedClient.openPrivateSession(nick,
					    0, true);
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
	if (!settings.isShowing())
	    settings.showAndWait();
    }

    protected void addPrivateChatTab(Tab privateChat) {
	Platform.runLater(new Runnable() {
	    public void run() {
		privateChatPane.getTabs().add(privateChat);
		privateChatPane.getSelectionModel().select(privateChat);
	    }
	});
    }

    protected void setTitleConnected() {
	setTitle("Connected - " + title);
    }

    protected void setTitleDisconnected() {
	setTitle("Disconnected - " + title);
    }

    private void setTitle(String title) {
	Platform.runLater(new Runnable() {
	    public void run() {
		primaryStage.setTitle(title);
	    }
	});
    }

    protected void addPrivateChatTab(String remoteNick, int port, boolean isHost) {
	FXMLLoader loader =
		new FXMLLoader(getClass().getResource("PrivateTab.fxml"));
	PrivateChat pc;
	try {
	    loader.setController(pc = new PrivateChat(remoteNick, port, isHost));
	    if (isHost) {
		associatedClient.connectRemoteUser(remoteNick,
			PrivateChat.getPort());
	    }
	    Tab privateChat = new Tab(remoteNick);
	    privateChat.setOnCloseRequest(new EventHandler<Event>() {
		public void handle(Event event) {
		    try {
			pc.close();
		    } catch (IOException e) {
			e.printStackTrace();
		    }
		}
	    });
	    SplitPane sp = loader.load();
	    privateChat.setContent(sp);
	    addPrivateChatTab(privateChat);
	    pc.start();
	    if (privateChatSessions == null)
		privateChatSessions = new Vector<PrivateChat>();
	    privateChatSessions.add(pc);
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    protected void closeAllPrivateChats() throws IOException {
	if (privateChatSessions != null)
	    for (PrivateChat pc : privateChatSessions) {
		    pc.close();
	    }
    }
}