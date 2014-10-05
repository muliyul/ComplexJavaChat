package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;

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
	out =
		new ObjectOutputStream(s.getOutputStream());
	in = new ObjectInputStream(s.getInputStream());
	startAuthDate = LocalDateTime.now();
	// setDaemon(true);
	start();
    }

    @Override
    public void run() {
	int n = 3;
	String nick = null;
	ClientHandler ch = null;
	try {
	    do {
		String s;

		if (n == 3)
		    s = "AUTH: Enter nickname";
		else
		    s = "AUTH: Enter nickname (" + n + " tries left)";

		out.writeUnshared(new Protocol(Protocol.Type.AUTH, s));

		Protocol clientResponse = (Protocol) in.readUnshared();
		nick = (String) clientResponse.getContent()[0];
		n--;
	    } while (!as.getServer().checkNickIsValid(nick)
		    && n > 0
		    && LocalDateTime.now().isBefore(
			    startAuthDate.plusMinutes(1)));
	    if (n > 0) {
		out.writeUnshared(new Protocol(Protocol.Type.MESSAGE,
			"Entering chat room as " + nick + "..."));
		out.writeUnshared(new Protocol(Protocol.Type.NICK, nick));
		as.getServer().addClient(
			nick,
			ch =
				new ClientHandler(nick, s, in, out, as
					.getServer()));
	    } else {
		out.writeUnshared(new Protocol(Protocol.Type.MESSAGE,
			"AUTH: Terminating connection..."));
		s.close();
	    }
	} catch (NullPointerException | IOException | ClassNotFoundException e) {
	    // e.printStackTrace();
	}
	as.getThreads().remove(this);
	if (ch != null)
	    ch.start();
    }
}
