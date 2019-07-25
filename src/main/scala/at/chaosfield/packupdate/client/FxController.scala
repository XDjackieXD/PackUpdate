package at.chaosfield.packupdate.client

import at.chaosfield.packupdate.client
import at.chaosfield.packupdate.common._
import javafx.application.Platform
import javafx.beans.property.{SimpleBooleanProperty, SimpleDoubleProperty, SimpleStringProperty}
import javafx.concurrent.Task
import javafx.event.Event
import javafx.fxml.FXML
import javafx.scene.control.{Label, ProgressBar}

import scala.collection.mutable.ArrayBuffer


/**
  * Created by Jakob (XDjackieXD) Riepler
  */
class FxController {
  @FXML private var status: Label = null
  @FXML private var progress: ProgressBar = null
  @FXML private var subProgress: ProgressBar = null
  @FXML private var subStatus: Label = null
  private var main: PackUpdate = null

  def setMain(main: PackUpdate): Unit = {
    this.main = main
    val updater = new client.FxController.MainWorker(main)
    progress.progressProperty.bind(updater.progressProperty)
    progress.visibleProperty().bindBidirectional(updater.progressBarShown)
    status.textProperty.bind(updater.messageProperty)

    subProgress.visibleProperty().bind(updater.subProgressBarShown)
    subProgress.progressProperty().bind(updater.subProgressValue)

    subStatus.textProperty().bind(updater.subStatusValue)

    updater.setOnSucceeded((t: Event) => {
        val returnValue = updater.getValue
        if (returnValue.nonEmpty) {
          main.errorAlert(returnValue)
        }
        main.close()
    })
    new Thread(updater).start()
  }
}

object FxController {
  class MainWorker(main: PackUpdate) extends Task[List[String]] {
    val progressBarShown = new SimpleBooleanProperty(this, "progress", false)
    val subProgressBarShown = new SimpleBooleanProperty(this, "subProgressShown", false)
    val subProgressValue: SimpleDoubleProperty = new SimpleDoubleProperty(this, "subProgress", 0)
    val subStatusValue = new SimpleStringProperty(this, "subStatus", "")
    override protected def call(): List[String] = {

      val errorLog = ArrayBuffer.empty[String]
      var subStatusMessage: Option[String] = None
      var subProgressInternal = (0, 0)
      var subProgressUnit = ProgressUnit.Scalar

      object GuiFeedback extends UiCallbacks {

        /**
          * Update progress indicator
          *
          * @param numProcessed the amount of items processed so far
          * @param numTotal     the amount of items to process in total
          */
        override def progressUpdate(numProcessed: Int, numTotal: Int): Unit = {
          updateProgress(numProcessed, numTotal)
        }

        /**
          * Update the status message
          *
          * @param status The status message to show
          */
        override def statusUpdate(status: String): Unit = {
          updateMessage(status)
        }

        /**
          * Called when a file conflict occurs
          *
          * @param fileName the conflicting file
          * @param remain   the remaining amount of conflicts
          */
        override def askConflict(fileName: String, remain: Int): ConflictResolution = ???

        /**
          *
          * @param message   the message to display
          * @param exception if this is associated with an exception, this exception
          */
        override def reportError(message: String, exception: Option[Exception]): Unit = {
          errorLog += message
          println(message)
          exception match {
            case Some(e) => e.printStackTrace()
            case _ =>
          }
        }

        /**
          * Is the progress bar shown
          *
          * @return true if the progress bar is shown
          */
        override def progressBar: Boolean = progressBarShown.get()

        /**
          * Show a progress indicator to the user
          */
        override def progressBar_=(value: Boolean): Unit = {
          progressBarShown.set(value)
        }

        override def log(logLevel: LogLevel, message: String): Unit = {
          println(format_log(logLevel, message))
        }

        /**
          * Is the secondary progress bar shown
          *
          * @return true if the progress bar is shown
          */
        override def subProgressBar: Boolean = subProgressBarShown.get()

        /**
          * Show a secondary progress indicator to the user
          */
        override def subProgressBar_=(value: Boolean): Unit = subProgressBarShown.set(value)

        override def subProgressUpdate(numProcessed: Int, numTotal: Int): Unit = {
          subProgressValue.set(numProcessed.toFloat / numTotal.toFloat)
          subProgressInternal = (numProcessed, numTotal)
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

        def updateSubStatus(): Unit = {
          val status = new StringBuilder

          subStatusMessage match {
            case Some(s) => status.append(s)
            case None =>
          }

          if (subProgressBarShown.get) {
            status.append(" (")
            status.append(subProgressUnit.render(subProgressInternal._1))
            if (subProgressUnit != ProgressUnit.Percent) {
              status.append("/")
              status.append(subProgressUnit.render(subProgressInternal._2))
            }
            status.append(")")
          }

          Platform.runLater(() => {
            subStatusValue.set(status.toString.trim)
          })
        }

        /**
          * Print a summary of the transaction that is about to be performed
          *
          * @param summary
          */
        override def printTransactionSummary(summary: List[(String, List[Update])]): Unit = Unit
      }
      new MainLogic(GuiFeedback).runUpdate(main.config)
      errorLog.toList
    }
  }
}