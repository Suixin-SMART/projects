import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Created by nomce on 14/01/16.
 */
public class Regroupement {
    private String name;
    private ArrayList<Matiere> matieres;
    private ArrayList<Epreuve> epreuves;

    public Regroupement(String name){
        this.name = name;
        matieres = new ArrayList<Matiere>();
        epreuves = new ArrayList<Epreuve>();
    }

    public void addMatiere(String name, int nbEtudiants){
        matieres.add(new Matiere(name, nbEtudiants));
    }

    public void matchEpreuves(HashMap<Integer,Epreuve> input){
        for (Matiere m : matieres) {

            for(Map.Entry<Integer, Epreuve> entry : input.entrySet()) {
                if (m.getName().equals(entry.getValue().getName())){
                    epreuves.add(entry.getValue());
                }
            }
        }
    }

    public ArrayList<Epreuve> getEpreuves(){
        return epreuves;
    }
    public ArrayList<Matiere> getMatieres(){
        return matieres;
    }

    public String getName(){
        return name;
    }

    public String toStringXML(){
        //<regroupement name="K3">
        String tmp = "      <regroupement name=\"" + name + "\">\n";
        for(Matiere e : matieres) {
            tmp += e.toStringXML();
        }
        tmp += "      </regroupement>\n";
        return tmp;
    }

}
