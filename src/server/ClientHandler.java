package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

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

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
	isConnected = true;
	Vector<Object> response;
	while (isConnected) {
	    try {
		response = (Vector<Object>) in.readUnshared();
		handle(response);
	    } catch (IOException | ClassNotFoundException e) {
		try {
		    Vector<Object> v = new Vector<Object>();
		    v.add(Protocol.I_QUIT);
		    handle(v);
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

    @SuppressWarnings({ "incomplete-switch", "unchecked" })
    private void handle(List<Object> res) throws IOException,
	    ClassNotFoundException {
	List<Object> resposne = new Vector<>();
	switch ((Protocol) res.get(0)) {
	case NICK: {
	    resposne.add(Protocol.NICK);
	    resposne.add(nick);
	    out.writeUnshared(resposne);
	    break;
	}
	case I_QUIT: {
	    server.broadcastMessage(nick, "has left.");
	    server.removeClient(nick);
	    s.close();
	    isConnected = false;
	    break;
	}
	case INITIATE_PEER_CONNECTION: {
	    String nick = (String) res.get(1);
	    int port = (int) res.get(2);
	    server.initiatePeerConnection(nick, port);
	    break;
	}
	case UPDATE_NICK_LIST: {
	    resposne.add(Protocol.UPDATE_NICK_LIST);
	    resposne.add((HashSet<String>) in.readObject());
	    out.writeUnshared(resposne);
	}
	case PEER_CONNECTION_REQUEST: {
	    resposne.add(Protocol.PEER_CONNECTION_REQUEST);
	    resposne.add(res.get(1)); // remote nick
	    resposne.add(res.get(2)); // remote port
	    out.writeUnshared(resposne);
	    break;
	}
	case MESSAGE: {
	    server.broadcastMessage(nick, (String) res.get(1));
	    break;
	}
	}
    }

    protected void sendMessage(String msg) throws IOException {
	Vector<Object> resposne = new Vector<Object>();
	resposne.add(Protocol.MESSAGE);
	resposne.add(msg);
	out.writeUnshared(resposne);
    }

    protected String getHostname() {
	return s.getInetAddress().getHostAddress();
    }

    public int getPeerPort() {
	return peerPort;
    }

    protected void sendClientList(HashSet<String> set) throws IOException {
	List<Object> response = new Vector<Object>();
	response.add(Protocol.UPDATE_NICK_LIST);
	response.add(set);
	out.writeUnshared(response);
    }

    protected void sendPort(String nick, int port) throws IOException {
	List<Object> response = new Vector<Object>();
	response.add(Protocol.PEER_CONNECTION_REQUEST);
	response.add(nick);
	response.add(port);
	out.writeUnshared(response);
    }

    protected void close() throws IOException {
	s.close();
    }
}
