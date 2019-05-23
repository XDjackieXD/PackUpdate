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
          val fileName = s"${component.name} - ${component.version}"
          val file = new File(minecraftDir, "mods/" + fileName)
          FileManager.writeStreamToFile(FileManager.retrieveUrl(component.downloadUrl), file)
      }
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
          val fileName = s"${component.name} - ${component.version}"
          val file = new File(minecraftDir, fileName)
          file.delete()
      }
    }
  }
}
