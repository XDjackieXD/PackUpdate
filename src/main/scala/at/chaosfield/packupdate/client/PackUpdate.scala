package at.chaosfield.packupdate.client

import java.io.IOException
import java.util

import at.chaosfield.packupdate.Main
import at.chaosfield.packupdate.common.MainConfig
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.control.{Alert, Label, TextArea}
import javafx.scene.layout.{GridPane, Priority, VBox}
import javafx.scene.paint.Color
import javafx.scene.{Group, Scene}
import javafx.stage.Stage

class PackUpdate extends Application {
  protected var primaryStage: Stage = null
  protected var rootLayout: VBox = null
  private[packupdate] val config = Main.options

  @throws[Exception]
  override def start(primaryStage: Stage): Unit = {
    this.primaryStage = primaryStage
    this.primaryStage.setTitle("Updating")
    initRootLayout()
  }

  def initRootLayout(): Unit = {
    try {
      val loader = new FXMLLoader
      loader.setLocation(classOf[PackUpdate].getResource("main.fxml"))
      rootLayout = loader.load[VBox]
      val scene = new Scene(rootLayout)
      primaryStage.setScene(scene)
      primaryStage.setResizable(false)
      primaryStage.show()
      loader.getController.asInstanceOf[FxController].setMain(this)
    } catch {
      case e: IOException =>
        e.printStackTrace()
    }
  }

  def errorAlert(errors: List[String]): Unit = {
    val alert = new Alert(Alert.AlertType.ERROR)
    alert.setTitle("PackUpdate Error")
    alert.setHeaderText("Something went wrong while updating your pack :(")
    val label = new Label("Log:")
    var textAreaText = ""
    for (error <- errors) {
      textAreaText += "\n" + error
    }
    val textArea = new TextArea(textAreaText)
    textArea.setEditable(false)
    textArea.setWrapText(true)
    textArea.setMaxWidth(Double.MaxValue)
    textArea.setMaxHeight(Double.MaxValue)
    GridPane.setVgrow(textArea, Priority.ALWAYS)
    GridPane.setHgrow(textArea, Priority.ALWAYS)
    val expContent = new GridPane
    expContent.setMaxWidth(Double.MaxValue)
    expContent.add(label, 0, 0)
    expContent.add(textArea, 0, 1)
    alert.getDialogPane.setContent(expContent)
    alert.initOwner(this.primaryStage)
    alert.showAndWait
  }

  def errorAlert(title: String, header: String, message: String): Unit = {
    val alert = new Alert(Alert.AlertType.ERROR)
    alert.setTitle(title)
    alert.setHeaderText(header)
    alert.setContentText(message)
    alert.initOwner(this.primaryStage)
    alert.showAndWait
  }

  def close(): Unit = {
    primaryStage.close()
  }
}