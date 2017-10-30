# PackUpdate

Disclaimer: As I don't use it myself anymore, I won't actively maintain this or add features apart from small fixes. Feel free to send pull requests though (and don't hesitate to poke me again should I forget to merge it).

## WTF?
A simple program that downloads and updates modpacks automatically.
It was written to work directly as a pre-launch command for MultiMC and features
 * Delta updates (only download mods that changed).
 * Easy creation of modpacks (just a text file with names, versions and download links)
 * No need for a huge amount of webspace or traffic!

## How?
You can find an example modpack in the folder "ExamplePack".
Basically add the Updater.jar into the instance zip in the minecraft folder
and add this line to the instance.cfg file
`PreLaunchCommand=java -jar "$INST_MC_DIR/Updater.jar" "Link to modpack.cfg file" "PackInfo.cfg" "$INST_MC_DIR" "PackUpdate.jar"`
The config file on your server is a csv with the following values:
Name,Version,Download-URL,Type
Where Type is either "mod", "config" or "resources":
"config" and "resources" are both zip files.
and get extracted into the root folder of your Minecraft instance.
