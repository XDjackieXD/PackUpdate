package at.chaosfield.packupdate

import java.io.File
import java.net.URL
import java.util.jar.Manifest

import at.chaosfield.packupdate.common.{DebugFlag, MainConfig, MainLogic, PackSide, Util}
import at.chaosfield.packupdate.frontend.{CliCallbacks, SwingFrontend}
import at.chaosfield.packupdate.generator.PackGenerator
import javax.swing.SwingUtilities
import net.sourceforge.argparse4j.ArgumentParsers
import net.sourceforge.argparse4j.impl.Arguments
import net.sourceforge.argparse4j.inf.{ArgumentAction, ArgumentParser, ArgumentParserException, Namespace, Subparser}

import scala.collection.JavaConverters._
import scala.collection.immutable.HashSet


object Main {

  val ProjectName = "PackUpdate"
  val UpdaterUpdaterReleasesURL = new URL("https://api.github.com/repos/XDjackieXD/PackUpdateUpdater/releases")
  val MultiMCMetadataLWJGL = new URL("https://v1.meta.multimc.org/org.lwjgl/")
  val PackUpdateReleaseUrl = new URL("https://api.github.com/repos/XDjackieXD/PackUpdate/releases")

  /// Hack to get this to run on JavaFx 7
  private[packupdate] var options: MainConfig = null

  lazy val Version: String = Manifest
    .map(_.getMainAttributes.getValue("Implementation-Version"))
    .getOrElse("Unknown")

  lazy val Manifest: Option[Manifest] = {
    getClass
      .getClassLoader
      .getResources("META-INF/MANIFEST.MF")
      .asScala
      .map(res => new Manifest(res.openStream()))
      .find(man => man.getMainAttributes.getValue("Implementation-Title") == ProjectName)
  }

  def createServerParser(parser: Subparser): Unit = {
    parser
      .addArgument("--run")
      .dest("run")
      .action(Arguments.storeTrue())
      .help("Run the Server in foreground. Implies --update")

    parser
      .addArgument("--accept-mojang-eula")
      .dest("eula")
      .action(Arguments.storeTrue())
      .help("By specifying this option you indicate that you accept the Mojang EULA located at https://account.mojang.com/documents/minecraft_eula")

    parser.addArgument("url")
      .dest("url")
      .help("The URL of the pack")

    parser.addArgument("--frontend-ui")
      .dest("ui")
      .choices("cli", "swing")
      .help("Change the Frontend in use. Default is cli")
  }

  def createClientParser(parser: Subparser): Unit = {
    parser.addArgument("url")
      .dest("url")
      .help("The URL of the pack")

    parser.addArgument("--frontend-ui")
      .dest("ui")
      .choices("cli", "swing")
      .help("Change the Frontend in use. Default is swing")
  }

  def createGeneratorParser(parser: Subparser): Unit = {
    parser.addArgument("--pack-url")
      .dest("url")
      .required(true)
      .help("The URL where the pack csv is located at")

    parser.addArgument("--out")
      .dest("out")
      .required(true)
      .help("The output zip file")

    parser.addArgument("--java-xms")
      .dest("xms")
      .help("The JVM Minimum Memory value")

    parser.addArgument("--java-xmx")
      .dest("xmx")
      .help("The JVM Maximum Memory value")

    // TODO: Allow manually specifying forge and mc version
    /*parser.addArgument("--forge-version")
      .dest("forge")
      .help("The version of Forge to use")*/

    parser.addArgument("--beta")
      .dest("beta")
      .action(Arguments.storeTrue())
      .help("Enable Beta versions of PackUpdate")

    parser.addArgument("--update-check-url")
      .dest("update-url")
      .help("URL to check updates against")

    parser.addArgument("--icon-key")
      .dest("icon-key")
      .help("The icon of the instance")
  }

  def createParser(): ArgumentParser = {
    val parser = ArgumentParsers
      .newFor(Main.ProjectName)
      .build()
      .description("Tool for maintaining Minecraft Mod Packs")

    val sub = parser.addSubparsers().help("Sub Commands").dest("COMMAND")

    createServerParser(sub.addParser("server").help("Operate in Server mode"))
    createClientParser(sub.addParser("client").help("Operate in Client mode"))
    createGeneratorParser(sub.addParser("generate").help("Generate a multimc pack"))

    parser
  }

  def getConfig(side: PackSide, options: Namespace) = {
    val mcDir = side match {
      case PackSide.Client => new File(Option(System.getenv("INST_MC_DIR")) match {
        case Some(dir) => dir
        case None =>
          System.err.println("Please run this program from inside MultiMC (as a PreLaunch command)")
          Util.exit(1)
      })
      case PackSide.Server => new File(".")
    }

    val remoteUrl = options.getString("url")

    val debugFlags =
      System.getProperty("at.chaosfield.packupdate.debug", "") match {
        case "" => HashSet.empty[DebugFlag]
        case list => HashSet.apply(
          list
            .split(",")
            .flatMap(flagName => DebugFlag.fromString(flagName) match {
              case Some(entry) =>
                Some(entry)
              case None =>
                println(s"Unknown flag $flagName")
                None
            }):_*
        )
      }

    MainConfig(mcDir, new URL(remoteUrl), side, options.get("accept-eula"), debugFlags)
  }

  def runCli(config: MainConfig): Unit = {
    val logic = new MainLogic(CliCallbacks)

    logic.runUpdate(config)
  }

  def runSwing(config: MainConfig): Unit = {
    val gui = Util.swingRun(new SwingFrontend)

    Util.swingRunAsync {
      gui.run()
    }

    val logic = new MainLogic(gui)

    logic.runUpdate(config)
    Util.exit(0) // TODO: Find a solution to this
  }

  def mainGenerate(namespace: Namespace): Unit = {
    PackGenerator.run(
      new URL(namespace.getString("url")),
      new File(namespace.getString("out")),
      CliCallbacks,
      Option(namespace.getString("xms").toInt),
      Option(namespace.getString("xmx").toInt),
      namespace.getBoolean("beta"),
      new URL(Option(namespace.getString("update-url")).getOrElse(Main.PackUpdateReleaseUrl.toString)),
      Option(namespace.getString("icon-key"))
    )
  }

  def mainClient(namespace: Namespace): Unit = {
    val config = getConfig(PackSide.Client, namespace)
    runFrontend(namespace, "swing", config)
  }

  def mainServer(namespace: Namespace): Unit = {
    val config = getConfig(PackSide.Server, namespace)
    runFrontend(namespace, "cli", config)
  }

  def runFrontend(namespace: Namespace, defaultUi: String, config: MainConfig): Unit = {
    Option(namespace.getString("ui")).getOrElse(defaultUi) match {
      case "cli" => runCli(config)
      case "swing" => runSwing(config)
    }
  }

  def main(args: Array[String]): Unit = {
    val parser = createParser()

    try {
      val options = parser.parseArgs(args)

      Option(options.getString("COMMAND")) match {
        case Some("client") => mainClient(options)
        case Some("server") => mainServer(options)
        case Some("generate") => mainGenerate(options)
        case Some(c) => System.err.println(s"Unknown subcommand $c")
        case _ => parser.printUsage()
      }
    } catch {
      case e: ArgumentParserException =>
        System.err.println(s"Error parsing arguments: ${e.getMessage}")
    }
  }
}
