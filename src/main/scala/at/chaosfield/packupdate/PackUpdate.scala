package at.chaosfield.packupdate

import javax.swing.JOptionPane

/**
  * This file exists purely for backwards compatibility. This class has the name of the old main class and will be launched by
  * the legacy updater. It then instructs the user to update the packupdate
  */
object PackUpdate {
  def main(args: Array[String]): Unit = {
    System.err.println("Your UpdaterUpdater is not compatible with this version of PackUpdate")
    System.err.println("More information on upgrading UpdaterUpdater: https://github.com/XDjackieXD/PackUpdate/wiki/Migrating-from-2.x-to-3.0")
    JOptionPane.showMessageDialog(
      null,
      "Sorry to interrupt, it seems like this version of PackUpdate is no longer compatible with your version of the UpdaterUpdater. \n" +
      "Please check the instance log for more information on how to upgrade.\n" +
      "Due to technical limitations on part of MultiMC we could not migrate you automatically. \n\n" +

      "If you are playing on a server you might want to contact your server owner for an updated pack zip file"
    )

    System.exit(1)
  }

  /*
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

    val config = Util.parseInstanceConfig(instConfig)
    val preLaunch = config("PreLaunchCommand")

    val preLaunchParts = Util.parseCommandLine(preLaunch)

    val log = StdoutLog

    preLaunchParts.length match {
      case 4 if preLaunchParts(0) == "java" || preLaunchParts(1) == "-jar" =>
        // Already converted
        println("Seems we already converted the instance.cfg and MultiMC did not reload the instance yet")

      case 6 if preLaunchParts(0) == "java" || preLaunchParts(1) == "-jar" =>

        JOptionPane.showMessageDialog(null, "Apparently i already updated MultiMC to use the new UpdaterUpdater, however MultiMC still launched the old Version. Restart MultiMC and try again")

        Util.exit(-1)

      case 7 if preLaunchParts(0) == "java" || preLaunchParts(1) == "-jar" =>
        // Legacy format
        println("Downloading latest UpdaterUpdater...")
        val path = Util.getUpdaterUpdaterPath(new URL("https://api.github.com/repos/XDjackieXD/PackUpdate/releases"), log)
        println(s"Found UpdaterUpdater at $path")

        val packupdateDir = new File(System.getenv("INST_MC_DIR"), "packupdate")

        packupdateDir.mkdirs()

        val (_, idx) = preLaunchParts.zipWithIndex.find(_._1.endsWith(".jar")).get

        val updaterUpdaterJar = new File(packupdateDir, "UpdaterUpdater.jar")

        FileManager.writeStreamToFile(FileManager.retrieveUrl(path.toURL, log)._1, updaterUpdaterJar)

        preLaunchParts(idx) = "$INST_MC_DIR/packupdate/UpdaterUpdater.jar"

        val finalConfig = config + ("PreLaunchCommand" -> Util.unparseCommandLine(preLaunchParts.init))

        //println(finalConfig)

        // Update MultiMC Config
        FileManager.writeStringToFile(instConfig, Util.unparseInstanceConfig(finalConfig))

        JOptionPane.showMessageDialog(null, "Hi there! I just updated UpdaterUpdater to a new version. This new version will be used upon next launch of this instance. Please reload and restart the MultiMC instance to continue")

        Util.exit(-1)

      case _ =>
        // This is an anknown state
        throw new IllegalStateException("Could not identify the correct method to convert the old to the new command line")
    }
  }
 */
}
