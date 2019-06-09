package at.chaosfield.packupdate

import java.io.File
import java.net.URL

import at.chaosfield.packupdate.client.PackUpdate
import at.chaosfield.packupdate.common.{MainConfig, PackSide}
import javafx.application.Application

object Client {

  /// Hack to get this to run on JavaFx 7
  private[packupdate] var options: MainConfig = null

  def run(options: MainConfig): Unit = {
    this.options = options
    Application.launch(classOf[PackUpdate])
  }

  def main(args: Array[String]) = {
    if (args.length != 1) {
      println("Usage: java -jar PackUpdate.jar <url>")
      System.exit(1)
    }

    run(new MainConfig(new File(Option(System.getenv("INST_MC_DIR")).get), new URL(args(0)), PackSide.Client))
  }
}
