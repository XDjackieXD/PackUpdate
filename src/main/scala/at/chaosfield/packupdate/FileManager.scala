package at.chaosfield.packupdate

import java.io.{BufferedReader, File, FileInputStream, FileNotFoundException, FileOutputStream, FileReader, IOException, InputStreamReader}
import java.net.URL
import java.util.zip.{ZipEntry, ZipInputStream}

import org.apache.commons.io.FileUtils

import scala.collection.mutable.ArrayBuffer
import scala.io.Source

object FileManager {

  val UserAgent = "PackUpdate Automated Mod Updater"

  //open an online file for reading.
  def getOnlineFile(fileUrl: String) = new BufferedReader(new InputStreamReader(new URL(fileUrl).openStream))

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

  def unzipLocalFile(zipFile: String, outputPath: String): Boolean = ???

  /**
    * Calculates the diff between two sets of installed components
    * @param local The currently installed set of local components
    * @param remote The currently installed set of remote components
    * @return A set of updates, this can then be applied as needed
    */
  def getUpdates(local: List[Component], remote: List[Component]): List[Update] = {
    (
      local.map(component => {
        remote.find(c => c.name == component.name) match {
          case Some(remote_version) => Update.UpdatedComponent(component, remote_version)
          case None => Update.RemovedComponent(component)
        }
      })
      ++
      remote.filter(component => !local.exists(c => c.name == component.name)).map(Update.NewComponent)
    )
  }

  def parsePackList(packList: Source): List[Component] =
    packList.getLines().map(s => Component.fromCSV(s.split(","))).toList

  def retrieveUrl(url: URL): Source = {
    val con = url.openConnection
    con.setRequestProperty("user-Agent", UserAgent)
    con.setConnectTimeout(5000)
    con.setReadTimeout(5000)
    Source.fromInputStream(con.getInputStream)
  }
}
