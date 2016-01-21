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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class Controller {

    @FXML
    private TextField importField,exportField,prioriteSalle,prioriteDuree,prioriteDist,timeout,deltaTime,resultField;
    @FXML
    private Button commencerButton;

    public void action() throws IOException, SAXException, ParserConfigurationException, URISyntaxException {
        DepartGUI dep = new DepartGUI(importField.getText());
        PrintWriter out = new PrintWriter(exportField.getText());
        out.print(dep.toString());
        out.close();
        dep.callSicstus(exportField.getText(),Integer.parseInt(prioriteSalle.getText()),Integer.parseInt(prioriteDuree.getText()),Integer.parseInt(prioriteDist.getText()),Integer.parseInt(timeout.getText()),Integer.parseInt(deltaTime.getText()),resultField.getText());
        //DepartGUI.openWebpage(getClass().getResource(exportField.getText())); //Paths.get(exportField.getText()).toUri()
    }
}