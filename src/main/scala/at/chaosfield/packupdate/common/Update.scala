package at.chaosfield.packupdate.common

import java.io.{File, IOException}

import at.chaosfield.packupdate.json.{InstalledComponent, InstalledFile}

sealed abstract class Update {
  def oldVersion: Option[InstalledComponent]
  def newVersion: Option[Component]
  def name = newOrOld.name
  def newOrOld: Component = newVersion.orElse(oldVersion.map(_.toComponent)).get
  def execute(config: MainConfig, ui: UiCallbacks): Array[InstalledFile]
}

object Update {
  case class NewComponent(component: Component) extends Update {
    override def oldVersion: Option[InstalledComponent] = None

    override def newVersion: Option[Component] = Some(component)

    override def execute(config: MainConfig, ui: UiCallbacks): Array[InstalledFile] = {
      ui.debug(s"NewComponent(${component.display}).execute()")
      executeInternal(config, component.flags.contains(ComponentFlag.Disabled), ui)
    }

    def runDownload(component: Component, file: File, ui: UiCallbacks): Unit = {
      ui.subStatusUpdate(Some("Downloading..."))
      ui.subUnit = ProgressUnit.Bytes

      FileManager.downloadWithHash(
        component.downloadUrl.get.toURL,
        file,
        ui,
        component.hash,
        progressCallback = {
          case (num, Some(total)) =>
            if (!ui.subProgressBar) {
              ui.subProgressBar = true
            }
            ui.subProgressUpdate(num, total)
          case (_, None) =>
        }
      )
      ui.subProgressBar = false
      ui.subStatusUpdate(None)
    }

    def executeInternal(config: MainConfig, disabled: Boolean, ui: UiCallbacks): Array[InstalledFile] = {
      component.componentType match {
        case ComponentType.Mod =>
          val file = Util.fileForComponent(component, config.minecraftDir, disabled = disabled)
          runDownload(component, file, ui)
          Array(InstalledFile(
            Util.absoluteToRelativePath(file, config.minecraftDir),
            component.hash.getOrElse(FileHash.forFile(file))
          ))
        case ComponentType.Config =>
          val file = File.createTempFile("packupdate", component.name + component.version)
          val configDir = new File(config.minecraftDir, "config")
          runDownload(component, file, ui)
          ui.subStatusUpdate(Some("Extracting..."))
          configDir.mkdirs()
          val files = FileManager.extractZip(file, configDir, ui)
          file.deleteOnExit()
          ui.subStatusUpdate(None)
          files.map(f => InstalledFile(Util.absoluteToRelativePath(f._1, config.minecraftDir), f._2)).toArray
        case ComponentType.Resource =>
          val file = File.createTempFile("packupdate", component.name + component.version)
          runDownload(component, file, ui)
          ui.subStatusUpdate(Some("Extracting..."))
          val files = FileManager.extractZip(file, config.minecraftDir, ui)
          file.deleteOnExit()
          ui.subStatusUpdate(None)
          files.map(f => InstalledFile(Util.absoluteToRelativePath(f._1, config.minecraftDir), f._2)).toArray
        case ComponentType.Forge =>
          val forge = Forge.fromVersion(component.version)
          config.packSide match {
            case PackSide.Server =>
              forge.everything.map{
                case (url, path) => {
                  ui.debug(s"Downloading $url to $path")
                  val dest = new File(config.minecraftDir, path)
                  dest.getParentFile.mkdirs()
                  try {
                    FileManager.writeStreamToFile(FileManager.retrieveUrl(url, ui)._1, dest)
                  } catch {
                    case e: IOException => e.printStackTrace()
                  }
                  InstalledFile(
                    Util.absoluteToRelativePath(dest, config.minecraftDir),
                    FileHash.forFile(dest)
                  )
                }
              }
            case PackSide.Client =>
              println("Warning: Skipping Forge on Side Client")
              Array.empty
          }

      }
    }
  }
  case class InvalidComponent(component: InstalledComponent) extends Update {
    override def oldVersion: Option[InstalledComponent] = Some(component)

    override def newVersion: Option[Component] = Some(component.toComponent)

    override def execute(config: MainConfig, ui: UiCallbacks): Array[InstalledFile] = {
      ui.debug(s"InvalidComponent(${component.display}).execute()")
      RemovedComponent(component).execute(config, ui)
      NewComponent(component.toComponent).executeInternal(config, component.flags.contains(ComponentFlag.Disabled), ui)
    }
  }
  case class UpdatedComponent(oldComponent: InstalledComponent, newComponent: Component) extends Update {
    override def oldVersion: Option[InstalledComponent] = Some(oldComponent)

    override def newVersion: Option[Component] = Some(newComponent)

    override def execute(config: MainConfig, ui: UiCallbacks): Array[InstalledFile] = {
      ui.debug(s"NewComponent(${oldComponent.display}, ${newComponent.display}).execute()")
      // TODO: Adopt Disabled Logic to new state keeping
      val disabled = if (newComponent.flags.contains(ComponentFlag.Optional)) {
        oldComponent.componentType == ComponentType.Mod && Util.fileForComponent(oldComponent.toComponent, config.minecraftDir, disabled = true).exists()
      } else {
        newComponent.flags.contains(ComponentFlag.Disabled)
      }
      RemovedComponent(oldComponent).execute(config, ui)
      NewComponent(newComponent).executeInternal(config, disabled, ui)
    }
  }
  case class RemovedComponent(component: InstalledComponent) extends Update {
    override def oldVersion: Option[InstalledComponent] = Some(component)

    override def newVersion: Option[Component] = None

    override def execute(config: MainConfig, ui: UiCallbacks): Array[InstalledFile] = {
      ui.debug(s"RemovedComponent(${component.display}).execute()")
      component.files.foreach(file => new File(config.minecraftDir, file.fileName).delete())
      Array.empty
    }
  }
}
