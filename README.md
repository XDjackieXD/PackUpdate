# PackUpdate

## WTF?
A simple program that downloads and updates modpacks automatically.
It avoids
 * Licensing issues
 * Downloading the whole pack on mod or config changes
and can easily be integrated into a MultiMC instance using Pre-Launch commands.

## How?
You can find an example modpack in the folder "ExamplePack".
Basically the config file on your server is a csv with the following values:
Name,Version,Download-URL,Type
Where Type is either "mod", "config" or "resources":
"config" and "resources" are both zip files.
the "config" zip gets extracted into the config folder of your Minecraft instance
and the "resources" zip gets extraced into the root folder of your Minecraft instance.