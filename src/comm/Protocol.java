package comm;

/**
 * Protocols in use by both server and client
 * @author Muli
 *
 */
public enum Protocol {
    NICK,
    I_QUIT,
    INITIATE_PEER_CONNECTION,
    PEER_CONNECTION_REQUEST,
    UPDATE_NICK_LIST,
    MESSAGE,
    AUTH,
}
