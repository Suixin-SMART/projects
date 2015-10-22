package lelann;
// Visidia imports
import visidia.simulation.process.algorithm.Algorithm;
import visidia.simulation.process.messages.Door;

// Reception thread
public class ReceptionRules extends Thread {
    
    LeLannMutualExclusion algo;
    
    public ReceptionRules( LeLannMutualExclusion a ) {
	
	algo = a;
    }
    
    public void run() {
	
	Door d = new Door();

	while( true ) {

	    SyncMessage m = (SyncMessage) algo.recoit(d);
	    int door = d.getNum();

	    switch (m.getMsgType()) {
		
	    case JETON :
		algo.receiveJETON( m.getMsgForme(), m.getMsgProc(), door );
		break;
		
	    default:
		System.out.println("Error message type");
	    }
	}
    }
}
