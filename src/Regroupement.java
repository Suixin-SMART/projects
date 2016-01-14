import java.util.ArrayList;
import java.util.StringJoiner;

/**
 * Created by nomce on 14/01/16.
 */
public class Regroupement {
    private String name;
    private ArrayList<Matiere> matieres;
    private ArrayList<Epreuve> epreuves;
    private boolean transfer = false;

    public Regroupement(String name){
        this.name = name;
        matieres = new ArrayList<Matiere>();
        epreuves = new ArrayList<Epreuve>();
    }

    public void addMatiere(String name, int nbEtudiants){
        matieres.add(new Matiere(name, nbEtudiants));
    }

    public void matchEpreuves(ArrayList<Epreuve> input){
        for (Matiere m : matieres) {
            for (Epreuve e : input){
                if (m.getName().equals(e.getName())){
                    epreuves.add(e);
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
}
