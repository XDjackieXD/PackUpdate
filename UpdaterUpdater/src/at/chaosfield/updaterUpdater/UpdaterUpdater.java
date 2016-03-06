package at.chaosfield.updaterUpdater;

import java.io.File;
import java.io.IOException;

/**
 * Created by Jakob (XDjackieXD) Riepler
 */
public class UpdaterUpdater{
    public static void main(String[] args){
        if(args.length < 4){
            System.out.printf("4 arguments are required! (Pack config url, local pack config filename, path to minecraft folder, path to old packupdater jar file)");
            return;
        }

        File updaterNew = new File(args[2] + File.separator + "PackUpdate-new.jar");
        File updaterOld = new File(args[2] + File.separator + args[3]);

        if(updaterNew.exists()){
            if(updaterOld.exists())
                if(!updaterOld.delete()){
                    System.out.println("Error deleting old updater");
                    return;
                }
            if(updaterNew.renameTo(updaterOld)){
                try{
                    runPackUpdate(args[2], args[3], "\"" + args[0] + "\" \"" + args[1] + "\" \"" + args[2] + "\"");
                }catch(IOException e){
                    e.printStackTrace();
                }
            }else
                System.out.println("Error renaming new updater!");
        }else
            System.out.println("New  version of updater doesn't exist!");
    }

    public static void runPackUpdate(String path, String packupdater, String args) throws IOException{
        String command = "\"" + System.getProperty("java.home") + File.separator + "bin" + File.separator + "java\" -jar \"" + path + "" + File.separator + packupdater + "\" " + args;
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
}
