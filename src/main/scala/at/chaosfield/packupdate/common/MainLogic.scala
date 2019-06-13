package at.chaosfield.packupdate.common

import java.io.File

import at.chaosfield.packupdate.Main
import org.apache.commons.io.FileUtils

import scala.collection.mutable.ArrayBuffer
import scala.io.Source

class MainLogic(ui: UiCallbacks) {
  def runUpdate(config: MainConfig): Unit = {
    ui.info(s"Packupdate Version: ${Main.Version}")
    try {
      val packupdateData = new File(config.minecraftDir, "packupdate")
      packupdateData.mkdirs()

      val legacyLocalFile = new File(config.minecraftDir, "packupdate.local")
      val localFile = new File(packupdateData, "local.cfg")

      if (legacyLocalFile.exists() && !localFile.exists()) {
        ui.info("Local state present in legacy location, moving to latest")
        FileUtils.moveFile(legacyLocalFile, localFile)
      }

      val localData = MainLogic.getLocalData(localFile)
      ui.statusUpdate("Updating Pack Metadata...")
      val remoteData = FileManager
        .parsePackList(Source.fromInputStream(FileManager.retrieveUrl(config.remoteUrl, ui)))
        .filter(c => c.neededOnSide(config.packSide))

      ui.statusUpdate("Calculating changes and checking integrity...")
      val updates = FileManager.getUpdates(localData, remoteData, config)
      val failedComponents = ArrayBuffer.empty[Component]

      // TODO: Remove temporary fix to transition system
      localData
        .filter(c => c.componentType == ComponentType.Forge || c.componentType == ComponentType.Mod)
        .foreach(component => {
          val legacyName = Util.fileForComponent(component, config.minecraftDir, legacy = true)
          val name = Util.fileForComponent(component, config.minecraftDir)
          if (name != legacyName && legacyName.exists()) {
            legacyName.delete()
          }
        })

      new File(config.minecraftDir, "mods").mkdirs()

      ui.progressBar = true
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
          update.execute(config, ui)
        } catch {
          // TODO: Mark mod as failed
          case e: Exception =>
            ui.reportError(s"Could not download ${update.name}: ${Util.exceptionToHumanReadable(e)}", Some(e))
            failedComponents += update.newOrOld
        }
      }
      ui.progressBar = false
      ui.statusUpdate("Writing local metadata")
      FileManager.writeMetadata(remoteData.filterNot(failedComponents.contains), localFile)
      ui.statusUpdate("Finished")
    } catch {
      case e: Exception =>
        e.printStackTrace()
        ui.reportError("Internal Error while trying to perform Update", Some(e))
    }
  }
}

object MainLogic {
  def getLocalData(localFile: File): List[Component] = {
    if (localFile.exists) {
      FileManager.parsePackList(Source.fromFile(localFile, "UTF-8"))
    } else {
      List.empty[Component]
    }
  }

  def getRunnableJar(mcDir: File): Option[File] = {
    val localFile = new File(mcDir, "packupdate" + File.separator + "local.cfg")
    getLocalData(localFile).find(c => c.componentType == ComponentType.Forge).map(Util.fileForComponent(_, mcDir))
  }
}
