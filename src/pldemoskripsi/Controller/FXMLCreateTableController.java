/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pldemoskripsi.Controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import org.voltdb.client.ClientResponse;
import org.voltdb.client.NoConnectionsException;
import org.voltdb.client.ProcCallException;

/**
 * FXML Controller class
 *
 * @author adit
 */
public class FXMLCreateTableController implements Initializable {

    @FXML
    private VBox vbMaster;

    @FXML
    private TextField tfTableName;

    @FXML
    private TextField tfJmlKolom;

    @FXML
    private Button btnSubmit;

    @FXML
    private Label labelNotif;

    private final FXMLDocumentController mainController;

    private Stage thisStage;

    private GridPane gp;

    private String namaTabel;
    private int jmlKolom;


    TextField[] tfNamaKolom;
    ComboBox[] cmbJenis;
    TextField[] tfPanjangKolom;
    CheckBox[] cbNullable;
    ComboBox[] cmbIndex;
    
    final ObservableList<String> listTipeData = FXCollections.observableArrayList("TINYINT", "SMALLINT", "INTEGER", "BIGINT", "FLOAT", "DECIMAL", "VARCHAR", "VARBINARY", "TIMESTAMP");

    final ObservableList<String> listTipeIndex = FXCollections.observableArrayList("--","PRIMARY KEY", "UNIQUE", "ASSUMEUNIQUE");
    
    public FXMLCreateTableController(FXMLDocumentController mainController) {
        this.mainController = mainController;

        // Create the new stage
        thisStage = new Stage();

        // Load the FXML file
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pldemoskripsi/View/FXMLCreateTable.fxml"));

            // Set this class as the controller
            loader.setController(this);

            // Load the scene
            thisStage.setScene(new Scene(loader.load()));

            // Setup the window/stage
            thisStage.setTitle("Perangkat Lunak Demo VoltDB - Create Table");

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
        this.tfTableName.setText(this.mainController.tfNamaTabel.getText());
        this.tfTableName.setEditable(false);
        this.tfJmlKolom.setText(this.mainController.tfJmlKolom.getText());
        this.tfJmlKolom.setEditable(false);

        this.jmlKolom = Integer.parseInt(tfJmlKolom.getText());
        this.namaTabel = tfTableName.getText();
        
        buildFormCreateTable();
    }

    /**
     * Method guna membuat form input pembuatan tabel
     */
    private void buildFormCreateTable() {
        Text text1 = new Text("Nama");
        Text text2 = new Text("Jenis");
        Text text3 = new Text("Panjang");
        Text text4 = new Text("Not null");
        Text text6 = new Text("Index");

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
        gp.add(text3, 2, 0);
        gp.add(text4, 3, 0);
        gp.add(text6, 4, 0);

        cbNullable = new CheckBox[jmlKolom];
        tfNamaKolom = new TextField[jmlKolom];
        tfPanjangKolom = new TextField[jmlKolom];
        cmbJenis = new ComboBox[jmlKolom];
        cmbIndex = new ComboBox[jmlKolom];
        

        //Pembuatan susunan textfield nama tabel
        for (int i = 0; i < tfNamaKolom.length; i++) {
            tfNamaKolom[i] = new TextField();
            gp.add(tfNamaKolom[i], 0, i + 1);
        }
        
        //Pembuatan susunan combobox jenis
        for (int i = 0; i < cmbJenis.length; i++) {
            cmbJenis[i] = new ComboBox();
            cmbJenis[i].setItems(listTipeData);
            cmbJenis[i].setValue("VARCHAR");
            gp.add(cmbJenis[i], 1, i + 1);
        }
        
        //Pembuatan susunan textfield panjang
        for (int i = 0; i < tfPanjangKolom.length; i++) {
            tfPanjangKolom[i] = new TextField();
            tfPanjangKolom[i].textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue,
                        String newValue) {
                    if (!newValue.matches("\\d*")) {
                        tfJmlKolom.setText(newValue.replaceAll("[^\\d]", ""));
                    }
                }
            });
            gp.add(tfPanjangKolom[i], 2, i + 1);
        }

        //Pembuatan susunan checkbox
        for (int i = 0; i < cbNullable.length; i++) {
            cbNullable[i] = new CheckBox();
            gp.add(cbNullable[i], 3, i + 1);
        }
        
        //Pembuatan susunan combobox index
        for (int i = 0; i < cmbIndex.length; i++) {
            cmbIndex[i] = new ComboBox();
            cmbIndex[i].setItems(listTipeIndex);
            cmbIndex[i].setValue("--");
            gp.add(cmbIndex[i], 4, i + 1);
        }

        vbMaster.getChildren().addAll(gp);

    }

    /**
     * Method guna menjalankan kueri CREATE TABLE
     * @throws IOException
     * @throws NoConnectionsException
     * @throws ProcCallException 
     */
    @FXML
    private void runQuery() throws IOException, NoConnectionsException, ProcCallException {
        try{
            ClientResponse response = this.mainController.client.callProcedure("@AdHoc",
                    constructQuery()
            );
            labelNotif.setText("Pembuatan table berhasil!");
            labelNotif.setStyle("-fx-text-fill: white;-fx-background-color:#4BB543;");
            mainController.refreshListTable();
            mainController.refreshListSP();
            mainController.refreshComboBoxPartisi();
        }catch(Exception e){
            labelNotif.setText("Pembuatan table gagal!");
            labelNotif.setStyle("-fx-text-fill: white;-fx-background-color:#ff0000;");
        }

    }

    /**
     * Method guna membangun kueri CREATE TABLE
     * @return 
     */
    private String constructQuery() {
        String createTableSql = "CREATE TABLE " + namaTabel + "("
                + generateKolom(tfNamaKolom, cmbJenis, tfPanjangKolom, cbNullable)
                + ");";
        return createTableSql;
    }

    /**
     * Method guna memproduksi kolom yang dipakai pada pembangungan kueri CREATE TABLE
     * @param tfNamaKolom
     * @param cmbJenis
     * @param tfPanjang
     * @param cbNullable
     * @return 
     */
    public String generateKolom(TextField[] tfNamaKolom, ComboBox[] cmbJenis, TextField[] tfPanjang, CheckBox[] cbNullable) {
        String res = "";
        for (int i = 0; i < jmlKolom; i++) {
            if(!cmbJenis[i].getValue().equals("VARCHAR")){
                res+= tfNamaKolom[i].getText()+" "+cmbJenis[i].getValue();
            } else{
                res+= tfNamaKolom[i].getText()+" "+cmbJenis[i].getValue()+"("+tfPanjang[i].getText()+")";
            }
            
            if(!cmbIndex[i].getValue().equals("--")){
                res+=" "+cmbIndex[i].getValue().toString();
            }
            
            if(cbNullable[i].selectedProperty().get()){
                res+=" NOT NULL";
            }
            
            if(i!=jmlKolom-1){
                res+=",";
            }
        }
        
        System.out.println(res);

        return res;
    }

}
