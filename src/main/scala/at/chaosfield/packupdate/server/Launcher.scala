package at.chaosfield.packupdate.server

import java.io.File
import java.net.{URL, URLClassLoader}
import java.util.jar.{Attributes, JarFile, Manifest}

import scala.collection.JavaConverters._

object Launcher {
  def launchServer(file: File, args: Array[String]): Unit = {
    val manifest = new JarFile(file).getManifest
    val cp = manifest.getMainAttributes.getValue("Class-Path").split(' ').map(path => new File("libraries", path).toURI.toURL)
    val loader = new URLClassLoader(cp ++ Array(file.toURI.toURL))
    val klass = Class.forName(manifest.getMainAttributes.getValue("Main-Class"), true, loader)
    val m = klass.getDeclaredMethod("main", classOf[Array[String]])
    m.invoke(null, args)
  }
}
