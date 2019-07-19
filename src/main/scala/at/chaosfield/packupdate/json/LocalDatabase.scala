package at.chaosfield.packupdate.json

import java.io.File
import java.net.URI

import at.chaosfield.packupdate.common._

case class LocalDatabase(
                        installedComponents: Array[InstalledComponent]
                        )

case class InstalledComponent(
                               name: String,
                               version: String,
                               componentType: ComponentType,
                               downloadUrl: Option[URI],
                               downloadHash: Option[FileHash],
                               files: Array[InstalledFile],
                               flags: Array[ComponentFlag]
                             ) {

  def toComponent: Component = new Component(name, version, downloadUrl, componentType, downloadHash, flags)
  def display = s"$name $version"

  def validateIntegrity(config: MainConfig) = {
    files.forall{
      case InstalledFile(fileName, hash) =>
        val file = new File(config.minecraftDir, fileName)
        if (file.exists) {
          val h = FileHash.forFile(file)
          if (h != hash) {
            println(s"sha256($name:$file) => $h, should be $hash")
          }
          h == hash
        } else {
          println(s"$name:$file does not exist")
          false
        }
    }
  }
}

object InstalledComponent {
  def fromRemote(c: Component, installedFiles: Array[InstalledFile]): InstalledComponent = {
    InstalledComponent(
      c.name,
      c.version,
      c.componentType,
      c.downloadUrl,
      c.hash,
      installedFiles,
      c.flags
    )
  }
}

case class InstalledFile(
                        fileName: String,
                        hash: FileHash
                        )