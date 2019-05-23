package at.chaosfield.packupdate

import scala.concurrent.Channel

/**
  * These callbacks will be called in the background thread. Please ensure to not block longer than possible
  */
trait UiCallbacks {
  /**
    * Show a progress indicator to the user
    */
  def showProgress()

  /**
    * Hide the previously shown progress indicator
    */
  def hideProgress()

  /**
    * Update progress indicator
    * @param numProcessed the amount of items processed so far
    * @param numTotal the amount of items to process in total
    */
  def progressUpdate(numProcessed: Int, numTotal: Int)

  /**
    * Update the status message
    * @param status The status message to show
    */
  def statusUpdate(status: String)

  /**
    * Called when a file conflict occurs
    * @param fileName the conflicting file
    * @param remain the remaining amount of conflicts
    */
  def askConflict(fileName: String, remain: Int): ConflictResolution
}
