package at.chaosfield.packupdate.frontend

import java.awt.event.{WindowEvent, WindowListener}
import java.awt.{Dimension, GridLayout}

import at.chaosfield.packupdate.common.{ConflictResolution, LogLevel, ProgressUnit, UiCallbacks, Update, Util}
import javafx.application.Platform
import javax.swing.{BoxLayout, JDialog, JFrame, JLabel, JProgressBar, SwingConstants, WindowConstants}

class SwingFrontend extends UiCallbacks {
  val jframe = new JFrame("PackUpdate - Updating Mods")

  val size = new Dimension(310, 100)
  jframe.setMinimumSize(size)
  jframe.setMaximumSize(size)
  jframe.setPreferredSize(size)

  val layout = new GridLayout(0, 1)
  jframe.setLayout(layout)
  jframe.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)

  val status1 = new JLabel("Launching", SwingConstants.CENTER)
  val status2 = new JLabel("", SwingConstants.CENTER)
  val progress1 = new JProgressBar()
  val progress2 = new JProgressBar()

  progress1.setVisible(false)
  progress2.setVisible(false)

  private val content = jframe.getContentPane

  content.add("status1", status1)
  content.add("progress1", progress1)
  content.add("status2", status2)
  content.add("progress2", progress2)

  jframe.pack()
  jframe.setVisible(true)

  private var subStatusMessage: Option[String] = None
  private var subProgressUnit = ProgressUnit.Scalar
  private var subProgressValue = (0, 0)

  /**
    *
    * @param message   the message to display
    * @param exception if this is associated with an exception, this exception
    */
  override def reportError(message: String, exception: Option[Exception]): Unit = ???

  /**
    * Is the progress bar shown
    *
    * @return true if the progress bar is shown
    */
  override def progressBar: Boolean = progress1.isVisible

  /**
    * Show a progress indicator to the user
    */
  override def progressBar_=(value: Boolean): Unit = progress1.setVisible(value)

  /**
    * Update progress indicator
    *
    * @param numProcessed the amount of items processed so far
    * @param numTotal     the amount of items to process in total
    */
  override def progressUpdate(numProcessed: Int, numTotal: Int): Unit = {
    progress1.setValue(numProcessed)
    progress1.setMaximum(numTotal)
  }

  /**
    * Update the status message
    *
    * @param status The status message to show
    */
  override def statusUpdate(status: String): Unit = status1.setText(status)

  /**
    * Is the secondary progress bar shown
    *
    * @return true if the progress bar is shown
    */
  override def subProgressBar: Boolean = progress2.isVisible

  /**
    * Show a secondary progress indicator to the user
    */
  override def subProgressBar_=(value: Boolean): Unit = {
    progress2.setVisible(value)
    updateSubStatus()
  }

  override def subProgressUpdate(numProcessed: Int, numTotal: Int): Unit = {
    progress2.setMaximum(numTotal)
    progress2.setValue(numProcessed)
    subProgressValue = (numProcessed, numTotal)
    updateSubStatus()
  }

  override def subUnit: ProgressUnit = subProgressUnit

  override def subUnit_=(unit: ProgressUnit): Unit = {
    subProgressUnit = unit
    updateSubStatus()
  }

  /**
    * Update the status message
    *
    * @param status The status message to show
    */
  override def subStatusUpdate(status: Option[String]): Unit = {
    subStatusMessage = status
    updateSubStatus()
  }

  /**
    * Called when a file conflict occurs
    *
    * @param fileName the conflicting file
    * @param remain   the remaining amount of conflicts
    */
  override def askConflict(fileName: String, remain: Int): ConflictResolution = ???

  /**
    * Print a summary of the transaction that is about to be performed
    *
    * @param summary
    */
  override def printTransactionSummary(summary: List[(String, List[Update])]): Unit = CliCallbacks.printTransactionSummary(summary)

  override def log(logLevel: LogLevel, message: String): Unit = CliCallbacks.log(logLevel, message)

  def updateSubStatus(): Unit = {
    val status = new StringBuilder

    subStatusMessage match {
      case Some(s) => status.append(s)
      case None =>
    }

    if (subProgressBar) {
      status.append(" (")
      status.append(subProgressUnit.render(subProgressValue._1))
      if (subProgressUnit != ProgressUnit.Percent) {
        status.append("/")
        status.append(subProgressUnit.render(subProgressValue._2))
      }
      status.append(")")
    }

    status2.setText(status.toString.trim)
  }
}