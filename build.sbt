name := "PackUpdate"

version := "3.0"

scalaVersion := "2.12.8"

libraryDependencies += "commons-io" % "commons-io" % "2.6"
libraryDependencies += "commons-codec" % "commons-codec" % "1.12"
libraryDependencies += "org.jline" % "jline" % "3.11.0"
libraryDependencies += "net.sourceforge.argparse4j" % "argparse4j" % "0.8.1"
libraryDependencies += "org.json4s" % "json4s-jackson_2.12" % "3.6.7"
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.2.0"

Compile / mainClass := Some("at.chaosfield.packupdate.Main")