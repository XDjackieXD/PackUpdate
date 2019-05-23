package at.chaosfield.packupdate

import java.io.File
import java.net.URL

import scala.io.Source

class MainLogic(ui: UiCallbacks) {
  def runUpdate(remoteUrl: URL, localFile: File, config: MainConfig): Unit = {
    val localData = getLocalData(localFile)
    ui.statusUpdate("Updating Pack Metadata...")
    val remoteData = FileManager.parsePackList(Source.fromInputStream(FileManager.retrieveUrl(remoteUrl)))

    val updates = FileManager.getUpdates(localData, remoteData)

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
        case e: Exception => ui.reportError(s"Could not download ${update.name}", Some(e))
      }
    }
    ui.hideProgress()
    ui.statusUpdate("Writing local metadata")
    FileManager.writeMetadata(remoteData, localFile)
    ui.statusUpdate("Finished")
  }

  def getLocalData(localFile: File): List[Component] = {
    if (localFile.exists) {
      FileManager.parsePackList(Source.fromFile(localFile, "UTF-8"))
    } else {
      List.empty[Component]
    }
  }
}
