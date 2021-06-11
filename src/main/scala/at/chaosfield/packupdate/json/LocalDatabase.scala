package at.chaosfield.packupdate.json

import java.io.File
import java.net.URI

import at.chaosfield.packupdate.common._

case class LocalDatabase(
                        installedComponents: Array[InstalledComponent],
                        var storedCredentials: Option[Credentials] = None
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

  def hasFlag(flag: ComponentFlag) = flags.contains(flag)

  def validateIntegrity(config: MainConfig, log: Log): Boolean = {
    files.forall(instFile => {
      val f = instFile.discoverActual(config.minecraftDir)
      val hash = instFile.hash
      f match {
        case Some((file, enabled)) =>
          // This used to be a single line, i separated the variables to increase readability
          val isMod = componentType == ComponentType.Mod
          val isOptionl = hasFlag(ComponentFlag.Optional)
          val wrongState = enabled == hasFlag(ComponentFlag.Disabled)
          if ((isMod && !isOptionl && wrongState) || (!isMod && wrongState)) {
            log.debug(s"The file $name:$file is in wrong enabled state, or disabling is not allowed for this component")
            false
          } else {
            val disableIntegrity = flags.contains(ComponentFlag.NoIntegrity)
            val isConfig = componentType == ComponentType.Config
            val forceOverwrite = flags.contains(ComponentFlag.ForceOverwrite)
            if (!disableIntegrity && (!isConfig || forceOverwrite)) {
              val h = FileHash.forFile(file)
              if (h != hash) {
                log.debug(s"sha256($name:$file) => $h, should be $hash")
              }
              h == hash
            } else {
              true
            }
          }
        case None =>
          log.debug(s"$name:${instFile.fileName} does not exist")
          false
      }
    })
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
                        ) {
  def discoverActual(base: File): Option[(File, Boolean)] = {
    val enabled = new File(base, enabledFile())
    val disabled = new File(base, enabledFile() + ".disabled")

    List((enabled, true), (disabled, false)).find(_._1.exists())
  }

  def enabledFile(): String = {
    if (fileName.endsWith(".disabled")) {
      fileName.substring(0, fileName.length - 9)
    } else {
      fileName
    }
  }

}

case class Credentials(username: String, password: String)