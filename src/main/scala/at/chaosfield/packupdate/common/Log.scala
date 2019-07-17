package at.chaosfield.packupdate.common

trait Log {
  def log(logLevel: LogLevel, message: String)
  def debug(message: String) = log(LogLevel.Debug, message)
  def info(message: String) = log(LogLevel.Info, message)
  def warning(message: String) = log(LogLevel.Warning, message)
  def error(message: String) = log(LogLevel.Error, message)
  protected def format_log(logLevel: LogLevel, message: String) = {
    s"[${logLevel.name}] $message"
  }
}

object StdoutLog extends Log {
  override def log(logLevel: LogLevel, message: String): Unit = println(s"[$logLevel] $message")
}