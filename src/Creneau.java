import java.util.ArrayList;

public class Creneau{
    private int debut;
    private int fin;
    private int jour;

    public Creneau(int jour,int debut, int fin){
        this.debut = debut;
        this.fin = fin;
        this.jour = jour;
    }

    public String toString(){
        return (debut + ((jour - 1) * 96)) + ", " + (fin + ((jour - 1) * 96));
    }

    public String toStringXML(){
        //<creneau-occupe jour="" debut="" fin=""/>
        return  "           <creneau-occupe jour=\"" + jour + "\" debut=\"" + debut + "\" fin=\"" + fin + "\"/>\n";
    }
}
