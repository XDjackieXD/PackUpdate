package at.chaosfield.packupdate

import java.io.File
import java.net.URL

import at.chaosfield.packupdate.common.{CliCallbacks, ConflictResolution, LogLevel, MainConfig, MainLogic, PackSide, ProgressUnit, UiCallbacks, Update, Util}
import at.chaosfield.packupdate.server.Launcher
import org.jline.terminal.TerminalBuilder

object Server {

  def run(config: MainConfig): Unit = {
    val logic = new MainLogic(CliCallbacks)

    logic.runUpdate(config)

    /*
    println("Launching Server...")
    MainLogic.getRunnableJar(config.minecraftDir) match {
      case Some(jar) => Launcher.launchServer(jar, args.tail)
      case None => println("No runnable jar found, not launching server")
    }
    */
  }

  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      println("Usage: packupdate-server.jar <url>")
      return
    }

    val config = MainConfig(new File("."), new URL(args(0)), PackSide.Server)
    run(config)
  }

}
