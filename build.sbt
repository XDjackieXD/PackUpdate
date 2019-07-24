name := "PackUpdate"

version := "3.0-rc7"

scalaVersion := "2.12.8"

//libraryDependencies += "org.openjfx" % "javafx-controls" % "11"
//libraryDependencies += "org.openjfx" % "javafx-fxml" % "11"
libraryDependencies += "commons-io" % "commons-io" % "2.6"
libraryDependencies += "commons-codec" % "commons-codec" % "1.12"
libraryDependencies += "org.jline" % "jline" % "3.11.0"
libraryDependencies += "net.sourceforge.argparse4j" % "argparse4j" % "0.8.1"
libraryDependencies += "org.json4s" % "json4s-jackson_2.12" % "3.6.7"
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.2.0"

//lazy val updaterUpdater = (project in file("UpdaterUpdater"))
Compile / mainClass := Some("at.chaosfield.packupdate.Main")
