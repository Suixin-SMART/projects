import java.util.HashMap;

public class Epreuve{
    private String name;
    private int nbEtudiants;
    private int debut;
    private int duree;
    private int fin;
    private int jour;
    private int id;
    private Salle salle;
    private int salleTemp;

    public Epreuve(int id, String name, int nb, int jour, int debut, int duree, int fin, int salle){
        this.name = name;
        this.nbEtudiants = nb;
        this.jour = jour;
        this.debut = debut;
        this.duree = duree;
        this.fin = fin;
        this.id = id;
        this.salleTemp = salle;
    }

    public String toString(){
        return "Name: " + name + " NbEtud: " + nbEtudiants + " Jour: " + jour + " Debut: " + debut + " Duree: " + duree + " Fin: " + fin + " Salle: " + salle.getName();
    }

    public void setSalleAfter(HashMap<Integer,Salle> salles){
        salle = salles.get(Integer.valueOf(salleTemp));

    }

    public int getStart(){
        if (debut > 0){
            return (jour  * 48) + debut;
        }else{
            return -1;
        }
    }

    public int getEnd(){
        if (fin > 0){
            return (jour  * 48) + fin;
        }else{
            return -1;
        }
    }

    public int getDuree(){
        return duree;
    }

    public int getNbEtudiants(){
        return nbEtudiants;
    }

    public String getName(){
        return name;
    }

    public int getId(){
        return id;
    }

    public void setDebut(int debut){
        jour = (debut / 96) % 5 +1;
        this.debut = debut - 96 * (jour-1);
    }

    public void setFin(int fin){
        this.fin = fin - 96 * (jour-1);
    }

    public int getIdSalle(){
        return salle.getId();
    }

    public void setSalle(Salle s){
        this.salle = s;
    }

    public String toStringXML(){
        //<epreuve name="Graph" nbEtudiants="54" duree="4" />
        String tmp = "      <epreuve name=\"" + name + "\" nbEtudiants=\"" + nbEtudiants + "\" duree=\"" + duree + "\" ";
        if (jour > -1){
            tmp += "jour=\"" + jour + "\" ";
        }

        if (debut > -1){
            tmp += "heureDebut=\"" + debut + "\" ";
        }

        if (fin > -1){
            tmp += "heureFin=\"" + fin + "\" ";
        }

        if (salle == null){
            tmp += "salle=\"" + null + "\" ";
        }else{
            tmp += "salle=\"" + salle.getName() + "\" ";
        }

        tmp += "/>\n";
        return tmp;
    }
}
