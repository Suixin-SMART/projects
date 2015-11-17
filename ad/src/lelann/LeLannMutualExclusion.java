package lelann;

// Java imports
import java.io.*;
import java.util.LinkedList;
import java.util.logging.*;

import commun.*;
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

    // Reception thread
    ReceptionRules rr = null;
    // State display frame
    DisplayFrame tableau;

    private Object objectSync = new Object();

    int routeur[];
    boolean initialized = true;

    private Logger log;

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
        nbNeighbors = getArity();
        totalProcessus = getNetSize();
        procNeighbor = (procId + 1) % totalProcessus;

        log = Logger.getLogger( "" + procId );

        Handler handler = null;
        try {
            handler = new FileHandler( "LeLann_"+procId+".log");
            log.setLevel(Level.INFO);
            SimpleFormatter formatter = new SimpleFormatter();
            handler.setFormatter(formatter);
            Logger.getLogger("" + procId).addHandler(handler);
        } catch (IOException e) {
            e.printStackTrace();
        }

        rr = new ReceptionRules( this );
        rr.start();

        log.info("Process " + procId + " as " + nbNeighbors + " neighbors");

        routeur = new int[totalProcessus];
        for(int i = 0;i<totalProcessus;i++){
            routeur[i] = -1;
        }
        routeur[procId] = 0;
        for (int i = 0; i < nbNeighbors; i++)
        {
            SyncMessage message = new SyncMessage(MsgType.REGISTER, procId);
            sendTo(i, message);
            log.info("Envoi REGISTER a " + i);
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

        tableau = new DisplayFrame(procId, objectSync);

        displayRoutage();


        log.info("Debut de la circulation du token");


        if ( procId == 0 ) {
            log.info("Processus initiateur");
            token = false;
            SyncMessage tm = new SyncMessage(MsgType.TOKEN, procNeighbor);
            boolean sent = sendTo( routeur[procNeighbor], tm );
            log.info("Envoi du TOKEN a " + procNeighbor);
        }

        while( true ) {

            // Try to access critical section
            askForCritical();

            // Access critical
            log.info("Entree en Section Critique");
            tableau.inSectionCritique = true;
            synchronized (objectSync) {
                displayState();
                tableau.continuerSectionCritique();

                try {
                    objectSync.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            tableau.demandeSectionCritique = false;
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
        log.info("Demande Section Critique");
        while( !token )
        {
            displayState();
            try { this.wait(); } catch( InterruptedException ie) {}
        }
    }

    // Rule 1 : receive REGISTER
    synchronized void receiveREGISTER( int procAuteur, int door)
    {
        log.info("Recu REGISTER de " + procAuteur);
        if (-1 == routeur[procAuteur])
        {
            routeur[procAuteur] = door;
            for (int i = 0; i < nbNeighbors; i++)
            {
                SyncMessage message = new SyncMessage(MsgType.REGISTER, procAuteur);
                sendTo(i, message);
                log.info("Envoi REGISTER a " + i);
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
                    log.info("Envoi END_REGISTER a " + i + ", par " + routeur[i]);
                }
            }
        }
    }

    // Rule 2 : receive REGISTER
    synchronized void receiveEND_REGISTER( int procAuteur, int procTarget, int d)
    {
        log.info("Recu END_REGISTER de " + procAuteur + " pour " + procTarget);
        if (procTarget != procId)
        {
            SyncMessage message = new SyncMessage(MsgType.END_REGISTER, procAuteur, procTarget);
            sendTo(routeur[procTarget], message);
            log.info("Envoi END_REGISTER a " + procTarget + ", par " + routeur[procTarget]);
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
    synchronized void receiveTOKEN( int procTarget, int d)
    {
        if (procTarget == procId)
        {
            log.info("Recu JETON pour moi! ");
            if ( tableau.getDemandeSectionCritique() )
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
                log.info("Envoi JETON a " + procNeighbor);
            }
        }
        else
        {
            log.info("Recu JETON pour " + procTarget);
            SyncMessage tm = new SyncMessage(MsgType.TOKEN, procTarget);
            sendTo( routeur[procTarget], tm );
            log.info("Envoi JETON a " + procNeighbor);
        }
    }

    // Rule 4 : receive FORME
    synchronized void receiveFORME( Forme forme, int procAuteur, int procTarget, int door)
    {
        SyncMessage tm;
        if (procId == procTarget) {
            log.info("Recu FORME de " + procAuteur);
            tableau.canvas.delivreForme(forme);
            if (procAuteur != procNeighbor) {
                tm = new SyncMessage(MsgType.FORME, forme, procAuteur, procNeighbor);
                sendTo(routeur[procNeighbor], tm);
                log.info("Envoi FORME de " + procAuteur + " a " + procNeighbor + " par " + routeur[procNeighbor]);
            }
        }
        else
        {
            log.info("Recu FORME de " + procAuteur + " pour " + procTarget);
            tm = new SyncMessage(MsgType.FORME, forme, procAuteur, procTarget);
            sendTo(routeur[procTarget], tm);
            log.info("Envoi FORME de " + procAuteur + " a " + procTarget + " par " + routeur[procTarget]);
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
                log.info("Envoi FORME a " + i + " par " + routeur[i]);
            }
        }


        tm = new SyncMessage(MsgType.TOKEN, procNeighbor);
        sendTo( routeur[procNeighbor], tm );
        log.info("Envoi TOKEN a " + procNeighbor + " par " + routeur[procNeighbor]);

    }

    // Access to receive function
    public SyncMessage recoit ( Door door )
    {
        SyncMessage sm = (SyncMessage)receive( door );
        return sm;
    }

    // Display routage
    void displayRoutage()
    {
        String builder = "Proc : " + procId + '\n';

        for (int i = 0; i < totalProcessus;i++){
            builder = builder + '\t' + "Vers " + i + " : " + routeur[i] + '\n';
        }

        log.info(builder);
    }

    // Display state
    void displayState()
    {

    }
}
