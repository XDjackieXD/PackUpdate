# PackUpdate

## WTF?
A simple program that downloads and updates modpacks automatically.
It was written to work directly as a pre-launch command for MultiMC and features
 * Delta updates (only download mods that changed).
 * Easy creation of modpacks (just a text file with names, versions and download links)
 * No need for a huge amount of webspace or traffic!

## How?
You can find an example modpack in the folder "ExamplePack".
Basically the config file on your server is a csv with the following values:
Name,Version,Download-URL,Type
Where Type is either "mod", "config" or "resources":
"config" and "resources" are both zip files.
the "config" zip gets extracted into the config folder of your Minecraft instance
and the "resources" zip gets extraced into the root folder of your Minecraft instance.
