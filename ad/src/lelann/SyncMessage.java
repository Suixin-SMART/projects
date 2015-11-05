package lelann;
import visidia.simulation.process.messages.Message;
import gui.Forme;
import java.util.LinkedList;

public class SyncMessage extends Message {

    MsgType type;
    int proc;
    int procTarget;
    LinkedList<Forme> forme;

    public SyncMessage() {}

    // Message REGISTER & TOKEN
    public SyncMessage( MsgType type, int proc) {
        this.type = type;
        this.proc = proc;
        this.procTarget = proc;
    }

    // Message END_REGISTER
    public SyncMessage( MsgType type, int proc, int procTarget) {
        this.type = type;
        this.proc = proc;
        this.procTarget = procTarget;
    }

    // Message FORME
    public SyncMessage( MsgType t, LinkedList<Forme> forme, int proc ) {
        type = t;
        this.forme = forme;
        procTarget = proc;
    }

    // Get Message Type
    public MsgType getMsgType() {

        return type;
    }

    // Get numero de processus
    public int getMsgProc() {

        return proc;
    }

    // Get numero du processus Target
    public int getMsgProcTarget() {
        return procTarget;
    }

    // Get la forme envoyée
    public LinkedList<Forme> getMsgForme() {

        return forme;
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
