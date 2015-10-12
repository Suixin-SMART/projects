package lelann;

// Java imports
import java.util.Queue;
import java.util.LinkedList;
import java.util.Random;
import gui.*;

// Visidia imports
import visidia.simulation.process.algorithm.Algorithm;
import visidia.simulation.process.messages.Door;

public class LeLannMutualExclusion extends Algorithm {
    
    // All nodes data
    int procId;
    int nbNeighbors;

    // To display the state
    boolean waitForCritical = false;
    boolean inCritical = false;

    // Reception thread
    ReceptionRules rr = null;
    // State display frame
    TableauBlancUI whiteBoard;

    public String getDescription() {

	return ("LeLann Algorithm for Mutual Exclusion");
    }

    @Override
    public Object clone() {
	return new LeLannMutualExclusion();
    }

    //
    // Nodes' code
    //
    @Override
    public void init() {

	procId = getId();
	Random rand = new Random( procId );

	rr = new ReceptionRules( this );
	rr.start();

	nbNeighbors = getArity();
	System.out.println("Process " + procId + " as " + nbNeighbors + " neighbors");	
	//F_H = new int[nbNeighbors+1];
	//for ( int i = 0 ; i < (nbNeighbors +1) ; i++ ) F_H[i] = 0;
	//F_M = new MsgType[nbNeighbors+1];

	// Display initial state
	whiteBoard = new TableauBlancUI();
	displayState();
	try { Thread.sleep( 15000 ); } catch( InterruptedException ie ) {}

	while( true ) {
	    
	    // Wait for some time before simulation
	    int time = 0;
	    time = ((procId + time) * 20000) + 1000;
	    System.out.println("Process " + procId + " wait for " + time);
	    try {
		Thread.sleep( time );
	    } catch( InterruptedException ie ) {}
	    
	    // Try to access critical section
	    waitForCritical = true;
	    askForCritical();
	    waitForCritical = false;
	    inCritical = true;
	    displayState();

	    // Simulate critical resource use
	    time = (1 + rand.nextInt(2)) * 1000;
	    System.out.println("Process " + procId + " enter SC " + time);
	    try {
		Thread.sleep( time );
	    } catch( InterruptedException ie ) {}
	    System.out.println("Process " + procId + " exit SC ");

	    // Release critical use
	    inCritical = false;
	    endCriticalUse();
	}
    }

    //--------------------
    // Rules
    //-------------------

    // Rule 1 : ask for critical section
    synchronized void askForCritical() {

    }

    // Rule 2 : receive REQ
    synchronized void receiveJETON( LinkedList<Forme> forme, int p, int d){

    }

    // Rule 3 :
    void endCriticalUse() {

    }

    // Access to receive function
    public SyncMessage recoit ( Door d ) {

		SyncMessage sm = (SyncMessage)receive( d );
		return sm;
    }

    // Display state
    void displayState() {

    }
}
