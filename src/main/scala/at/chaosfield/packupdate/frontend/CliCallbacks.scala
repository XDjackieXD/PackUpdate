package at.chaosfield.packupdate.frontend

import at.chaosfield.packupdate.common._
import org.jline.terminal.TerminalBuilder

object CliCallbacks extends UiCallbacks {

  var currentStatus = ""
  var currentTotal = 0
  var currentProgress = 0
  var progressShown = false

  var currentSubStatus: Option[String] = None
  var currentSubTotal = 0
  var currentSubProgress = 0
  var subProgressShown = false
  var subProgressUnit = ProgressUnit.Scalar

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
    val data = new StringBuilder
    if (progressShown) {
      data.append(s"[${currentProgress + 1}/$currentTotal] ")
    }

    data.append(currentStatus)

    if (subProgressShown || currentSubStatus.isDefined) {
      data.append(": ")
    }

    if (subProgressShown) {
      data.append("[")
      data.append(subProgressUnit.render(currentSubProgress))
      if (subProgressUnit != ProgressUnit.Percent) {
        data.append("/")
        data.append(subProgressUnit.render(currentSubTotal))
      }
      if (currentSubStatus.isDefined) {
        data.append("] ")
      }
    }

    currentSubStatus match {
      case Some(status) =>
        data.append(status)
      case None =>
    }

    val widthHint = terminal.getWidth

    val width = if (widthHint < 10) {
      80
    } else {
      widthHint
    }

    print(data + " " * (width - data.length) + "\r")
  }

  lazy val terminal = TerminalBuilder.terminal()

  override def log(logLevel: LogLevel, message: String): Unit = {
    println(format_log(logLevel, message))
    redraw()
  }

  /**
    * Is the secondary progress bar shown
    *
    * @return true if the progress bar is shown
    */
  override def subProgressBar: Boolean = subProgressShown

  /**
    * Show a secondary progress indicator to the user
    */
  override def subProgressBar_=(value: Boolean): Unit = {
    subProgressShown = value
    redraw()
  }

  /**
    *
    * @param numProcessed
    * @param numTotal
    */
  override def subProgressUpdate(numProcessed: Int, numTotal: Int): Unit = {
    currentSubProgress = numProcessed
    currentSubTotal = numTotal
    redraw()
  }

  /**
    * Update the status message
    *
    * @param status The status message to show
    */
  override def subStatusUpdate(status: Option[String]): Unit = {
    currentSubStatus = status
    redraw()
  }

  override def subUnit: ProgressUnit = subProgressUnit

  override def subUnit_=(unit: ProgressUnit): Unit = {
    subProgressUnit = unit
    redraw()
  }

  /**
    * Print a summary of the transaction that is about to be performed
    *
    * @param summary
    */
  override def printTransactionSummary(summary: List[(String, List[Update])]): Unit = {
    summary.foreach{case (name, updates) => {
      if (updates.nonEmpty) {
        println(s"$name: ${updates.map(_.name).mkString(", ")}")
      }
    }}
    redraw()
  }
}
