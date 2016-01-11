package at.chaosfield.packupdate;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

/**
 * Created by Jakob (XDjackieXD) Riepler
 */
public class FxController{

    @FXML
    private Label status;

    @FXML
    private ProgressBar progress;

    private List<String> parameters;

    private PackUpdate main;
    private Stage primaryStage;

    public void setMain(PackUpdate main){
        this.main = main;
        this.primaryStage = main.primaryStage;
        this.parameters = main.getParameters().getRaw();

        Task updater = new Task<String[]>(){
            @Override
            protected String[] call(){
                try{
                    HashMap<String, String[]> updateables = FileManager.getAvailableUpdates(parameters.get(0), parameters.get(2) + File.separator + parameters.get(1));
                    updateMessage("To Update: " + updateables.size());

                    int current = 0;
                    updateProgress(current, updateables.size());

                    final String modsPath = parameters.get(2) + File.separator + "mods" + File.separator;
                    final String configPath = parameters.get(2) + File.separator + "config";
                    final String resourcesPath = parameters.get(2);

                    for(Map.Entry<String, String[]> entry : updateables.entrySet()){
                        updateMessage("Updating " + entry.getKey());

                        switch(entry.getValue()[3]){
                            case "mod":
                                if(!entry.getValue()[2].equals("")){ //If URL is not empty -> download new Version
                                    if(!entry.getValue()[1].equals("")) //If old version exists delete it
                                        if(!FileManager.deleteLocalFile(modsPath + entry.getKey() + "-" + entry.getValue()[1] + ".jar"))
                                            return new String[]{"Delete Failed", "Could not delete file", entry.getKey() + "-" + entry.getValue()[1] + ".jar"};
                                    FileManager.downloadFile(entry.getValue()[2], modsPath + entry.getKey() + "-" + entry.getValue()[0] + ".jar");
                                }else{
                                    if(!FileManager.deleteLocalFile(modsPath + entry.getKey() + "-" + entry.getValue()[1] + ".jar"))
                                        return new String[]{"Delete Failed", "Could not delete file", entry.getKey() + "-" + entry.getValue()[1] + ".jar"};
                                }
                                break;

                            case "config":
                                if(!entry.getValue()[2].equals("")){ //If URL is not empty -> download new Version
                                    if(!FileManager.deleteLocalFolderContents(configPath)) //delete current config files
                                        return new String[]{"Delete Failed", "Could not delete current config", "Either deleting the config folder's content\nor creating an empty config folder failed."};
                                    FileManager.downloadFile(entry.getValue()[2], configPath + File.separator + entry.getKey() + "-" + entry.getValue()[0] + ".zip");
                                    if(!FileManager.unzipLocalFile(configPath + File.separator + entry.getKey() + "-" + entry.getValue()[0] + ".zip", configPath + File.separator))
                                        return new String[]{"Unpack Failed", "Could net unpack config", "The zip file seems to be corrupted."};
                                }else{
                                    if(!FileManager.deleteLocalFolderContents(configPath))
                                        return new String[]{"Delete Failed", "Could not delete current config", "Either deleting the config folder's content\nor creating an empty config folder failed."};
                                }
                                break;

                            case "resources":
                                if(!entry.getValue()[2].equals("")){ //If URL is not empty -> download new Version
                                    if(!FileManager.deleteLocalFolderContents(resourcesPath + File.separator + "resources")) //delete current config files
                                        return new String[]{"Delete Failed", "Could not delete current resources", "Either deleting the resources folder's content\nor creating an empty resources folder failed."};
                                    if(!FileManager.deleteLocalFolderContents(resourcesPath + File.separator + "scripts"))
                                        return new String[]{"Delete Failed", "Could not delete current scripts", "Either deleting the scripts folder's content\nor creating an empty scripts folder failed."};
                                    FileManager.downloadFile(entry.getValue()[2], resourcesPath + File.separator + "resources" + File.separator + entry.getKey() + "-" + entry.getValue()[0] + ".zip");
                                    if(!FileManager.unzipLocalFile(resourcesPath + File.separator + "resources" + File.separator + entry.getKey() + "-" + entry.getValue()[0] + ".zip", resourcesPath + File.separator))
                                        return new String[]{"Unpack Failed", "Could net unpack resources", "The zip file seems to be corrupted."};
                                }else{
                                    if(!FileManager.deleteLocalFolderContents(resourcesPath + File.separator + "resources"))
                                        return new String[]{"Delete Failed", "Could not delete current resources", "Either deleting the resources folder's content\nor creating an empty resources folder failed."};
                                    if(!FileManager.deleteLocalFolderContents(resourcesPath + File.separator + "scripts"))
                                        return new String[]{"Delete Failed", "Could not delete current scripts", "Either deleting the scripts folder's content\nor creating an empty scripts folder failed."};
                                }
                                break;

                            default:

                        }
                        current++;
                        updateProgress(current, updateables.size());
                        System.out.println("Successfully updated " + entry.getKey());
                    }

                    FileManager.deleteLocalFile(parameters.get(2) + File.separator + parameters.get(1));
                    FileManager.downloadFile(parameters.get(0), parameters.get(2) + File.separator + parameters.get(1));

                }catch(IOException e){
                    e.printStackTrace();
                    return new String[]{"Error While Updating", "Could not update", "Got IOException while updating!\nPlease take a look at the log for a stacktrace."};
                }
                return null;
            }
        };

        progress.progressProperty().bind(updater.progressProperty());
        status.textProperty().bind(updater.messageProperty());
        updater.setOnSucceeded(t -> {
            String[] returnValue = (String[])updater.getValue();
            if(returnValue != null){
                System.out.println(returnValue[1] + ": " + returnValue[2]);
                main.errorAlert(returnValue[0], returnValue[1], returnValue[2]);
            }
            primaryStage.close();
        });
        new Thread(updater).start();
    }

    @FXML
    private void initialize(){

    }
}
