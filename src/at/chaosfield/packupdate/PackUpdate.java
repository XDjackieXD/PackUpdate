package at.chaosfield.packupdate;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

/**
 * Created by Jakob (XDjackieXD) Riepler
 */
public class PackUpdate extends Application{

    protected Stage primaryStage;
    protected VBox rootLayout;

    //The first parameter has to be the link to the online Pack Info CSV file,
    //the second parameter has to be the location of the local Pack Info CSV file inside the modpack root and
    //the third parameter has to be the path to the modpack root where the mods folder should go.
    List<String> parameters;

    public static void main(String[] args){
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Updating");
        parameters = this.getParameters().getRaw();
        if(parameters.size() != 3){
            errorAlert("Wrong Parameters",
                    "Pack Updater was provided with the wrong Parameters",
                    "If you did not modify any instance settings\nplease contact the modpack author!");
            primaryStage.close();
            return;
        }

        initRootLayout();
    }

    public void initRootLayout(){
        try{
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(PackUpdate.class.getResource("main.fxml"));
            rootLayout = loader.load();

            Scene scene = new Scene(rootLayout);
            primaryStage.setScene(scene);
            primaryStage.show();

            ((FxController) loader.getController()).setMain(this);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void errorAlert(String title, String header, String message){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.initOwner(this.primaryStage);
        alert.showAndWait();
    }
}
