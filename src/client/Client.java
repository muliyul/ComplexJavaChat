package client;

import comm.Protocol;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
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
    private String nick;

    // GUI RELATED
    private ClientController controller;
    
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
	controller = new ClientController(primaryStage,this);
	loader.setController(controller);
	primaryStage.setTitle("Muli's chat GUI ver.01a");
	primaryStage.setScene(new Scene(loader.load()));
	primaryStage.setResizable(false);
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
	connect(controller.getHostname(), controller.getPort());
    }

    protected void sendMessage(String text) throws IOException {
	resposne.add(text);
	controller.clearMsgBar();
	out.writeUnshared(resposne);
	resposne.clear();
    }

    protected void connect(String ip, int port) {
	boolean again;
	do {
	    again = false;
	    try {
		appendToChat("Attempting to connect to " + controller.getHostname() + ":" + port);
		s = new Socket(ip, port);
		out = new ObjectOutputStream(s.getOutputStream());
		in = new ObjectInputStream(s.getInputStream());
	    } catch (IOException e) {
		appendToChat("Server not running on " + controller.getHostname() + ':' + port
			+ " or check firewall");
		System.exit(-1);
		controller.showSettingsAndWait();
		again = true;
	    }
	} while (again);

	listener = new Thread(this);
	//listener.setDaemon(true);
	listener.start();
    }

    protected void disconnect() {
	try {
	    appendToChat("Leaving chat room...");
	    controller.setTitle("Disconnected - " + controller.getTitle());
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
	    appendToChat(s);
	    Client.this.resposne.add(Protocol.MESSAGE);
	    break;
	}
	case AUTH: {// TODO
	    String s = (String) response.get(1);
	    appendToChat(s);
	    Client.this.resposne.add(Protocol.AUTH);
	    break;
	}
	}
    }

    protected synchronized void appendToChat(String str) {
	controller.appendToChatArea(str);
    }

    private synchronized void updateNickList(Set<String> o) {
	controller.updateNickList(o);
    }

    protected void openPrivateSession(String nick, boolean isHost, int port) {
	FXMLLoader loader =
		new FXMLLoader(getClass().getResource("PrivateTab.fxml"));
	try {
	    PrivateChat pc = new PrivateChat(nick, isHost, port);
	    loader.setController(pc);
	    if (isHost) {
		connectRemoteUser(Client.this.nick, pc.getPort());
	    }
	    Tab privateChat = new Tab(nick);
	    loader.setRoot(privateChat);
	    loader.load();
	    controller.addPrivateChatTab(privateChat);
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

    protected boolean isConnected() {
	return connected;
    }

    protected void setConnected(boolean b) {
	connected = b;
    }
    
    public String getNick() {
	return nick;
    }
}
