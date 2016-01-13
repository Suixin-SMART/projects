import java.util.ArrayList;

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

    public void storeEpreuvesObjects(ArrayList<Epreuve> epreuves){
        for (Epreuve e : epreuves){
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
}
