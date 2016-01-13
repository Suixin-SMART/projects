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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by nomce on 3/7/14.
 */
public class DepartGUI extends Application {
    private ArrayList<Epreuve> epreuves;
    private ArrayList<Salle> salles;
    private ArrayList<EpreuvesCommune> epreuvesCommunes;



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
        /* END XML Parser */
    }

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

    public String toString(){
        String tmpEpreuves = "", tmpListEpreuves = "", listTasks="", listSalles = "";

        boolean first = true;
        for (Epreuve tmpA : epreuves){

            //tasks
            listTasks = listTasks + "task(";
            if (tmpA.getStart()<0){
                listTasks = listTasks + "S"+tmpA.getId();
            }else{
                listTasks = listTasks + tmpA.getStart();
            }
            listTasks = listTasks + ", " + tmpA.getDuree()+", ";
            if (tmpA.getEnd()<0){
                listTasks = listTasks + "E"+tmpA.getId();
            }else{
                listTasks = listTasks + tmpA.getEnd();
            }

            if (tmpA.getId() == epreuves.size()){
                tmpEpreuves = tmpEpreuves + "["+ "S"+tmpA.getId()+ ","+ "E"+tmpA.getId()+ ",Salle"+tmpA.getId()+"]\n";
                listTasks = listTasks + ", "+tmpA.getNbEtudiants()+", Salle"+tmpA.getId()+")\n";
            }else{
                tmpEpreuves = tmpEpreuves + "["+ "S"+tmpA.getId()+ ","+ "E"+tmpA.getId()+ ",Salle"+tmpA.getId()+"],\n";

                listTasks = listTasks + ", "+tmpA.getNbEtudiants()+", Salle"+tmpA.getId()+"),\n";
            }


            //liste des epreuves
            if (first){
                if (tmpA.getStart()<0){
                    tmpListEpreuves =  "["+ "S"+tmpA.getId()+ ","+ "E"+tmpA.getId();
                }else{
                    tmpListEpreuves =  "["+tmpA.getStart()+ ","+tmpA.getEnd();
                }
                listSalles = "[Salle"+tmpA.getId();
                first = false;
            }else{
                if (tmpA.getStart()<0){
                    tmpListEpreuves = tmpListEpreuves + ",S"+tmpA.getId()+ ","+ "E"+tmpA.getId();
                }else{
                    tmpListEpreuves = tmpListEpreuves + ",S"+tmpA.getId()+ ","+ "E"+tmpA.getId();
                }
                listSalles = listSalles + ",Salle"+tmpA.getId();
            }
        }
        tmpListEpreuves = tmpListEpreuves + "]";
        listSalles = listSalles + "]";
        String text = ":-set_prolog_flag(toplevel_print_options, [max_depth(0)]).\n" +
                ":- use_module(library(clpfd)).\n" +
                ":- use_module(library(lists)).\n" +
                "\n" +
                "incompatible(Debut1, Fin1, Debut2, Fin2, DeltaTime):-\n" +
                "        (DeltaTime #=< (Debut1 - Fin2)) #\\ (DeltaTime #=< (Debut2 - Fin1)).\n" +
                "\n" +
                "examensCompatibles(Salle1, Debut1, Fin1, Salle2, Debut2, Fin2):-\n" +
                "        ((Fin1 - Debut1 #= Fin2 - Debut2) #/\\ (Salle1 #= Salle2) #/\\ (Debut1 #= Debut2 #/\\ Fin1 #= Fin2))\n" +
                "        #\\ ((Fin1 - Debut1 #= Fin2 - Debut2) #/\\ (Salle1 #= Salle2) #/\\ ((Fin1 #=< Debut2) #\\ (Fin2 #=< Debut1)))\n" +
                "        #\\ ((Fin1 - Debut1 #= Fin2 - Debut2) #/\\ (Salle1 #\\= Salle2))\n" +
                "        #\\ ((Fin1 - Debut1 #\\= Fin2 - Debut2) #/\\ (Salle1 #= Salle2) #/\\ ((Fin1 #=< Debut2) #\\ (Fin2 #=< Debut1)))\n" +
                "        #\\ ((Fin1 - Debut1 #\\= Fin2 - Debut2) #/\\ (Salle1 #\\= Salle2)).\n" +
                "\n" +
                "compteNombreFoisSalleUtilisee(_Salle, _Debut, _Fin, [], Counter, Counter):-!.\n" +
                "compteNombreFoisSalleUtilisee(Salle1, Debut1, Fin1, [task(Debut2, _Duree2, Fin2, _Effectif2, Salle2) | ListeTask], Counter, Result):-\n" +
                "        (Salle1 #= Salle2 #/\\ Debut1 #= Debut2 #/\\ Fin1 #= Fin2 #/\\ NewCounter #= Counter + 1)\n" +
                "        #\\ ((#\\ (Salle1 #= Salle2 #/\\ Debut1 #= Debut2 #/\\ Fin1 #= Fin2)) #/\\ NewCounter #= Counter),\n" +
                "        compteNombreFoisSalleUtilisee(Salle1, Debut1, Fin1, ListeTask, NewCounter, Result),\n" +
                "        !.\n" +
                "\n" +
                "compteNombreSallesAffectees([], Counter, Counter):-!.\n" +
                "compteNombreSallesAffectees([task(Debut, _Duree, Fin, _Contenance, Salle) | ListeTask], Counter, Result):-\n" +
                "        compteNombreFoisSalleUtilisee(Salle, Debut, Fin, ListeTask, 0, NombreDoublon),\n" +
                "        (NombreDoublon #= 0 #/\\ NewCounter #= Counter + 1)\n" +
                "        #\\ (NombreDoublon #> 0 #/\\ NewCounter #= Counter),\n" +
                "        compteNombreSallesAffectees(ListeTask, NewCounter, Result),\n" +
                "        !.\n" +
                "\n" +
                "%generationTask(IdSalle, Contenance, ListeHorairesPris, Buffer, ListeTask)\n" +
                "generationTask(_IdSalle, _Contenance, [], Buffer, Buffer):-!.\n" +
                "generationTask(IdSalle, Contenance, [[Debut, Fin] | ListeHorairesPris], Buffer, ListeTask):-\n" +
                "        Duree #= Fin - Debut,\n" +
                "        append(Buffer, [task(Debut, Duree, Fin, Contenance, IdSalle)], NewBuffer),\n" +
                "        generationTask(IdSalle, Contenance, ListeHorairesPris, NewBuffer, ListeTask),\n" +
                "        !.\n" +
                "\n" +
                "generationMachine([], _NbActuelSalle, BufferMachines, BufferMachines, BufferTasks, BufferTasks):-!.\n" +
                "generationMachine([[_Salle, Contenance, HorairesPris] | ListeSalles], NbActuelSalles, BufferMachines, ListeMachines, BufferTasks, ListeTasks):-\n" +
                "        append(BufferMachines, [machine(NbActuelSalles, Contenance)], NewBufferMachines),\n" +
                "        generationTask(NbActuelSalles, Contenance, HorairesPris, BufferTasks, NewBufferTasks),\n" +
                "        NewNbSalles #= NbActuelSalles + 1,\n" +
                "        generationMachine(ListeSalles, NewNbSalles, NewBufferMachines, ListeMachines, NewBufferTasks, ListeTasks),\n" +
                "        !.\n" +
                "\n" +
                "generationExamens([], BufferTasks, BufferTasks):-!.\n" +
                "generationExamens([[_Examen, Duree, Effectif] | ResteExamens], BufferTasks, Result):-\n" +
                "        append(BufferTasks, [task(_Debut, Duree, _Fin, Effectif, _IdSalle)], NewBuffer),\n" +
                "        generationExamens(ResteExamens, NewBuffer, Result),\n" +
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
                "schedule(PriorityDuration,PrioritySalles, TimeOut, DeltaTime, Salles, ListeTasksSallesPrises,\n" +
                "         [" + tmpEpreuves + "],\n" +
                "         Result):-\n" +
                "\n" +
                "        Total = " + tmpListEpreuves + ",\n" +
                "\n" +
                "        % Déclaration du lundi 0h au vendredi 23h59 par 1/2 heure (48 points par jour).\n" +
                "        domain(Total, 0, 240),\n"+
                "Tasks = [\n" + listTasks + "],\n"+
                "append(Tasks, ListeTasksSallesPrises, NewTasks),\n";

                //TODO: Incopatibles double for avec un if
        for (EpreuvesCommune e : epreuvesCommunes ) {
            text += "incompatible(S" + e.getEpreuve(1).getId() + ",E" + e.getEpreuve(1).getId() + ",S"+ e.getEpreuve(2).getId() + ",E" + e.getEpreuve(2).getId() + ", DeltaTime),\n";
        }


           text += "        generationClausesExamensCompatiblesEnDuree(Tasks),\n" +
                    "\n" +
                    "        cumulatives(NewTasks, Salles, [bound(upper), task_intervals(true)]),";

        //TODO:  Calcul de la duree totale de tous les epreuves compatibles

        text +=  "       % Calcul de la durée totale des épreuves de M1"+
        "DM11 #= E1 - S1, DM12 #= E1 - S2, DM13 #= E1 - S3, DM14 #= E1 - S4, DM15 #= E1 - S5,"+
                "DM21 #= E2 - S1, DM22 #= E2 - S2, DM23 #= E2 - S3, DM24 #= E2 - S4, DM25 #= E2 - S5,"+
                "DM31 #= E3 - S1, DM32 #= E3 - S2, DM33 #= E3 - S3, DM34 #= E3 - S4, DM35 #= E3 - S5,"+
                "DM41 #= E4 - S1, DM42 #= E4 - S2, DM43 #= E4 - S3, DM44 #= E4 - S4, DM45 #= E4 - S5,"+
                "DM51 #= E5 - S1, DM52 #= E5 - S2, DM53 #= E5 - S3, DM54 #= E5 - S4, DM55 #= E5 - S5,"+
                "M1Durations = [DM11, DM12, DM13, DM14, DM15,"+
                "DM21, DM22, DM23, DM24, DM25,"+
                "DM31, DM32, DM33, DM34, DM35,"+
                "DM41, DM42, DM43, DM44, DM45,"+
                "DM51, DM52, DM53, DM54, DM55],"+
        "maximum(MaxDurationM1, M1Durations),"+

        "% Calcul de la durée totale des épreuves de L1"+
        "DL66 #= E6 - S6, DL67 #= E6 - S7, DL68 #= E6 - S8, DL69 #= E6 - S9,"+
                "DL76 #= E7 - S6, DL77 #= E7 - S7, DL78 #= E7 - S8, DL79 #= E7 - S9,"+
                "DL86 #= E8 - S6, DL87 #= E8 - S7, DL88 #= E8 - S8, DL89 #= E8 - S9,"+
                "DL96 #= E9 - S6, DL97 #= E9 - S7, DL98 #= E9 - S8, DL99 #= E9 - S9,"+
                "L1Durations = [DL66, DL67, DL68, DL69,"+
                "DL76, DL77, DL78, DL79,"+
                "DL86, DL87, DL88, DL89,"+
                "DL96, DL97, DL98, DL99],"+
        "maximum(MaxDurationL1, L1Durations),";



        text += "% Calcul du nombre de salles affectées\n" +
                "        compteNombreSallesAffectees(Tasks, 0, Result),\n" +
                "\n" +
                "        ToMinimize #= PriorityDuration * (MaxDurationM1 + MaxDurationL1) + PrioritySalles * Result,"+// * " + /* TODO : METTRE LES DUREES DES DIFFERENTES PROMOTIONS*/
                "\n      append(Total, " + listSalles + ", Vars),\n" +
                "        format('Before.~n', []),\n" +
                "\n" +
                "        statistics(runtime, [T0| _]),\n" +
                "        labeling([minimize(ToMinimize), time_out( TimeOut, _LabelingResult)], Vars),\n" +
                "        statistics(runtime, [T1|_]),\n" +
                "        TLabelling is T1 - T0,\n" +
                "        format('labeling took ~3d sec.~n', [TLabelling]).\n" +
                "\n" +
                "runSchedule(PriorityDuration,PrioritySalles, TimeOut, DeltaTime, ListeSalles, L, End) :-\n" +
                "        generationMachine(ListeSalles, 1, [], SallesMachines, [], ListeTasksPrises),\n" +
                "        statistics(runtime, [T0| _]),\n" +
                "        schedule(PriorityDuration, PrioritySalles, TimeOut, DeltaTime, SallesMachines, ListeTasksPrises, L, End),\n" +
                "        statistics(runtime, [T1|_]),\n" +
                "        T is T1 - T0,\n" +
                "        format('schedule/8 took ~3d sec.~n', [T]).";

        return text;
    }

    public void callSicstus(String filename){
        SICStus sp = null;
        HashMap results;

        // Loading the prolog file.
        try {

            // Creation d'un object SICStus
            sp = new SICStus();

            // Chargement d'un fichier prolog .pl
            sp.load("./"+filename);

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
            for (Salle s : salles) {
                if (salles.size() == i){
                    tmpSalles += s.toString();
                }else {
                    tmpSalles += s.toString() + ", ";
                }
            }
            tmpSalles += "]";

            String tmpEpreuves = "[";
            i = 1;
            for (Epreuve e: epreuves) {
                if (epreuves.size() == i){
                    tmpEpreuves += "E" + e.getId() + "]";
                }else{
                    tmpEpreuves += "E" + e.getId() + ",";
                }
                i++;

            }

                    System.out.println(tmpSalles);

            int facteurDuree = 0;
            int facteurNbSalle = 1;
            int timeOut = 10000;
            int deltaTime = 2;
            String requete = "runSchedule(" + facteurDuree + ", " + facteurNbSalle + ", " + timeOut + ", " + deltaTime +
                    ", "+tmpSalles+", "+tmpEpreuves+", End).";

            System.out.println(requete);
            Query qu = sp.openQuery(requete, results);
            qu.nextSolution();

            // on vérifie qu'il y a une solution
            if (!(results.isEmpty()))
            {
                System.out.println(results);

                // Extraction de la solution.
                String e1 = results.get("E1").toString();
                // ...
            }
            else
            {
                System.out.println("Error : No solution !");
            }

            // fermeture de la requète
            System.err.println("Fermeture requete");
            qu.close();
            results.clear();

        }
        catch (SPException e) {
            System.err.println("Exception prolog\n" + e);
        }
        // autres exceptions levées par l'utilisation du Query.nextSolution()
        catch (Exception e) {
            System.err.println("Other exception : " + e);
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("Emploi de temps");
        primaryStage.setScene(new Scene(root, 400, 375));
        primaryStage.show();
    }

    public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
        launch(args);
    }

    public static void openWebpage(URI uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(uri);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            Runtime runtime = Runtime.getRuntime();
            try {
               runtime.exec("xdg-open " + uri);
            } catch (IOException e) {
               // TODO Auto-generated catch block
               e.printStackTrace();
            }
        }
    }

    public static void openWebpage(URL url) {
        try {
            openWebpage(url.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

}
