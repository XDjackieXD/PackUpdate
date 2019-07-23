package at.chaosfield.packupdate.common

import java.io.{File, FileInputStream, FileNotFoundException, IOException}
import java.net.URI

import at.chaosfield.packupdate.json.{ForgeInstallSpec, InstalledComponent, InstalledFile, LibraryInformation, serializer}
import org.apache.commons.io.FileUtils
import org.json4s.jackson.JsonMethods

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.xml.Elem

sealed abstract class Update {
  def oldVersion: Option[InstalledComponent]
  def newVersion: Option[Component]
  def name = newOrOld.name
  def newOrOld: Component = newVersion.orElse(oldVersion.map(_.toComponent)).get
  def execute(config: MainConfig, ui: UiCallbacks): Array[InstalledFile]
}

object Update {
  case class NewComponent(component: Component) extends Update {
    override def oldVersion: Option[InstalledComponent] = None

    override def newVersion: Option[Component] = Some(component)

    override def execute(config: MainConfig, ui: UiCallbacks): Array[InstalledFile] = {
      ui.debug(s"NewComponent(${component.display}).execute()")
      executeInternal(config, component.flags.contains(ComponentFlag.Disabled), ui)
    }

    def runDownload(component: Component, file: File, ui: UiCallbacks): Unit = {
      ui.subStatusUpdate(Some("Downloading..."))
      ui.subUnit = ProgressUnit.Bytes

      FileManager.downloadWithHash(
        component.downloadUrl.get.toURL,
        file,
        ui,
        component.hash,
        progressCallback = {
          case (num, Some(total)) =>
            if (!ui.subProgressBar) {
              ui.subProgressBar = true
            }
            ui.subProgressUpdate(num, total)
          case (_, None) =>
        }
      )
      ui.subProgressBar = false
      ui.subStatusUpdate(None)
    }

    def executeInternal(config: MainConfig, disabled: Boolean, ui: UiCallbacks): Array[InstalledFile] = {
      component.componentType match {
        case ComponentType.Mod =>
          val file = Util.fileForComponent(component, config.minecraftDir, disabled = disabled)
          runDownload(component, file, ui)
          Array(InstalledFile(
            Util.absoluteToRelativePath(file, config.minecraftDir),
            component.hash.getOrElse(FileHash.forFile(file))
          ))
        case ComponentType.Config =>
          val file = File.createTempFile("packupdate", component.name + component.version)
          val configDir = new File(config.minecraftDir, "config")
          runDownload(component, file, ui)
          ui.subStatusUpdate(Some("Extracting..."))
          configDir.mkdirs()
          val files = FileManager.extractZip(file, configDir, component.hasFlag(ComponentFlag.ForceOverwrite), ui)
          file.deleteOnExit()
          ui.subStatusUpdate(None)
          files.map(f => InstalledFile(Util.absoluteToRelativePath(f._1, config.minecraftDir), f._2)).toArray
        case ComponentType.Resource =>
          val file = File.createTempFile("packupdate", component.name + component.version)
          runDownload(component, file, ui)
          ui.subStatusUpdate(Some("Extracting..."))
          val files = FileManager.extractZip(file, config.minecraftDir, component.hasFlag(ComponentFlag.ForceOverwrite), ui)
          file.deleteOnExit()
          ui.subStatusUpdate(None)
          files.map(f => InstalledFile(Util.absoluteToRelativePath(f._1, config.minecraftDir), f._2)).toArray
        case ComponentType.Forge =>
          config.packSide match {
            case PackSide.Server =>
              val file = File.createTempFile("packupdate", component.name + component.version)
              val dir = Util.createTempDir()
              runDownload(component, file, ui)
              ui.subStatusUpdate(Some("Extracting..."))
              val files = FileManager.extractZip(file, dir, component.hasFlag(ComponentFlag.ForceOverwrite), ui)
              file.deleteOnExit()
              // Look into the forge jar
              val data = JsonMethods.parse(new FileInputStream(new File(dir, "install_profile.json")))
              val forgeData = data.extract[ForgeInstallSpec](serializer.formats, manifest[ForgeInstallSpec])
              ui.subStatusUpdate(Some("Installing main forge jar..."))
              FileUtils.copyFile(new File(dir, forgeData.install.filePath), new File(forgeData.install.filePath))
              ui.subStatusUpdate(Some("Calculating dependencies..."))

              val libDir = new File(config.minecraftDir, "libraries")
              libDir.mkdirs()

              def getDependencies(data: Elem): Seq[MavenPath] = {
                val deps = data \\ "dependencies"
                if (deps.nonEmpty) {
                  deps
                    .head
                    .child
                    .filter(dep => (dep \\ "scope").text == "compile")
                    .map(dependency => {
                      val group = (dependency \\ "groupId").text
                      val artifact = (dependency \\ "artifactId").text
                      val version = (dependency \\ "version").text

                      new MavenPath(group, artifact, version, None)
                    })
                } else {
                  Array.empty[MavenPath]
                }
              }


              val libs = mutable.HashSet.empty[MavenPath]
              val repoMap = mutable.HashMap.empty[MavenPath, URI]

              val mavenSources = List(
                new URI("https://repo1.maven.org/maven2/"),
                new URI("https://libraries.minecraft.net/")
              )

              ui.subProgressBar = false

              forgeData
                .versionInfo
                .libraries
                .filterNot(a => a.name.domain == "net.minecraftforge" && a.name.name == "forge")
                .filter(a => a.serverreq)
                .foreach(mcLib => {
                  val libQueue = mutable.Queue(mcLib.name)
                  while (libQueue.nonEmpty) {
                    val lib = libQueue.dequeue()
                    println(s"Processing ${lib.getMavenPath}...")
                    libs += lib
                    try {
                      val pom = mcLib.getPom(lib, ui)
                      println("Pom: " + pom.toString)
                      getDependencies(pom).filterNot(libs.contains).foreach(dep => {
                        println(s"  -> Dependency ${dep.getMavenPath}")
                        libQueue.enqueue(dep)
                        mcLib.url match {
                          case Some(url) => repoMap(lib) = url
                          case None =>
                        }
                      })
                    } catch {
                      case e: FileNotFoundException =>
                        e.printStackTrace()
                    }
                  }
                })

              val len = libs.size
              ui.progressUpdate(0, len)
              ui.subProgressBar = true

              val ret = libs
                .toArray
                .zipWithIndex
                .flatMap{case (library, idx) => {
                  ui.subStatusUpdate(Some(s"Downloading Library ${idx + 1}/$len ${library.getMavenPath}"))
                  val dest = new File(libDir, library.getFilePath)
                  dest.getParentFile.mkdirs()

                  val repo = repoMap.get(library).toList ++ mavenSources

                  try {
                    library.downloadTo(repo, dest, ui, progressCallback = {
                      case (num, Some(max)) => {
                        ui.subProgressUpdate(num, max)
                      }
                      case _ =>
                    })

                    Some(InstalledFile(Util.absoluteToRelativePath(dest, config.minecraftDir), FileHash.forFile(dest)))
                  } catch {
                    case e: FileNotFoundException =>
                      println(s"Could not download ${library.getMavenPath} from List(${repo.mkString(", ")}):")
                      e.printStackTrace()
                      None
                  }
                }}

              ui.subProgressBar = false
              ret
            case PackSide.Client =>
              ui.debug("Installing Forge on client not yet supported")
              Array.empty
          }
      }
    }
  }
  case class InvalidComponent(component: InstalledComponent) extends Update {
    override def oldVersion: Option[InstalledComponent] = Some(component)

    override def newVersion: Option[Component] = Some(component.toComponent)

    override def execute(config: MainConfig, ui: UiCallbacks): Array[InstalledFile] = {
      ui.debug(s"InvalidComponent(${component.display}).execute()")
      RemovedComponent(component).execute(config, ui)
      NewComponent(component.toComponent).executeInternal(config, component.flags.contains(ComponentFlag.Disabled), ui)
    }
  }
  case class UpdatedComponent(oldComponent: InstalledComponent, newComponent: Component) extends Update {
    override def oldVersion: Option[InstalledComponent] = Some(oldComponent)

    override def newVersion: Option[Component] = Some(newComponent)

    override def execute(config: MainConfig, ui: UiCallbacks): Array[InstalledFile] = {
      ui.debug(s"NewComponent(${oldComponent.display}, ${newComponent.display}).execute()")
      // TODO: Adopt Disabled Logic to new state keeping
      val disabled = if (newComponent.flags.contains(ComponentFlag.Optional)) {
        oldComponent.componentType == ComponentType.Mod && Util.fileForComponent(oldComponent.toComponent, config.minecraftDir, disabled = true).exists()
      } else {
        newComponent.flags.contains(ComponentFlag.Disabled)
      }
      RemovedComponent(oldComponent).execute(config, ui)
      NewComponent(newComponent).executeInternal(config, disabled, ui)
    }
  }
  case class RemovedComponent(component: InstalledComponent) extends Update {
    override def oldVersion: Option[InstalledComponent] = Some(component)

    override def newVersion: Option[Component] = None

    override def execute(config: MainConfig, ui: UiCallbacks): Array[InstalledFile] = {
      ui.debug(s"RemovedComponent(${component.display}).execute()")
      component.files.foreach(file => new File(config.minecraftDir, file.fileName).delete())
      Array.empty
    }
  }
}
