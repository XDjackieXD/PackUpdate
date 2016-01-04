package at.chaosfield.packupdate;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;

/**
 * Created by Jakob (XDjackieXD) Riepler & Phillip (Canitzp) Canitz
 */
public class FileManager{

    //open an online file for reading.
    public static BufferedReader getOnlineFile(String fileUrl) throws IOException{
        return new BufferedReader(new BufferedReader(new InputStreamReader(new URL(fileUrl).openStream())));
    }

    public static boolean deleteLocalFile(String fileName){
        File file = new File(fileName);
        return file.delete();
    }

    public static boolean deleteLocalFolderContents(String path){
        try{
            File file = new File(path);
            if(file.exists())
                FileUtils.cleanDirectory(file);
            else
                return file.mkdir();
            return true;
        }catch(IOException e){
            return false;
        }
    }

    public static boolean unzipLocalFile(String zipFile, String outputPath){
        //Unzip the config file

        byte[] buffer = new byte[1024];

        try{
            File input = new File(zipFile);
            File output = new File(outputPath);

            if(!output.exists()){
                output.mkdir();
            }

            ZipInputStream zis = new ZipInputStream(new FileInputStream(input));
            ZipEntry ze;

            while((ze = zis.getNextEntry()) != null){
                if(ze.isDirectory())
                    continue;

                File file = new File(outputPath + File.separator + ze.getName());
                new File(file.getParent()).mkdirs();
                FileOutputStream fos = new FileOutputStream(file);
                int length;
                while((length = zis.read(buffer)) > 0)
                    fos.write(buffer, 0, length);
                fos.close();
            }
            zis.closeEntry();
            zis.close();

            return true;
        }catch(Exception e){
            return false;
        }
    }

    //open a local file for reading. Create an empty one if it doesn't exist
    public static BufferedReader getLocalFile(String fileName) throws IOException{
        File file = new File(fileName);
        BufferedReader reader = null;
        for(int i = 0; i < 3; i++){
            try{
                reader = new BufferedReader(new FileReader(file));
            }catch(FileNotFoundException e){
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
        }
        return reader;
    }

    //open a local file for writing. Create an empty one if it doesn't exist
    public static BufferedWriter writeLocalFile(String fileName) throws IOException{
        File file = new File(fileName);
        BufferedWriter writer = null;
        for(int i = 0; i < 3; i++){
            try{
                writer = new BufferedWriter(new FileWriter(file));
            }catch(FileNotFoundException e){
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
        }
        return writer;
    }

    //Download a binary file to a given location
    public static void downloadFile(String fileUrl, String destination) throws IOException{
        FileUtils.copyURLToFile(new URL(fileUrl), new File(destination));
    }

    //Parse a PackInfo CSV file "name,version,download url,type"
    //type is either "resource", "mod" or "config".
    //config has to be a zip file that gets extracted into the config folder after deleting the original content
    //mod had to be a jar file
    //resource has to be a zip file that gets extracted into the resources folder
    private static HashMap<String, String[]> parsePackinfo(BufferedReader packinfo) throws IOException{
        HashMap<String, String[]> parsedInfo = new HashMap<String, String[]>();
        String tmp;
        while((tmp = packinfo.readLine()) != null){
            if(!tmp.equals("")){ //Ignore empty lines
                String[] parsed = tmp.split(",");
                if(parsed.length == 4){
                    parsedInfo.put(parsed[0], new String[]{parsed[1], parsed[2], parsed[3]});
                }
            }
        }
        return parsedInfo;
    }

    //Get all mods that need to be updated
    //If returned URL is empty, the entry has to be deleted & if the local version is empty there was no previous version installed.
    public static HashMap<String, String[]> getAvailableUpdates(String onlineVersionFile, String localVersionFile) throws IOException{
        HashMap<String, String[]> onlinePackInfo = parsePackinfo(getOnlineFile(onlineVersionFile));
        HashMap<String, String[]> localPackInfo = parsePackinfo(getLocalFile(localVersionFile));
        HashMap<String, String[]> needsUpdate = new HashMap<String, String[]>(); //Key: Name Value: New Version, Old Version, Download URL, Type
        if(onlinePackInfo.isEmpty()) return needsUpdate;
        for(Map.Entry<String, String[]> entry : onlinePackInfo.entrySet()){
            if(localPackInfo.containsKey(entry.getKey())){
                if(!localPackInfo.get(entry.getKey())[0].equals(entry.getValue()[0])){
                    //Entry existed previously and needs to be updated
                    needsUpdate.put(entry.getKey(), new String[]{entry.getValue()[0], localPackInfo.get(entry.getKey())[0], entry.getValue()[1], entry.getValue()[2]});
                }
            }else{
                //Entry didn't exist previously and needs to be downloaded
                needsUpdate.put(entry.getKey(), new String[]{entry.getValue()[0], "", entry.getValue()[1], entry.getValue()[2]});
            }
            localPackInfo.remove(entry.getKey());
        }
        //Entry doesn't exist in the online list anymore. Has to be deleted! -> set new version and download url to ""
        for(Map.Entry<String, String[]> entry : localPackInfo.entrySet()){
            needsUpdate.put(entry.getKey(), new String[]{"", entry.getValue()[0], "", entry.getValue()[2]});
        }
        return needsUpdate;
    }
}
