package at.chaosfield.packupdate

import java.io.{File, FileNotFoundException, IOException}
import java.net.{SocketTimeoutException, UnknownHostException}
import java.nio.file.Files

object Util {
  def fileForComponent(component: Component, minecraftDir: File): File = {
    component.componentType match {
      case ComponentType.Mod =>
        val fileName = s"${component.name} - ${component.version}.jar"
        new File(minecraftDir, "mods/" + fileName)
      case ComponentType.Forge => new File(minecraftDir, s"forge-${component.version}.jar")
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
}
