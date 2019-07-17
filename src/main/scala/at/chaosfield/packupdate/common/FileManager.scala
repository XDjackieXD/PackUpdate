package at.chaosfield.packupdate.common

import java.io._
import java.net.{HttpURLConnection, URL, URLConnection}
import java.util.zip.ZipInputStream

import org.apache.commons.io.FileUtils

import scala.annotation.tailrec
import scala.io.Source

object FileManager {
  final val UserAgent = "PackUpdate Automated Mod Updater"

  //open an online file for reading.
  def getOnlineFile(fileUrl: URL) = new BufferedReader(new InputStreamReader(fileUrl.openStream))

  def deleteLocalFile(fileName: String): Boolean = new File(fileName).delete()

  def deleteLocalFolderContents(path: String): Boolean = try {
    val file = new File(path)
    if (file.exists) {
      FileUtils.cleanDirectory(file)
      true
    } else {
      file.mkdir()
    }
  } catch {
    case _: IOException => false
  }

  /**
    * Calculates the diff between two sets of installed components
    * @param local The currently installed set of local components
    * @param remote The currently installed set of remote components
    * @return A set of updates, this can then be applied as needed
    */
  def getUpdates(local: List[Component], remote: List[Component], config: MainConfig): List[Update] = {
    (
      local.flatMap(component => {
        remote.find(c => c.name == component.name) match {
          case Some(remote_version) => if (remote_version.version != component.version) {
            Some(Update.UpdatedComponent(component, remote_version))
          } else {
            if (remote_version.verifyChecksum(config)) {
              None
            } else {
              Some(Update.InvalidComponent(remote_version))
            }
          }
          case None => Some(Update.RemovedComponent(component))
        }
      })
      ++
      remote.filter(component => !local.exists(c => c.name == component.name)).map(Update.NewComponent)
    )
  }

  def parsePackList(packList: Source): List[Component] =
    packList.getLines().filter(l => l.length() > 0 && !l.startsWith("#")).map(s => Component.fromCSV(s.split(","))).toList

  def retrieveUrl(url: URL, log: Log): InputStream = {
    log.debug(s"Downloading $url")
    @tailrec
    def request(url: URL): URLConnection = {
      log.debug(s" -> Trying $url")
      val con = url.openConnection
      con.setRequestProperty("user-Agent", UserAgent)
      con.setConnectTimeout(5000)
      con.setReadTimeout(5000)
      con match {
        case http: HttpURLConnection => {
          http.setInstanceFollowRedirects(false)
          http.getResponseCode match {
            case HttpURLConnection.HTTP_MOVED_PERM | HttpURLConnection.HTTP_MOVED_TEMP | 307 =>
              request(new URL(http.getHeaderField("Location")))
            case _ =>
              http
          }
        }
        case _ => con
      }
    }
    request(url).getInputStream
  }

  def writeStreamToFile(source: InputStream, file: File): Unit = {
    val buf = new Array[Byte](1024)
    val dest = new FileOutputStream(file)
    var finished = false
    while (!finished) {
      val bytesRead = source.read(buf)
      if (bytesRead == -1) {
        finished = true
      } else {
        dest.write(buf, 0, bytesRead)
      }
    }
  }

  def writeMetadata(data: List[Component], localFile: File) = {
    val s = new PrintStream(new FileOutputStream(localFile))
    s.println(data.map(_.toCSV).mkString("\n"))
  }

  /**
    * Extract a zip file to a given directory
    * @param zipFile the zip to extract
    * @param dest the directory to extract to
    * @return A map with the key being the file names and the value being the sha256 sum
    */
  def extractZip(zipFile: File, dest: File, log: Log): Map[String, String] = {
    dest.mkdirs()
    val zipStream = new ZipInputStream(new FileInputStream(zipFile))

    var entry = zipStream.getNextEntry
    while (entry != null) {

      val name = entry.getName
      if (!(name.contains("../") || name.startsWith("/"))) {
        val file = new File(dest, name)
        if (entry.isDirectory) {
          file.mkdir()
        } else {
          log.debug(s"Extract $file")
          FileManager.writeStreamToFile(zipStream, file)
        }
      } else {
        log.warning("Attempt for directory traversal blocked")
      }

      entry = zipStream.getNextEntry
    }
    Map.empty[String, String]
  }

  def writeStringToFile(file: File, string: String): Unit = {
    val stream = new FileWriter(file)
    stream.write(string)
    stream.flush()
    stream.close()
  }
}
