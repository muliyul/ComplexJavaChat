package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashSet;

import comm.Protocol;

/**
 * Client handler for each client connected.
 * 
 * @author Muli
 *
 */
public class ClientHandler extends Thread {
    private static int IdGenerator = 1;
    @SuppressWarnings("unused")
    private int id;
    private Socket s;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Server server;
    private String nick;
    private boolean isConnected;
    private int peerPort;

    public ClientHandler(String nick, Socket s, ObjectInputStream in,
	    ObjectOutputStream out, Server server) throws IOException {
	this.id = IdGenerator++;
	this.s = s;
	this.server = server;
	this.out = out;
	this.in = in;
	this.nick = nick;
	// setDaemon(true);
    }

    @Override
    public void run() {
	isConnected = true;
	while (isConnected) {
	    try {
		Protocol p = (Protocol) in.readUnshared();
		handle(p);
	    } catch (IOException | ClassNotFoundException e) {
		try {
		    handle(new Protocol(Protocol.Type.I_QUIT));
		    isConnected = false;
		} catch (ClassNotFoundException | IOException e2) {
		}
		try {
		    s.close();
		} catch (IOException e1) {
		    e1.printStackTrace();
		}
	    }
	}
    }

    private void handle(Protocol p) throws IOException, ClassNotFoundException {
	Object[] params = p.getContent();
	switch (p.getType()) {
	case NICK: { // RECIEVE NICK REQUEST
	    out.writeUnshared(new Protocol(Protocol.Type.NICK, nick));
	    break; // return client's nick
	}
	case I_QUIT: { // RECIEVE QUIT ALERT
	    server.removeClient(nick);
	    s.close();
	    isConnected = false;
	    break;
	}
	case INITIATE_PEER_CONNECTION: { // RECIEVE PRIVATE CHAT HOST
	    String nickToSendTo = (String) params[0];
	    int port = (int) params[1];
	    server.initiatePeerConnection(nick, nickToSendTo, port);
	    break;
	}
	case CLIENT_PEER_CONNECTION_REQUEST: { // SEND PRIVATE CHAT HOST
	    out.writeUnshared(p);
	    break;
	}
	case UPDATE_NICK_LIST: {
	    out.writeUnshared(p);
	    break;
	}
	case MESSAGE: {
	    server.broadcastMessage(nick, (String) params[0]);
	    break;
	}
	default:
	    break;
	}
    }

    protected void sendMessage(String msg) throws IOException {
	out.writeUnshared(new Protocol(Protocol.Type.MESSAGE, msg));
    }

    protected String getHostname() {
	return s.getInetAddress().getHostAddress();
    }

    public int getPeerPort() {
	return peerPort;
    }

    protected void sendClientList(HashSet<String> set) throws IOException {
	out.writeUnshared(new Protocol(Protocol.Type.UPDATE_NICK_LIST, set));
    }

    protected void sendPrivateChatHostDetails(String nick, int port) throws IOException {
	out.writeUnshared(new Protocol(Protocol.Type.CLIENT_PEER_CONNECTION_REQUEST,
		nick, port));
    }

    protected void close() throws IOException {
	s.close();
    }
}
