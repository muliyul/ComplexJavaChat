package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class PrivateChat extends Thread {
    @FXML
    private TextArea chatArea;

    @FXML
    private TextField msgBar;

    @FXML
    private Button sendButton;

    private ServerSocket s;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean isConnected;
    private String nick;
    private boolean isHost;
    private String remoteHost;
    private int remotePort;

    public PrivateChat(String nick, boolean isHost, int remotePort) {
	this.remotePort = remotePort;
	int port = 2300;
	boolean portTaken;
	if (this.isHost = isHost) {
	    do {
		portTaken = false;
		try {
		    this.s = new ServerSocket(port++);
		} catch (IOException e1) {
		    portTaken = true;
		}
	    } while (portTaken);
	}
	this.nick = nick;
    }

    public void send(ActionEvent event) {
	if (!msgBar.getText().equals("") || msgBar.getText() != null) {
	    try {
		send(nick + msgBar.getText());
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	    event.consume();
	}
    }

    public void sendBar(KeyEvent event) {
	if (event.getCode().equals(KeyCode.ENTER))
	    if (!msgBar.getText().equals("") || msgBar.getText() != null) {
		try {
		    send(nick + msgBar.getText());
		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }
	event.consume();
    }

    private void send(String str) throws IOException {
	if (out != null) {
	    append(nick + str);
	    out.writeObject(nick + str);
	}
    }

    private void append(String str) {
	Platform.runLater(new Runnable() {
	    public void run() {
		chatArea.appendText("[ "
			+ LocalTime.now().format(
				DateTimeFormatter.ofPattern("H:m:s")) + " ] "
			+ str + System.getProperty("line.separator"));
	    }
	});
    }

    @Override
    public void run() {
	if (isHost) {
	    try {
		Socket remote = s.accept();
		isConnected = true;
		out = new ObjectOutputStream(remote.getOutputStream());
		in = new ObjectInputStream(remote.getInputStream());
		while (isConnected) {
		    String msg;
		    try {
			msg = (String) in.readObject();
			append(msg);
		    } catch (IOException | ClassNotFoundException e) {
		    }
		}
	    } catch (IOException e) {
	    }
	} else {
	    try {
		Socket remote = new Socket(remoteHost, remotePort);
		out = new ObjectOutputStream(remote.getOutputStream());
		in = new ObjectInputStream(remote.getInputStream());
		while (isConnected) {
		    String msg;
		    try {
			msg = (String) in.readObject();
			append(msg);
		    } catch (IOException | ClassNotFoundException e) {
		    }
		}
		remote.close();
	    } catch (IOException e) {
	    }
	}
    }

    public int getPort() {
	if (s != null)
	    return s.getLocalPort();
	return -1;
    }
}
