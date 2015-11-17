package ricartAgrawala;

// Java imports

import gui.Forme;
import visidia.simulation.process.algorithm.Algorithm;
import visidia.simulation.process.messages.Door;

import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.*;

import commun.*;

// Visidia imports

public class RicartAgrawalaMutualExclusion extends Algorithm {

    // All nodes data
    int procId;
    int procNeighbor;

    int nbNeighbors;
    int totalProcessus;

    //dataAlgo
    int H = 0;
    int HSC = 0;
    boolean R = false;
    boolean tabRelDiffere[];
    int nREL = 0;

    // Reception thread
    ReceptionRules rr = null;
    // State display frame
    DisplayFrame tableau;

    private Object objectSync = new Object();

    int routeur[];
    boolean initialized = true;

    private Logger log;

    public String getDescription() {

        return ("Ricart-Agrawala Algorithm for Mutual Exclusion");
    }

    @Override
    public Object clone() {
        return new RicartAgrawalaMutualExclusion();
    }

    //
    // Nodes' code
    //
    @Override
    public void init() {

        int speed = 4;
        procId = getId();
        nbNeighbors = getArity();
        totalProcessus = getNetSize();
        procNeighbor = (procId + 1) % totalProcessus;

        log = Logger.getLogger("" + procId);

        Handler handler = null;
        try {
            handler = new FileHandler("RicartAgrawala_" + procId + ".log");
            log.setLevel(Level.INFO);
            SimpleFormatter formatter = new SimpleFormatter();
            handler.setFormatter(formatter);
            Logger.getLogger("" + procId).addHandler(handler);
        } catch (IOException e) {
            e.printStackTrace();
        }

        rr = new ReceptionRules(this);
        rr.start();

        log.info("Process " + procId + " as " + nbNeighbors + " neighbors");

        tabRelDiffere = new boolean[totalProcessus];
        routeur = new int[totalProcessus];
        for (int i = 0; i < totalProcessus; i++) {
            routeur[i] = -1;
            tabRelDiffere[i] = false;

        }
        routeur[procId] = 0;
        for (int i = 0; i < nbNeighbors; i++) {
            SyncMessage message = new SyncMessage(MsgType.REGISTER, procId);
            sendTo(i, message);
            log.info("Envoi END_REGISTER a " + i);
        }

        log.info("Debut table de routage");

        while (initialized) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }
        }

        log.info("Fin table de routage");

        tableau = new DisplayFrame(procId, objectSync);

        displayRoutage();

        while (true) {

            // Try to access critical section
            while (!tableau.demandeSectionCritique) {
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
                tableau.continuerSectionCritique();

                try {
                    objectSync.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            tableau.demandeSectionCritique = false;
            // Release critical use
            endCriticalUse();
            log.info("Fin de Section Critique");
        }


    }

    //--------------------
    // Rules
    //-------------------

    // Rule 1 : ask for critical section
    synchronized void askForCritical() {
        log.info("Demande de Section Critique");
        R = true;
        HSC = H + 1;
        nREL = totalProcessus;
        SyncMessage message;
        for (int i = 0; i < totalProcessus; i++) {
            message = new SyncMessage(MsgType.REQ, procId, i, HSC);
            sendTo(routeur[i], message);
        }
        while (0 < nREL) {
            try {
                this.wait();
            } catch (InterruptedException ie) {
            }
        }
    }

    // Rule 2 : receive REGISTER
    synchronized void receiveREGISTER(int procAuteur, int door) {
        log.info("Recu REGISTER de " + procAuteur);
        if (-1 == routeur[procAuteur]) {
            routeur[procAuteur] = door;
            for (int i = 0; i < nbNeighbors; i++) {
                SyncMessage message = new SyncMessage(MsgType.REGISTER, procAuteur);
                sendTo(i, message);
                log.info("Envoi REGISTER a " + i);
            }

            int nbNonInitialized = 0;
            for (int i = 0; i < totalProcessus; i++) {
                if (-1 != routeur[i]) {
                    nbNonInitialized++;
                }
            }

            if (nbNonInitialized == totalProcessus) {
                // Send End to All
                for (int i = 0; i < totalProcessus; i++) {
                    SyncMessage message = new SyncMessage(MsgType.END_REGISTER, procId, i);
                    sendTo(routeur[i], message);
                    log.info("Envoi END_REGISTER a " + i + ", par " + routeur[i]);
                }
            }
        }
    }

    // Rule 3 : receive REGISTER
    synchronized void receiveEND_REGISTER(int procAuteur, int procTarget, int door) {
        log.info("Recu END_REGISTER de " + procAuteur + " pour " + procTarget);
        if (procTarget != procId) {
            SyncMessage message = new SyncMessage(MsgType.END_REGISTER, procAuteur, procTarget);
            sendTo(routeur[procTarget], message);
            log.info("Envoi END_REGISTER a " + procTarget + ", par " + routeur[procTarget]);
        }

        int nbInitialized = 0;

        for (int i = 0; i < totalProcessus; i++) {
            if (-1 != routeur[i]) {
                nbInitialized++;
            }
        }

        if (totalProcessus == nbInitialized) {
            initialized = false;
        }
    }

    // Rule 4 : receive REQ
    synchronized void receiveREQ(int procAuteur, int procTarget, int H_P, int door) {
        if (procTarget == procId) {
            log.info("Recu REQ(" + H_P + ") de " + procAuteur);
            if (H < H_P) {
                H = H_P;
            }
            H++;
            if (R && ((HSC < H_P) || ((HSC == H_P) && procTarget < procAuteur))) {
                tabRelDiffere[procAuteur] = true;
            } else {
                SyncMessage message = new SyncMessage(MsgType.REL, procId, procAuteur);
                sendTo(routeur[procAuteur], message);
                log.info("Envoi REL a " + procAuteur);
            }
        } else {
            log.info("Recu REQ(" + H_P + ") de " + procAuteur + " pour " + procTarget);
            SyncMessage message = new SyncMessage(MsgType.REQ, procAuteur, procTarget, H_P);
            sendTo(routeur[procTarget], message);
            log.info("Envoi REQ(" + H_P + ") a " + procTarget + " par " + routeur[procTarget]);
        }
    }

    // Rule 5 : receive REL
    synchronized void receiveREL(int procAuteur, int procTarget, int door)
    {

        if (procTarget == procId)
        {
            log.info("Recu REL de " + procTarget);
            nREL--;
            this.notify();
        }
        else
        {
            log.info("Recu REL de " + procTarget + " pour " + procTarget);
            SyncMessage message = new SyncMessage(MsgType.REL, procAuteur, procTarget);
            sendTo(routeur[procTarget], message);
            log.info("Envoi REL a " + procAuteur + " par " + routeur[procTarget]);
        }
    }

    // Rule 6 : receive FORME
    synchronized void receiveFORME( Forme forme, int procAuteur, int procTarget, int d)
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


    // Rule 7 :
    void endCriticalUse()
    {
        R = false;
        SyncMessage tm;

        if (tableau.canvas.getFormes().size() > 0) {
            for (int i = 0; i < totalProcessus; i++) {
                tm = new SyncMessage(MsgType.FORME, tableau.canvas.getFormes().getLast(), procId, i);
                sendTo(routeur[i], tm);
                log.info("Envoi FORME a " + i + " par " + routeur[i]);
                if (tabRelDiffere[i]){
                    tm = new SyncMessage(MsgType.REL, procId, i);
                    sendTo( routeur[i], tm );
                    log.info("Envoi REL a " + i + " par " + routeur[i]);
                    tabRelDiffere[i] = false;
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
