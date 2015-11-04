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
    int procNeighbor;

    int nbNeighbors;
    int totalProcessus;
    boolean token = false;

    // To display the state
    boolean waitForCritical = false;
    boolean inCritical = false;

    // Reception thread
    ReceptionRules rr = null;
    // State display frame
    TableauBlancUI whiteBoard;

    int routeur[];
    boolean initRouteur[];
    boolean initialized = true;

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
    public void init()
    {
        int speed = 4;
        procId = getId();
        Random rand = new Random( procId );


        rr = new ReceptionRules( this );
        rr.start();

        nbNeighbors = getArity();
        System.out.println("Process " + procId + " as " + nbNeighbors + " neighbors");

        totalProcessus = getNetSize();
        routeur = new int[totalProcessus];
        initRouteur = new boolean[totalProcessus];
        for(int i = 0;i<totalProcessus;i++){
            routeur[i] = -1;
            initRouteur[i] = false;
        }
        routeur[procId] = 0;
        initRouteur[procId] = true;
        for (int i = 0; i < nbNeighbors; i++)
        {
            SyncMessage message = new SyncMessage(MsgType.REGISTER, procId);
            sendTo(i, message);
        }

        while(initialized)
        {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        displayRoutage();
        procNeighbor = (procId + 1) % totalProcessus;

        if ( procId == 0 ) {
            token = false;
            SyncMessage tm = new SyncMessage(MsgType.TOKEN, procNeighbor);
            boolean sent = sendTo( routeur[procNeighbor], tm );
        }

        while( true ) {

            // Wait for some time
            int time = ( 3 + rand.nextInt(10)) * speed * 1000;
            System.out.println("Process " + procId + " wait for " + time);
            try {
                Thread.sleep( time );
            } catch( InterruptedException ie ) {}

            // Try to access critical section
            waitForCritical = true;
            askForCritical();

            // Access critical
            waitForCritical = false;
            inCritical = true;

            displayState();

            // Simulate critical resource use
            time = (1 + rand.nextInt(3)) * 1000;
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
    synchronized void askForCritical()
    {
      while( !token )
      {
          displayState();
          try { this.wait(); } catch( InterruptedException ie) {}
      }
    }

    // Rule 1 : receive REGISTER
    synchronized void receiveREGISTER( int p, int d)
    {

        if (-1 == routeur[p])
        {
            routeur[p] = d;
            for (int i = 0; i < nbNeighbors; i++)
            {
                SyncMessage message = new SyncMessage(MsgType.REGISTER, p);
                sendTo(i, message);
            }

            int nbNonInitialized = 0;
            for (int i = 0; i < totalProcessus; i++)
            {
                if (-1 != routeur[i])
                {
                    nbNonInitialized++;
                }
            }

            if (nbNonInitialized == totalProcessus)
            {
                // Send End to All
                for (int i = 0; i < totalProcessus; i++) {
                    SyncMessage message = new SyncMessage(MsgType.END_REGISTER, procId, i);
                    sendTo(routeur[i], message);
                }
            }
        }
    }

    // Rule 2 : receive REGISTER
    synchronized void receiveEND_REGISTER( int p, int procTarget, int d)
    {
        if (procTarget == procId)
        {
            initRouteur[p] = true;
        }
        else
        {
            SyncMessage message = new SyncMessage(MsgType.END_REGISTER, p, procTarget);
            sendTo(routeur[procTarget], message);
        }

        int nbInitialized = 0;

        for (int i = 0; i < totalProcessus; i++)
        {
            if (-1 != routeur[i])
            {
                nbInitialized++;
            }
        }

        if (totalProcessus == nbInitialized)
        {
            initialized = false;
        }
    }

    // Rule 3 : receive TOKEN
    synchronized void receiveTOKEN( int p, int d)
    {
        if (p == procId)
        {
            // Jeton pour moi !
            if ( waitForCritical == true )
            {
                token = true;
                displayState();
                notify();
            }
            else
            {
                // Forward token to successor
                SyncMessage tm = new SyncMessage(MsgType.TOKEN, procNeighbor);
                boolean sent = sendTo( routeur[procNeighbor], tm );
            }
        }
        else
        {
            SyncMessage tm = new SyncMessage(MsgType.TOKEN, p);
            sendTo( routeur[p], tm );
        }
    }

    // Rule 3 :
    void endCriticalUse()
    {
      token = false;
      SyncMessage tm = new SyncMessage(MsgType.TOKEN, procNeighbor);
      boolean sent = sendTo( routeur[procNeighbor], tm );
    }

    // Access to receive function
    public SyncMessage recoit ( Door d )
    {
        SyncMessage sm = (SyncMessage)receive( d );
        return sm;
    }

    // Display routage
    void displayRoutage()
    {
        String b = "Proc : " + procId + '\n';

        for (int i = 0; i < totalProcessus;i++){
            b = b + '\t' + "Vers " + i + " : " + routeur[i] + '\n';
        }

        System.out.println(b);
    }

    // Display state
    void displayState()
    {

    }
}
