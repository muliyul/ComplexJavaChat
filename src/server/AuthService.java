package server;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Vector;

/**
 * Provides the authenticating stage of the connection handling multiple-users.
 * @author Muli Yulzary
 *
 */
public class AuthService {
    
    private Server server;
    private List<AuthServiceWorker> threads;

    public AuthService(Server s) {
	this.server = s;
	threads = new Vector<AuthServiceWorker>();
    }

    protected void closeAll() throws IOException {
	for (AuthServiceWorker t : threads) {
	   // t.sendError("Server is shutting down");
	    t.interrupt();
	}
    }

    protected synchronized void auth(Socket s) throws IOException {
	threads.add(new AuthServiceWorker(this,s));
    }
    
    protected Server getServer() {
	return server;
    }
    
    protected List<AuthServiceWorker> getThreads() {
	return threads;
    }

}
