package at.chaosfield.packupdate

import javafx.concurrent.Task
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.stage.Stage
import java.io.File
import java.io.IOException
import java.lang.Override
import java.lang.String
import java.net.URL
import java.util

import javafx.application.Application
import javafx.event.Event
import javafx.concurrent.Worker

import scala.collection.mutable.ArrayBuffer
import scala.collection.convert.ToJavaImplicits


/**
  * Created by Jakob (XDjackieXD) Riepler
  */
class FxController {
  @FXML private var status: Label = null
  @FXML private var progress: ProgressBar = null
  private var parameters: util.List[String] = null
  private var main: PackUpdate = null

  def setMain(main: PackUpdate): Unit = {
    this.main = main
    this.parameters = main.getParameters.getRaw
    val log = ArrayBuffer.empty[String]
    val updater = new Task[List[String]]() {
      override protected def call(): List[String] = {

        object GuiFeedback extends UiCallbacks {
          /**
            * Show a progress indicator to the user
            */
          override def showProgress(): Unit = Unit

          /**
            * Hide the previously shown progress indicator
            */
          override def hideProgress(): Unit = Unit

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
            log += message
            println(message)
          }
        }

        val remote = new URL(parameters.get(0))
        val local = new File(parameters.get(1))
        val minecraftDir = new File(parameters.get(2))

        new MainLogic(GuiFeedback).runUpdate(remote, local, new MainConfig(minecraftDir))
        log.toList
      }
    }
    progress.progressProperty.bind(updater.progressProperty)
    status.textProperty.bind(updater.messageProperty)

    updater.setOnSucceeded((t: Event) => {
        val returnValue = updater.getValue
        if (returnValue.nonEmpty) {
          main.errorAlert(returnValue)
        }
        main.stop()
    })
    new Thread(updater).start()
  }
}
