import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.xml.sax.SAXException;
import se.sics.jasper.Query;
import se.sics.jasper.SICStus;
import se.sics.jasper.SPException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class Controller {

    @FXML
    private TextField importField,exportField,prioriteSalle,prioriteDuree,prioriteDist,timeout,deltaTime,resultField,debutMin,finMax;
    @FXML
    private Button commencerButton;
    DepartGUI dep;

    public void action() throws IOException, SAXException, ParserConfigurationException, URISyntaxException {
        dep = new DepartGUI(importField.getText());
        PrintWriter out = new PrintWriter(exportField.getText());
        out.print(dep.generateFileProlog(Integer.parseInt(debutMin.getText()),Integer.parseInt(finMax.getText())));
        out.close();
    }

    public void action2() throws IOException, SAXException, ParserConfigurationException, URISyntaxException {
        String resultFile = resultField.getText() + ".txt";
        dep.callSicstus(exportField.getText(),Integer.parseInt(prioriteSalle.getText()),
                Integer.parseInt(prioriteDuree.getText()),Integer.parseInt(prioriteDist.getText()),
                Integer.parseInt(timeout.getText()),Integer.parseInt(deltaTime.getText()),resultFile);
    }

    public void action3() throws IOException, SAXException, ParserConfigurationException, URISyntaxException {
        String inputFile = resultField.getText() + ".txt";
        String outputFile = resultField.getText() + ".xml";
        String[] result = DepartGUI.readFile(inputFile, Charset.defaultCharset()).split("\n");
        dep.addTimetable(result);
        try(  PrintWriter out = new PrintWriter(outputFile)  ){
            out.println( dep.generateXML() );
        }
    }
}