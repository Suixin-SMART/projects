/**
 * Created by nomce on 14/01/16.
 */
public class Matiere {
    private String name;
    private int nbEtudiants;

    public Matiere(String name, int nb){
        this.name = name;
        this.nbEtudiants = nb;
    }

    public String getName() {
        return name;
    }

    public int getNbEtudiants() {
        return nbEtudiants;
    }
}
