package at.chaosfield.packupdate

import java.io.File
import java.net.URL

import scala.io.Source

class MainLogic(ui: UiCallbacks) {
  def runUpdate(remoteUrl: URL, localFile: File, config: MainConfig): Unit = {
    try {
      val localData = getLocalData(localFile)
      ui.statusUpdate("Updating Pack Metadata...")
      val remoteData = FileManager.parsePackList(Source.fromInputStream(FileManager.retrieveUrl(remoteUrl)))

      val updates = FileManager.getUpdates(localData, remoteData)

      new File(config.minecraftDir, "mods").mkdirs()

      updates.zipWithIndex.foreach { case (update, idx) =>
        val verb = update match {
          case Update.NewComponent(_) => "Installing"
          case Update.RemovedComponent(_) => "Removing"
          case Update.UpdatedComponent(_, _) => "Updating"
        }
        ui.statusUpdate(s"$verb ${update.name}")
        ui.progressUpdate(idx, updates.length)
        try {
          update.execute(config.minecraftDir)
        } catch {
          // TODO: Mark mod as failed
          case e: Exception =>
            ui.reportError(s"Could not download ${update.name}", Some(e))
            e.printStackTrace()
        }
      }
      ui.hideProgress()
      ui.statusUpdate("Writing local metadata")
      FileManager.writeMetadata(remoteData, localFile)
      ui.statusUpdate("Finished")
    } catch {
      case e: Exception =>
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
