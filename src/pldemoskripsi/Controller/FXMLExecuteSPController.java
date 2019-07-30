/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pldemoskripsi.Controller;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ResourceBundle;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.voltdb.VoltTable;
import org.voltdb.client.NoConnectionsException;
import org.voltdb.client.ProcCallException;

/**
 * FXML Controller class
 *
 * @author adit
 */
public class FXMLExecuteSPController implements Initializable {

    @FXML
    private TextField tfNamaSP;

    @FXML
    private Button btnExecSP;

    @FXML
    private VBox vbMaster;

    @FXML
    private Label labelNotif;

    private GridPane gp;

    TextField[] tfNilaiParam;
    ComboBox[] cmbTipe;

    Object[] obj;

    final ObservableList<String> listTipeData = FXCollections.observableArrayList("TINYINT", "SMALLINT", "INTEGER", "BIGINT", "FLOAT", "DECIMAL", "VARCHAR", "VARBINARY", "TIMESTAMP");

    private final FXMLDocumentController mainController;

    private Stage thisStage;

    private String namaSP;

    public FXMLExecuteSPController(FXMLDocumentController mainController) {
        this.mainController = mainController;

        // Create the new stage
        thisStage = new Stage();

        // Load the FXML file
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pldemoskripsi/View/FXMLExecuteSP.fxml"));

            // Set this class as the controller
            loader.setController(this);

            // Load the scene
            thisStage.setScene(new Scene(loader.load()));

            // Setup the window/stage
            thisStage.setTitle("Perangkat Lunak Demo VoltDB - Execute Stored Procedure");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Show the stage that was loaded in the constructor
     */
    public void showStage() {
        thisStage.showAndWait();
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
        this.namaSP = this.mainController.lvListSP.getSelectionModel().getSelectedItems().toString();
        this.tfNamaSP.setText(namaSP.substring(1, namaSP.length() - 1));
        this.tfNamaSP.setEditable(false);
        obj = new Object[this.mainController.jumlahParam];

        buildFormExecSP();
    }

    @FXML
    private void executeSP() throws IOException, NoConnectionsException, ProcCallException {
        try {
            generateObj();
            Class<?> params[] = new Class[obj.length];
            for (int i = 0; i < obj.length; i++) {
                if (obj[i] instanceof Long) {
                    params[i] = Long.TYPE;
                } else if (obj[i] instanceof String) {
                    params[i] = String.class;
                } else if (obj[i] instanceof Double) {
                    params[i] = Double.TYPE;
                }
            }

            this.mainController.vbResults.getChildren().removeAll(this.mainController.tableResults);
            this.mainController.tableResults = new TableView();
            
            Constructor<?> constructor = Class.forName("pldemoskripsi.Controller.FXMLExecuteSPController").getConstructor(pldemoskripsi.Controller.FXMLDocumentController.class);
            
            Object myObj = constructor.newInstance(this.mainController);
            
            Method myObjMethod = myObj.getClass().getMethod("runSP", Object[].class);
            myObjMethod.setAccessible(true);
            
            System.out.println(myObjMethod+" ");
            myObjMethod.invoke(myObj, new Object[] {obj});

            labelNotif.setText("Berhasil!");
            labelNotif.setStyle("-fx-text-fill: white;-fx-background-color:#4BB543;");

        } catch (Exception e) {
            labelNotif.setText("Gagal!");
            labelNotif.setStyle("-fx-text-fill: white;-fx-background-color:#ff0000;");
            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, e);
        }

    }
    
    public void runSP(Object... obj) throws IOException, NoConnectionsException, ProcCallException{
        this.mainController.vbResults.getChildren().removeAll(this.mainController.tableResults);
        this.mainController.tableResults = new TableView();
        this.mainController.resultsQuery = this.mainController.client.callProcedure(this.tfNamaSP.getText(), obj).getResults();
        this.mainController.displayQueryResults(this.mainController.resultsQuery);
        this.mainController.refreshListTable();
    }

    private void buildFormExecSP() {
        Text text1 = new Text("Nilai");
        Text text2 = new Text("Tipe");

        gp = new GridPane();

        //Setting size for the pane  
        gp.setMinSize(400, 200);

        //Setting the padding  
        gp.setPadding(new Insets(10, 10, 10, 10));

        //Setting the vertical and horizontal gaps between the columns 
        gp.setVgap(5);
        gp.setHgap(5);

        //Setting the Grid alignment 
        gp.setAlignment(Pos.CENTER);

        gp.add(text1, 0, 0);
        gp.add(text2, 1, 0);

        tfNilaiParam = new TextField[this.mainController.jumlahParam];
        cmbTipe = new ComboBox[this.mainController.jumlahParam];

        //Pembuatan susunan textfield nama tabel
        for (int i = 0; i < tfNilaiParam.length; i++) {
            tfNilaiParam[i] = new TextField();
            gp.add(tfNilaiParam[i], 0, i + 1);
        }

        //Pembuatan susunan combobox jenis
        for (int i = 0; i < cmbTipe.length; i++) {
            cmbTipe[i] = new ComboBox();
            cmbTipe[i].setItems(listTipeData);
            cmbTipe[i].setValue("VARCHAR");
            gp.add(cmbTipe[i], 1, i + 1);
        }

        vbMaster.getChildren().addAll(gp);

    }

    private void generateObj() {
        for (int i = 0; i < this.mainController.jumlahParam; i++) {
            if (cmbTipe[i].getSelectionModel().getSelectedItem().toString().equalsIgnoreCase("varchar")
                    || cmbTipe[i].getSelectionModel().getSelectedItem().toString().equalsIgnoreCase("timestamp")
                    || cmbTipe[i].getSelectionModel().getSelectedItem().toString().equalsIgnoreCase("varbinary")) {
                obj[i] = new String(tfNilaiParam[i].getText());
            } else if (cmbTipe[i].getSelectionModel().getSelectedItem().toString().equalsIgnoreCase("float")) {
                obj[i] = new Double(tfNilaiParam[i].getText());
            } else if (cmbTipe[i].getSelectionModel().getSelectedItem().toString().equalsIgnoreCase("TINYINT")
                    || cmbTipe[i].getSelectionModel().getSelectedItem().toString().equalsIgnoreCase("SMALLINT")
                    || cmbTipe[i].getSelectionModel().getSelectedItem().toString().equalsIgnoreCase("BIGINT")
                    || cmbTipe[i].getSelectionModel().getSelectedItem().toString().equalsIgnoreCase("INTEGER")) {
                obj[i] = new Long(tfNilaiParam[i].getText());
            }
            System.out.println(obj[i]);
            System.out.println(cmbTipe[i].getSelectionModel().getSelectedItem().toString());
        }
    }

}
