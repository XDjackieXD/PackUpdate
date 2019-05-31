package at.chaosfield.packupdate.server

import java.io.File
import java.net.URLClassLoader
import java.util.jar.{Attributes, JarFile, Manifest}

import scala.collection.JavaConverters._

object Launcher {
  def launchServer(file: File, args: Array[String]): Unit = {
    val manifest = new JarFile(file).getManifest
    val loader = new URLClassLoader(Array(file.toURI.toURL))
    val klass = Class.forName(manifest.getMainAttributes.getValue("Main-Class"), true, loader)
    val m = klass.getDeclaredMethod("main", classOf[Array[String]])
    m.invoke(null, args)
  }
}
