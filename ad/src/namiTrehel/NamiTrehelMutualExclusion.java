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
        for (int i = 0; i < nbNeighbors; i++)
        {
            SyncMessage message = new SyncMessage(MsgType.REGISTER, procId);
            sendTo(i, message);
            log.info("Envoi REGISTER a " + i);
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
        log.info("Demande Section Critique Token: " + owner);
        SC = true;
        if (-1 != owner){
            SyncMessage message = new SyncMessage(MsgType.REQ, procId, owner, next);
            sendTo(routeur[owner], message);
            log.info("Envoi REQ a " + owner + " par " + routeur[owner]);
            owner = -1;
            while( !jeton )
            {
                try { this.wait(); } catch( InterruptedException ie) {}
            }
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
    synchronized void receiveEND_REGISTER( int procAuteur, int procTarget, int door)
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

    // Rule 3.0 : receive REQ
    synchronized void receiveREQ( int procAuteur,int procTarget, int parameterNextProc, int door)
    {
        if (procTarget == procId)
        {
            log.info("Recu REQ de " + procAuteur + " Parameter: " + parameterNextProc + " Owner: " + owner);

            if (-1 == owner) {
                if (SC){
                    next = parameterNextProc;
                }else{
                    jeton = false;
                    SyncMessage message = new SyncMessage(MsgType.TOKEN, procId, procAuteur);
                    sendTo(routeur[procAuteur], message);
                    log.info("Envoi TOKEN a " + procAuteur);
                }
            }else{
                SyncMessage message = new SyncMessage(MsgType.REQ, procAuteur, owner, parameterNextProc);
                sendTo(routeur[owner], message);
                log.info("Envoi REQ a " + owner);
            }
            owner = parameterNextProc;
        }
        else
        {
            log.info("Recu REQ de " + procAuteur + " vers: " + procTarget + " Parameter: " + parameterNextProc + " Owner: " + owner);
            SyncMessage message = new SyncMessage(MsgType.REQ, procAuteur, procTarget, parameterNextProc);
            sendTo(routeur[procTarget], message);
            log.info("Envoi REQ a " + procTarget);
        }
    }

    // Rule 3 : receive JETON
    synchronized void receiveJETON( int procAuteur, int procTarget, int d)
    {
        if (procTarget == procId)
        {
            log.info("Recu JETON de " + procAuteur);
            jeton = true;
            this.notify();
        }
        else
        {
            log.info("Recu JETON de " + procAuteur + " vers " + procTarget);
            SyncMessage message = new SyncMessage(MsgType.TOKEN, procAuteur, procTarget);
            sendTo(routeur[procTarget], message);
            log.info("Envoi JETON de " + procAuteur + " vers " + procTarget);
        }
    }

    // Rule 4 : receive FORME
    synchronized void receiveFORME( Forme forme, int procAuteur, int procTarget, int door)
    {
        SyncMessage tm;

        if (procId == procTarget) {
            log.info("Recu FORME de " + procAuteur);
            tableau.canvas.delivreForme(forme);
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
        SC = false;
        SyncMessage tm;

        if (tableau.canvas.getFormes().size() > 0) {
            for (int i = 0; i < totalProcessus; i++) {
                tm = new SyncMessage(MsgType.FORME, tableau.canvas.getFormes().getLast(), procId, i);
                sendTo(routeur[i], tm);
                log.info("Envoi FORME a " + i + " par " + routeur[i]);
                if (-1 != next){
                    tm = new SyncMessage(MsgType.TOKEN, procId, next);
                    sendTo( routeur[next], tm );
                    log.info("Envoi TOKEN a " + next + " par " + routeur[next]);
                    next = -1;
                    jeton = false;
                }
            }
        }

    }

    // Access to receive function
    public SyncMessage recoit (Door door )
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
