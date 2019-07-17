package at.chaosfield.packupdate

import java.io.File
import java.net.URL

import at.chaosfield.packupdate.common.{StdoutLog, Util}
import javax.swing.JOptionPane

import scala.io.Source

/**
  * This file exists purely for backwards compatibility. This class has the name of the old main class and will be launched by
  * the legacy updater. It then instructs the user to update the packupdate
  */
object PackUpdate {
  /*
  def main(args: Array[String]): Unit = {
    System.err.println("Your UpdaterUpdater is not compatible with this version of PackUpdate")
    System.err.println("More information on upgrading UpdaterUpdater: https://github.com/XDjackieXD/PackUpdate/wiki/Migrating-from-2.x-to-3.0")
    JOptionPane.showMessageDialog(
      null,
      "Oh dear, it seems like this version of PackUpdate is no longer compatible with your version of the UpdaterUpdater. " +
      "Please check the instance log for more information on how to upgrade.")

    System.exit(1)
  }
  */
  def main(args: Array[String]): Unit = {
    try {
      run(args)
    } catch {
      case e: Exception =>
        println("### CRASH LOG ###")
        e.printStackTrace()
        println("There was an error during attempt to automatically upgrade UpdaterUpdater. Please follow the instructions at https://github.com/XDjackieXD/PackUpdate/wiki/Migrating-from-2.x-to-3.0")
        Util.exit(1)
    }
  }

  def run(args: Array[String]): Unit = {
    println("Detected Legacy UpdaterUpdater.")

    val instConfig = new File(System.getenv("INST_DIR"), "instance.cfg")

    val config = Source
      .fromFile(instConfig, "UTF-8")
      .getLines()
      .filter(_.contains("="))
      .map(line => {
        val split = line.split('=')
        (split(0), split.lift(1).getOrElse(""))
      })
      .toMap

    val preLaunch = config("PreLaunchCommand")

    val preLaunchParts = Util.parseCommandLine(preLaunch)

    preLaunchParts.length match {
      case 4 if preLaunchParts(0) == "java" || preLaunchParts(1) == "-jar" =>
        // Already converted
        println("Seems we already converted the instance.cfg and MultiMC did not reload the instance yet")
      case 7 if preLaunchParts(0) == "java" || preLaunchParts(1) == "-jar" =>
        // Legacy format
        println("Downloading latest UpdaterUpdater...")
        val path = Util.getUpdaterUpdaterPath(new URL("https://api.github.com/repos/XDjackieXD/PackUpdate/releases"), StdoutLog)
        println(s"Found UpdaterUpdater at $path")


      case _ =>
        // This is an anknown state
        throw new IllegalStateException("Could not identify the correct method to convert the old to the new command line")
    }
  }
}
