package at.chaosfield.updaterUpdater;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Created by Jakob (XDjackieXD) Riepler
 */
public class UpdaterUpdater{

    public static final String apiUrl = "https://api.github.com/repos/XDjackieXD/PackUpdate/releases/latest";


    public static void main(String[] args){
        new UpdaterUpdater(args);
    }

    public UpdaterUpdater(String[] args){
        if(args.length != 4){
            System.out.println("4 Parameters are required (link to online pack config, location of the local pack config inside modpack root, modpack root, location of PackUpdate jar inside modpack root)");
        }

        String version = null;

        try{
            JarFile packUpdateJar = new JarFile(args[2] + File.separator + args[3]);
            ZipEntry packUpdateEntry = packUpdateJar.getEntry("version.txt");
            if(packUpdateEntry != null){
                InputStream input = packUpdateJar.getInputStream(packUpdateEntry);
                version = IOUtils.toString(input, Charset.forName("ISO-8859-1"));
            }else
                System.out.println("[PackUpdate Updater] Warning: could not get version of original PackUpdate. Downloading it now.");
        }catch(IOException e){
            System.out.println("[PackUpdate Updater] Warning: could not find original PackUpdate. Downloading it now.");
        }

        try{
            if(version==null || hasUpdate(version))
                if(!downloadPackUpdate(args[2] + File.separator + args[3]))
                    System.out.println("[PackUpdate Updater] Update Failed.");
        }catch(IOException e){
            System.out.println("[PackUpdate Updater] Update Failed.");
            e.printStackTrace();
        }

        try{
            JarRunner.addFile(new File(args[2] + File.separator + args[3]));
            Method mainMethod = ClassLoader.getSystemClassLoader().loadClass("at.chaosfield.packupdate.PackUpdate").getDeclaredMethod("main", String[].class);
            mainMethod.invoke(null, (Object)new String[]{args[0], args[1], args[2]});
        }catch(IOException | InvocationTargetException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException e){
            System.out.println("[PackUpdate Updater] Execution of PackUpdater failed");
            e.printStackTrace();
        }

    }

    public static boolean hasUpdate(String version) throws IOException{
        JSONObject jsonObject = getJSON(apiUrl);
        return !(jsonObject.get("tag_name")).equals(version);
    }

    public static boolean downloadPackUpdate(String path) throws IOException{
        JSONObject jsonRelease = getJSON(apiUrl);

        for(Object asset : jsonRelease.getJSONArray("assets")){
            if(((JSONObject) asset).getString("name").startsWith("PackUpdate")){

                FileUtils.copyURLToFile(new URL(((JSONObject) asset).getString("browser_download_url")), new File(path));

                if(getLength(path) == ((JSONObject) asset).getLong("size"))
                    return true;

            }
        }
        return false;
    }

    private static JSONObject getJSON(String url) throws IOException{
        BufferedReader release = new BufferedReader(new BufferedReader(new InputStreamReader(new URL(url).openStream())));

        StringBuilder responseStrBuilder = new StringBuilder();
        String inputStr;
        while((inputStr = release.readLine()) != null)
            responseStrBuilder.append(inputStr);

        return new JSONObject(responseStrBuilder.toString());
    }

    public static long getLength(String filePath) throws IOException{
        File file = new File(filePath);
        if(file.exists())
            return file.length();
        return 0;
    }
}
