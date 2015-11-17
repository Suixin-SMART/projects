package namiTrehel;

// Java imports

import commun.DisplayFrame;
import commun.MsgType;
import commun.SyncMessage;
import gui.Forme;
import visidia.simulation.process.algorithm.Algorithm;
import visidia.simulation.process.messages.Door;

import java.io.IOException;
import java.util.logging.*;

// Visidia imports

public class NamiTrehelMutualExclusion extends Algorithm {

    // All nodes data
    int procId;
    int procNeighbor;

    int nbNeighbors;
    int totalProcessus;

    //dataAlgo
    int owner = -1;
    int next = -1;
    boolean jeton = false;
    boolean  SC = false;

    // Reception thread
    ReceptionRules rr = null;
    // State display frame
    DisplayFrame tableau;

    private Object objectSync = new Object();

    int routeur[];
    boolean initialized = true;

    private Logger log;


    public String getDescription() {

        return ("Nami-Trehel Algorithm for Mutual Exclusion");
    }

    @Override
    public Object clone() {
        return new NamiTrehelMutualExclusion();
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
            handler = new FileHandler( "Nami-Trehel_"+procId+".log");
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
        log.info("Envoi de message d'identification aux autres processus.");
        for (int i = 0; i < nbNeighbors; i++)
        {
            SyncMessage message = new SyncMessage(MsgType.REGISTER, procId);
            sendTo(i, message);
        }

        //initialisation Algorithmique
        if (0 == procId){
            jeton = true;
            owner = -1;
        }else{
            owner = 0;
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

        while( true ) {

            // Try to access critical section
            //System.out.println("Section critique: " + tableau.demandeSectionCritique);


            while (!tableau.demandeSectionCritique){
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
            }

            askForCritical();
            // Access critical
            log.info("Entree en Section Critique");
            tableau.inSectionCritique = true;
            synchronized (objectSync) {
                displayState();
                tableau.demandeSectionCritique = false;
                tableau.continuerSectionCritique();

                try {
                    objectSync.wait();
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
        System.out.println("Asked SC! Token owner: " + owner);
        SC = true;
        if (-1 != owner){
            SyncMessage message = new SyncMessage(MsgType.REQ, procId, owner, next);
            sendTo(routeur[owner], message);
            owner = -1;
            while( !jeton )
            {
                try { this.wait(); } catch( InterruptedException ie) {}
            }
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
        if (procTarget != procId)
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

    // Rule 3.0 : receive REQ
    synchronized void receiveREQ( int pAuth,int pTarget, int parameterNextProc, int d)
    {
        if (pTarget == procId)
        {
            System.out.println("REQ! Target: " + pTarget + " Auth: " + pAuth + " Parameter: " + parameterNextProc + " Owner: " + owner);

            if (-1 == owner) {
                if (SC){
                    next = parameterNextProc;
                }else{
                    jeton = false;
                    SyncMessage message = new SyncMessage(MsgType.TOKEN, procId, pAuth);
                    sendTo(routeur[pAuth], message);
                }
            }else{
                SyncMessage message = new SyncMessage(MsgType.REQ, pAuth, owner, parameterNextProc);
                sendTo(routeur[owner], message);
            }
            owner = parameterNextProc;
        }
        else
        {
            System.out.println("Transport REQ! Target: " + pTarget + " Auth: " + pAuth + " Parameter: " + parameterNextProc);
            SyncMessage message = new SyncMessage(MsgType.REQ, pAuth, pTarget, parameterNextProc);
            sendTo(routeur[pTarget], message);
        }
    }

    // Rule 3 : receive JETON
    synchronized void receiveJETON( int procAuth, int procTarget, int d)
    {
        if (procTarget == procId)
        {
            System.out.println("JETON : Target: " + procTarget + " Auth: " + procAuth);
            jeton = true;
            this.notify();
        }
        else
        {
            System.out.println("Transport JETON : Target: " + procTarget + " Auth: " + procAuth);
            SyncMessage message = new SyncMessage(MsgType.TOKEN, procAuth, procTarget);
            sendTo(routeur[procTarget], message);
        }
    }

    // Rule 4 : receive FORME
    synchronized void receiveFORME( Forme forme, int pInitiateur, int pTarget, int d)
    {
        SyncMessage tm;

        if (procId == pTarget) {
            tableau.canvas.delivreForme(forme);
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
        SC = false;
        SyncMessage tm;

        if (tableau.canvas.getFormes().size() > 0) {
            for (int i = 0; i < totalProcessus; i++) {
                tm = new SyncMessage(MsgType.FORME, tableau.canvas.getFormes().getLast(), procId, i);
                sendTo(routeur[i], tm);
                if (-1 != next){
                    tm = new SyncMessage(MsgType.TOKEN, procId, next);
                    sendTo( routeur[next], tm );
                    next = -1;
                    jeton = false;
                }
            }
        }

    }

    // Access to receive function
    public SyncMessage recoit (Door d )
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
