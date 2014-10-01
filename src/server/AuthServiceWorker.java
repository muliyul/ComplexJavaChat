package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Vector;

import comm.Protocol;

/**
 * A worker for each client attempting to authenticate.
 * 
 * @author Muli Yulzary
 *
 */
public class AuthServiceWorker extends Thread {
    private Socket s;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private LocalDateTime startAuthDate;
    private AuthService as;

    public AuthServiceWorker(AuthService as, Socket s) throws IOException {
	this.s = s;
	this.as = as;
	out = new ObjectOutputStream(s.getOutputStream());
	in = new ObjectInputStream(s.getInputStream());
	startAuthDate = LocalDateTime.now();
	//setDaemon(true);
	start();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
	int n = 3;
	String nick = null;
	ClientHandler ch = null;
	List<Object> response = new Vector<>();
	List<Object> clientResponse;
	try {
	    do {
		response.add(Protocol.AUTH);

		if (n == 3)
		    response.add("AUTH: Enter nickname");
		else
		    response.add("AUTH: Enter nickname (" + n
			    + " tries left)");
		
		out.writeUnshared(response);
		
		clientResponse = (Vector<Object>) in.readUnshared();
		nick = (String) clientResponse.get(1);
		n--;
		response.clear();
	    } while (!as.getServer().checkNickIsValid(nick)
		    && n > 0
		    && LocalDateTime.now().isBefore(
			    startAuthDate.plusMinutes(1)));
	    if (n > 0) {
		response.add(Protocol.MESSAGE);
		response.add("Entering chat room as " + nick + "...");
		out.writeUnshared(response);
		response.clear();
		response.add(Protocol.NICK);
		response.add(nick);
		out.writeUnshared(response);
		response.clear();
		as.getServer().addClient(
			nick,
			ch =
				new ClientHandler(nick, s, in, out, as
					.getServer()));
	    } else {
		response.add(Protocol.MESSAGE);
		response.add("AUTH: Terminating connection...");
		out.writeUnshared(response);
		s.close();
	    }
	} catch (IOException | ClassNotFoundException e) {
	    // e.printStackTrace();
	}
	as.getThreads().remove(this);
	if (ch != null)
	    ch.start();
    }

    /*public void sendError(String string) throws IOException {
	out.writeObject(Protocol.MESSAGE);
	out.writeUTF(string);
    }*/
}
