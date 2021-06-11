package at.chaosfield.packupdate.common

/**
  * These callbacks will be called in the background thread. Please ensure to not block longer than possible
  */
trait UiCallbacks extends Log {
  /**
    *
    * @param message the message to display
    * @param exception if this is associated with an exception, this exception
    */
  def reportError(message: String, exception: Option[Exception]): Unit

  /**
    * Is the progress bar shown
    * @return true if the progress bar is shown
    */
  def progressBar: Boolean

  /**
    * Show a progress indicator to the user
    */
  def progressBar_=(value: Boolean)

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
    * Is the secondary progress bar shown
    * @return true if the progress bar is shown
    */
  def subProgressBar: Boolean

  /**
    * Show a secondary progress indicator to the user
    */
  def subProgressBar_=(value: Boolean)

  def subProgressUpdate(numProcessed: Int, numTotal: Int)

  def subUnit: ProgressUnit

  def subUnit_=(unit: ProgressUnit)

  /**
    * Update the status message
    * @param status The status message to show
    */
  def subStatusUpdate(status: Option[String])

  /**
    * Called when a file conflict occurs
    * @param fileName the conflicting file
    * @param remain the remaining amount of conflicts
    */
  def askConflict(fileName: String, remain: Int): ConflictResolution

  /**
    * Print a summary of the transaction that is about to be performed
    * @param summary
    */
  def printTransactionSummary(summary: List[(String, List[Update])]): Unit

  /**
    * Asks the user interactively provide user credentials
    * @param prefillUsername if provided, the username is pre-filled with this value
    * @param message if provided a message provided by the server on why the user should authenticate
    * @return [[None]], if authentication was canceled, otherwise the entered credentials
    */
  def askAuthentication(prefillUsername: Option[String], message: Option[String]): Option[AuthResult]

  /**
    * Called before program exits
    */
  def finish(): Unit
}
