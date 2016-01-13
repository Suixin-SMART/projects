public class Epreuve{
    private String name;
    private int nbEtudiants;
    private int debut;
    private int duree;
    private int fin;
    private int jour;
    private int id;

    public Epreuve(int id, String name, int nb, int jour, int debut, int duree, int fin){
        this.name = name;
        this.nbEtudiants = nb;
        this.jour = jour;
        this.debut = debut;
        this.duree = duree;
        this.fin = fin;
        this.id = id;
    }

    public String toString(){
        return "Name: " + name + " NbEtud: " + nbEtudiants + " Jour: " + jour + " Debut: " + debut + " Duree: " + duree + " Fin: " + fin;
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
}
