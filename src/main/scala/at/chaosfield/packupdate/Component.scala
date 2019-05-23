package at.chaosfield.packupdate

import java.net.URL

class Component(val name: String, val version: String, val downloadUrl: URL, val componentType: ComponentType, val hash: Option[String], val flags: Array[ComponentFlag]) {
  def toCSV = {
    Array(
      name,
      version,
      downloadUrl.toString,
      componentType.stringValue,
      hash,
      flags.map(_.internalName).mkString(";")
    ).mkString(",")
  }
}

object Component {
  def fromCSV(data: Array[String]) = {
    new Component(
      data(0), // name
      data(1), // version
      new URL(data(2)), // downloadUrl
      ComponentType.parse(data(3)), // componentType
      data.lift(4), // hash
      data.lift(5).map(_.split(';')).getOrElse(Array.empty[String]).flatMap(ComponentFlag.fromString) // flags
    )
  }
}
