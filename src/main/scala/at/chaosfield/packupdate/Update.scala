package at.chaosfield.packupdate

sealed abstract class Update {
  def oldVersion: Option[Component]
  def newVersion: Option[Component]
}

object Update {
  case class NewComponent(component: Component) extends Update {
    override def oldVersion: Option[Component] = None

    override def newVersion: Option[Component] = Some(component)
  }
  case class UpdatedComponent(oldComponent: Component, newComponent: Component) extends Update {
    override def oldVersion: Option[Component] = Some(oldComponent)

    override def newVersion: Option[Component] = Some(newComponent)
  }
  case class RemovedComponent(component: Component) extends Update {
    override def oldVersion: Option[Component] = Some(component)

    override def newVersion: Option[Component] = None
  }
}
