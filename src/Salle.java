import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Salle{
    private String name;
    private int id;
    private int capacite;
    private HashMap<Integer,Creneau> creneaux;
    static int nbTotal = 0;
    private int nbCreneaux;

    public Salle(String name, int capacite){
        this.name = name;
        this.capacite = capacite;
        creneaux = new HashMap<>();
        nbTotal++;
        id = nbTotal;
        nbCreneaux = 0;
    }

    public void addCreneau(int jour, int debut, int fin){
        nbCreneaux++;
        creneaux.put(nbCreneaux,new Creneau(jour, debut, fin));
    }

    public String toString(){
        //["Amphi A", 94,[[0, 2], [8, 12], [16, 30], [34, 64], [68, 72], [82,108], [118,120]]]
        String text = "[ \"" + name + "\", " + capacite + ", [";

        int i = 1;
        if (!creneaux.isEmpty()) {
            for(Map.Entry<Integer, Creneau> entry : creneaux.entrySet()) {
                if (creneaux.size() == i) {
                    text += "[" + entry.getValue().toString() + "]";
                } else {
                    text += "[" + entry.getValue().toString() + "],";
                }
                i++;
            }
        }else{
            text+= "";
        }
        text += "]]";


        return text;
    }

    public String toStringXML(){
        //<salle name="Amphi_A" capacite="94">
        if (creneaux.size() == 0){
            return "        <salle name=\"" + name + "\" capacite=\"" + capacite + "\"/>\n";
        }else{
            String tmp = "      <salle name=\"" + name + "\" capacite=\"" + capacite + "\">\n";
            for(Map.Entry<Integer, Creneau> entry : creneaux.entrySet()) {
                tmp += entry.getValue().toStringXML();
            }
            tmp += "        </salle>\n";
            return tmp;
        }
    }

    public int getId(){
        return id;
    }

    public String getName(){
        return name;
    }
}
