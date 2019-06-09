package at.chaosfield.packupdate.common

import java.io.{File, IOException}
import java.net.URL

sealed abstract class Update {
  def oldVersion: Option[Component]
  def newVersion: Option[Component]
  def name = oldVersion.orElse(newVersion).get.name
  def execute(config: MainConfig, logLevel: Log)
}

object Update {
  case class NewComponent(component: Component) extends Update {
    override def oldVersion: Option[Component] = None

    override def newVersion: Option[Component] = Some(component)

    override def execute(config: MainConfig, log: Log): Unit = {
      executeInternal(config, component.flags.contains(ComponentFlag.Disabled), log)
    }

    def executeInternal(config: MainConfig, disabled: Boolean,log: Log): Unit = {
      component.componentType match {
        case ComponentType.Mod =>
          FileManager.writeStreamToFile(FileManager.retrieveUrl(component.downloadUrl.get.toURL, log), Util.fileForComponent(component, config.minecraftDir))
        case ComponentType.Config =>
          val file = File.createTempFile("packupdate", component.name + component.version)
          val configDir = new File(config.minecraftDir, "config")
          FileManager.writeStreamToFile(FileManager.retrieveUrl(component.downloadUrl.get.toURL, log), file)
          configDir.mkdirs()
          FileManager.extractZip(file, configDir, log)
          file.deleteOnExit()
        case ComponentType.Resource =>
          val file = File.createTempFile("packupdate", component.name + component.version)
          FileManager.writeStreamToFile(FileManager.retrieveUrl(component.downloadUrl.get.toURL, log), file)
          FileManager.extractZip(file, config.minecraftDir, log)
          file.deleteOnExit()
        case ComponentType.Forge =>
          val forge = Forge.fromVersion(component.version)
          config.packSide match {
            case PackSide.Server =>
              forge.everything.foreach{
                case (url, path) => {
                  log.debug(s"Downloading $url to $path")
                  val dest = new File(config.minecraftDir, path)
                  dest.getParentFile.mkdirs()
                  try {
                    FileManager.writeStreamToFile(FileManager.retrieveUrl(url, log), dest)
                  } catch {
                    case e: IOException => e.printStackTrace()
                  }
                }
              }
            case PackSide.Client =>
              println("Warning: Skipping Forge on Side Client")
          }

      }
    }
  }
  case class InvalidComponent(component: Component) extends Update {
    override def oldVersion: Option[Component] = Some(component)

    override def newVersion: Option[Component] = Some(component)

    override def execute(config: MainConfig, log: Log): Unit = {
      UpdatedComponent(component, component).execute(config, log)
    }
  }
  case class UpdatedComponent(oldComponent: Component, newComponent: Component) extends Update {
    override def oldVersion: Option[Component] = Some(oldComponent)

    override def newVersion: Option[Component] = Some(newComponent)

    override def execute(config: MainConfig, log: Log): Unit = {
      val disabled = if (newComponent.flags.contains(ComponentFlag.Optional)) {
        oldComponent.componentType == ComponentType.Mod && Util.fileForComponent(oldComponent, config.minecraftDir, disabled = true).exists()
      } else {
        newComponent.flags.contains(ComponentFlag.Disabled)
      }
      RemovedComponent(oldComponent).execute(config, log)
      NewComponent(newComponent).executeInternal(config, disabled, log)
    }
  }
  case class RemovedComponent(component: Component) extends Update {
    override def oldVersion: Option[Component] = Some(component)

    override def newVersion: Option[Component] = None

    override def execute(config: MainConfig, log: Log): Unit = {
      component.componentType match {
        case ComponentType.Mod | ComponentType.Forge | ComponentType.Minecraft =>
          Util.fileForComponent(component, config.minecraftDir).delete()
          Util.fileForComponent(component, config.minecraftDir, disabled = true).delete()
        case ComponentType.Config | ComponentType.Resource =>
          println("Warning: Uninstallation of Config or Resource files not supported")
      }
    }
  }
}
