package at.chaosfield.packupdate

class Component(val name: String, val version: String, val downloadUrl: String, val componentType: ComponentType, val hash: Option[String], val flags: Array[ComponentFlag])

object Component {
  def fromCSV(data: Array[String]) = {
    new Component(
      data(0), // name
      data(1), // version
      data(2), // downloadUrl
      ComponentType.parse(data(3)), // componentType
      data.lift(4), // hash
      data.lift(5).map(_.split(';')).getOrElse(Array.empty[String]).flatMap(ComponentFlag.fromString) // flags
    )
  }
}
