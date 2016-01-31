public class Epreuve{
    private String name;
    private int nbEtudiants;
    private int debut;
    private int duree;
    private int fin;
    private int jour;
    private int id;
    private int salle;

    public Epreuve(int id, String name, int nb, int jour, int debut, int duree, int fin, int salle){
        this.name = name;
        this.nbEtudiants = nb;
        this.jour = jour;
        this.debut = debut;
        this.duree = duree;
        this.fin = fin;
        this.id = id;
        this.salle = salle;
    }

    public String toString(){
        return "Name: " + name + " NbEtud: " + nbEtudiants + " Jour: " + jour + " Debut: " + debut + " Duree: " + duree + " Fin: " + fin + " Salle: " + salle;
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
        this.debut = debut;
    }

    public void setFin(int fin){
        this.fin = fin;
    }

    public int getIdSalle(){
        return salle;
    }

    public void setSalle(int salle){
        this.salle = salle;
    }
}
