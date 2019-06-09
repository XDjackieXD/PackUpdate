package at.chaosfield.packupdate.common

import java.io.File
import java.net.URL

sealed abstract class Update {
  def oldVersion: Option[Component]
  def newVersion: Option[Component]
  def name = oldVersion.orElse(newVersion).get.name
  def execute(config: MainConfig)
}

object Update {
  case class NewComponent(component: Component) extends Update {
    override def oldVersion: Option[Component] = None

    override def newVersion: Option[Component] = Some(component)

    override def execute(config: MainConfig): Unit = {
      executeInternal(config, component.flags.contains(ComponentFlag.Disabled))
    }

    def executeInternal(config: MainConfig, disabled: Boolean): Unit = {
      component.componentType match {
        case ComponentType.Mod =>
          FileManager.writeStreamToFile(FileManager.retrieveUrl(component.downloadUrl.get.toURL), Util.fileForComponent(component, config.minecraftDir))
        case ComponentType.Config =>
          val file = File.createTempFile("packupdate", component.name + component.version)
          FileManager.writeStreamToFile(FileManager.retrieveUrl(component.downloadUrl.get.toURL), file)
          val configDir = new File(config.minecraftDir, "config")
          configDir.mkdirs()
          FileManager.extractZip(file, configDir)
          file.deleteOnExit()
        case ComponentType.Resource =>
          val file = File.createTempFile("packupdate", component.name + component.version)
          FileManager.writeStreamToFile(FileManager.retrieveUrl(component.downloadUrl.get.toURL), file)
          FileManager.extractZip(file, config.minecraftDir)
          file.deleteOnExit()
        case ComponentType.Forge =>
          config.packSide match {
            case PackSide.Server =>
              val version = component.version
              val url = new URL(s"https://files.minecraftforge.net/maven/net/minecraftforge/forge/$version/forge-$version-universal.jar")
              FileManager.writeStreamToFile(FileManager.retrieveUrl(url), Util.fileForComponent(component, config.minecraftDir))
            case PackSide.Client =>
              println("Warning: Skipping Forge on Side Client")
          }

      }
    }
  }
  case class InvalidComponent(component: Component) extends Update {
    override def oldVersion: Option[Component] = Some(component)

    override def newVersion: Option[Component] = Some(component)

    override def execute(config: MainConfig): Unit = {
      UpdatedComponent(component, component).execute(config)
    }
  }
  case class UpdatedComponent(oldComponent: Component, newComponent: Component) extends Update {
    override def oldVersion: Option[Component] = Some(oldComponent)

    override def newVersion: Option[Component] = Some(newComponent)

    override def execute(config: MainConfig): Unit = {
      val disabled = if (newComponent.flags.contains(ComponentFlag.Optional)) {
        oldComponent.componentType == ComponentType.Mod && Util.fileForComponent(oldComponent, config.minecraftDir, disabled = true).exists()
      } else {
        newComponent.flags.contains(ComponentFlag.Disabled)
      }
      RemovedComponent(oldComponent).execute(config)
      NewComponent(newComponent).executeInternal(config, disabled)
    }
  }
  case class RemovedComponent(component: Component) extends Update {
    override def oldVersion: Option[Component] = Some(component)

    override def newVersion: Option[Component] = None

    override def execute(config: MainConfig): Unit = {
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
