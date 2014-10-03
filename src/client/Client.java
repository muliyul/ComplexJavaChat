package client;

import comm.Protocol;
import comm.Protocol.Type;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * Client application.
 * 
 * @author Muli Yulzary
 *
 */
public class Client extends Application implements Runnable {

    public static void main(String[] args) {
	launch(args);
    }

    // COMM RELATED
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Thread listener;
    private boolean isConnected;
    private String nickname;
    private Type msgType;

    // GUI RELATED
    private ClientController clientController;

    public Client() {
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
	FXMLLoader loader =
		new FXMLLoader(getClass().getResource("ClientGUI.fxml"));
	clientController =
		new ClientController("Muli's chat GUI ver.01a", primaryStage,
			this);
	loader.setController(clientController);
	primaryStage.setScene(new Scene(loader.load()));
	primaryStage.setResizable(false);
	primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
	    public void handle(WindowEvent event) {
		event.consume();
		try {
		    if (socket != null) {
			out.writeUnshared(new Protocol(Protocol.Type.I_QUIT));
			socket.close();
		    }
		} catch (IOException e) {
		    // e.printStackTrace();
		} finally {
		    primaryStage.close();
		}
	    }
	});
	primaryStage.show();
	// connect(clientController.getHostname(), clientController.getPort());
    }

    protected void connect(String ip, int port) {
	try {
	    appendToChat("Attempting to connect to "
		    + clientController.getHostname() + ":" + port);
	    clientController.setTitleConnected();
	    socket = new Socket(ip, port);
	    out = new ObjectOutputStream(socket.getOutputStream());
	    in = new ObjectInputStream(socket.getInputStream());
	    listener = new Thread(this);
	    // listener.setDaemon(true);
	    isConnected = true;
	    listener.start();
	} catch (IOException e) {
	    e.printStackTrace();
	    appendToChat("Server not running on "
		    + clientController.getHostname() + ':' + port
		    + " or check firewall");
	    // System.exit(-1);
	    clientController.showSettingsAndWait();
	}
    }

    protected void disconnect() {
	try {
	    appendToChat("Leaving chat room...");
	    clientController.setTitleDisconnected();
	    socket.close();
	    isConnected = false;
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    protected synchronized void send(String s) throws IOException {
	if (msgType.equals(Type.MESSAGE))
	    sendMessage(s);
	else if (msgType.equals(Type.AUTH))
	    sendAuth(s);
    }

    private void sendAuth(String s) throws IOException {
	clientController.clearMsgBar();
	out.writeUnshared(new Protocol(Type.AUTH, s));
    }

    private void sendMessage(String text) throws IOException {
	clientController.clearMsgBar();
	out.writeUnshared(new Protocol(Type.MESSAGE, text));
    }

    protected synchronized void appendToChat(String str) {
	clientController.appendToChatArea(str);
    }

    @Override
    public void run() {
	try {
	    while (isConnected) {
		Protocol p = (Protocol) in.readUnshared();
		handle(p);
	    }
	} catch (IOException  e) {
	    e.printStackTrace();
	} catch (ClassCastException | ClassNotFoundException e) {
	}
    }

    @SuppressWarnings({ "unchecked", "incomplete-switch" })
    private void handle(Protocol response) throws ClassCastException,
	    ClassNotFoundException, IOException {
	Object[] params = response.getContent();
	switch (response.getType()) {
	case NICK: {
	    this.nickname = (String) params[0];
	    break;
	}
	case CLIENT_PEER_CONNECTION_REQUEST: {
	    String nick = (String) params[0];
	    int port = (int) params[1];
	    Platform.runLater(new Runnable() {
		public void run() {
		    openPrivateSession(nick, port, false);
		}
	    });
	    break;
	}
	case UPDATE_NICK_LIST: {
	    updateNickList((HashSet<String>) params[0]);
	    break;
	}
	case MESSAGE: {
	    String s = (String) params[0];
	    appendToChat(s);
	    msgType = Type.MESSAGE;
	    break;
	}
	case AUTH: {
	    String s = (String) params[0];
	    appendToChat(s);
	    msgType = Type.AUTH;
	    break;
	}
	}
    }

    protected void openPrivateSession(String remoteNick, int port,
	    boolean isHost) {
	clientController.addPrivateChatTab(remoteNick, port, isHost);
    }

    void connectRemoteUser(String remoteNick, int port) throws IOException {
	out.writeUnshared(new Protocol(Protocol.Type.INITIATE_PEER_CONNECTION,
		port));
    }

    private synchronized void updateNickList(Set<String> o) {
	clientController.updateNickList(o);
    }

    public String getNickname() {
	return nickname;
    }

    protected boolean isConnected() {
	return isConnected;
    }
}
