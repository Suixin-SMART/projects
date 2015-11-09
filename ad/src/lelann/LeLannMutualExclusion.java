package lelann;

// Java imports
import java.io.*;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ThreadFactory;
import java.util.logging.*;

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
    DisplayFrame tableau;

    public Object bla = new Object();

    int routeur[];
    boolean initRouteur[];
    boolean initialized = true;
    Writer writer;

    private static final Logger log = Logger.getLogger( LeLannMutualExclusion.class.getName() );


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

        Handler handler = null;
        try {
            handler = new FileHandler( "LeLann_"+procId+".log");
            log.setLevel(Level.INFO);
            SingleLineFormatter formatter = new SingleLineFormatter();
            handler.setFormatter(formatter);
            Logger.getLogger("").addHandler(handler);
        } catch (IOException e) {
            e.printStackTrace();
        }

        int speed = 4;
        procId = getId();
        nbNeighbors = getArity();
        totalProcessus = getNetSize();
        procNeighbor = (procId + 1) % totalProcessus;

        rr = new ReceptionRules( this );
        rr.start();

        log.info("Process " + procId + " as " + nbNeighbors + " neighbors");

        routeur = new int[totalProcessus];
        initRouteur = new boolean[totalProcessus];
        for(int i = 0;i<totalProcessus;i++){
            routeur[i] = -1;
            initRouteur[i] = false;
        }
        routeur[procId] = 0;
        initRouteur[procId] = true;
        log.info("Envoi de message d'identification aux autres processus.");
        for (int i = 0; i < nbNeighbors; i++)
        {
            SyncMessage message = new SyncMessage(MsgType.REGISTER, procId);
            sendTo(i, message);
        }

        log.info("Debut table de routage");

        while(initialized)
        {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }
        }

        log.info("Fin table de routage");

        tableau = new DisplayFrame(procId, this);

        displayRoutage();


        log.info("Debut de la circulation du token");


        if ( procId == 0 ) {
            log.info("Processus initiateur");
            token = false;
            SyncMessage tm = new SyncMessage(MsgType.TOKEN, procNeighbor);
            boolean sent = sendTo( routeur[procNeighbor], tm );
        }

        while( true ) {

            // Try to access critical section
            System.out.println("Section critique: " + tableau.demandeSectionCritique);
            askForCritical();

            // Access critical
            log.info("Entree en Section Critique");
            tableau.inSectionCritique = true;
            synchronized (bla) {
                displayState();
                tableau.continuerSectionCritique();

                try {
                    bla.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Release critical use
            log.info("Fin de Section Critique");
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
        System.out.println("JETON : " + p);

        if (p == procId)
        {
            // Jeton pour moi !
            if ( tableau.getDemandeSectionCritique() )
            {
                System.out.println("Proc: " + procId + " got token!");
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

    // Rule 4 : receive FORME
    synchronized void receiveFORME( Forme forme, int pInitiateur, int pTarget, int d)
    {
        SyncMessage tm;

        if (procId == pTarget) {
            LinkedList<Forme> canvasList = tableau.canvas.getFormes();
            boolean changed = false;
            if (!canvasList.contains(forme)) {
                System.out.println("-------> Updated Canvas!!! ProcID: " + procId + " Vers: " + pTarget);
                tableau.canvas.delivreForme(forme);
            }


            if (pInitiateur != procNeighbor) {
                tm = new SyncMessage(MsgType.FORME, forme, pInitiateur, procNeighbor);
                sendTo(routeur[procNeighbor], tm);
            }
        }
        else
        {
            tm = new SyncMessage(MsgType.FORME, forme, pInitiateur, pTarget);
            sendTo(routeur[pTarget], tm);
        }
    }


    // Rule 3 :
    void endCriticalUse()
    {
      token = false;
        SyncMessage tm;

        if (tableau.canvas.getFormes().size() > 0) {
            for (int i = 0; i < totalProcessus; i++) {
                tm = new SyncMessage(MsgType.FORME, tableau.canvas.getFormes().getLast(), procId, procNeighbor);
                sendTo(routeur[procNeighbor], tm);
            }
        }


        tm = new SyncMessage(MsgType.TOKEN, procNeighbor);
        sendTo( routeur[procNeighbor], tm );

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

        log.info(b);
    }

    // Display state
    void displayState()
    {

    }
}
