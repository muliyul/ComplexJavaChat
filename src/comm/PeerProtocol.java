package comm;

import java.io.Serializable;

public class PeerProtocol implements Serializable{
    private static final long serialVersionUID = 4761042184525082668L;

    public enum Type {
	MESSAGE, FILE
    }

    private Type type;
    private Object[] content;

    public PeerProtocol(Type type, Object... content) {
	this.type = type;
	this.content = content;
    }

    public void setContent(Object... content) {
	this.content = content;
    }

    public Type getType() {
	return type;
    }

    public Object[] getContent() {
	return content;
    }
}
