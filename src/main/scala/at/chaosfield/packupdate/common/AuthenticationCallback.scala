package at.chaosfield.packupdate.common

import at.chaosfield.packupdate.common.error.AuthenticationFailure

trait AuthenticationCallback {
  @throws[AuthenticationFailure]
  def authenticate(message: Option[String], defaultUsername: Option[String]): Option[(String, String)]

  def confirmCredentials(username: String, password: String)
}
