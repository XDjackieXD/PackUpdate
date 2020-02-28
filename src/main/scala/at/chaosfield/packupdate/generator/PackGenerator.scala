package at.chaosfield.packupdate.generator

import java.io.{File, FileOutputStream}
import java.net.URL
import java.util.Properties
import java.util.zip.{ZipEntry, ZipOutputStream}

import at.chaosfield.packupdate.Main
import at.chaosfield.packupdate.common._
import at.chaosfield.packupdate.json._
import org.json4s.jackson.{JsonMethods, Serialization}

import scala.io.Source

object PackGenerator {

  def run(url: URL, out: File, ui: UiCallbacks, xms: Option[Int], xmx: Option[Int], beta: Boolean, updaterUrl: URL, icon: Option[String]): Unit = {

    val packData = FileManager.parsePackList(Source.fromInputStream(FileManager.retrieveUrl(url, ui)._1))

    val mcVersion = packData
      .find(_.componentType == ComponentType.Forge)
      .map(_.version.split("-").head)
      .getOrElse(throw new RuntimeException("Could not determine minecraft version"))

    val forgeVersion = packData
      .find(_.componentType == ComponentType.Forge)
      .map(_.version.split("-").last)

    val zipStream = new ZipOutputStream(new FileOutputStream(out))

    val updaterUpdaterReleasesData = JsonMethods.parse(FileManager.retrieveUrl(Main.UpdaterUpdaterReleasesURL, ui)._1)
    val updaterUpdaterReleases = updaterUpdaterReleasesData
      .camelizeKeys
      .extract[Array[GithubRelease]](serializer.formats, manifest[Array[GithubRelease]])

    val updaterRelease = updaterUpdaterReleases.head
    val updaterVersion = updaterRelease.tagName

    val updaterDownload = updaterRelease.assets.find(_.name.endsWith(".jar")).get.browserDownloadUrl

    val updaterFile = s"packupdate/UpdaterUpdater-$updaterVersion.jar"

    val (stream, size) = FileManager.retrieveUrl(updaterDownload.toURL, ui)

    zipStream.putNextEntry(new ZipEntry(".minecraft/"))
    zipStream.putNextEntry(new ZipEntry(".minecraft/packupdate/"))
    zipStream.putNextEntry(new ZipEntry(".minecraft/" + updaterFile))

    if (size.isDefined) {
      ui.subProgressBar = true
      ui.subUnit = ProgressUnit.Bytes
    }

    val buffer = new Array[Byte](4096)
    var finished = false
    var bytesWritten = 0
    while (!finished) {
      val count = stream.read(buffer)
      if (count > 0) {
        bytesWritten += count
        size match {
          case Some(s) => ui.subProgressUpdate(bytesWritten, s)
          case None =>
        }
        zipStream.write(buffer, 0, count)
      } else {
        finished = true
      }
    }
    zipStream.closeEntry()

    ui.subProgressBar = false

    zipStream.putNextEntry(new ZipEntry("instance.cfg"))

    val instConfig = Util.unparseInstanceConfig(
      generateMultiMCConfig(
        Array(
          "$INST_JAVA",
          "-jar",
          "$INST_MC_DIR/" + updaterFile,
          "client",
          url.toString
        ),
        (xms, xmx),
        icon
      )
    )
    zipStream.write(instConfig.getBytes("UTF-8"))
    zipStream.closeEntry()

    zipStream.putNextEntry(new ZipEntry("mmc-pack.json"))

    val lwjglMetadataData = JsonMethods.parse(FileManager.retrieveUrl(Main.MultiMCMetadataLWJGL, ui)._1)
    val lwjglMetadata = lwjglMetadataData.extract[MultiMCMetadata](serializer.formats, manifest[MultiMCMetadata])

    val lwjglVersion = lwjglMetadata.versions.head.version

    val mmcPack = MultiMCPack(
      formatVersion = 1,
      components = Array(
        MultiMCComponent(
          uid = "org.lwjgl",
          version = lwjglVersion,
          dependencyOnly = true,
          important = false
        ),
        MultiMCComponent(
          uid = "net.minecraft",
          version = mcVersion,
          dependencyOnly = false,
          important = true
        )
      ) ++ forgeVersion.map(forge => {
        MultiMCComponent(
          uid = "net.minecraftforge",
          version = forge,
          dependencyOnly = false,
          important = false
        )
      })
    )
    zipStream.write(Serialization.write(mmcPack)(serializer.formats).getBytes("UTF-8"))
    zipStream.closeEntry()

    zipStream.putNextEntry(new ZipEntry(".minecraft/packupdate/updater.properties"))
    val prop = new Properties()
    prop.setProperty("beta", if (beta) {"true"} else {"false"})
    prop.setProperty("apiUrl", updaterUrl.toString)

    prop.store(zipStream, "PackUpdateUpdater config")
    zipStream.closeEntry()

    zipStream.close()
  }

  private def generateMultiMCConfig(launchCommand: Array[String], java: (Option[Int], Option[Int]), icon: Option[String]) = {
    val memoryOverride = if (java._1.isDefined || java._2.isDefined) {"true"} else {"false"}

    val additions = List(
      java._1.map(value => "MinMemAlloc" -> value.toString),
      java._2.map(value => "MaxMemAlloc" -> value.toString),
      icon.map(value => "iconKey" -> value)
    )

    Map(
      "InstanceType" -> "OneSix",
      "MCLaunchMethod" -> "LauncherPart",
      "OverrideCommands" -> "true",
      "PreLaunchCommand" -> Util.unparseCommandLine(launchCommand),
      "OverrideMemory" -> memoryOverride
    ) ++ additions.flatten.toMap
  }



}
