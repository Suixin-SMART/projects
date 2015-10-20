package lelann;
import visidia.simulation.process.messages.Message;
import gui.Forme;
import java.util.LinkedList;

public class SyncMessage extends Message {
    
    MsgType type;
    int proc;
    LinkedList<Forme> forme;

    public SyncMessage() {}
    
    public SyncMessage( MsgType t, LinkedList<Forme> forme, int p ) {
	
	type = t;
	this.forme = forme;
	proc = p;
    }
    
    public MsgType getMsgType() {

	return type;
    }

    public LinkedList<Forme> getMsgForme() {

	return forme;
    }

    public int getMsgProc() {

	return proc;
    }
    
    @Override
    public Message clone() {
	return new SyncMessage();
    }
    
    @Override 
    public String toString() {

	String r = type.toString() + "_" + proc;
	return r;
    }

    @Override 
    public String getData() {

	return this.toString();
    }

}
