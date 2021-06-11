package at.chaosfield.packupdate.frontend

import java.awt.{Component, Dimension, Frame, Graphics, GridLayout}

import at.chaosfield.packupdate.common._
import javax.swing._

import scala.collection.mutable.ArrayBuffer

class SwingFrontend extends UiCallbacks {

  val panel = new JPanel //("PackUpdate - Updating Mods")

  panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS))

  //val layout = new GridLayout(0, 1)
  //panel.setLayout(layout)
  //jframe.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)

  val status1 = new JLabel("Launching", SwingConstants.CENTER)
  val status2 = new JLabel("", SwingConstants.CENTER)
  val progress1 = new JProgressBar()
  val progress2 = new JProgressBar()

  progress1.setVisible(false)
  progress2.setVisible(false)

  panel.add("status1", status1)
  panel.add("progress1", progress1)
  panel.add("status2", status2)
  panel.add("progress2", progress2)

  val pane = new JOptionPane(
    "",
    JOptionPane.INFORMATION_MESSAGE,
    JOptionPane.DEFAULT_OPTION,
    SwingFrontend.EmptyIcon,
    Array.empty,
    null
  )

  private val dialog = pane.createDialog(new Frame,"PackUpdate - Updating Mods")

  dialog.add(panel)

  val size = new Dimension(310, 125)
  dialog.setMinimumSize(size)
  dialog.setMaximumSize(size)
  dialog.setPreferredSize(size)

  dialog.pack()
  dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)

  private var subStatusMessage: Option[String] = None
  private var subProgressUnit = ProgressUnit.Scalar
  private var subProgressValue = (0, 0)

  private val errorList = ArrayBuffer.empty[String]

  def run(): Unit = {
    dialog.setVisible(true)
  }

  /**
    *
    * @param message   the message to display
    * @param exception if this is associated with an exception, this exception
    */
  override def reportError(message: String, exception: Option[Exception]): Unit = {
    error(message)
    exception match {
      case Some(e) => e.printStackTrace()
      case None =>
    }
    errorList += message
  }

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
    Util.swingRun {
      progress1.setValue(numProcessed)
      progress1.setMaximum(numTotal)
    }
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
    Util.swingRun {
      progress2.setVisible(value)
      updateSubStatus()
    }
  }

  override def subProgressUpdate(numProcessed: Int, numTotal: Int): Unit = {
    subProgressValue = (numProcessed, numTotal)
    Util.swingRun {
      progress2.setMaximum(numTotal)
      progress2.setValue(numProcessed)
      updateSubStatus()
    }
  }

  override def subUnit: ProgressUnit = subProgressUnit

  override def subUnit_=(unit: ProgressUnit): Unit = {
    subProgressUnit = unit
    Util.swingRun {
      updateSubStatus()
    }
  }

  /**
    * Update the status message
    *
    * @param status The status message to show
    */
  override def subStatusUpdate(status: Option[String]): Unit = {
    subStatusMessage = status
    Util.swingRun {
      updateSubStatus()
    }
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

  override def log(logLevel: LogLevel, message: String): Unit = println(format_log(logLevel, message))

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

  /**
    * Called before program exits
    */
  override def finish(): Unit = {
    if (errorList.nonEmpty) {
      val panel = new JPanel()
      panel.setLayout(new GridLayout(0, 1))
      val errorBox = new JTextArea(errorList.mkString("\n"))
      errorBox.setEditable(false)
      panel.add(errorBox)

      JOptionPane.showMessageDialog(panel, panel, "PackUpdate encountered errors during operation", JOptionPane.PLAIN_MESSAGE)
    }
  }

  /**
    * Asks the user interactively provide user credentials
    *
    * @param prefillUsername if provided, the username is pre-filled with this value
    * @param message         if provided a message provided by the server on why the user should authenticate
    * @return [[None]], if authentication was canceled, otherwise the entered credentials
    */
  override def askAuthentication(prefillUsername: Option[String], message: Option[String]): Option[AuthResult] = ???
}

object SwingFrontend {
  object EmptyIcon extends Icon {
    override def paintIcon(c: Component, g: Graphics, x: Int, y: Int): Unit = ()

    override def getIconWidth: Int = 0

    override def getIconHeight: Int = 0
  }
}