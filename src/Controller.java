import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

public class Controller {

    @FXML
    private TextField importField,exportField,prioriteSalle,prioriteDuree,prioriteDist,
                        timeout,deltaTime,resultField,debutMin,finMax;
    @FXML
    private Button commencerButton;
    @FXML
    private TextField debutRepas, finRepas, dureeRepas;

    DepartGUI dep;

    /**
     * Action liée au bouton "Action!"
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws URISyntaxException
     */
    public void action() throws IOException, SAXException, ParserConfigurationException, URISyntaxException {
        dep = new DepartGUI(importField.getText());
        PrintWriter out = new PrintWriter(exportField.getText());
        out.print(dep.generateFileProlog(Integer.parseInt(debutMin.getText()),Integer.parseInt(finMax.getText()),
                Integer.parseInt(debutRepas.getText()),Integer.parseInt(finRepas.getText()),Integer.parseInt(dureeRepas.getText())));
        out.close();
    }

    /**
     * Action liée au bouton "Prolog"
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws URISyntaxException
     */

    public void action2() throws IOException, SAXException, ParserConfigurationException, URISyntaxException {
        String resultFile = resultField.getText() + ".txt";
        dep.callSicstus(exportField.getText(),Integer.parseInt(prioriteSalle.getText()),
                Integer.parseInt(prioriteDuree.getText()),Integer.parseInt(prioriteDist.getText()),
                Integer.parseInt(timeout.getText()),Integer.parseInt(deltaTime.getText()),resultFile);
    }

    /**
     * Action liee au bouton "Fin"
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws URISyntaxException
     */
    public void action3() throws IOException, SAXException, ParserConfigurationException, URISyntaxException {
        String inputFile = resultField.getText() + ".txt";
        String outputFile = resultField.getText() + ".xml";
        String[] result = DepartGUI.readFile(inputFile, Charset.defaultCharset()).split("\n");
        dep.addTimetable(result);
        try(  PrintWriter out = new PrintWriter(outputFile)  ){
            out.println( dep.generateXML() );
        }

        //Affichage visuel dans un table
        Stage stage = new Stage();
        Parent root = FXMLLoader.load(getClass().getResource("sample2.fxml"));
        stage.setTitle("Emploi de temps");
        stage.setScene(new Scene(root, 800, 600));
        stage .show();

    }
}