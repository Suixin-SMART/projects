import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class Controller {

    @FXML
    private TextField importField,exportField,prioriteSalle,prioriteDuree,prioriteDist,
                        timeout,deltaTime,resultField,debutMin,finMax;
    @FXML
    private Button commencerButton;
    @FXML
    private TextField debutRepas, finRepas, dureeRepas;
    @FXML
    private TableView<String> tView;
    @FXML
    private TableColumn<Epreuve, String> salleCol,lundiCol, mardiCol, mercrediCol, jeudiCol, vendrediCol;

    DepartGUI dep;

    public void action() throws IOException, SAXException, ParserConfigurationException, URISyntaxException {
        dep = new DepartGUI(importField.getText());
        PrintWriter out = new PrintWriter(exportField.getText());
        out.print(dep.generateFileProlog(Integer.parseInt(debutMin.getText()),Integer.parseInt(finMax.getText()),
                Integer.parseInt(debutRepas.getText()),Integer.parseInt(finRepas.getText()),Integer.parseInt(dureeRepas.getText())));
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

        //Creating tree items
        /*TreeItem<Epreuve> childNode1 = new TreeItem<>(dep.);
        TreeItem<Epreuve> childNode2 = new TreeItem<>("Child Node 2");
        TreeItem<Epreuve> childNode3 = new TreeItem<>("Child Node 3");

        //Creating the root element
        final TreeItem<String> rootbla = new TreeItem<>("Root node");
        rootbla.setExpanded(true);

        //Adding tree items to the root
        rootbla.getChildren().setAll(childNode1, childNode2, childNode3);
        salleCol.setCellValueFactory();
        */

        final ObservableList<Epreuve> dataEpreuves = FXCollections.
                                        observableArrayList(new ArrayList<Epreuve>(dep.getEpreuves().values()));
        final ObservableList<Salle> dataSalles = FXCollections.
                observableArrayList(new ArrayList<Salle>(dep.getSalles().values()));

        salleCol.setCellValueFactory(new PropertyValueFactory<Epreuve,String>("sallePublic"));



        Stage stage = new Stage();
        Parent root = FXMLLoader.load(getClass().getResource("sample2.fxml"));
        stage.setTitle("Emploi de temps");
        stage.setScene(new Scene(root, 800, 600));
        stage .show();

    }
}