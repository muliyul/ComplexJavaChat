package server;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class Server extends Application implements Runnable {
    protected Logger logger;

    private Thread listener;
    private AuthService as;
    private boolean isRunning;
    private ServerSocket serversocket;
    private HashMap<String, ClientHandler> handlers;

    public static void main(String[] args) throws IOException {
	launch();
    }

    public Server() throws IOException {
	this(5050);
    }

    public Server(int port) throws IOException {
	serversocket = new ServerSocket(port);
	handlers = new HashMap<String, ClientHandler>();
	handlers.put("@Auth", null);
	as = new AuthService(this);
	logger = Logger.getLogger("server_" + port);
	logger.setUseParentHandlers(false);
	FileHandler fh;
	if(!new File("logs").exists())
	    new File("logs").mkdir();
	logger.addHandler(fh =
		new FileHandler("logs\\server_" + port + ".log", false));
	fh.setFormatter(new Formatter() {
	    public String format(LogRecord record) {
		return LocalDateTime.now().format(
			DateTimeFormatter.ofPattern("[dd.MM.uu HH:mm:ss]"))
			+ " - "
			+ record.getLevel()
			+ ": "
			+ record.getMessage()
			+ System.getProperty("line.separator")
			+ System.getProperty("line.separator");
	    }
	});
	restart();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
	FXMLLoader loader =
		new FXMLLoader(getClass().getResource("Server.fxml"));
	ServerGUIController sgc;
	loader.setController(sgc = new ServerGUIController(this));
	primaryStage.setTitle("Muli's multi-client chat server ver.01a");
	primaryStage.setScene(new Scene(loader.load()));
	primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
	    public void handle(WindowEvent event) {
		stop();
	    }
	});
	sgc.setLabel("Server is running on port " + serversocket.getLocalPort());
	primaryStage.setIconified(true);
	primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/Resources/MuliChat.bmp")));
	primaryStage.show();
    }

    @Override
    public void run() {
	isRunning = true;
	System.out.println("Server running on port " + serversocket.getLocalPort());
	logger.info("Server started running on port "
		+ serversocket.getLocalPort() + '.');
	while (isRunning) {
	    try {
		Socket s = serversocket.accept();
		as.auth(s);
	    } catch (IOException e) {
	    }
	}
    }

    public void stop() {
	logger.info("Server shutting down...");
	try {
	    broadcastMessage("Auth", "Server shutting down! bye bye!");
	    serversocket.close();
	    as.closeAll();
	    closeAll();
	} catch (IOException e) {
	}
	if (isRunning) {
	    isRunning = false;
	    System.out.println("Server is not listening");
	}
    }

    private void closeAll() throws IOException {
	for (ClientHandler ch : handlers.values()) {
	    if (ch != null)
		ch.close();
	}
    }

    public void restart() {
	listener = new Thread(this);
	listener.start();
    }

    protected synchronized void broadcastMessage(String nick, String msg)
	    throws IOException {
	for (ClientHandler ch : handlers.values()) {
	    if (ch != null) {
		ch.sendMessage(nick + ": " + msg);
	    }
	}
    }

    protected synchronized void broadcastClientList() throws IOException {
	logger.info("Broadcasting client list...");
	for (ClientHandler ch : handlers.values()) {
	    if (ch != null) {
		ch.sendClientList(new HashSet<String>(handlers.keySet()));
	    }
	}
    }

    protected synchronized void addClient(String nick, ClientHandler ch)
	    throws IOException {
	logger.info("Client " + nick + " authenticated and connected.");
	handlers.put(nick, ch);
	broadcastMessage("Auth", nick + " has joined the chat.");
	broadcastClientList();
    }

    protected synchronized void removeClient(String nick) throws IOException {
	handlers.remove(nick);
	broadcastMessage("Auth", nick + " has left.");
	broadcastClientList();
    }

    public boolean checkNickIsValid(String nick) {
	return !nickInUse(nick) && passNickValidation(nick);
    }

    private boolean nickInUse(String nick) {
	Set<String> set = handlers.keySet();
	if (set.contains(nick))
	    return true;
	return false;
    }

    private boolean passNickValidation(String nick) {
	if (nick.equalsIgnoreCase("auth") || nick.equals("")
		|| nick.length() < 4)
	    return false;
	return true;
    }

    public boolean isRunning() {
	return isRunning;
    }

    protected void initiatePeerConnection(String host, String remote, int port)
	    throws IOException {
	handlers.get(remote).sendPrivateChatHostDetails(host, port);
    }

    public int getLocalPort() {
	return serversocket.getLocalPort();
    }
}
