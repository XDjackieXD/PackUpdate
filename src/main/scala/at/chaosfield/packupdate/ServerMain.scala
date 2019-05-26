package at.chaosfield.packupdate

import java.io.File
import java.net.URL

import org.jline.terminal.TerminalBuilder

object ServerMain {

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
    override def showProgress(): Unit = {
      progressShown = true
      redraw()
    }

    /**
      * Hide the previously shown progress indicator
      */
    override def hideProgress(): Unit = {
      progressShown = false
      redraw()
    }

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
        s"[$currentProgress/$currentTotal] "
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

  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      println("Usage: java -jar packupdate-server.jar <url>")
      println("Initializes everything needed for PackUpdate to run inside this directory")
      return
    }

    val config = MainConfig(new File("."), PackSide.Server)
    val logic = new MainLogic(CliCallbacks)

    val packupdateData = new File("packupdate")
    packupdateData.mkdir()
    logic.runUpdate(new URL(args(0)), new File(packupdateData, "local.cfg"), config)
  }
}
