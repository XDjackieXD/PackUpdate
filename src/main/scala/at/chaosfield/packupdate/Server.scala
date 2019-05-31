package at.chaosfield.packupdate

import java.io.File
import java.net.URL

import at.chaosfield.packupdate.common.{ConflictResolution, MainConfig, MainLogic, PackSide, UiCallbacks, Util}
import at.chaosfield.packupdate.server.Launcher
import org.jline.terminal.TerminalBuilder

object Server {

  object CliCallbacks extends UiCallbacks {

    var currentStatus = ""
    var currentTotal = 0
    var currentProgress = 0
    var progressShown = false

    /**
      *
      * @param message   the message to display
      * @param exception if this is associated with an exception, this exception
      */
    override def reportError(message: String, exception: Option[Exception]): Unit = {
      exception match {
        case Some(e) =>
          if (Util.isExceptionCritical(e))
            e.printStackTrace()
        case None =>
      }
      println(message)
      redraw()
    }

    /**
      * Show a progress indicator to the user
      */
    override def progressBar_=(value: Boolean): Unit = {
      progressShown = value
      redraw()
    }

    override def progressBar: Boolean = progressShown

    /**
      * Update progress indicator
      *
      * @param numProcessed the amount of items processed so far
      * @param numTotal     the amount of items to process in total
      */
    override def progressUpdate(numProcessed: Int, numTotal: Int): Unit = {
      currentProgress = numProcessed
      currentTotal = numTotal
      redraw()
    }

    /**
      * Update the status message
      *
      * @param status The status message to show
      */
    override def statusUpdate(status: String): Unit = {
      currentStatus = status
      redraw()
    }

    /**
      * Called when a file conflict occurs
      *
      * @param fileName the conflicting file
      * @param remain   the remaining amount of conflicts
      */
    override def askConflict(fileName: String, remain: Int): ConflictResolution = ???

    def redraw() = {
      val data = (if (progressShown) {
        s"[${currentProgress + 1}/$currentTotal] "
      } else {
        ""
      }) + currentStatus

      val widthHint = terminal.getWidth

      val width = if (widthHint < 10) {
        80
      } else {
        widthHint
      }

      print(data + " " * (width - data.length) + "\r")
    }

    lazy val terminal = TerminalBuilder.terminal()
  }

  def run(config: MainConfig): Option[File] = {
    val logic = new MainLogic(CliCallbacks)

    val packupdateData = new File("packupdate")
    packupdateData.mkdirs()
    val localFile = new File(packupdateData, "local.cfg")
    logic.runUpdate(localFile, config)

    MainLogic.getRunnableJar(localFile, config.minecraftDir)
  }

  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      println("Usage: packupdate-server.jar <url>")
    }

    val config = new MainConfig(new File("."), new URL(args(0)), PackSide.Server)
    val runnableJar = run(config)

    if (args.length > 1) {
      println("Launching Server...")
      runnableJar match {
        case Some(jar) => Launcher.launchServer(jar, args.tail)
        case None => println("No runnable jar found, not launching server")
      }
    }


  }
}
