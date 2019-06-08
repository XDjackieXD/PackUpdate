package at.chaosfield.packupdate.common

/*
import java.net.URL

class Forge(version: String) {

  def mcVersion = version.split("-")(0)

  def mcMajorVersion = {
    val s = mcVersion.split('.')
    println(s"$mcVersion: ${s.length}")
    s"${s(0)}.${s(1)}"
  }

  def everything = {
    Array(
      (new URL(s"https://libraries.minecraft.net/$launchwrap"), s"libraries/$launchwrap"),
    )
  }

  def launchwrap = s"net/minecraft/launchwrapper/$mcMajorVersion/launchwrapper-$mcMajorVersion.jar"
}
*/

import java.io.{File, InputStreamReader}
import java.net.URL
import java.util
import java.util.zip.{ZipEntry, ZipFile, ZipInputStream}

import at.chaosfield.packupdate.common.Forge.ForgeInfo
import com.google.gson.Gson
import org.apache.commons.io.input.ReaderInputStream

import scala.collection.JavaConverters._

class Forge(info: ForgeInfo) {
  def libraries: Array[(URL, String)] = {
    info.versionInfo.libraries.filter(_.artifact != "forge").map(lib => {
      (new URL(lib.toURL), lib.filepath)
    })
  }

  def mainDownload: (URL, String) = {
    (new URL(s"https://files.minecraftforge.net/maven/net/minecraftforge/forge/$version/forge-$version-universal.jar"), "forge-$version-universal.jar")
  }

  def version = info.install.version.split(" ")(1)

  def everything: Array[(URL, String)] = {
    libraries.map(lib => (lib._1, "libraries/" + lib._2)) ++ Array(mainDownload)
  }
}

object Forge {

  def fromVersion(version: String) = {
    val stream = new URL(s"https://files.minecraftforge.net/maven/net/minecraftforge/forge/$version/forge-$version-installer.jar").openStream()
    val zip = new ZipInputStream(stream)
    var entry: ZipEntry = null
    do {
      entry = zip.getNextEntry
      println(entry.getName)
    } while(entry.getName != "install_profile.json")

    val size = entry.getSize.asInstanceOf[Int]
    val data = new Array[Byte](size)

    var off: Int = 0

    do {
      off += zip.read(data, off, size - off)
    } while (off < size)

    fromJSON(new String(data))
  }

  def fromJSON(str: String) = {
    val gson = new Gson()
    new Forge(gson.fromJson(str, classOf[ForgeInfo]))
  }

  class ForgeInfo {
    var versionInfo: VersionInfo = _
    var install: InstallInfo = _
  }

  class InstallInfo {
    var filePath: String = _
    var version: String = _
  }

  class VersionInfo {
    var id: String = _
    var libraries: Array[LibraryInfo] = _
  }

  class LibraryInfo {
    var name: String = _
    var url: String = _
    var serverreq: String = _
    var clientreq: String = _

    def group = parts(0)
    def artifact = parts(1)
    def version = parts(2)
    private def parts = name.split(":")

    def filepath = s"${group.replace('.', '/')}/$artifact/$version/$artifact-$version.jar"
    def toURL = s"${Option(url).getOrElse("https://libraries.minecraft.net/")}$filepath"
  }
}