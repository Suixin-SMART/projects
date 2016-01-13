import java.util.ArrayList;

public class Salle{
    private String name;
    private int capacite;
    private ArrayList<Creneau> creneaux;

    public Salle(String name, int capacite){
        this.name = name;
        this.capacite = capacite;
        creneaux = new ArrayList<Creneau>();
    }

    public void addCreneau(int jour, int debut, int fin){
        creneaux.add(new Creneau(jour, debut, fin));
    }

    public String toString(){
        //["Amphi A", 94,[[0, 2], [8, 12], [16, 30], [34, 64], [68, 72], [82,108], [118,120]]]
        String text = "[ \"" + name + "\", " + capacite + ", [";

        int i = 1;
        for (Creneau e : creneaux) {
            if (creneaux.size() == i){
                text += "[" + e.toString() + "]";
            }else{
                text += "[" + e.toString() + "],";
            }
            i++;
        }
        text += "]]";

        return text;
    }
}
