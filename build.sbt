name := "PackUpdate"

version := "2.5"

scalaVersion := "2.12.8"

libraryDependencies += "commons-io" % "commons-io" % "2.6"
libraryDependencies += "commons-codec" % "commons-codec" % "1.12"
libraryDependencies += "org.json" % "json" % "20180813"
libraryDependencies += "org.jline" % "jline" % "3.11.0"

//lazy val updaterUpdater = (project in file("UpdaterUpdater"))
Compile / mainClass := Some("at.chaosfield.packupdate.Main")