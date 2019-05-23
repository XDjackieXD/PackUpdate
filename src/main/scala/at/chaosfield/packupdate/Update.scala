package at.chaosfield.packupdate

import java.io.File

sealed abstract class Update {
  def oldVersion: Option[Component]
  def newVersion: Option[Component]
  def name = oldVersion.orElse(newVersion).get.name
  def execute(minecraftDir: File)
}

object Update {
  case class NewComponent(component: Component) extends Update {
    override def oldVersion: Option[Component] = None

    override def newVersion: Option[Component] = Some(component)

    override def execute(minecraftDir: File): Unit = {
      component.componentType match {
        case ComponentType.Mod =>
          FileManager.writeStreamToFile(FileManager.retrieveUrl(component.downloadUrl), Util.fileForComponent(component, minecraftDir))
        case ComponentType.Config =>
          val file = File.createTempFile("packupdate", component.name + component.version)
          FileManager.writeStreamToFile(FileManager.retrieveUrl(component.downloadUrl), file)
          FileManager.extractZip(file, new File(minecraftDir, "config"))
          file.deleteOnExit()
        case ComponentType.Resource =>
          val file = File.createTempFile("packupdate", component.name + component.version)
          FileManager.writeStreamToFile(FileManager.retrieveUrl(component.downloadUrl), file)
          FileManager.extractZip(file, minecraftDir)
          file.deleteOnExit()
      }
    }
  }
  case class InvalidComponent(component: Component) extends Update {
    override def oldVersion: Option[Component] = Some(component)

    override def newVersion: Option[Component] = Some(component)

    override def execute(minecraftDir: File): Unit = {
      UpdatedComponent(component, component).execute(minecraftDir)
    }
  }
  case class UpdatedComponent(oldComponent: Component, newComponent: Component) extends Update {
    override def oldVersion: Option[Component] = Some(oldComponent)

    override def newVersion: Option[Component] = Some(newComponent)

    override def execute(minecraftDir: File): Unit = {
      RemovedComponent(oldComponent).execute(minecraftDir)
      NewComponent(newComponent).execute(minecraftDir)
    }
  }
  case class RemovedComponent(component: Component) extends Update {
    override def oldVersion: Option[Component] = Some(component)

    override def newVersion: Option[Component] = None

    override def execute(minecraftDir: File): Unit = {
      component.componentType match {
        case ComponentType.Mod =>
          Util.fileForComponent(component, minecraftDir).delete()
        case ComponentType.Config | ComponentType.Resource =>
          println("Warning: Uninstallation of Config or Resource files not supported")
      }
    }
  }
}
