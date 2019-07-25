package at.chaosfield.packupdate

import java.io.File
import java.net.URL
import java.util.jar.Manifest

import at.chaosfield.packupdate.common.{MainConfig, MainLogic, PackSide, Util}
import net.sourceforge.argparse4j.ArgumentParsers
import net.sourceforge.argparse4j.impl.Arguments
import net.sourceforge.argparse4j.inf.{ArgumentAction, ArgumentParser, ArgumentParserException, Namespace, Subparser}
import net.sourceforge.argparse4j.internal.UnrecognizedArgumentException

import scala.collection.JavaConverters._


object Main {

  val ProjectName = "PackUpdate"

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
      .`type`(classOf[URL])
      .help("The URL of the pack")
  }

  def createClientParser(parser: Subparser): Unit = {
    parser.addArgument("url")
      .dest("url")
      .`type`(classOf[URL])
      .help("The URL of the pack")
  }

  def createGeneratorParser(parser: Subparser): Unit = {
    parser.addArgument("--pack-url")
      .dest("url")
      .required(true)
      .nargs(1)
      .`type`(classOf[URL])
      .help("The URL where the pack csv is located at")

    parser.addArgument("--out")
      .dest("out")
      .required(true)
      .help("The output zip file")
      .nargs(1)

    parser.addArgument("--java-xms")
      .dest("xms")
      .help("The JVM Minimum Memory value")
      .nargs(1)

    parser.addArgument("--java-xmx")
      .dest("xmx")
      .help("The JVM Maximum Memory value")
      .nargs(1)

    parser.addArgument("--forge-version")
      .dest("forge")
      .help("The version of Forge to use")
      .nargs(1)
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

    MainConfig(mcDir, new URL(remoteUrl), side, options.get("accept-eula"))
  }

  def mainClient(namespace: Namespace): Unit = {
    val config = getConfig(PackSide.Client, namespace)

    Client.run(config)
  }

  def mainServer(namespace: Namespace): Unit = {
    val config = getConfig(PackSide.Server, namespace)

    Server.run(config)
  }

  def mainGenerate(namespace: Namespace): Unit = {
    ???
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
