package at.chaosfield.packupdate

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
      component.componentType match {
        case ComponentType.Mod =>
          FileManager.writeStreamToFile(FileManager.retrieveUrl(component.downloadUrl.get), Util.fileForComponent(component, config.minecraftDir))
        case ComponentType.Config =>
          val file = File.createTempFile("packupdate", component.name + component.version)
          FileManager.writeStreamToFile(FileManager.retrieveUrl(component.downloadUrl.get), file)
          FileManager.extractZip(file, new File(config.minecraftDir, "config"))
          file.deleteOnExit()
        case ComponentType.Resource =>
          val file = File.createTempFile("packupdate", component.name + component.version)
          FileManager.writeStreamToFile(FileManager.retrieveUrl(component.downloadUrl.get), file)
          FileManager.extractZip(file, config.minecraftDir)
          file.deleteOnExit()
        case ComponentType.Forge =>
          config.packSide match {
            case PackSide.Server =>
              val version = component.version
              val url = new URL(s"https://files.minecraftforge.net/maven/net/minecraftforge/forge/$version/forge-$version-universal.jar")
              FileManager.writeStreamToFile(FileManager.retrieveUrl(url), Util.fileForComponent(component, config.minecraftDir))
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
      RemovedComponent(oldComponent).execute(config)
      NewComponent(newComponent).execute(config)
    }
  }
  case class RemovedComponent(component: Component) extends Update {
    override def oldVersion: Option[Component] = Some(component)

    override def newVersion: Option[Component] = None

    override def execute(config: MainConfig): Unit = {
      component.componentType match {
        case ComponentType.Mod =>
          Util.fileForComponent(component, config.minecraftDir).delete()
        case ComponentType.Config | ComponentType.Resource =>
          println("Warning: Uninstallation of Config or Resource files not supported")
      }
    }
  }
}
