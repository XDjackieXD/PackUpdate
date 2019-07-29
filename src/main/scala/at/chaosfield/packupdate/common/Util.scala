package at.chaosfield.packupdate.common

import java.io.{File, FileNotFoundException, IOException}
import java.net.{SocketTimeoutException, URI, URL, UnknownHostException}
import java.nio.file.Files

import at.chaosfield.packupdate.PackUpdateClassloader
import at.chaosfield.packupdate.common.error.ChecksumException
import at.chaosfield.packupdate.json.GithubRelease
import org.json4s._
import org.json4s.jackson.JsonMethods

import scala.collection.mutable.ArrayBuffer
import scala.io.Source

object Util {

  /**
    * Get the file where a [[Component]] should be put. This is only valid for single-file components
    *
    * @param component The [[Component]] to the file for
    * @param minecraftDir the directory minecraft is installed in
    * @param disabled `true`, if the path for the disabled version of this component should be used
    * @return
    */
  def fileForComponent(component: Component, minecraftDir: File, disabled: Boolean = false): File = {
    component.componentType match {
      case ComponentType.Mod =>
        val fileNamePre = s"${component.name}-${component.version}.jar" + (if (disabled) { ".disabled" } else { "" })
        new File(minecraftDir, "mods/" + fileNamePre.replaceAll("[^a-zA-Z0-9_\\.\\- ]", "_"))
      case ComponentType.Forge =>
        new File(minecraftDir, s"forge-${component.version}.jar")
      case t =>
        throw new Exception(s"ComponentType ${component.componentType.name()} does not have a dedicated file")
    }
  }

  /**
    * Generates a human readable [[String]] for a given [[Exception]]
    * @param e the [[Exception]] to get the [[String]] for
    * @return the final [[String]]
    */
  def exceptionToHumanReadable(e: Exception): String = e match {
    case e: UnknownHostException => s"Unknown host ${e.getMessage}"
    case e: FileNotFoundException => s"File not found: ${e.getMessage}"
    case e: SocketTimeoutException => e.getMessage
    case e: IOException => e.getMessage
    case e: ChecksumException => e.getMessage
    case e: Exception => s"${e.getClass.getName}: ${e.getMessage}"
  }

  /**
    * Determines if a given [[Exception]] is deemed critical. Critical means that this exception is
    * not expected to occur during normal Operation, but rather caused by a bug.
    * If an [[Exception]] is critical the program may display a stack trace in the log,
    * if not, it should avoid doing so, as to not clutter the log
    *
    * An example for non-critical exceptions are network errors
    * @param e
    * @return `true`, if the given [[Exception]] should be treated as critical
    */
  def isExceptionCritical(e: Exception): Boolean = e match {
    case _: UnknownHostException |
         _: FileNotFoundException |
         _: SocketTimeoutException |
         _: ChecksumException => false
    case _ => true
  }

  /**
    * Creates a temporary directory. Ensures that the directory is deleted before
    * the application exits
    * @return a [[File]] referencing the created directory
    */
  def createTempDir(): File = {
    val dir = new File(Files.createTempDirectory("packupdate").toUri)

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = dir.delete()
    })
    dir
  }

  /**
    * Takes a command line [[String]] and parses it into parameters. Takes quotation into account
    * @param commandLine a command line [[String]]
    * @return An [[Array]] of [[String]]s, containing the separate parameter
    */
  def parseCommandLine(commandLine: String): Array[String] = {
    val ret = ArrayBuffer.empty[String]
    var tmp: Option[String] = None
    var escape = false
    var quotedString = false

    def appendChar(c: Char): Unit = {
      tmp = Some(tmp.getOrElse("") + c)
    }

    def endToken(): Unit = {
      tmp match {
        case Some(token) => ret += token
        case None =>
      }
      tmp = None
    }

    commandLine.foreach(c =>
      if (escape) {
        escape = false
        appendChar(c)
      } else {
        c match {
          case '\\' => escape = true
          case '"' | '\'' => quotedString = !quotedString
          case ' ' | '\t' if !quotedString => endToken()
          case _ => appendChar(c)
        }
      })

    endToken()

    ret.toArray
  }

  /**
    * Makes a command line string from individual parameters
    * @param params The parameters this command line should represent
    * @return the final command line
    */
  def unparseCommandLine(params: Array[String]): String = {
    params
      .map(param => {
        param
          .replace("\\", "\\\\")
          .replace("\"", "\\\"")
      })
      .map(param => {
        if (param.contains(" ") || param.contains("\\") || param.contains("$")) {
          '"' + param + '"'
        } else {
          param
        }
      })
      .mkString(" ")
  }

  /**
    * Exit the application. This is a wrapper around [[System.exit]] which uses the scala
    * type system to assert that it will not return
    * @param code The numeric exit code
    * @return [[Nothing]] - this function will never return
    */
  def exit(code: Int): Nothing = {
    System.exit(code)
    throw new Exception("Unreachable code! exit() was called but program did not exit")
  }

  /**
    * Get the releases of PackUpdate.
    * @param url the API [[URL]] to query
    * @param log any kind of [[Log]]. Will write debug messages here
    * @return All releases of PackUpdate, newest first
    */
  def getReleases(url: URL, log: Log): Array[GithubRelease] = {
    val value = JsonMethods.parse(Source.fromInputStream(FileManager.retrieveUrl(url, log)._1).mkString)

    value.camelizeKeys.extract[Array[GithubRelease]](org.json4s.DefaultFormats, manifest[Array[GithubRelease]])
  }

  /**
    * Gets the [[URI]] of the latest version of UpdaterUpdater
    * @param url the API [[URL]] to query
    * @param log any kind of [[Log]]. Will write debug messages here
    * @return A direct download link to the latest version of UpdaterUpdater
    */
  def getUpdaterUpdaterPath(url: URL, log: Log): URI =
    getReleases(url, log)(0).assets.find(_.name.startsWith("UpdaterUpdater")).get.browserDownloadUrl

  /**
    * Parses a MultiMC instance config
    * @param instConfig the [[File]] where the config is located
    * @return The key/value pairs contained in the config
    */
  def parseInstanceConfig(instConfig: File): Map[String, String] = {
    val s = Source.fromFile(instConfig, "UTF-8")

    val ret = s
      .getLines()
      .filter(_.contains("="))
      .map(line => {
        val split = line.split('=')
        (split(0), split.lift(1).getOrElse(""))
      })
      .toMap

    s.close()
    ret
  }

  def unparseInstanceConfig(config: Map[String, String]): String = {
    config.map(s => s"${s._1}=${s._2}").mkString("\n")
  }

  /**
    * Turns an absolute file path into a relative one
    * @param path The input [[File]]
    * @param to The [[File]] that will be the base of the relative path
    * @return the final relative path
    */
  def absoluteToRelativePath(path: File, to: File): String = {
    to.toURI.relativize(path.toURI).getPath
  }

  def downloadJfx(config: MainConfig, log: Log): Unit = {
    val jfxClassifier = System.getProperty("os.name") match {
      case name if name.startsWith("Linux") => "linux"
      case name if name.startsWith("Mac") => "mac"
      case name if name.startsWith("Windows") => "win"
      case _ => throw new Exception("Unknown platform!")
    }

    val artList = List(
      ("javafx-base", "11"),
      ("javafx-controls", "11"),
      ("javafx-fxml", "11"),
      ("javafx-graphics", "11")
    )

    artList
      .map(info => new MavenPath("org.openjfx", info._1, info._2, Some(jfxClassifier)))
      .foreach(path => {
        val dest = new File(config.minecraftDir, s"packupdate/jfx/${path.getFilePath}")
        if (!dest.exists()) {
          dest.getParentFile.mkdirs()
          path.downloadTo(List(new URI("https://repo.maven.apache.org/maven2/")), dest, log)
        }
        PackUpdateClassloader.addURL(dest.toURI.toURL)
      })

  }
}