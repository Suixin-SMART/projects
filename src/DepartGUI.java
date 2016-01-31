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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nomce on 3/7/14.
 */
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

    public void addTimetable(String[] input) {
        String[] t;
        Epreuve tmp;
        for(int i = 0; i < input.length; i++){
            if (!input[i].startsWith("End=")) {
                //split Analyse=.(50,.(55,.(1,[]))) in an array [Analyse, 50, 55,1]
                t = input[i].substring(0,input[i].length() - 6).split("=.\\(|,.\\(|,\\[\\]\\)\\)\\)");
                t[0] = t[0].substring(1);
                //System.out.println(t[0] + " " + t[1] + " " + t[2] + " " + t[3]);
                tmp = epreuves.get(Integer.parseInt(t[0]));
                tmp.setDebut(Integer.parseInt(t[1]));
                tmp.setFin(Integer.parseInt(t[2]));
                tmp.setSalle(Integer.parseInt(t[3]));
                System.out.println(tmp);
            }
        }
    }

    public static String readFile(String path, Charset encoding)
            throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
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
        for(Map.Entry<Integer, Epreuve> tmpEpreuve : epreuves.entrySet()) {

            //tasks
            listTasks = listTasks + "task(";
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
                tmpEpreuves = tmpEpreuves + "["+ "S"+tmpEpreuve.getValue().getId()+ ","+ "E"+tmpEpreuve.getValue().getId()+ ",Salle"+tmpEpreuve.getValue().getId()+"]\n";
                listTasks = listTasks + ", "+tmpEpreuve.getValue().getNbEtudiants()+", Salle"+tmpEpreuve.getValue().getId()+")\n";
            }else{
                tmpEpreuves = tmpEpreuves + "["+ "S"+tmpEpreuve.getValue().getId()+ ","+ "E"+tmpEpreuve.getValue().getId()+ ",Salle"+tmpEpreuve.getValue().getId()+"],\n";

                listTasks = listTasks + ", "+tmpEpreuve.getValue().getNbEtudiants()+", Salle"+tmpEpreuve.getValue().getId()+"),\n";
            }


            //liste des epreuves
            if (first){
                if (tmpEpreuve.getValue().getStart()<0){
                    tmpListEpreuves =  "["+ "S"+tmpEpreuve.getValue().getId()+ ","+ "E"+tmpEpreuve.getValue().getId();
                }else{
                    tmpListEpreuves =  "["+tmpEpreuve.getValue().getStart()+ ","+tmpEpreuve.getValue().getEnd();
                }
                listSalles = "[Salle"+tmpEpreuve.getValue().getId();
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

        for (EpreuvesCommune e : epreuvesCommunes ) {
            text += "incompatible(S" + e.getEpreuve(1).getId() + ",E" + e.getEpreuve(1).getId() + ",S"+ e.getEpreuve(2).getId() + ",E" + e.getEpreuve(2).getId() + ", DeltaTime),\n";
        }


           text += "        generationClausesExamensCompatiblesEnDuree(Tasks),\n" +
                    "\n" +
                    "        cumulatives(NewTasks, Salles, [bound(upper), task_intervals(true)]),\n";

        String dureeTotaleTableau;
        String regroupementsOptimisees = "";
        for (Regroupement r: regroupements){
            text += "% Calcul de la durée totale des épreuves de " + r.getName() + "\n";
            dureeTotaleTableau = "";
            for (Epreuve x : r.getEpreuves()){
                for (Epreuve y : r.getEpreuves()){
                    text += "D_" + r.getName() + "_" + x.getId() + "_" + y.getId() + " #= E" + x.getId() + " - S" + y.getId() + ",";
                    dureeTotaleTableau += "D_" + r.getName() + "_" + x.getId() + "_" + y.getId() + ",";
                }
                text += "\n";
            }

            text += r.getName()+"Durations = [" + dureeTotaleTableau.substring(0, dureeTotaleTableau.length() - 1) + "],\n";
            text += "maximum(MaxDuration" + r.getName() + ", " + r.getName() + "Durations),\n";
            regroupementsOptimisees += "MaxDuration" + r.getName() + "+";
        }

        text += "% Calcul du nombre de salles affectées\n" +
                "        compteNombreSallesAffectees(Tasks, 0, Result),\n" +
                "\n" +
                "        ToMinimize #= PriorityDuration * (" + regroupementsOptimisees.substring(0, regroupementsOptimisees.length() - 1) + ") + PrioritySalles * Result,"+// * " + /* TODO : METTRE LES DUREES DES DIFFERENTES PROMOTIONS*/
                "\n      append(Total, " + listSalles + ", Vars),\n" +
                "\n" +
                "        statistics(runtime, [T0| _]),\n" +
                "        labeling([minimize(ToMinimize), most_constrained, time_out( TimeOut, _LabelingResult)], Vars),\n" +
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

    public void callSicstus(String inputFile, int prioriteSalle, int prioriteDuree, int prioriteDist, int tOut, int dTime, String outputFile){
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
        primaryStage.setScene(new Scene(root, 800, 375));
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
