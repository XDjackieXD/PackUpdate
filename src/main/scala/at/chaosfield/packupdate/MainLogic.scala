package at.chaosfield.packupdate

import java.io.File
import java.net.URL

import scala.io.Source

class MainLogic(ui: UiCallbacks) {
  def runUpdate(remoteUrl: URL, localFile: File, config: MainConfig): Unit = {
    try {
      val localData = getLocalData(localFile)
      ui.statusUpdate("Updating Pack Metadata...")
      val remoteData = FileManager
        .parsePackList(Source.fromInputStream(FileManager.retrieveUrl(remoteUrl)))
        .filter(c => c.neededOnSide(config.packSide))

      ui.statusUpdate("Calculating changes and checking integrity...")
      val updates = FileManager.getUpdates(localData, remoteData, config)

      new File(config.minecraftDir, "mods").mkdirs()

      ui.showProgress()
      updates.zipWithIndex.foreach { case (update, idx) =>
        val verb = update match {
          case Update.NewComponent(_) => "Installing"
          case Update.RemovedComponent(_) => "Removing"
          case Update.UpdatedComponent(_, _) => "Updating"
          case Update.InvalidComponent(_) => "Repairing"
        }
        ui.statusUpdate(s"$verb ${update.name}")
        ui.progressUpdate(idx, updates.length)
        try {
          update.execute(config)
        } catch {
          // TODO: Mark mod as failed
          case e: Exception =>
            ui.reportError(s"Could not download ${update.name}: ${Util.exceptionToHumanReadable(e)}", Some(e))
        }
      }
      ui.hideProgress()
      ui.statusUpdate("Writing local metadata")
      FileManager.writeMetadata(remoteData, localFile)
      ui.statusUpdate("Finished")
    } catch {
      case e: Exception =>
        e.printStackTrace()
        ui.reportError("Internal Error while trying to perform Update", Some(e))
    }
  }

  def getLocalData(localFile: File): List[Component] = {
    if (localFile.exists) {
      FileManager.parsePackList(Source.fromFile(localFile, "UTF-8"))
    } else {
      List.empty[Component]
    }
  }
}
