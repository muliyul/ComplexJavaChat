package client;

import comm.Protocol;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
import javafx.stage.WindowEvent;

/**
 * Client application.
 * 
 * @author Muli Yulzary
 *
 */
public class Client extends Application implements Runnable {

    // COMM RELATED
    private Socket s;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Thread listener;
    private boolean connected;
    private String hostname;
    private int port;
    private String nick;

    // GUI RELATED
    @FXML
    private SplitPane topLevel;

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

    @FXML
    private Button settingsApply;

    @FXML
    private Button settingsCancel;

    @FXML
    private TextField hostnameField;

    @FXML
    private TextField portField;
    private List<Object> resposne;

    public static void main(String[] args) {
	launch(args);
    }

    public Client() {
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
	resposne = new Vector<Object>();
	FXMLLoader loader =
		new FXMLLoader(getClass().getResource("ClientGUI.fxml"));
	loader.setController(this);
	primaryStage.setTitle("Muli's chat GUI ver.01a");
	primaryStage.setScene(new Scene(loader.load()));
	primaryStage.setResizable(false);
	loader = new FXMLLoader(getClass().getResource("Settings.fxml"));
	loader.setController(this);
	settings = new Stage();
	settings.setTitle("Settings");
	settings.setScene(new Scene(loader.load()));
	msgBar.setOnKeyPressed(new EventHandler<KeyEvent>() {
	    public void handle(KeyEvent event) {
		if (event.getCode().equals(KeyCode.ENTER)) {
		    if (!msgBar.getText().equals("")
			    || msgBar.getText() != null)
			try {
			    sendMessage(msgBar.getText());
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
			sendMessage(msgBar.getText());
		    } catch (IOException e) {
		    }
		event.consume();
	    }
	});
	connectDisconnect.setOnAction(new EventHandler<ActionEvent>() {
	    public void handle(ActionEvent event) {
		if (connected) {
		    disconnect();
		} else {
		    connect(hostname, port);
		}
		connected = !connected;
		event.consume();
	    }
	});
	settingsButton.setOnAction(new EventHandler<ActionEvent>() {
	    public void handle(ActionEvent event) {
		settings.show();
		event.consume();
	    }
	});
	settingsApply.setOnAction(new EventHandler<ActionEvent>() {
	    public void handle(ActionEvent event) {
		String lastHost = hostname;
		int lastPort = port;
		try {
		    hostname = hostnameField.getText();
		    port = Integer.parseInt(portField.getText());
		} catch (IllegalArgumentException e) {
		    hostname = lastHost;
		    port = lastPort;
		}
		settings.close();
		hostnameField.setText(hostname);
		portField.setText("" + port);
	    }
	});
	settingsCancel.setOnAction(new EventHandler<ActionEvent>() {
	    public void handle(ActionEvent event) {
		settings.close();
		hostnameField.setText(hostname);
		portField.setText("" + port);
		event.consume();
	    }
	});
	primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
	    public void handle(WindowEvent event) {
		try {
		    resposne.clear();
		    resposne.add(Protocol.I_QUIT);
		    s.close();
		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }
	});
	primaryStage.show();
	connect(hostname = "localhost", port = 5050);
    }

    protected void sendMessage(String text) throws IOException {
	resposne.add(text);
	msgBar.clear();
	out.writeUnshared(resposne);
	resposne.clear();
    }

    protected void connect(String ip, int port) {
	boolean again;
	do {
	    again = false;
	    try {
		appendText("Attempting to connect to " + hostname + ":" + port);
		s = new Socket(ip, port);
		out = new ObjectOutputStream(s.getOutputStream());
		in = new ObjectInputStream(s.getInputStream());
	    } catch (IOException e) {
		appendText("Server not running on " + hostname + ':' + port
			+ " or check firewall");
		System.exit(-1);
		settings.showAndWait();
		again = true;
	    }
	} while (again);

	listener = new Thread(this);
	listener.setDaemon(true);
	listener.start();
    }

    protected void disconnect() {
	try {
	    s.close();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
	connected = true;
	try {
	    while (connected) {
		List<Object> response = (Vector<Object>) in.readUnshared();
		handle(response);
	    }
	} catch (IOException | ClassNotFoundException e) {
	} catch (ClassCastException e) {
	    e.printStackTrace();
	}
    }

    @SuppressWarnings({ "unchecked", "incomplete-switch" })
    private void handle(List<Object> response) throws ClassCastException,
	    ClassNotFoundException, IOException {
	switch ((Protocol) response.get(0)) {
	case NICK: {
	    this.nick = (String) response.get(1);
	    break;
	}
	case PEER_CONNECTION_REQUEST: {
	    String nick = (String) response.get(1);
	    int port = (int) response.get(2);
	    Platform.runLater(new Runnable() {
		public void run() {
		    openPrivateSession(nick, false, port);
		}
	    });
	    break;
	}
	case UPDATE_NICK_LIST: {
	    updateNickList((HashSet<String>) response.get(1));
	    break;
	}
	case MESSAGE: {// TODO
	    String s = (String) response.get(1);
	    appendText(s);
	    Client.this.resposne.add(Protocol.MESSAGE);
	    break;
	}
	case AUTH: {// TODO
	    String s = (String) response.get(1);
	    appendText(s);
	    Client.this.resposne.add(Protocol.AUTH);
	    break;
	}
	}
    }

    protected synchronized void appendText(String str) {
	Platform.runLater(new Runnable() {
	    public void run() {
		chatArea.appendText("[ "
			+ LocalTime.now().format(
				DateTimeFormatter.ofPattern("HH:mm:ss"))
			+ " ] " + str + System.getProperty("line.separator"));
	    }
	});
    }

    private synchronized void updateNickList(Set<String> o) {
	Platform.runLater(new Runnable() {
	    public void run() {
		List<String> nicks;
		Collections.sort(nicks = new Vector<String>(o));
		nickList.getChildren().removeAll(nickList.getChildren());
		for (String nick : nicks) {
		    Label t;
		    nickList.getChildren().add(t = new Label(nick));
		    if (!nick.equals(Client.this.nick)) {
			t.setOnMouseClicked(new EventHandler<MouseEvent>() {
			    public void handle(MouseEvent event) {
				if (event.getClickCount() >= 2) {
				    openPrivateSession(nick, true, 0);
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

    private void openPrivateSession(String nick, boolean isHost, int port) {
	FXMLLoader loader =
		new FXMLLoader(getClass().getResource("PrivateTab.fxml"));
	try {
	    PrivateChat pc;
	    loader.setController(pc = new PrivateChat(nick, isHost, port));
	    if (isHost) {
		connectRemoteUser(Client.this.nick, pc.getPort());
	    }
	    Tab privateChat = new Tab(nick);
	    loader.setRoot(privateChat);
	    loader.load();
	    privateChatPane.getTabs().add(privateChat);
	    privateChatPane.getSelectionModel().select(privateChat);
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    private void connectRemoteUser(String nick, int port) throws IOException {
	resposne.clear();
	resposne.add(Protocol.INITIATE_PEER_CONNECTION);
	resposne.add(nick);
	resposne.add(port);
	out.writeUnshared(resposne);
	resposne.clear();
    }
}
