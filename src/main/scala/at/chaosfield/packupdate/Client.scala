package at.chaosfield.packupdate

import java.io.File
import java.net.URL

import at.chaosfield.packupdate.client.PackUpdate
import at.chaosfield.packupdate.common.{MainConfig, PackSide, Util}
import javafx.application.Application

object Client {

  /// Hack to get this to run on JavaFx 7
  private[packupdate] var options: MainConfig = null

  def run(options: MainConfig): Unit = {
    this.options = options
    Application.launch(classOf[PackUpdate])
  }

  def main(args: Array[String]): Unit = {
    if (args.length != 1) {
      println("Usage: java -jar PackUpdate.jar <url>")
      System.exit(1)
    }

    val mcDir = Option(System.getenv("INST_MC_DIR")) match {
      case Some(mcDir) => mcDir
      case None =>
        System.err.println("Please run this program from inside MultiMC (as a PreLaunch command)")
        Util.exit(1)
    }

    try {
      run(MainConfig(new File(mcDir), new URL(args(0)), PackSide.Client))
    } catch {
      case e: NoClassDefFoundError if e.getMessage == "javafx/application/Application" =>
        System.err.println("Please install JavaFX. Please note that the version of JavaFX needs to match the version of Java.")
        Util.exit(1)
    }
  }
}
