package at.chaosfield.packupdate.common

import java.io.{File, FileNotFoundException, IOException}
import java.net.{SocketTimeoutException, URL, UnknownHostException}
import java.nio.file.Files

import at.chaosfield.packupdate.json.GithubRelease
import org.json.{JSONArray, JSONObject}
import org.json4s._
import org.json4s.jackson.JsonMethods

import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConverters._
import scala.io.Source
import scala.reflect.ClassTag

object Util {
   def fileForComponent(component: Component, minecraftDir: File, legacy: Boolean = false, disabled: Boolean = false): File = {
    component.componentType match {
      case ComponentType.Mod =>
        val fileNamePre = s"${component.name} - ${component.version}.jar" + (if (disabled) { ".disabled" } else { "" })
        val fileName = if (legacy)
          fileNamePre
        else
          fileNamePre.replaceAll("[^a-zA-Z0-9_\\.\\- ]", "_")
        new File(minecraftDir, "mods/" + fileName)
      case ComponentType.Forge =>
        new File(minecraftDir, s"forge-${component.version}.jar")
      case t =>
        throw new Exception(s"ComponentType ${component.componentType.name()} does not have a dedicated file")
    }
  }

  def exceptionToHumanReadable(e: Exception): String = e match {
    case e: UnknownHostException => s"Unknown host ${e.getMessage}"
    case e: FileNotFoundException => s"File not found: ${e.getMessage}"
    case e: SocketTimeoutException => e.getMessage
    case e: IOException => e.getMessage
    case e: Exception => s"${e.getClass.getName}: ${e.getMessage}"
  }

  def isExceptionCritical(e: Exception): Boolean = e match {
    case _: UnknownHostException | _: FileNotFoundException | _: SocketTimeoutException => false
    case _ => true
  }

  def createTempDir() = {
    val dir = new File(Files.createTempDirectory("packupdate").toUri)

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = dir.delete()
    })
  }

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

  def exit(code: Int): Nothing = {
    System.exit(code)
    throw new Exception("Unreachable code! exit() was called but program did not exit")
  }

  def getReleases(url: URL, log: Log): Array[GithubRelease] = {
    val value = JsonMethods.parse(Source.fromInputStream(FileManager.retrieveUrl(url, log)).mkString)

    value.camelizeKeys.extract[Array[GithubRelease]](org.json4s.DefaultFormats, manifest[Array[GithubRelease]])
  }

  def getUpdaterUpdaterPath(url: URL, log: Log): String =
    getReleases(url, log)(0).assets.find(_.name.startsWith("UpdaterUpdater")).get.browserDownloadUrl

}
