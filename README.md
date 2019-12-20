# PackUpdate

Now maintained again by Kilobyte22

## WTF?
A simple program that downloads and updates modpacks automatically.
It was written to work directly as a pre-launch command for MultiMC and features
 * Delta updates (only download mods that changed).
 * Easy creation of modpacks (just a text file with names, versions, download links and checksums)
 * No need for a huge amount of webspace or traffic!

## How?
For a detailed explanation on how to set up PackUpdate see the [Wiki](https://github.com/XDjackieXD/PackUpdate/wiki)

## How to build?
Install [Scala](https://scala-lang.org/) and [SBT](https://www.scala-sbt.org/), then run `sbt assembly`. This will produce a standalone jar. 
For development you can use IntelliJ Idea with the scala plugin, which has native support for sbt