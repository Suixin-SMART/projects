import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;

public class XMLParseur extends DefaultHandler{

    private ArrayList<Epreuve> epreuves;
    private ArrayList<Salle> salles;
    private ArrayList<EpreuvesCommune> epreuvesCommunes;
    private ArrayList<Regroupement> regroupements;
    private Epreuve epreuve;
    private Salle salle;
    private EpreuvesCommune epreuveCommune;
    private StringBuffer buffer;
    private Regroupement regroupement;
    private int nbEpreuves = 1;

    public ArrayList<Epreuve> getEpreuves(){
        return epreuves;
    }
    public ArrayList<Salle> getSalles(){
        return salles;
    }
    public ArrayList<EpreuvesCommune> getEpreuvesCommunes(){
        for(EpreuvesCommune e : epreuvesCommunes){
            e.storeEpreuvesObjects(epreuves);
        }
        return epreuvesCommunes;
    }

    public ArrayList<Regroupement> getRegroupements(){
        for(Regroupement e : regroupements){
            e.matchEpreuves(epreuves);
        }
        return regroupements;
    }

    @Override
    public void startDocument() throws SAXException {
        epreuves = new ArrayList<Epreuve>();
        salles = new ArrayList<Salle>();
        epreuvesCommunes = new ArrayList<EpreuvesCommune>();
        regroupements = new ArrayList<Regroupement>();
    }

    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts)throws SAXException {
        if(localName == "epreuve"){
            int jour, heureFin, heureDebut;
            try{jour = Integer.parseInt(atts.getValue("jour"));}catch(NumberFormatException e){jour = -1;}
            try{heureDebut = Integer.parseInt(atts.getValue("heureDebut"));}catch(NumberFormatException e){heureDebut = -1;}
            try{heureFin = Integer.parseInt(atts.getValue("heureFin"));}catch(NumberFormatException e){heureFin = -1;}
            epreuve = new Epreuve(nbEpreuves++,atts.getValue("name"),
                                  Integer.parseInt(atts.getValue("nbEtudiants")),
                                    jour,heureDebut,
                                  Integer.parseInt(atts.getValue("duree")),heureFin);
        }else if(localName == "salle"){
            salle = new Salle(atts.getValue("name"),Integer.parseInt(atts.getValue("capacite")));
        }else if(localName == "epreuvesCommune"){
            epreuveCommune = new EpreuvesCommune(atts.getValue("idEpreuve1"),atts.getValue("idEpreuve2"));
        }else if(localName == "creneau-occupe"){
            salle.addCreneau(Integer.parseInt(atts.getValue("jour")),Integer.parseInt(atts.getValue("debut")),Integer.parseInt(atts.getValue("fin")));
        }else if(localName == "regroupement"){
            regroupement = new Regroupement(atts.getValue("name"));
        }else if(localName == "matiere"){
            regroupement.addMatiere(atts.getValue("name"),Integer.parseInt(atts.getValue("nbEtudiants")));
        }else{
            this.buffer = new StringBuffer();
        }
    }

    @Override
    public void characters(char[] chars, int start, int length) throws SAXException {
        String lecture = new String(chars,start,length);
        if(buffer != null)
            buffer.append(lecture);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

        if (localName == "epreuve") {
            epreuves.add(epreuve);
        }else if(localName == "salle"){
            salles.add(salle);
        }else if(localName == "epreuvesCommune"){
            epreuvesCommunes.add(epreuveCommune);
        }else if(localName == "regroupement"){
            regroupements.add(regroupement);
        }
    }

    @Override
    public void endDocument() throws SAXException {
        System.out.println("Finished parsing!");
    }
    /** END XML Handler **/
}


class MyErrorHandler implements ErrorHandler {
    private PrintStream out;

    MyErrorHandler(PrintStream out) {
        this.out = out;
    }

    private String getParseExceptionInfo(SAXParseException spe) {
        String systemId = spe.getSystemId();

        if (systemId == null) {
            systemId = "null";
        }

        return "URI=" + systemId + " Line="
                + spe.getLineNumber() + ": " + spe.getMessage();

    }

    public void warning(SAXParseException spe) throws SAXException {
        out.println("Warning: " + getParseExceptionInfo(spe));
    }

    public void error(SAXParseException spe) throws SAXException {
        String message = "Error: " + getParseExceptionInfo(spe);
        throw new SAXException(message);
    }

    public void fatalError(SAXParseException spe) throws SAXException {
        String message = "Fatal Error: " + getParseExceptionInfo(spe);
        throw new SAXException(message);
    }
}
