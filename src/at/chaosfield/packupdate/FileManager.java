package at.chaosfield.packupdate;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
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

            Boolean hadFiles = false;

            while((ze = zis.getNextEntry()) != null){
                hadFiles = true;

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

            return hadFiles;
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

    //Download a binary file to a given location
    public static void downloadFile(String fileUrl, String destination) throws IOException{
        //FileUtils.copyURLToFile(new URL(fileUrl), new File(destination));

        OutputStream outStream = null;
        HttpURLConnection urlCon = null;

        InputStream inStream = null;
        try {
            byte[] buf;
            int byteRead, byteWritten = 0;
            outStream = new BufferedOutputStream(new FileOutputStream(destination));

            URL url, base, next;
            String location;

            while(true){
                url = new URL(fileUrl);
                urlCon = (HttpURLConnection) url.openConnection();
                urlCon.setConnectTimeout(15000);
                urlCon.setReadTimeout(15000);
                urlCon.setInstanceFollowRedirects(false);

                switch(urlCon.getResponseCode()){
                    case HttpURLConnection.HTTP_MOVED_PERM:
                    case HttpURLConnection.HTTP_MOVED_TEMP:
                    case 307:
                        location = urlCon.getHeaderField("Location");
                        base = new URL(fileUrl);
                        next = new URL(base, location);
                        fileUrl = next.toExternalForm();
                        continue;
                }

                break;
            }

            inStream = urlCon.getInputStream();
            buf = new byte[1024];
            while ((byteRead = inStream.read(buf)) != -1) {
                outStream.write(buf, 0, byteRead);
                byteWritten += byteRead;
            }
        }finally {
            if(inStream != null)
                inStream.close();
            if(outStream != null)
                outStream.close();
        }
    }

    //Parse a PackInfo CSV file "name,version,download url,type"
    //type is either "resource", "mod" or "config".
    //config has to be a zip file that gets extracted into the config folder after deleting the original content
    //mod had to be a jar file
    //resource has to be a zip file that gets extracted into the resources folder
    private static HashMap<String, String[]> parsePackinfo(BufferedReader packinfo) throws IOException{
        HashMap<String, String[]> parsedInfo = new HashMap<>();
        String tmp;
        while((tmp = packinfo.readLine()) != null){
            if(!(tmp.equals("") || tmp.startsWith("#"))){ //Ignore empty lines and allow comments with "#"
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
        HashMap<String, String[]> needsUpdate = new HashMap<>(); //Key: Name Value: New Version, Old Version, Download URL, Type
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

    public static boolean writeLocalConfig(HashMap<String, String[]> objects, String fileName){

        HashMap<String, String[]> packInfo = new HashMap<>();

        try{
            packInfo = parsePackinfo(getLocalFile(fileName));
        }catch(IOException e){
            System.out.println("[PackInfo] Warning: could not get previous config. Ignore this if it is the first launch of the pack.");
        }

        for(Map.Entry<String, String[]> entry : objects.entrySet()){
            packInfo.put(entry.getKey(), new String[]{entry.getValue()[0], entry.getValue()[2], entry.getValue()[3]});
        }

        try{
            PrintWriter writer = new PrintWriter(fileName, "UTF-8");
            for(Map.Entry<String, String[]> entry : packInfo.entrySet()){
                if(!entry.getValue()[2].equals("") && !entry.getValue()[0].equals(""))
                    writer.println(entry.getKey() + "," + entry.getValue()[0] + "," + entry.getValue()[1] + "," + entry.getValue()[2]);
            }
            writer.close();
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
