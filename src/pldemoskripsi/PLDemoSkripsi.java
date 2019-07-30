/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pldemoskripsi;

import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.voltdb.client.ClientConfig;
import org.voltdb.client.ClientFactory;

/**
 *
 * @author adit
 */
public class PLDemoSkripsi extends Application {
    
    @Override
    public void start(Stage stage) throws Exception {
         org.voltdb.client.Client client = null;
        ClientConfig config = null;
        try {
            config = new ClientConfig("", "");

            client = ClientFactory.createClient(config);

            client.createConnection("localhost", 21211);
            
            Parent root = FXMLLoader.load(getClass().getResource("View/FXMLDocument.fxml"));
        
            Scene scene = new Scene(root);

            stage.setScene(scene);
            stage.setTitle("Perangkat Lunak Demo VoltDB");
            stage.show();
        } catch (java.io.IOException e) {
            Parent root = FXMLLoader.load(getClass().getResource("View/FXMLServerOffline.fxml"));
        
            Scene scene = new Scene(root);

            stage.setScene(scene);
            stage.setTitle("Perangkat Lunak Demo VoltDB");
            stage.show();
            Logger.getLogger(PLDemoSkripsi.class.getName()).log(Level.SEVERE, null, e);
        }
        
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
