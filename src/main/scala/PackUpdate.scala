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
      "Oh dear, it seems like this version of PackUpdate is no longer compatible with your version of the UpdaterUpdater. " +
      "Please check the instance log for more information on how to upgrade.")

    System.exit(1)
  }
}
