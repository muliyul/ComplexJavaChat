package client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import comm.PeerProtocol;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

public class PrivateChat extends Thread {

    @FXML
    private TextArea chatArea;

    @FXML
    private TextField msgBar;

    @FXML
    private Button sendButton;

    private static int port = 2300;
    private ServerSocket serverSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    @SuppressWarnings("unused")
    private boolean isConnected;
    private String nickname;
    private boolean isHost;
    private String remoteHost;
    private int remotePort;
    private Socket remote;

    public PrivateChat(String nick, int remotePort, boolean isHost) {
	this.remotePort = remotePort;
	this.isHost = isHost;
	this.nickname = nick;
    }

    public void send(ActionEvent event) {
	if (!msgBar.getText().equals("") || msgBar.getText() != null) {
	    try {
		send(msgBar.getText());
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	    event.consume();
	}
    }

    private void send(String str) throws IOException {
	if (out != null) {
	    append(nickname + ": " + str);
	    out.writeUnshared(new PeerProtocol(PeerProtocol.Type.MESSAGE,
		    nickname + ": " + str));
	    msgBar.clear();
	} else {
	    append("** Connection has not been established yet! **");
	}
    }

    public void sendBar(KeyEvent event) {
	if (event.getCode().equals(KeyCode.ENTER))
	    if (!msgBar.getText().equals("") || msgBar.getText() != null) {
		try {
		    send(msgBar.getText());
		} catch (IOException e) {
		    e.printStackTrace();
		}
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
	try {
	    if (isHost) {
		boolean portTaken;
		do {
		    portTaken = false;
		    try {
			this.serverSocket = new ServerSocket(port++);
		    } catch (IOException e1) {
			portTaken = true;
		    }
		} while (portTaken);
		remote = serverSocket.accept();
	    } else
		try {
		    remote = new Socket(remoteHost, remotePort);
		} catch (IOException e) {
		    append("** Unable to connect to remote! please try again! **");
		}
	    out =
		    new ObjectOutputStream(new BufferedOutputStream(
			    remote.getOutputStream()));
	    in =
		    new ObjectInputStream(new BufferedInputStream(
			    remote.getInputStream()));
	} catch (IOException e) {
	}

	while (isConnected = true) {
	    try {
		handle((PeerProtocol) in.readUnshared());
	    } catch (NullPointerException | IOException
		    | ClassNotFoundException e) {
	    }
	}

	if (!isHost) {
	    try {
		remote.close();
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
    }

    private void handle(PeerProtocol peerProtocol)
	    throws ClassNotFoundException, IOException {
	Object[] params = peerProtocol.getContent();
	switch (peerProtocol.getType()) {
	case FILE: {
	    String fileName = (String) params[0];
	    long size = (long) params[1];
	    try {
		recieveFile(fileName, (int) size);
	    } catch (InterruptedException e) {
		e.printStackTrace();
	    }
	    break;
	}
	case MESSAGE: {
	    append((String) params[0]);
	    break;
	}
	default:
	    break;
	}
    }

    private String showFileSaveDialog() {
	return "G:/Desktop";
    }

    public static int getPort() {
	return port;
    }

    protected void close() throws IOException {
	if (isHost) {
	    serverSocket.close();
	}
	isConnected = false;
    }

    public void openSendFileDialog() throws IOException {
	FXMLLoader loader =
		new FXMLLoader(getClass().getResource("SendFileDialog.fxml"));
	Stage sfd = new Stage();
	loader.setController(new SendFileDialogController(sfd, this));
	sfd.setTitle("Send Files...");
	sfd.setScene(new Scene(loader.load()));
	sfd.show();
    }

    protected void sendFile(SendFileDialogController sendFileDialogController,
	    File file) throws InterruptedException {
	new Thread() {
	    @Override
	    public void run() {
		if (file.length() > Integer.MAX_VALUE) {
		    append("File too large! (2GB)");
		}
		try {
		    InputStream buf =
			    new BufferedInputStream(new FileInputStream(file));
		    out.writeUnshared(new PeerProtocol(PeerProtocol.Type.FILE,
			    file.getName(), file.length()));
		    int sum = 0, len;
		    byte[] buffer = new byte[remote.getSendBufferSize()];
		    out.flush();
		    out.reset();
		    while ((len = buf.read(buffer)) > 0) {
			out.write(buffer, 0, len);
			sum += len;
			sendFileDialogController.updatePercentage((double) sum
				/ file.length() * 100);
		    }
		    out.flush();
		    System.out.println("done sending");
		    sendFileDialogController.updatePercentage(100);
		    buf.close();
		} catch (IOException e) {
		    e.printStackTrace();
		} finally {
		    synchronized (PrivateChat.this) {
			PrivateChat.this.notify();
		    }
		}
	    }
	}.start();
	synchronized (this) {
	    wait();
	}
    }

    private void recieveFile(String fileName, int size)
	    throws InterruptedException {
	new Thread() {
	    @Override
	    public void run() {
		try {
		    String location = showFileSaveDialog();
		    OutputStream os =
			    new FileOutputStream(new File(location + '/'
				    + fileName), false);
		    byte[] buffer = new byte[remote.getReceiveBufferSize()];
		    int len;
		    while ((len = in.read(buffer)) < size) {
			os.write(buffer, 0, len);
		    }
		    System.out.println("**********RAF CLOSED");
		    os.close();
		} catch (IOException e) {
		    e.printStackTrace();
		} finally {
		    synchronized (PrivateChat.this) {
			PrivateChat.this.notify();
		    }
		}
	    }
	}.start();
	synchronized (this) {
	    wait();
	}
    }
}
