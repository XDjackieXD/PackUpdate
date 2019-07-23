package at.chaosfield.packupdate.json

import java.io.{File, FileNotFoundException}
import java.net.URI
import scala.xml.XML

import at.chaosfield.packupdate.common.{FileManager, Log, MavenPath}
import org.json4s.jackson.JsonMethods

case class ForgeInstallSpec(
                           install: InstallInformation,
                           versionInfo: VersionInformation,
                           spec: Int = 0
                           )

case class InstallInformation(
                               profileName: String,
                               target: String,
                               path: MavenPath,
                               version: String,
                               filePath: String,
                               welcome: String,
                               minecraft: String,
                               mirrorList: URI,
                               logo: String,
                               modList: String
                             )

case class VersionInformation(
                             id: String,
                             `type`: String,
                             minecraftArguments: String,
                             mainClass: String,
                             inheritsFrom: String,
                             jar: String,
                             libraries: Array[LibraryInformation]
                             )

case class LibraryInformation(
                             name: MavenPath,
                             url: Option[URI],
                             checksums: Array[String],
                             serverreq: Boolean = false,
                             clientreq: Boolean = false
                             ) {

  def getPom(mavenPath: MavenPath, log: Log): xml.Elem = {
    var lastException: Option[Exception] = None
    val tryUrls = url.toList ++ LibraryInformation.RepoList
    for (url <- tryUrls) {
      try {
        val pomUrl = url.resolve(mavenPath.getPom.getFilePath).toURL
        val data = FileManager.readStreamToString(FileManager.retrieveUrl(pomUrl, log)._1)

        return XML.loadString(data)
      } catch {
        case e: FileNotFoundException =>
          lastException = Some(e)
          log.debug(s"File not found at $url, trying next...")
      }
    }
    throw lastException.get
  }
}

object LibraryInformation {
  final val RepoList = List("https://repo.maven.apache.org/maven2/", "https://libraries.minecraft.net/").map(new URI(_))
}