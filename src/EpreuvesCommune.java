import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EpreuvesCommune{
  private Epreuve epreuve1;
  private Epreuve epreuve2;
  private String tempEpreuve1;
  private String tempEpreuve2;

  public EpreuvesCommune(Epreuve e1, Epreuve e2){
      epreuve1 = e1;
      epreuve2 = e2;
  }

  public EpreuvesCommune(String e1, String e2){
      tempEpreuve1 = e1;
      tempEpreuve2 = e2;
  }

    /**
     * Cette fonction est appelee apres le parsing du fichier XML en entree.
     * Les epreuves sont les pointeurs vers les epreuves, donc pour chaque tempEpreuve nous devons trouver
     * l'epreuve correspondant
     * @param epreuves HashMap des epreuves
     */
    public void storeEpreuvesObjects(HashMap<Integer,Epreuve> epreuves){

        for(Map.Entry<Integer, Epreuve> entry : epreuves.entrySet()) {
            Epreuve e = entry.getValue();
            if (e.getName().equals(tempEpreuve1)){
                epreuve1 = e;
            }
            if (e.getName().equals(tempEpreuve2)){
                epreuve2 = e;
            }
        }
    }

    public Epreuve getEpreuve(int id){
        if (id == 1){
            return epreuve1;
        }else{
            return epreuve2;
        }
    }

    public String toString(){
        if (epreuve1 != null){
            return "    " + epreuve1.toString() + "\n    " + epreuve2.toString() + "\n";
        }else{
            return "    " + tempEpreuve1.toString() + "\n    " + tempEpreuve2.toString() + "\n";
        }
    }

    public String toStringXML(){
        //<epreuvesCommune idEpreuve1="Graph" idEpreuve2="GL" />
        return "        <epreuvesCommune idEpreuve1=\"" + epreuve1.getName() + "\" idEpreuve2=\"" + epreuve2.getName() + "\" />\n";
    }
}
