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
    private Protocol preparedProtocol;

    // GUI RELATED
    private ClientController clientController;

    public Client() {
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
	FXMLLoader loader =
		new FXMLLoader(getClass().getResource("ClientGUI.fxml"));
	clientController = new ClientController("Muli's chat GUI ver.01a", primaryStage, this);
	loader.setController(clientController);
	primaryStage.setScene(new Scene(loader.load()));
	primaryStage.setResizable(false);
	primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
	    public void handle(WindowEvent event) {
		try {
		    out.writeUnshared(new Protocol(Protocol.Type.I_QUIT));
		    socket.close();
		} catch (IOException e) {
		    //e.printStackTrace();
		}
	    }
	});
	primaryStage.show();
	connect(clientController.getHostname(), clientController.getPort());
    }

    protected void connect(String ip, int port) {
	boolean again;
	do {
	    again = false;
	    try {
		appendToChat("Attempting to connect to "
			+ clientController.getHostname() + ":" + port);
		clientController.setTitleConnected();
		socket = new Socket(ip, port);
		out = new ObjectOutputStream(socket.getOutputStream());
		in = new ObjectInputStream(socket.getInputStream());
	    } catch (IOException e) {
		appendToChat("Server not running on "
			+ clientController.getHostname() + ':' + port
			+ " or check firewall");
		System.exit(-1);
		clientController.showSettingsAndWait();
		again = true;
	    }
	} while (again);

	listener = new Thread(this);
	// listener.setDaemon(true);
	listener.start();
    }

    protected void disconnect() {
	try {
	    appendToChat("Leaving chat room...");
	    clientController.setTitleDisconnected();
	    socket.close();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    protected void sendMessage(String text) throws IOException {
        preparedProtocol.setContent(text);
        clientController.clearMsgBar();
        out.writeUnshared(preparedProtocol);
    }

    protected synchronized void appendToChat(String str) {
        clientController.appendToChatArea(str);
    }

    @Override
    public void run() {
	isConnected = true;
	try {
	    while (isConnected) {
		handle((Protocol) in.readUnshared());
	    }
	} catch (IOException | ClassNotFoundException e) {
	} catch (ClassCastException e) {
	    e.printStackTrace();
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
	    preparedProtocol = new Protocol(Type.MESSAGE);
	    break;
	}
	case AUTH: {
	    String s = (String) params[0];
	    appendToChat(s);
	    preparedProtocol = new Protocol(Type.AUTH);
	    break;
	}
	}
    }

    protected void openPrivateSession(String remoteNick, int port, boolean isHost) {
        FXMLLoader loader =
        	new FXMLLoader(getClass().getResource("PrivateTab.fxml"));
        try {
            PrivateChat pc = new PrivateChat(remoteNick, port, isHost);
            loader.setController(pc);
            if (isHost) {
        	connectRemoteUser(remoteNick, port);
            }
            Tab privateChat = new Tab(remoteNick);
            loader.setRoot(privateChat);
            loader.load();
            clientController.addPrivateChatTab(privateChat);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connectRemoteUser(String remoteNick, int port) throws IOException {
        out.writeUnshared(new Protocol(Protocol.Type.INITIATE_PEER_CONNECTION, port));
    }

    private synchronized void updateNickList(Set<String> o) {
	clientController.updateNickList(o);
    }

    protected boolean isConnected() {
	return isConnected;
    }

    protected void setConnected(boolean b) {
	isConnected = b;
    }

    public String getNickname() {
	return nickname;
    }
}
