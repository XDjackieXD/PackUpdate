package at.chaosfield.packupdate.common

import java.io._
import java.net.{HttpURLConnection, URL, URLConnection}
import java.security.{DigestInputStream, MessageDigest}
import java.util.zip.ZipInputStream

import at.chaosfield.packupdate.common.error.{ChecksumException, InfiniteRedirectException}
import at.chaosfield.packupdate.json.InstalledComponent

import scala.annotation.tailrec
import scala.collection.mutable
import scala.io.Source

object FileManager {
  /**
    * This is the user agent that any download operation will use
    */
  final val UserAgent = "PackUpdate Automated Mod Updater"

  /**
    * Calculates the diff between two sets of installed components
    * @param local The currently installed set of local components
    * @param remote The currently installed set of remote components
    * @return A set of updates, this can then be applied as needed
    */
  def getUpdates(local: Array[InstalledComponent], remote: List[Component], config: MainConfig, ui: UiCallbacks): List[Update] = {
    ui.subProgressUpdate(0, local.length + 1)
    ui.subUnit = ProgressUnit.Scalar
    ui.subProgressBar = true

    val existingComponents = local.zipWithIndex.flatMap{case (component, index) => {
      ui.subProgressUpdate(index, local.length + 1)
      ui.subStatusUpdate(Some(component.name))
      remote.find(c => c.name == component.name) match {
        case Some(remote_version) => if (remote_version.version != component.version) {
          Some(Update.UpdatedComponent(component, remote_version))
        } else {
          if (component.validateIntegrity(config, ui)) {
            None
          } else {
            Some(Update.InvalidComponent(component))
          }
        }
        case None => Some(Update.RemovedComponent(component))
      }
    }}

    ui.subProgressUpdate(local.length, local.length + 1)
    ui.subStatusUpdate(Some("Processing new components..."))
    val newComponents = remote.filter(component => !local.exists(c => c.name == component.name)).map(Update.NewComponent)

    ui.subProgressBar = false
    ui.subStatusUpdate(None)

    (existingComponents ++ newComponents).toList
  }

  /**
    * Parse a given pack csv
    * @param packList a [[Source]] of the pack list
    * @return the parsed pack list
    */
  def parsePackList(packList: Source): List[Component] =
    packList.getLines().filter(l => l.length() > 0 && !l.startsWith("#")).map(s => Component.fromCSV(s.split(","))).toList

  /**
    * Download a resource from a given [[URL]]. Will follow redirects
    * @param url the [[URL]] to download from
    * @param log a [[Log]] to write debug messages to
    * @param maxRecursion will abort with a [[InfiniteRedirectException]] if there are more redirects
    *                     than specified by this number. Defaults to 10
    * @return an [[InputStream]] which contains the actual data as well as the total size
    */
  @throws[InfiniteRedirectException]
  def retrieveUrl(url: URL, log: Log, maxRecursion: Int = 10): (InputStream, Option[Int]) = {
    log.info(s"Downloading $url...")
    @tailrec
    def request(url: URL, recursionLevel: Int): (URLConnection, Option[Int]) = {
      if (recursionLevel > maxRecursion) {
        log.warning(s"  => Too many redirects (stopped after $recursionLevel)")
        throw new InfiniteRedirectException(recursionLevel)
      }
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
              request(new URL(http.getHeaderField("Location")), recursionLevel + 1)
            case _ =>
              (http, Option(http.getHeaderField("Content-Length")).map(_.toInt))
          }
        }
        case _ => (con, None)
      }
    }
    val (con, len) = request(url, 0)
    (con.getInputStream, len)
  }


  /**
    * Writes a given [[InputStream]] to a specified file, overwriting the existing contents
    * @param source the stream to read from
    * @param file the file to write to
    */
  def writeStreamToFile(source: InputStream, file: File, progressCallback: Int => Unit = _ => ()): Unit = {
    val buf = new Array[Byte](4096)
    val dest = new FileOutputStream(file)
    var finished = false
    var totalBytesRead = 0
    var count = 0

    progressCallback(0)

    while (!finished) {
      val bytesRead = source.read(buf)
      totalBytesRead += bytesRead
      count += 1
      if (bytesRead == -1) {
        finished = true
      } else {
        dest.write(buf, 0, bytesRead)
      }
      if (count % 20 == 0) {
        progressCallback(totalBytesRead)
      }
    }
  }

  /**
    * Extract a zip file to a given directory
    * @param zipFile the zip to extract
    * @param dest the directory to extract to
    * @return A map with the key being the file and the value being the corresponding sha256 sum
    */
  def extractZip(zipFile: File, dest: File, overwrite: Boolean, log: Log): Map[File, FileHash] = {
    // TODO: Actually do overwrite checking
    dest.mkdirs()
    val zipStream = new ZipInputStream(new FileInputStream(zipFile))

    val ret = mutable.Map.empty[File, FileHash]

    var entry = zipStream.getNextEntry
    while (entry != null) {

      val name = entry.getName
      if (!(name.contains("../") || name.startsWith("/"))) {
        val file = new File(dest, name)
        if (entry.isDirectory) {
          file.mkdir()
        } else {
          log.debug(s"Extracting $file")
          val stream = new DigestInputStream(zipStream, MessageDigest.getInstance("SHA-256"))
          FileManager.writeStreamToFile(stream, file)
          ret(file) = new FileHash(stream.getMessageDigest.digest)
        }
      } else {
        log.warning("Attempt for directory traversal blocked")
      }

      entry = zipStream.getNextEntry
    }
    ret.toMap
  }

  def writeStringToFile(file: File, string: String): Unit = {
    val stream = new FileWriter(file)
    stream.write(string)
    stream.flush()
    stream.close()
  }

  def readFileToString(file: File): String = {
    val s = Source.fromFile(file, "UTF-8")
    val str = s.mkString
    s.close()
    str
  }

  def readStreamToString(stream: InputStream): String = {
    val s = Source.fromInputStream(stream)
    val str = s.mkString
    s.close()
    str
  }

  /**
    *
    * @param url the [[URL]] to download
    * @param file where to download the file to
    * @param log a [[Log]] where status messages will be logged
    * @param hash optionally a SHA-256 sum to check against. If [[Some]], the downloaded file will be
    *             checked against this value and a [[ChecksumException]] thrown if it doesn't match
    * @param maxRedirects the maximum number of redirects to follow. If this value is exceeded, a
    *                     [[InfiniteRedirectException]] will be thrown
    * @throws InfiniteRedirectException if there were too many redirects
    * @throws ChecksumException if the a hash was specified but did not match the downloaded file
    */
  @throws[InfiniteRedirectException]
  @throws[ChecksumException]
  def downloadWithHash(url: URL, file: File, log: Log, hash: Option[FileHash], maxRedirects: Int = 10, progressCallback: (Int, Option[Int]) => Unit = (_, _) => () ): Unit = {
    val (connection, fileSize) = FileManager.retrieveUrl(url, log, maxRedirects)
    val stream = new DigestInputStream(connection, MessageDigest.getInstance("SHA-256"))
    FileManager.writeStreamToFile(stream, file, progress => progressCallback(progress, fileSize))
    hash match {
      case Some(h) =>
        val actualHash = new FileHash(stream.getMessageDigest.digest())
        if (h != actualHash) {
          log.debug(s"Checksum was $actualHash, should have been $h")
          log.error("Checksum of downloaded file did not match expected value")
          throw new ChecksumException("Downloaded file is corrupt")
        }
      case None =>
    }
  }
}
