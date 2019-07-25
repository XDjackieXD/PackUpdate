package at.chaosfield.packupdate.common

import java.io.File

import at.chaosfield.packupdate.Main
import at.chaosfield.packupdate.json.{InstalledComponent, InstalledFile, LocalDatabase, serializer}
import org.json4s._
import org.json4s.jackson.{JsonMethods, Serialization}

import scala.io.Source

class MainLogic(ui: UiCallbacks) {
  def runUpdate(config: MainConfig): Unit = {

    ui.info(s"Packupdate Version: ${Main.Version}")
    try {
      val packupdateData = new File(config.minecraftDir, "packupdate")
      packupdateData.mkdirs()

      val legacyLocalFile = new File(packupdateData, "local.cfg")
      val localFile = new File(packupdateData, "local.json")

      if (legacyLocalFile.exists() && !localFile.exists()) {
        val pack = FileManager.parsePackList(Source.fromFile(legacyLocalFile, "UTF-8"))
        val newData = pack.flatMap(component => component.componentType match {
          case ComponentType.Mod | ComponentType.Forge =>
            Some(InstalledComponent(
              component.name,
              component.version,
              component.componentType,
              component.downloadUrl,
              component.hash,
              Array(
                InstalledFile(Util.absoluteToRelativePath(Util.fileForComponent(component, config.minecraftDir), config.minecraftDir), component.hash.getOrElse(FileHash.Invalid))
              ),
              component.flags
            ))
          case _ => None
        })
        val localData = LocalDatabase(newData.toArray)

        FileManager.writeStringToFile(localFile, Serialization.write(localData)(serializer.formats))
      }

      val localData = MainLogic.getLocalData(localFile)
      ui.statusUpdate("Updating Pack Metadata...")
      val remoteData = FileManager
        .parsePackList(Source.fromInputStream(FileManager.retrieveUrl(config.remoteUrl, ui)._1))
        .filter(c => c.neededOnSide(config.packSide))

      ui.statusUpdate("Calculating changes and checking integrity...")
      val updates = FileManager.getUpdates(localData.installedComponents, remoteData, config, ui)

      val types = List(
        "Newly Installed" -> classOf[Update.NewComponent],
        "Updated" -> classOf[Update.UpdatedComponent],
        "Removed" -> classOf[Update.RemovedComponent],
        "Corrupt" -> classOf[Update.InvalidComponent]
      )

      val summary = types.map{case (label, klass) => {
        (label, updates.filter(_.getClass == klass))
      }}

      ui.printTransactionSummary(summary)

      new File(config.minecraftDir, "mods").mkdirs()

      ui.progressBar = true
      val components = updates.zipWithIndex.map { case (update, idx) =>
        val verb = update match {
          case Update.NewComponent(_) => "Installing"
          case Update.RemovedComponent(_) => "Removing"
          case Update.UpdatedComponent(_, _) => "Updating"
          case Update.InvalidComponent(_) => "Repairing"
        }
        ui.statusUpdate(s"$verb ${update.name}")
        ui.progressUpdate(idx, updates.length)
        try {
          val files = update.execute(config, ui)
          update.newVersion match {
            case Some(newComp) =>
              Some(InstalledComponent.fromRemote(newComp, files))
            case None =>
              // The component has been fully removed and therefore should no longer be tracked locally
              None
          }
        } catch {
          case e: Exception =>
            ui.reportError(s"Could not download ${update.name}: ${Util.exceptionToHumanReadable(e)}", Some(e))
            // The component failed and therefore we should not track it locally.
            // After all it hasn't actually been installed
            None
        }
      }
      ui.progressBar = false
      ui.statusUpdate("Writing local metadata")

      // Ensure that untouched components will be preserved in the local state
      val finalComponents = (localData
          .installedComponents
          .filter(c => !updates.exists(u => u.newOrOld.name == c.name))) ++ components.flatten
      val updatedLocalData = LocalDatabase(finalComponents)

      FileManager.writeStringToFile(localFile, Serialization.write(updatedLocalData)(serializer.formats))

      ui.statusUpdate("Finished")
    } catch {
      case e: Exception =>
        e.printStackTrace()
        ui.reportError("Internal Error while trying to perform Update", Some(e))
    }
  }
}

object MainLogic {
  def getLocalData(localFile: File): LocalDatabase = {
    if (localFile.exists) {
      val data = JsonMethods.parse(FileManager.readFileToString(localFile))

      data.extract[LocalDatabase](serializer.formats, manifest[LocalDatabase])
    } else {
      LocalDatabase(installedComponents = Array.empty)
    }
  }

  def getRunnableJar(mcDir: File): Option[File] = {
    val localFile = new File(mcDir, "packupdate" + File.separator + "local.cfg")
    getLocalData(localFile)
      .installedComponents
      .find(c => c.componentType == ComponentType.Forge)
      .map(c => Util.fileForComponent(c.toComponent, mcDir))
  }
}
