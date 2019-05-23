package at.chaosfield.packupdate

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Stage
import java.io.IOException
import java.util


/**
  * Created by Jakob (XDjackieXD) Riepler
  */
class PackUpdate extends Application {
  protected var primaryStage: Stage = null
  protected var rootLayout: VBox = null
  //The first parameter has to be the link to the online Pack Info CSV file,
  //the second parameter has to be the location of the local Pack Info CSV file inside the modpack root and
  //the third parameter has to be the path to the modpack root where the mods folder should go.
  private[packupdate] var parameters: util.List[String] = null

  @throws[Exception]
  override def start(primaryStage: Stage): Unit = {
    this.primaryStage = primaryStage
    this.primaryStage.setTitle("Updating")
    parameters = this.getParameters.getRaw
    if (parameters.size != 3) {
      this.primaryStage.setScene(new Scene(new Group, 300, 300, Color.BLACK))
      errorAlert("Wrong Parameters", "Pack Updater was provided with the wrong Parameters", "If you did not modify any instance settings\nplease contact the modpack author!")
      primaryStage.close()
      return
    }
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
    import scala.collection.JavaConversions._
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

  def main(args: Array[String]): Unit = {
    Application.launch(args:_*)
  }
}