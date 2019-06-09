name := "PackUpdate"

version := "3.0-rc2"

scalaVersion := "2.12.8"

libraryDependencies += "commons-io" % "commons-io" % "2.6"
libraryDependencies += "commons-codec" % "commons-codec" % "1.12"
libraryDependencies += "org.json" % "json" % "20180813"
libraryDependencies += "org.jline" % "jline" % "3.11.0"
libraryDependencies += "net.sourceforge.argparse4j" % "argparse4j" % "0.8.1"

//lazy val updaterUpdater = (project in file("UpdaterUpdater"))
Compile / mainClass := Some("at.chaosfield.packupdate.Client")
