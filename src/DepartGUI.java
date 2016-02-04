import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import se.sics.jasper.Query;
import se.sics.jasper.SICStus;
import se.sics.jasper.SPException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InterfaceAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class DepartGUI extends Application {
    private HashMap<Integer,Epreuve> epreuves;
    private HashMap<Integer,Salle> salles;
    private ArrayList<EpreuvesCommune> epreuvesCommunes;
    private ArrayList<Regroupement> regroupements;



    public DepartGUI() throws SAXException, ParserConfigurationException, IOException {

    }

    public DepartGUI(String filename) throws SAXException, ParserConfigurationException, IOException {
        /* XML Parser */
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        XMLParseur parseur = new XMLParseur();
        SAXParser saxParser = spf.newSAXParser();
        XMLReader xmlReader = saxParser.getXMLReader();
        xmlReader.setContentHandler(parseur);
        xmlReader.setErrorHandler(new MyErrorHandler(System.err));
        xmlReader.parse(convertToFileURL(filename));
        epreuves = parseur.getEpreuves();
        salles = parseur.getSalles();
        epreuvesCommunes = parseur.getEpreuvesCommunes();
        regroupements = parseur.getRegroupements();

        /* END XML Parser */
    }

    /**
     * Convertir le sortie du prolog en objet Epreuve.
     * Remplir les objets deja crees avec les donnees calculees (debut, fin, salle)
     * @param input     la liste de lignes du fichier prolog en tableau de Strings
     */
    public void addTimetable(String[] input) {
        String[] t;
        Epreuve tmp;
        for(int i = 0; i < input.length; i++){
            if (!input[i].startsWith("End=")) {
                //split Analyse=.(50,.(55,.(1,[]))) in an array [Analyse, 50, 55,1]
                t = input[i].substring(0,input[i].length() - 6).split("=.\\(|,.\\(|,\\[\\]\\)\\)\\)");
                t[0] = t[0].substring(1);
                //remplir l'objet Epreuve avec les donneess du prolog
                tmp = epreuves.get(Integer.parseInt(t[0]));
                tmp.setDebut(Integer.parseInt(t[1]));
                tmp.setFin(Integer.parseInt(t[2]));
                tmp.setSalle(salles.get(Integer.parseInt(t[3])));
            }
        }
    }

    /**
     * Lire un fichier du disk
     * @param path      le nom du fichier
     * @param encoding  son enconding
     */
    public static String readFile(String path, Charset encoding)
            throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    /**
     * Obtenir le chemin absolut d'un fichier
     * @param filename  chemin relatif du fichier
     */
    private static String convertToFileURL(String filename) {
        String path = new File(filename).getAbsolutePath();
        if (File.separatorChar != '/') {
            path = path.replace(File.separatorChar, '/');
        }

        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return "file:" + path;
    }


    /**
     * Generer le fichier prolog qui va etre utilisée par la fonction callSicstus
     */
    public String generateFileProlog(int horaireOuverture, int horaireFermeture, int debutRepas,
                                     int finRepas, int dureeRepas){

        //Fusion de TreeSets compatibles pour generer les incompatibilites de repas
        HashMap<Integer,TreeSet<Epreuve>> superMap = new HashMap<>();
        for (EpreuvesCommune e : epreuvesCommunes) {
            TreeSet<Epreuve> t1 = e.getEpreuve(1).getTreeSet();
            TreeSet<Epreuve> t2 = e.getEpreuve(2).getTreeSet();
            if (!t1.equals(t2))
            {
                if (t1.size() < t2.size())
                {
                    e.getEpreuve(2).fusionEnsemble(t1);
                }
                else
                {
                    e.getEpreuve(1).fusionEnsemble(t2);
                }

            }
        }

        //isolation de chaque TreeSet pour eviter les duplicata
        int b = 1;
        for(Map.Entry<Integer, Epreuve> tmpEpreve : epreuves.entrySet())
        {
            if (!superMap.containsValue(tmpEpreve.getValue().getTreeSet())){
                superMap.put(b,tmpEpreve.getValue().getTreeSet());
                b++;
            }

        }

        String tmpEpreuves = "", tmpListEpreuves = "", listTasks="", listSalles = "";

        String tmpContrainteEpreuvesNonRepas = "";

        boolean first = true;
        for(Map.Entry<Integer, Epreuve> tmpEpreuve : epreuves.entrySet()) {

            tmpContrainteEpreuvesNonRepas += "         Salle" + tmpEpreuve.getValue().getId() + " #\\= SalleRepas,\n";

            //generer la liste de tasks
            listTasks = listTasks + "         task(";
            if (tmpEpreuve.getValue().getStart()<0){
                listTasks = listTasks + "S"+tmpEpreuve.getValue().getId();
            }else{
                listTasks = listTasks + tmpEpreuve.getValue().getStart();
            }
            listTasks = listTasks + ", " + tmpEpreuve.getValue().getDuree()+", ";
            if (tmpEpreuve.getValue().getEnd()<0){
                listTasks = listTasks + "E"+tmpEpreuve.getValue().getId();
            }else{
                listTasks = listTasks + tmpEpreuve.getValue().getEnd();
            }

            if (tmpEpreuve.getValue().getId() == epreuves.size()){
                tmpEpreuves = tmpEpreuves + "          ["+ "S"+tmpEpreuve.getValue().getId()+ ","+ "E"+tmpEpreuve.getValue().getId()+ ",Salle"+tmpEpreuve.getValue().getId()+"]\n";
                listTasks = listTasks + ", "+tmpEpreuve.getValue().getNbEtudiants()+", Salle"+tmpEpreuve.getValue().getId()+")\n";
            }else{
                tmpEpreuves = tmpEpreuves + "          ["+ "S"+tmpEpreuve.getValue().getId()+ ","+ "E"+tmpEpreuve.getValue().getId()+ ",Salle"+tmpEpreuve.getValue().getId()+"],\n";

                listTasks = listTasks + ", "+tmpEpreuve.getValue().getNbEtudiants()+", Salle"+tmpEpreuve.getValue().getId()+"),\n";
            }


            //generer la liste des epreuves
            if (first){
                if (tmpEpreuve.getValue().getStart()<0){
                    tmpListEpreuves =  "["+ "S"+tmpEpreuve.getValue().getId()+ ","+ "E"+tmpEpreuve.getValue().getId();
                }else{
                    tmpListEpreuves =  "["+tmpEpreuve.getValue().getStart()+ ","+tmpEpreuve.getValue().getEnd();
                }
                listSalles = "[SalleRepas,Salle"+tmpEpreuve.getValue().getId();
                first = false;
            }else{
                if (tmpEpreuve.getValue().getStart()<0){
                    tmpListEpreuves = tmpListEpreuves + ",S"+tmpEpreuve.getValue().getId()+ ","+ "E"+tmpEpreuve.getValue().getId();
                }else{
                    tmpListEpreuves = tmpListEpreuves + ",S"+tmpEpreuve.getValue().getId()+ ","+ "E"+tmpEpreuve.getValue().getId();
                }
                listSalles = listSalles + ",Salle"+tmpEpreuve.getValue().getId();
            }
        }
        tmpListEpreuves = tmpListEpreuves + "]";
        listSalles = listSalles + "]";
        String text = ":-set_prolog_flag(toplevel_print_options, [max_depth(0)]).\n" +
                ":- use_module(library(clpfd)).\n" +
                ":- use_module(library(lists)).\n" +
                "\n" +
                "\n" +
                "%%%%%%%%%%%%%%%%%%%%%%%%%%\n" +
                "%%% Incompatible exams %%%\n" +
                "%%%%%%%%%%%%%%%%%%%%%%%%%%\n" +
                "/* Ajoute la contrainte que deux examens sont incompatibles :\n" +
                "   - Soit l'épreuve 1 est après l'épreuve 2 (séparés par un DeltaTime\n" +
                "   - Soit l'épreuve 2 est après l'épreuve 1 (séparés par un DeltaTime */\n" +
                "%incompatible(+Debut1, +Fin1, +Debut2, +Fin2, +DeltaTime)\n" +
                "incompatible(Debut1, Fin1, Debut2, Fin2, DeltaTime):-\n" +
                "        (DeltaTime #=< (Debut1 - Fin2)) #\\ (DeltaTime #=< (Debut2 - Fin1)).\n" +
                "\n" +

                "examensCompatibles(Salle1, Debut1, Fin1, Salle2, Debut2, Fin2):-\n" +
                "        ((Fin1 - Debut1 #= Fin2 - Debut2) #/\\ (Salle1 #= Salle2) #/\\ (Debut1 #= Debut2 #/\\ Fin1 #= Fin2))\n" +
                "        #\\ ((Fin1 - Debut1 #= Fin2 - Debut2) #/\\ (Salle1 #= Salle2) #/\\ ((Fin1 #=< Debut2) #\\ (Fin2 #=< Debut1)))\n" +
                "        #\\ ((Fin1 - Debut1 #= Fin2 - Debut2) #/\\ (Salle1 #\\= Salle2))\n" +
                "        #\\ ((Fin1 - Debut1 #\\= Fin2 - Debut2) #/\\ (Salle1 #= Salle2) #/\\ ((Fin1 #=< Debut2) #\\ (Fin2 #=< Debut1)))\n" +
                "        #\\ ((Fin1 - Debut1 #\\= Fin2 - Debut2) #/\\ (Salle1 #\\= Salle2)).\n" +
                "\n\n" +
                "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n" +
                "%%% Spacing exams in the same room %%%\n" +
                "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n" +
                "/* Ajoute la contrainte comme quoi quand on a deux épreuves, on peut\n" +
                "   - les avoir simplement dans des salles différentes\n" +
                "   - les avoir dans la même salle aux mêmes horaires\n" +
                "   - les avoir dans la même salle à des horaires différents ET séparé par DeltaTime (au minimum) */\n" +
                "% espacementSalle(+Salle1, +Debut1, +Fin1, +Salle2, +Debut2, +Fin2, +DeltaTime)\n" +
                "espacementSalle(Salle1, Debut1, Fin1, Salle2, Debut2, Fin2, DeltaTime):-\n" +
                "        (Salle1 #\\= Salle2)\n" +
                //"        #\\ (Salle1 #= Salle2).\n\n"+
                "        #\\/ ((Salle1 #= Salle2) #/\\ (Debut1 #= Debut2) #/\\ (Fin1 #= Fin2))\n" +
                "        #\\/ ((Salle1 #= Salle2) #/\\ (DeltaTime #=< (Debut1 - Fin2)))\n"+
                "        #\\/ ((Salle1 #= Salle2) #/\\ (DeltaTime #=< (Debut2 - Fin1))).\n\n"+


                "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n" +
                "%%% Counting the number of rooms affected %%%\n" +
                "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n" +
                "/* Compte le nombre de fois qu'une salle a été utilisée en évitant les doublons.\n" +
                "   La salle n'est pas comptée si elle est réservée plus tard dans la liste (sera donc comptée à la fin) */\n" +
                "%compteNombreFoisSalleUtilisee(+Salle, +Debut, +Fin, -ListeTask, +Counter, -Result)\n" +
                "compteNombreFoisSalleUtilisee(_Salle, _Debut, _Fin, [], Counter, Counter):-!.\n" +
                "compteNombreFoisSalleUtilisee(Salle1, Debut1, Fin1, [task(Debut2, _Duree2, Fin2, _Effectif2, Salle2) | ListeTask], Counter, Result):-\n" +
                "        (Salle1 #= Salle2 #/\\ Debut1 #= Debut2 #/\\ Fin1 #= Fin2 #/\\ NewCounter #= Counter + 1)\n" +
                "        #\\ ((#\\ (Salle1 #= Salle2 #/\\ Debut1 #= Debut2 #/\\ Fin1 #= Fin2)) #/\\ NewCounter #= Counter),\n" +
                "        compteNombreFoisSalleUtilisee(Salle1, Debut1, Fin1, ListeTask, NewCounter, Result),\n" +
                "        !.\n" +
                "\n" +
                "/* Compte le nombre de salles différentes affectées en évitant les doublons exprimé en contraintes.\n" +
                "   Ce prédicat fait appel récursivement au précédent pour parcourir toutes les tâches affectées. */\n" +
                "%compteNombreSallesAffectees(+ListeTask, +Buffer, -Result)\n" +
                "compteNombreSallesAffectees([], Counter, Counter):-!.\n" +
                "compteNombreSallesAffectees([task(Debut, _Duree, Fin, _Contenance, Salle) | ListeTask], Counter, Result):-\n" +
                "        compteNombreFoisSalleUtilisee(Salle, Debut, Fin, ListeTask, 0, NombreDoublon),\n" +
                "        (NombreDoublon #= 0 #/\\ NewCounter #= Counter + 1)\n" +
                "        #\\ (NombreDoublon #> 0 #/\\ NewCounter #= Counter),\n" +
                "        compteNombreSallesAffectees(ListeTask, NewCounter, Result),\n" +
                "        !.\n" +
                "\n" +
                "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n" +
                "%%% Generation of the tasks for a machine %%%\n" +
                "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n" +
                "/* Génère les tâches fixes correspondant aux créneaux occupés des salles. */\n" +
                "%generationTask(+IdSalle, +Contenance, +ListeHorairesPris, +Buffer, -ListeTask)\n" +
                "%generationTask(IdSalle, Contenance, ListeHorairesPris, Buffer, ListeTask)\n" +
                "generationTask(_IdSalle, _Contenance, [], Buffer, Buffer):-!.\n" +
                "generationTask(IdSalle, Contenance, [[Debut, Fin] | ListeHorairesPris], Buffer, ListeTask):-\n" +
                "        Duree #= Fin - Debut,\n" +
                "        append(Buffer, [task(Debut, Duree, Fin, Contenance, IdSalle)], NewBuffer),\n" +
                "        generationTask(IdSalle, Contenance, ListeHorairesPris, NewBuffer, ListeTask),\n" +
                "        !.\n" +
                "\n" +
                "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n" +
                "%%% Generation of the machines %%%\n" +
                "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n" +
                "/* Génère la liste de machines utilisées par le cumulatives scheduling */\n" +
                "%generationMachine(+ListeSalle, +NbActuelSalle, +BufferMachines, -ListeMachines, +BufferTasks, -ListeTasks)\n" +
                "generationMachine([], _NbActuelSalle, BufferMachines, BufferMachines, BufferTasks, BufferTasks):-!.\n" +
                "generationMachine([[_Salle, Contenance, HorairesPris] | ListeSalles], NbActuelSalles, BufferMachines, ListeMachines, BufferTasks, ListeTasks):-\n" +
                "        append(BufferMachines, [machine(NbActuelSalles, Contenance)], NewBufferMachines),\n" +
                "        generationTask(NbActuelSalles, Contenance, HorairesPris, BufferTasks, NewBufferTasks),\n" +
                "        NewNbSalles #= NbActuelSalles + 1,\n" +
                "        generationMachine(ListeSalles, NewNbSalles, NewBufferMachines, ListeMachines, NewBufferTasks, ListeTasks),\n" +
                "        !.\n" +
                "\n" +
                "%examensCompatibles(Salle1, S1, E1, Salle2, S2, E2),\n" +
                "generationClausesExamensCompatiblesEnDureeRecursive(_Salle1, _Debut1, _Fin1, []):-!.\n" +
                "generationClausesExamensCompatiblesEnDureeRecursive(Salle1, Debut1, Fin1, [task(Debut2, _Duree, Fin2, _Effectif, Salle2) | Reste]):-\n" +
                "        examensCompatibles(Salle1, Debut1, Fin1, Salle2, Debut2, Fin2),\n" +
                "        generationClausesExamensCompatiblesEnDureeRecursive(Salle1, Debut1, Fin1, Reste),\n" +
                "        !.\n" +
                "\n" +
                "generationClausesExamensCompatiblesEnDuree([]):-!.\n" +
                "generationClausesExamensCompatiblesEnDuree([task(Debut, _Duree, Fin, _Effectif, Salle) | ListeExamensTask]):-\n" +
                "        generationClausesExamensCompatiblesEnDureeRecursive(Salle, Debut, Fin, ListeExamensTask),\n" +
                "        generationClausesExamensCompatiblesEnDuree(ListeExamensTask),\n" +
                "        !.\n" +
                "\n" +
                "%%%%%%%%%%%%%%%%%%%%%%%%%\n" +
                "%%% Scheduling Method %%%\n" +
                "%%%%%%%%%%%%%%%%%%%%%%%%%\n" +
                "/* Method de Scheduling qui gère toutes les contraintes et lance le labeling. */\n" +
                "%schedule(+PriorityDuration,+PrioritySalles,+TimeOut,+DeltaTime,+Salles,+ListeTasksSallesPrises,-Results,-Return)\n" +
                "schedule(PriorityDuration,PrioritySalles, TimeOut, DeltaTime, Salles, ListeTasksSallesPrises,\n" +
                "         [\n" + tmpEpreuves + "         ],\n" +
                "         ToMinimize):-\n" +
                "\n" +
                "        /* Liste des dates de début et de fin de toutes les épreuves à affecter. */\n" +
                "        Total = " + tmpListEpreuves + ",\n" +
                "\n" +
                "        /* Ensemble de contraintes sur les heures d'ouverture et de fermeture des épreuves (Heure min et max pour l'organisation) */\n";

        for(Map.Entry<Integer, Epreuve> e : epreuves.entrySet()) {
            text += "        S" + e.getValue().getId() + " #>= " + horaireOuverture + " #/\\ E" + e.getValue().getId() + "#=< " + horaireFermeture + "\n" +
                    "        #\\ S" + e.getValue().getId() + " #>= " + (horaireOuverture + 96)+ " #/\\ E" + e.getValue().getId() + "#=< " + (horaireFermeture + 96) + "\n" +
                    "        #\\ S" + e.getValue().getId() + " #>= " + (horaireOuverture + 192) + " #/\\ E" + e.getValue().getId() + "#=< " + (horaireFermeture + 192) + "\n" +
                    "        #\\ S" + e.getValue().getId() + " #>= " + (horaireOuverture + 288) + " #/\\ E" + e.getValue().getId() + "#=< " + (horaireFermeture + 288) + "\n" +
                    "        #\\ S" + e.getValue().getId() + " #>= " + (horaireOuverture + 384) + " #/\\ E" + e.getValue().getId() + "#=< " + (horaireFermeture + 384) + ",\n";
        }

        text += "        /* Création des tâches correspondant aux examens à organiser. */\n" +
                "        Tasks = [\n" + listTasks + "         ],\n";

        text += "        /* Définition de la salle de repas avec son numéro machine utilisé par les tâches. */\n" +
                "        SalleRepas #= " + (Salle.getNbSalles()+1) + ",\n";

        text += "        /* Interdiction aux tâches d'épreuves d'être affectées à la salle de repas. */\n" +
                tmpContrainteEpreuvesNonRepas;

        text += "        /* Création des tâches correspondant aux heures de repas.\n" +
                "           1 tâche fictive par jour et par groupe qui n'apparait pas dans le rendu final. */\n" +
                "        TasksRepas=[\n";

        String tmpContraintesRepas = "";
        String tmpNameRepas = "";
        String tmpIncompRepas = "";
        for(Map.Entry<Integer, TreeSet<Epreuve>> e : superMap.entrySet()) {
            text += "        task(SRepas" + e.getKey() + "1," + dureeRepas + ",ERepas" + e.getKey() + "1,1,SalleRepas),\n";
            text += "        task(SRepas" + e.getKey() + "2," + dureeRepas + ",ERepas" + e.getKey() + "2,1,SalleRepas),\n";
            text += "        task(SRepas" + e.getKey() + "3," + dureeRepas + ",ERepas" + e.getKey() + "3,1,SalleRepas),\n";
            text += "        task(SRepas" + e.getKey() + "4," + dureeRepas + ",ERepas" + e.getKey() + "4,1,SalleRepas),\n";
            text += "        task(SRepas" + e.getKey() + "5," + dureeRepas + ",ERepas" + e.getKey() + "5,1,SalleRepas),\n";
            tmpContraintesRepas += "        SRepas" + e.getKey() + "1 #>= " + debutRepas + " #/\\ ERepas" + e.getKey() + "1#=< "+ finRepas +",\n";
            tmpContraintesRepas += "        SRepas" + e.getKey() + "2 #>= " + (debutRepas + 96) + " #/\\ ERepas" + e.getKey() + "2#=< "+ (finRepas + 96) +",\n";
            tmpContraintesRepas += "        SRepas" + e.getKey() + "3 #>= " + (debutRepas + 192) + " #/\\ ERepas" + e.getKey() + "3#=< "+ (finRepas + 192) +",\n";
            tmpContraintesRepas += "        SRepas" + e.getKey() + "4 #>= " + (debutRepas + 288) + " #/\\ ERepas" + e.getKey() + "4#=< "+ (finRepas + 288) +",\n";
            tmpContraintesRepas += "        SRepas" + e.getKey() + "5 #>= " + (debutRepas + 384) + " #/\\ ERepas" + e.getKey() + "5#=< "+ (finRepas + 384) +",\n";
            tmpNameRepas += "SRepas" + e.getKey() + "1, ERepas" + e.getKey() + "1,";
            tmpNameRepas += "SRepas" + e.getKey() + "2, ERepas" + e.getKey() + "2,";
            tmpNameRepas += "SRepas" + e.getKey() + "3, ERepas" + e.getKey() + "3,";
            tmpNameRepas += "SRepas" + e.getKey() + "4, ERepas" + e.getKey() + "4,";
            tmpNameRepas += "SRepas" + e.getKey() + "5, ERepas" + e.getKey() + "5,";

            for (Epreuve eppJordan: e.getValue()) {
                tmpIncompRepas += "        incompatible(S"+ eppJordan.getId() +",E"+ eppJordan.getId() +",SRepas" + e.getKey() + "1,ERepas" + e.getKey() + "1, 0),\n";
                tmpIncompRepas += "        incompatible(S"+ eppJordan.getId() +",E"+ eppJordan.getId() +",SRepas" + e.getKey() + "2,ERepas" + e.getKey() + "2, 0),\n";
                tmpIncompRepas += "        incompatible(S"+ eppJordan.getId() +",E"+ eppJordan.getId() +",SRepas" + e.getKey() + "3,ERepas" + e.getKey() + "3, 0),\n";
                tmpIncompRepas += "        incompatible(S"+ eppJordan.getId() +",E"+ eppJordan.getId() +",SRepas" + e.getKey() + "4,ERepas" + e.getKey() + "4, 0),\n";
                tmpIncompRepas += "        incompatible(S"+ eppJordan.getId() +",E"+ eppJordan.getId() +",SRepas" + e.getKey() + "5,ERepas" + e.getKey() + "5, 0),\n";

            }
        }
        text += "        /* Définition du domaine restreint pour les tâches de repas aux heures dédiées. */\n";
        text = text.substring(0, text.length()-2);
        tmpNameRepas = tmpNameRepas.substring(0, tmpNameRepas.length()-1);
        text += "],\n";
        text += tmpContraintesRepas;

        text += "        /* Création de la liste de tâches complète avec :\n" +
                "           - les tâches représentant les épreuves à passer\n" +
                "           - les tâches représentant les horaires indisponibles\n" +
                "           - les tâches représentant les heures de repas */\n" +
                "        append(Tasks, ListeTasksSallesPrises, TasksTmp),\n";
        text += "        append(TasksTmp, TasksRepas, NewTasks),\n";

        text += "        /* Fusion de toutes les variables de début et fin de tâches. */\n" +
                "        append(Total, [" + tmpNameRepas + "], NewTotal),\n";

        text += "        /* Déclaration du domaine de définition des variables de début et de fin de tâches. */\n" +
                "        domain(NewTotal, 0, 480),";

        text += "        /* Les heures de repas d'un groupe sont incompatibles avec les heures d'examens. */\n" +
                tmpIncompRepas +
                "        /* Prédicats d'incompatibilité concernant les horaires.\n" +
                "           Les  épreuves dont les horaires sont rendus incompatibles sont les épreuves qui ont au moins un étudiant en commun. */\n";

        for (EpreuvesCommune e : epreuvesCommunes ) {
            text += "        incompatible(S" + e.getEpreuve(1).getId() + ",E" + e.getEpreuve(1).getId() + ",S"+ e.getEpreuve(2).getId() + ",E" + e.getEpreuve(2).getId() + ", DeltaTime),\n";
        }

        text += "        /* Prédicats générés pour s'assurer qu'un temps minimum est laissé entre chaque affectation de salles. */\n";

        for(Map.Entry<Integer, Epreuve> e1 : epreuves.entrySet()) {
            for(Map.Entry<Integer, Epreuve> e2 : epreuves.entrySet()) {
                if (e1.getValue().getId() < e2.getValue().getId())
                   text += "        espacementSalle(Salle" + e1.getValue().getId() + ",S" + e1.getValue().getId()  + ",E" + e1.getValue().getId() + ",Salle" + e2.getValue().getId() + ",S" + e2.getValue().getId()  + ",E" + e2.getValue().getId() + ", DeltaTime),\n";
            }
        }

           text += "        /* Génère les contraintes empêchant les épreuves de durées différentes d'être mutualisées. */\n" +
                   "        generationClausesExamensCompatiblesEnDuree(Tasks),\n" +
                   "\n        /* Ajout de la Salle de Repas fictive à la liste des machines. */\n" +
                   "        append(Salles,[machine(SalleRepas,100000) | []],NewSalles),\n" +
                   "        /* Contraint les Tasks à être placée sur les Machines en utilisant les resources comme bornes supérieures. */\n" +
                   "        cumulatives(NewTasks, NewSalles, [bound(upper), task_intervals(true)]),\n\n" +
                   "        /* Calcul de l'étalement des épreuves pour chaque groupe.\n" +
                   "           Ces résultats seront utilisés dans la minimisation. */\n";

        String dureeTotaleTableau;
        String regroupementsOptimisees = "";
        for (Regroupement r: regroupements){
            text += "        % Calcul de la durée totale des épreuves de " + r.getName() + "\n";
            dureeTotaleTableau = "";
            for (Epreuve x : r.getEpreuves()){
                text += "        ";
                for (Epreuve y : r.getEpreuves()){
                    text += "D_" + r.getName() + "_" + x.getId() + "_" + y.getId() + " #= E" + x.getId() + " - S" + y.getId() + ",";
                    dureeTotaleTableau += "D_" + r.getName() + "_" + x.getId() + "_" + y.getId() + ",";
                }
                text += "\n";
            }

            text += "        " + r.getName()+"Durations = [" + dureeTotaleTableau.substring(0, dureeTotaleTableau.length() - 1) + "],\n";
            text += "        maximum(MaxDuration" + r.getName() + ", " + r.getName() + "Durations),\n";
            regroupementsOptimisees += "        MaxDuration" + r.getName() + "+";
        }

        text += "        /* Calcul du nombre de salles affectées pour la minimisation. */\n" +
                "        compteNombreSallesAffectees(Tasks, 0, Result),\n" +
                "\n" +
                "        /* Fonction objectif à minimiser. */\n" +
                "        ToMinimize #= PriorityDuration * (" + regroupementsOptimisees.substring(0, regroupementsOptimisees.length() - 1) + ") + PrioritySalles * Result,"+// * " + /* TODO : METTRE LES DUREES DES DIFFERENTES PROMOTIONS*/
                "        /* Union de toutes les variables à identifier dans le labeling. */\n" +
                "\n      append(NewTotal, " + listSalles + ", Vars),\n" +
                "\n" +
                "        /* Exécution du labeling pour identifier les variables et trouver une solution.\n" +
                "           Le temps d'exécution est borné par un TimeOut et affiché pour voir si cela a été fini en avance. */\n" +
                "        statistics(runtime, [T0| _]),\n" +
                "        labeling([minimize(ToMinimize), time_out( TimeOut, _LabelingResult)], Vars),\n" +
                "        statistics(runtime, [T1|_]),\n" +
                "        TLabelling is T1 - T0,\n" +
                "        format('labeling took ~3d sec.~n', [TLabelling]).\n" +
                "\n" +
                "%%%%%%%%%%%%%%%%%%%\n" +
                "%%% Main Method %%%\n" +
                "%%%%%%%%%%%%%%%%%%%\n" +
                "/* Lance le schedule après avoir généré les machines. Calcule le temps total d'exécution. */\n" +
                "% runSchedule(+PriorityDuration,+PrioritySalles,+TimeOut,+DeltaTime,+ListeSalles,-Results,-End)\n" +
                "runSchedule(PriorityDuration,PrioritySalles, TimeOut, DeltaTime, ListeSalles, L, End) :-\n" +
                "        generationMachine(ListeSalles, 1, [], SallesMachines, [], ListeTasksPrises),\n" +
                "        statistics(runtime, [T0| _]),\n" +
                "        schedule(PriorityDuration, PrioritySalles, TimeOut, DeltaTime, SallesMachines, ListeTasksPrises, L, End),\n" +
                "        statistics(runtime, [T1|_]),\n" +
                "        T is T1 - T0,\n" +
                "        format('schedule/8 took ~3d sec.~n', [T]).";

        return text;
    }

    public void callSicstus(String inputFile, int prioriteSalle, int prioriteDuree,
                            int prioriteDist, int tOut, int dTime, String outputFile){
        SICStus sp = null;
        HashMap results;

        // Loading the prolog file.
        try {

            // Creation d'un object SICStus
            sp = new SICStus();

            // Chargement d'un fichier prolog .pl
            sp.load("./"+inputFile);

        }
        // Exception déclenchée par SICStus lors de la création de l'objet sp
        catch (SPException e)
        {
            System.err.println("Exception SICStus Prolog : " + e);
            e.printStackTrace();
            System.exit(-2);
        }

        // HashMap utilisée pour stocker les solutions
        results = new HashMap();

        try {

            // Creation d'une requete (Query) Sicstus
            //  - en fonction de la saisie de l'utilisateur
            //  - instanciera results avec les résultats de la requète

            String tmpSalles = "[";
            int i = 1;
            for(Map.Entry<Integer, Salle> entry : salles.entrySet()) {
                if (salles.size() == i){
                    tmpSalles += entry.getValue().toString();
                }else {
                    tmpSalles += entry.getValue().toString() + ", ";
                }
                i++;
            }
            tmpSalles += "]";

            String tmpEpreuves = "[";
            i = 1;
            for(Map.Entry<Integer, Epreuve> entry : epreuves.entrySet()) {
                if (epreuves.size() == i){
                    tmpEpreuves += "E"+ entry.getValue().getId() + "]";
                }else{
                    tmpEpreuves += "E"+ entry.getValue().getId() + ",";
                }
                i++;

            }

                    System.out.println(tmpSalles);

			int denominateurDuree = regroupements.size();
			int denominateurSalle = 1;
			int denominateurDistance = 1;
			
            int facteurDuree = prioriteDuree * denominateurSalle * denominateurDistance; //0
            int facteurNbSalle = prioriteSalle * denominateurDuree * denominateurDistance; //1
            int facteurDistance = prioriteDist * denominateurDuree * denominateurSalle; //0

            int timeOut = tOut; //10000
            int deltaTime = dTime; //2
            String requete = "runSchedule(" + facteurDuree + ", " + facteurNbSalle + ", " + timeOut + ", " + deltaTime +
                    ", "+tmpSalles+", "+tmpEpreuves+", End).";

            System.out.println(requete);

            Query qu = sp.openQuery(requete, results);
            qu.nextSolution();

            // on vérifie qu'il y a une solution
            if (!(results.isEmpty()))
            {

                PrintWriter out = new PrintWriter(outputFile);

                //System.out.println(results);
                for (Object r: results.entrySet()){
                    System.out.println(r.toString());
                    out.print(r.toString() + "\n");
                }

                out.close();

                // Extraction de la solution.
                // ...
            }
            // fermeture de la requète
            System.err.println("Fermeture requete");
            qu.close();
        }
        catch (SPException e) {
            System.err.println("Exception prolog\n" + e);
        }
        // autres exceptions levées par l'utilisation du Query.nextSolution()
        catch (Exception e) {
            System.err.println("Other exception : " + e + " Message : " + e.getMessage());

        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("Emploi de temps");
        primaryStage.setScene(new Scene(root, 850, 375));
        primaryStage.show();
    }

    public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
        launch(args);
    }

    /**
     * Creation de la contenu du XML fichier du sortie
     * @return fichier XML genere avec les salles, horaires de debut et fin remplis
     */
    public String generateXML(){
        String tmp = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<!DOCTYPE optimisation [\n" +
                "        <!ELEMENT optimisation (regroupements,epreuves,salles,epreuvesAyantDesEtudiantsEnCommun)>\n" +
                "        <!ELEMENT regroupements (regroupement)*>\n" +
                "        <!ELEMENT regroupement (matiere)*>\n" +
                "        <!ATTLIST regroupement\n" +
                "                name              ID       #REQUIRED\n" +
                "                >\n" +
                "        <!ELEMENT matiere EMPTY>\n" +
                "        <!ATTLIST matiere\n" +
                "                name              IDREF       #REQUIRED\n" +
                "                nbEtudiants       CDATA       #REQUIRED\n" +
                "                >\n" +
                "        <!ELEMENT epreuves (epreuve)*>\n" +
                "        <!ELEMENT epreuve EMPTY>\n" +
                "        <!ATTLIST epreuve\n" +
                "                name             ID          #REQUIRED\n" +
                "                nbEtudiants      CDATA       #REQUIRED\n" +
                "                jour             CDATA\t    #IMPLIED\n" +
                "                heureDebut       CDATA       #IMPLIED\n" +
                "                duree            CDATA       #REQUIRED\n" +
                "                heureFin         CDATA       #IMPLIED\n" +
                "                salle            IDREF       #IMPLIED\n" +
                "                >\n" +
                "        <!ELEMENT salles (salle)*>\n" +
                "        <!ELEMENT salle (creneau-occupe)*>\n" +
                "        <!ATTLIST salle\n" +
                "                name             ID          #REQUIRED\n" +
                "                capacite         CDATA       #REQUIRED\n" +
                "                >\n" +
                "        <!ELEMENT epreuvesAyantDesEtudiantsEnCommun (epreuvesCommune)*>\n" +
                "        <!ELEMENT epreuvesCommune EMPTY>\n" +
                "        <!ATTLIST epreuvesCommune\n" +
                "                idEpreuve1      IDREF       #REQUIRED\n" +
                "                idEpreuve2      IDREF       #REQUIRED\n" +
                "                >\n" +
                "        <!ELEMENT creneau-occupe EMPTY>\n" +
                "        <!ATTLIST creneau-occupe\n" +
                "                jour            CDATA  \t  #REQUIRED\n" +
                "                debut           CDATA       #REQUIRED\n" +
                "                fin             CDATA       #REQUIRED\n" +
                "                >\n" +
                "        ]>\n" +
                "<optimisation>\n" +
                "  <regroupements>\n";
        for (Regroupement r : regroupements ) {
            tmp += r.toStringXML();
        }
        tmp += "    </regroupements>\n" +
                "    <epreuves>\n";
        for(Map.Entry<Integer, Epreuve> entry : epreuves.entrySet()) {
            tmp += entry.getValue().toStringXML();
        }
        tmp += "    </epreuves>\n" +
                "   <salles>\n";
        for(Map.Entry<Integer, Salle> entry : salles.entrySet()) {
            tmp += entry.getValue().toStringXML();
        }
        tmp += "    </salles>\n" +
                "   <epreuvesAyantDesEtudiantsEnCommun>\n";
        for (EpreuvesCommune e: epreuvesCommunes) {
            tmp += e.toStringXML();
        }
        tmp += "    </epreuvesAyantDesEtudiantsEnCommun>\n" +
                "</optimisation>";
        return tmp;
    }

    public HashMap<Integer,Epreuve> getEpreuves(){
        return epreuves;
    }

    public HashMap<Integer, Salle> getSalles() {
        return salles;
    }

    public ArrayList<EpreuvesCommune> getEpreuvesCommunes() {
        return epreuvesCommunes;
    }

    public ArrayList<Regroupement> getRegroupements() {
        return regroupements;
    }
}
