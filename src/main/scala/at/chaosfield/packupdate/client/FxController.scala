package at.chaosfield.packupdate.client

import java.io.File
import java.net.URL
import java.util

import at.chaosfield.packupdate.common._
import javafx.beans.property.{DoubleProperty, SimpleBooleanProperty, SimpleDoubleProperty}
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
  private var main: PackUpdate = null

  def setMain(main: PackUpdate): Unit = {
    this.main = main
    val _log = ArrayBuffer.empty[String]
    val updater = new Task[List[String]]() {
      val progressBarShown = new SimpleBooleanProperty(this, "progress", false)
      override protected def call(): List[String] = {

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
            _log += message
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
        }

        new MainLogic(GuiFeedback).runUpdate(main.config)
        _log.toList
      }
    }
    progress.progressProperty.bind(updater.progressProperty)
    progress.visibleProperty().bindBidirectional(updater.progressBarShown)
    status.textProperty.bind(updater.messageProperty)

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
