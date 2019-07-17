name := "PackUpdate"

version := "3.0-rc4"

scalaVersion := "2.12.8"

libraryDependencies += "org.openjfx" % "javafx-controls" % "11"
libraryDependencies += "org.openjfx" % "javafx-fxml" % "11"
libraryDependencies += "commons-io" % "commons-io" % "2.6"
libraryDependencies += "commons-codec" % "commons-codec" % "1.12"
libraryDependencies += "com.google.code.gson" % "gson" % "2.8.5"
libraryDependencies += "org.jline" % "jline" % "3.11.0"
libraryDependencies += "net.sourceforge.argparse4j" % "argparse4j" % "0.8.1"
libraryDependencies += "org.eclipse.aether" % "aether-api" % "1.1.0"
libraryDependencies += "org.json" % "json" % "20180813"

//lazy val updaterUpdater = (project in file("UpdaterUpdater"))
Compile / mainClass := Some("at.chaosfield.packupdate.Client")