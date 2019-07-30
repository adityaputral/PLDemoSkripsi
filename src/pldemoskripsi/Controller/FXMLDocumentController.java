/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pldemoskripsi.Controller;

import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ResourceBundle;
import java.util.logging.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.*;
import javafx.collections.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Callback;

import org.voltdb.*;
import org.voltdb.client.*;

/**
 *
 * @author adit
 */
public class FXMLDocumentController implements Initializable {

    ////////////////////////LEFT SIDE//////////////////
    @FXML
    private AnchorPane apTabel;

    @FXML
    private AnchorPane apSP;

    @FXML
    private Accordion accordionListTables;

    @FXML
    public ListView lvListSP;

    @FXML
    public TextField tfNamaTabel;

    @FXML
    public TextField tfJmlKolom;

    @FXML
    private Button btnCreateTabel;
    ////////////////////////LEFT SIDE/////////////////

    //===============================================//
    /////////////////////TAB JELAJAHI/////////////////
    @FXML
    private VBox vbJelajah;

    @FXML
    private VBox vbSPSQLStatement;

    @FXML
    private VBox vbSPExecPlan;
    /////////////////////TAB JELAJAHI/////////////////

    //===============================================//
    /////////////////////TAB PARTISI/////////////////
    @FXML
    private ComboBox cbNamaTabelPartisi;

    @FXML
    private TextField tfNamaKolomPartisi;

    @FXML
    private Button btnPartisi;
    /////////////////////TAB PARTISI/////////////////

    //===============================================//
    /////////////////////TAB SQL/////////////////
    @FXML
    private Button btnSubmitQuery;

    @FXML
    private TextArea taAdHocQuery;
    /////////////////////TAB SQL/////////////////

    //===============================================//
    //////////////TAB OPERASI LAINNYA///////////////
    @FXML
    private Button btnTruncateDb;

    @FXML
    private Button btnDropDb;

    @FXML
    private TextField tfTabelDropTruncate;

    //===============================================//
    //////////////RIGHT SIDE///////////////
    @FXML
    public VBox vbResults;
    //////////////RIGHT SIDE///////////////

    //===============================================//
    //////////////FOOTER///////////////
    @FXML
    private Label labelNotification;
    //////////////FOOTER///////////////

    TitledPane[] tpsTable;

    Label[][] labels;
    TableView tableResults = new TableView();
    TableView tableJelajah = new TableView();

    TextArea taSPStatement = new TextArea();
    TextArea taSPExecPlan = new TextArea();

    ContextMenu cmExecSP = new ContextMenu();
    MenuItem menuItemExecute = new MenuItem("Execute");
    MenuItem menuItemDrop = new MenuItem("Drop");

    VoltTable[] resultsTable;
    VoltTable[] resultsColumn;

    VoltTable[] resultsQuery;

    VoltTable[] resultsSP;

    org.voltdb.client.Client client = null;
    ClientConfig config = null;

    ClientStatsContext periodicStatsContext;
    long benchmarkStart;

    int jumlahParam;

    @FXML
    private Stage stage;
    private Scene scene;
    Parent P;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            //Pembangunan koneksi ke database VoltDB
            config = new ClientConfig("", "");
            client = ClientFactory.createClient(config);
            client.createConnection("localhost", 21211);

            //Menampilkan daftar tabel pada database VoltDB
            displayListTables();

            //Pengaturan panjang dan lebar accordion daftar tabel pada scroll pane
            apTabel.setPrefWidth(accordionListTables.getPrefWidth());
            apTabel.setPrefHeight(accordionListTables.getPrefHeight());

            //Menampilkan daftar stored procedure pada database VoltDB
            displayListSP();

            //Pengaturan panjang dan lebar accordion stored procedure pada scroll pane
            apSP.setPrefWidth(lvListSP.getPrefWidth());
            apSP.setPrefHeight(lvListSP.getPrefHeight());

            cmExecSP.getItems().add(menuItemExecute);
            cmExecSP.getItems().add(menuItemDrop);

            lvListSP.setContextMenu(cmExecSP);

            //Build combo box pada tab partisi
            for (int i = 0; i < accordionListTables.getPanes().size(); i++) {
                cbNamaTabelPartisi.getItems().add(accordionListTables.getPanes().get(i).getText());
            }
            
            if(tpsTable.length!=0){
                cbNamaTabelPartisi.setValue(accordionListTables.getPanes().get(0).getText());
            }

            //Listener untuk menampilkan seluruh isi tabel yang di pilih pada accordion daftar tabel
            tfJmlKolom.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue,
                        String newValue) {
                    if (!newValue.matches("\\d*")) {
                        tfJmlKolom.setText(newValue.replaceAll("[^\\d]", ""));
                    }
                }
            });

        } catch (IOException ex) {
            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Method guna menampilkan daftar stored procedure yang ada dalam database
     * VoltDB
     */
    private void displayListSP() {
        try {
            resultsTable = client.callProcedure("@SystemCatalog",
                    "PROCEDURES").getResults();
            retrieveListSP(resultsTable);

            lvListSP.getSelectionModel().selectedItemProperty().addListener(
                    new ChangeListener<String>() {
                public void changed(ObservableValue<? extends String> ov,
                        String old_val, String new_val) {
                    try {
                        runProcColSP(new_val);
                        initJelajahSP(new_val);
                        dropStoredProcedure(new_val);
                        executeStoredProcedure();
                    } catch (ProcCallException ex) {
                        Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method guna mendapatkan seluruh nama stored procedure yang ada dalam
     * database VoltDB
     *
     * @param result hasil dari eksekusi kueri pada method displayListSP
     */
    private void retrieveListSP(VoltTable[] result) {
        for (VoltTable results : result) {
            getSPName(results);
        }
    }

    /**
     * Method guna mengkoleksi nama dari stored procedure
     *
     * @param t1
     */
    public void getSPName(VoltTable t1) {
        int i = 0;
        int j = 0;
        String tempRes = "";
        ObservableList<String> data = FXCollections.observableArrayList();
        while (t1.advanceRow()) {
            String resColumn = "";
            String spName = t1.getString(t1.getColumnIndex("SPECIFIC_NAME"));
            data.add(spName);
            lvListSP.setItems(data);
            i++;
        }

    }

    /**
     * Method guna menginisiasi pembuatan text area yang berisi SQL Statement
     * dan Execution plan dari stored procedure yang dipilih pada tab jelajahi
     *
     * @param kueriSql
     */
    private void initJelajahSP(String namaSP) throws NoConnectionsException, ProcCallException, IOException {

        vbSPExecPlan.getChildren().removeAll(taSPExecPlan);
        vbSPSQLStatement.getChildren().removeAll(taSPStatement);

        taSPExecPlan = new TextArea();
        taSPStatement = new TextArea();

        taSPExecPlan.setEditable(false);
        taSPStatement.setEditable(false);

        resultsQuery = client.callProcedure("@ExplainProc",
                namaSP).getResults();
        displayJelajahSP(resultsQuery);
    }

    /**
     * Method guna menampilkan execution plan dan SQL Statement dari stored
     * procedure yang dipilih pada tab Jelajahi
     *
     * @param results
     */
    public void displayJelajahSP(VoltTable[] results) {

        for (VoltTable result : results) {
            buildJelajahSP(result);
        }
        vbSPExecPlan.getChildren().add(taSPExecPlan);
        vbSPSQLStatement.getChildren().add(taSPStatement);
    }

    /**
     * Method guna mengisi text area SQL Statement dan execution plan dari
     * stored procedure pada tab jelajahi
     *
     * @param t
     */
    public void buildJelajahSP(VoltTable t) {
        int i = 0;
        int j = 0;
        String tempRes = "";
        while (t.advanceRow()) {
            String resColumn = "";
            String spName = t.getString(t.getColumnIndex("SQL_STATEMENT"));
            taSPStatement.setText(spName);

            String execPlan = t.getString(t.getColumnIndex("EXECUTION_PLAN"));
            taSPExecPlan.setText(execPlan);

            i++;
        }
    }

    private void dropStoredProcedure(String namaSP) {
        String kueri = "DROP PROCEDURE " + namaSP + ";";
        menuItemDrop.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    client.callProcedure("@AdHoc", kueri);
                    refreshListSP();
                } catch (IOException ex) {
                    Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ProcCallException ex) {
                    Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    private void executeStoredProcedure() {
        if (lvListSP.getSelectionModel().isEmpty()) {
            labelNotification.setText("Gagal - Harap pilih stored procedure yang akan dieksekusi.");
            labelNotification.setStyle("-fx-text-fill: white;-fx-background-color:#ff0000;");
        } else {
            FXMLExecuteSPController esp = new FXMLExecuteSPController(this);
            menuItemExecute.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    esp.showStage();
                }
            });
        }
    }

    /**
     * Method untuk berpindah ke scene pembuatan tabel
     *
     * @throws IOException
     */
    @FXML
    private void createTable() throws IOException {
        if (tfNamaTabel.getText() == null || tfNamaTabel.getText().trim().isEmpty()
                || tfJmlKolom.getText() == null || tfJmlKolom.getText().trim().isEmpty()) {
            labelNotification.setText("Gagal - Harap isi nama tabel dan jumlah kolom.");
            labelNotification.setStyle("-fx-text-fill: white;-fx-background-color:#ff0000;");
        } else {
            FXMLCreateTableController ct = new FXMLCreateTableController(this);
            ct.showStage();
        }
    }

    /**
     * Method guna menginisiasi pembuatan tabel pada tab
     *
     * @param kueriSql Jelajahi
     */
    private void initJelajah(String kueriSql) throws NoConnectionsException, ProcCallException, IOException {
        vbJelajah.getChildren().removeAll(tableJelajah);
        tableJelajah = new TableView();
        resultsQuery = client.callProcedure("@AdHoc",
                kueriSql
        ).getResults();
        displayJelajah(resultsQuery);
    }

    /**
     * Method guna menampilkan isi dari tab Jelajahi
     *
     * @param results
     */
    public void displayJelajah(VoltTable[] results) {

        for (VoltTable result : results) {
            buildTableJelajah(result);
        }
        vbJelajah.getChildren().addAll(tableJelajah);
    }

    public void buildTableJelajah(VoltTable t) {
        final int colCount = t.getColumnCount();
        TableColumn[] colResult = new TableColumn[colCount];
        ObservableList<String> data = FXCollections.observableArrayList();

        int rowCount = 1;
        t.resetRowPosition();
        while (t.advanceRow()) {
            rowCount++;
            data = FXCollections.observableArrayList();
            for (int col = 0; col < colCount; col++) {
                final int j = col;
                colResult[col] = new TableColumn<>(t.getColumnName(col));
                switch (t.getColumnType(col)) {
                    case TINYINT:
                    case SMALLINT:
                    case BIGINT:
                    case INTEGER:
                        data.add(t.getLong(col) + "");
                        break;
                    case VARBINARY:
                        byte[] bt = t.getVarbinary(col);
                        String s = new String(bt);
                        data.add(s);
                        break;
                    case STRING:
                        data.add(t.getString(col) + "");
                        break;
                    case TIMESTAMP:
                        data.add(t.getTimestampAsTimestamp(col) + "");
                        break;
                    case DECIMAL:
                        data.add(t.getDecimalAsBigDecimal(col) + "");
                        break;
                    case FLOAT:
                        data.add(t.getDouble(col) + "");
                        break;
                }
                if (colCount > 1) {
                    colResult[col].setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ObservableList, String>, ObservableValue<String>>() {
                        public ObservableValue<String> call(TableColumn.CellDataFeatures<ObservableList, String> param) {
                            return new SimpleStringProperty(param.getValue().get(j).toString());
                        }
                    });
                }
                if (rowCount == 2) {
                    tableJelajah.getColumns().addAll(colResult[col]);
                }

            }
            tableJelajah.getItems().add(data);
        }

    }

    /**
     * Method guna menampilkan daftar tabel yang ada dalam database VoltDB
     */
    private void displayListTables() {
        try {
            resultsTable = client.callProcedure("@SystemCatalog",
                    "TABLES").getResults();
            resultsColumn = client.callProcedure("@SystemCatalog",
                    "COLUMNS").getResults();
            retrieveListTables(resultsTable, resultsColumn);
            accordionListTables.getPanes().addAll(tpsTable);

            accordionListTables.expandedPaneProperty().addListener(new ChangeListener<TitledPane>() {
                @Override
                public void changed(ObservableValue<? extends TitledPane> ov,
                        TitledPane old_val, TitledPane new_val) {
                    if (new_val != null) {
                        try {
                            initJelajah("select * from " + accordionListTables.getExpandedPane().getText() + ";");
                        } catch (ProcCallException ex) {
                            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IOException ex) {
                            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            });

            if(tpsTable.length!=0){
                accordionListTables.setExpandedPane(tpsTable[0]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method guna mendapatkan seluruh nama tabel yang ada dalam database VoltDB
     *
     * @param results hasil dari eksekusi perintah untuk mendapatkan daftar
     * tabel pada method displayListTables
     * @param results2 hasil dari eksekusi perintah untuk mendapatkan daftar
     * tabel pada method displayListTables
     */
    public void retrieveListTables(VoltTable[] results, VoltTable[] results2) {
        for (VoltTable result : results) {
            tpsTable = new TitledPane[result.getRowCount()];
            for (VoltTable result2 : results2) {
                labels = new Label[result.getRowCount()][result2.getRowCount()];
                getTablesName(result, result2);
            }
        }
    }

    /**
     * Method guna mengkoleksi nama tabel dan daftar kolom yang ada pada tabel
     * tersebut
     *
     * @param t1
     */
    public void getTablesName(VoltTable t1, VoltTable t2) {
        int i = 0;
        int j = 0;
        String tempRes = "";
        while (t1.advanceRow()) {
            String resColumn = "";
            String tableName = t1.getString(t1.getColumnIndex("TABLE_NAME"));
            String tableType = t1.getString(t1.getColumnIndex("TABLE_TYPE"));
            do {
                if (t2.advanceRow() && t2.getString(t2.getColumnIndex("TABLE_NAME")).equalsIgnoreCase(tableName)) {
                    String condition = t2.getString(t2.getColumnIndex("TABLE_NAME"));
                    String columnName = t2.getString(t2.getColumnIndex("COLUMN_NAME"));
                    String typeName = t2.getString(t2.getColumnIndex("TYPE_NAME"));
                    long columnSize = t2.getLong(t2.getColumnIndex("COLUMN_SIZE"));
                    String partitionColumn = "";
                    if (t2.getString(t2.getColumnIndex("REMARKS")) != null) {
                        partitionColumn = "[" + t2.getString(t2.getColumnIndex("REMARKS")) + "]";
                    }
                    resColumn += tempRes + columnName + " - " + typeName + "(" + columnSize + ") " + partitionColumn + "\n";
                    tempRes = "";
                } else {
                    String tempcolumnName = t2.getString(t2.getColumnIndex("COLUMN_NAME"));
                    String tempTypeName = t2.getString(t2.getColumnIndex("TYPE_NAME"));
                    long tempColumnSize = t2.getLong(t2.getColumnIndex("COLUMN_SIZE"));
                    String tempPartitionColumn = "";
                    if (t2.getString(t2.getColumnIndex("REMARKS")) != null) {
                        tempPartitionColumn = "[" + t2.getString(t2.getColumnIndex("REMARKS")) + "]";
                    }
                    tempRes = tempcolumnName + " - " + tempTypeName + "(" + tempColumnSize + ")" + tempPartitionColumn + "\n";
                    break;
                }

            } while (t2.getString(t2.getColumnIndex("TABLE_NAME")).equalsIgnoreCase(tableName));

            tpsTable[i] = new TitledPane(tableName, new Label(resColumn));
            i++;
        }
    }

    private void runProcColSP(String namaSP) throws NoConnectionsException, ProcCallException, IOException {
        resultsQuery = client.callProcedure("@SystemCatalog",
                "PROCEDURECOLUMNS"
        ).getResults();
        displayProcColSPResults(resultsQuery, namaSP);
    }

    public void displayProcColSPResults(VoltTable[] results, String namaSP) {
        for (VoltTable result : results) {
            displayFinalProcColSP(result, namaSP);
        }
    }

    public void displayFinalProcColSP(VoltTable t, String namaSP) {
        int p = 0;
        final int colCount = t.getColumnCount();
        int rowCount = 1;
        t.resetRowPosition();
        while (t.advanceRow()) {
            if (t.getString(t.getColumnIndex("PROCEDURE_NAME")).equalsIgnoreCase(namaSP)) {
                p++;
            }
        }
        System.out.println(p);
        this.jumlahParam = p;
    }

    /**
     * Method guna mengirimkan kueri ke database VoltDB dan memberikan feedback
     * status keberhasilan pengeksekusian kueri
     *
     * @throws ProcCallException
     * @throws IOException
     */
    @FXML
    private void submitQuery() throws ProcCallException, IOException {
        try {
            long startTime = System.nanoTime();
            runQuery(taAdHocQuery.getText().replaceAll("\n", System.getProperty("line.separator")));
            long endTime = System.nanoTime();

            double execTime = (endTime - startTime) / 1_000_000_000.0;

            labelNotification.setText("Success - " + new DecimalFormat("#.#####").format(execTime) + " seconds");
            labelNotification.setStyle("-fx-text-fill: white;-fx-background-color:#4BB543;");
            refreshListTable();
            refreshComboBoxPartisi();
            refreshListSP();
        } catch (Exception ex) {
            labelNotification.setText("Failed");
            labelNotification.setStyle("-fx-text-fill: white;-fx-background-color:#ff0000;");
            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Method guna mengirimkan kueri ke database VoltDB
     *
     * @param kueriSql kueri SQL yang di input oleh user
     * @throws NoConnectionsException
     * @throws ProcCallException
     * @throws IOException
     */
    private void runQuery(String kueriSql) throws NoConnectionsException, ProcCallException, IOException {
        vbResults.getChildren().removeAll(tableResults);
        tableResults = new TableView();

        resultsQuery = client.callProcedure("@AdHoc",
                kueriSql
        ).getResults();
        displayQueryResults(resultsQuery);
    }

    /**
     * Method guna menampilkan hasil kueri yang di input oleh user
     */
    public void displayQueryResults(VoltTable[] results) {
        for (VoltTable result : results) {
            displayTable(result);
        }
        vbResults.getChildren().addAll(tableResults);
    }

    /**
     * Method guna menampilkan hasil eksekusi kueri dalam bentuk tabel
     *
     * @param t hasil eksekusi kueri yang diinput oleh user
     */
    public void displayTable(VoltTable t) {
        final int colCount = t.getColumnCount();
        TableColumn[] colResult = new TableColumn[colCount];
        ObservableList<String> data = FXCollections.observableArrayList();
        int rowCount = 1;
        t.resetRowPosition();
        while (t.advanceRow()) {
            rowCount++;

            data = FXCollections.observableArrayList();
            for (int col = 0; col < colCount; col++) {
                final int j = col;
                colResult[col] = new TableColumn<>(t.getColumnName(col));

                switch (t.getColumnType(col)) {
                    case TINYINT:
                    case SMALLINT:
                    case BIGINT:
                    case INTEGER:
                        data.add(t.getLong(col) + "");
                        break;
                    case VARBINARY:
                        data.add(t.getVarbinary(col) + "");
                        break;
                    case STRING:
                        data.add(t.getString(col) + "");
                        break;
                    case TIMESTAMP:
                        data.add(t.getTimestampAsTimestamp(col) + "");
                        break;
                    case DECIMAL:
                        data.add(t.getDecimalAsBigDecimal(col) + "");
                        break;
                    case FLOAT:
                        data.add(t.getDouble(col) + "");
                        break;
                }
                if (colCount > 1) {
                    colResult[col].setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ObservableList, String>, ObservableValue<String>>() {
                        public ObservableValue<String> call(TableColumn.CellDataFeatures<ObservableList, String> param) {
                            return new SimpleStringProperty(param.getValue().get(j).toString());
                        }
                    });
                }

                if (rowCount == 2) {
                    tableResults.getColumns().addAll(colResult[col]);
                }

            }
            tableResults.getItems().add(data);
        }

    }

    /**
     * Method guna melakukan drop tabel
     */
    @FXML
    private void dropTable() {
        try {
            String kueriSql = "DROP TABLE " + tfTabelDropTruncate.getText() + ";";
            long startTime = System.nanoTime();
            client.callProcedure("@AdHoc",
                    kueriSql
            );
            long endTime = System.nanoTime();

            double execTime = (endTime - startTime) / 1_000_000_000.0;

            labelNotification.setText("Success - " + new DecimalFormat("#.#####").format(execTime) + " seconds");

            labelNotification.setStyle("-fx-text-fill: white;-fx-background-color:#4BB543;");
            refreshListTable();
            refreshComboBoxPartisi();
            refreshListSP();
        } catch (Exception ex) {
            labelNotification.setText("Failed");
            labelNotification.setStyle("-fx-text-fill: white;-fx-background-color:#ff0000;");
            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Method guna melakukan truncate tabel
     */
    @FXML
    private void truncateTable() {
        try {
            String kueriSql = "TRUNCATE TABLE " + tfTabelDropTruncate.getText() + ";";
            long startTime = System.nanoTime();
            client.callProcedure("@AdHoc",
                    kueriSql
            );
            long endTime = System.nanoTime();

            double execTime = (endTime - startTime) / 1_000_000_000.0;

            labelNotification.setText("Success - " + new DecimalFormat("#.#####").format(execTime) + " seconds");

            labelNotification.setStyle("-fx-text-fill: white;-fx-background-color:#4BB543;");
            refreshListTable();
            refreshListSP();
        } catch (Exception ex) {
            labelNotification.setText("Failed");
            labelNotification.setStyle("-fx-text-fill: white;-fx-background-color:#ff0000;");
            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Method guna melakukan partisi kolom pada tabel
     */
    @FXML
    private void partitionTable() {
        try {
            String kueriSql = "PARTITION TABLE " + cbNamaTabelPartisi.getSelectionModel().getSelectedItem().toString() + " ON COLUMN " + tfNamaKolomPartisi.getText() + ";";
            long startTime = System.nanoTime();
            client.callProcedure("@AdHoc",
                    kueriSql
            );
            long endTime = System.nanoTime();

            double execTime = (endTime - startTime) / 1_000_000_000.0;

            labelNotification.setText("Success - " + new DecimalFormat("#.#####").format(execTime) + " seconds");

            labelNotification.setStyle("-fx-text-fill: white;-fx-background-color:#4BB543;");
            refreshListTable();
            refreshListSP();
        } catch (Exception ex) {
            labelNotification.setText("Failed");
            labelNotification.setStyle("-fx-text-fill: white;-fx-background-color:#ff0000;");
        }
    }

    /**
     * Method guna melakukan refresh list tabel
     */
    public void refreshListTable() {
        accordionListTables.getPanes().removeAll(tpsTable);
        vbJelajah.getChildren().removeAll(tableJelajah);
        displayJelajah(resultsQuery);
        displayListTables();
    }

    /**
     * Method guna melakukan refresh combo box pada tab partisi
     */
    public void refreshComboBoxPartisi() {
        cbNamaTabelPartisi.getItems().clear();
        //Build combo box pada tab partisi
        for (int i = 0; i < accordionListTables.getPanes().size(); i++) {
            cbNamaTabelPartisi.getItems().add(accordionListTables.getPanes().get(i).getText());
        }
        cbNamaTabelPartisi.setValue(accordionListTables.getPanes().get(0).getText());
    }

    public void refreshListSP() {
        lvListSP.getItems().removeAll();

        displayListSP();
    }

}
