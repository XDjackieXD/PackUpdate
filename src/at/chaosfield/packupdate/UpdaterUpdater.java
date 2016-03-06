package at.chaosfield.packupdate;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Created by Jakob (XDjackieXD) Riepler
 */
public class UpdaterUpdater{
    public static boolean hasUpdate() throws IOException{
        JSONObject jsonObject = getJSON(PackUpdate.apiUrl);
        return !(jsonObject.get("tag_name")).equals(PackUpdate.version);
    }

    public static boolean downloadPackUpdate(String path) throws IOException{
        JSONObject jsonRelease = getJSON(PackUpdate.apiUrl);

        for(Object asset: jsonRelease.getJSONArray("assets")){
            if(((JSONObject)asset).getString("name").startsWith("PackUpdate")){

                FileManager.downloadFile(((JSONObject)asset).getString("browser_download_url"), path + File.separator + "PackUpdate-new.jar");

                if(getLength(new URL(new URL("file:"), path + File.separator + "PackUpdate-new.jar")) == ((JSONObject)asset).getLong("size"))
                    return true;

            }
        }
        return false;
    }

    public static boolean downloadUpdater(String path) throws IOException{
        JSONObject jsonRelease = getJSON(PackUpdate.apiUrl);

        for(Object asset: jsonRelease.getJSONArray("assets")){
            if(((JSONObject)asset).getString("name").startsWith("UpdaterUpdater")){

                FileManager.downloadFile(((JSONObject)asset).getString("browser_download_url"), path + File.separator + "UpdaterUpdater.jar");

                if(getLength(new URL(new URL("file:"), path + File.separator + "UpdaterUpdater.jar")) == ((JSONObject)asset).getLong("size"))
                    return true;

            }
        }
        return false;
    }

    public static void runUpdater(String path, String args) throws IOException{
        String command = "\"" + System.getProperty("java.home") + File.separator + "bin" + File.separator + "java\" -jar \"" + path + "" + File.separator + "UpdaterUpdater.jar\" " + args;
        String[] runCmd;
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            runCmd = new String[]{"cmd", "/c", "start" ,command};
        }else{
            runCmd = new String[]{"/bin/sh", "-c", command};
        }

        Runtime.getRuntime().exec(runCmd);

        System.out.printf("Ran command. Exiting now.");
        System.exit(0);
    }

    private static JSONObject getJSON(String url) throws IOException{
        BufferedReader release = FileManager.getOnlineFile(url);

        StringBuilder responseStrBuilder = new StringBuilder();
        String inputStr;
        while((inputStr = release.readLine()) != null)
            responseStrBuilder.append(inputStr);

        return new JSONObject(responseStrBuilder.toString());
    }

    public static long getLength(URL url) throws IOException{
        InputStream stream = null;
        try {
            stream = url.openStream();
            return stream.available();
        } finally {
            if(stream != null){
                stream.close();
            }
        }
    }
}
