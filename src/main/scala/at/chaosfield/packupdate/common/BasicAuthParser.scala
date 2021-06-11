package at.chaosfield.packupdate.common

import scala.util.parsing.combinator._

class BasicAuthParser extends RegexParsers {
  // A RegexParsers automatically ignores whitespace by default.
  // Technically this means this parser accepts inputs it shouldn't, but i don't really care

  // A single token according to RFC2616
  def token: Parser[String] = """[^()<>@,;:\\"/\[\]?={} \t]+""".r ^^ { _.toString }

  // A quoted string according to RFC2616
  def quotedString: Parser[String] = """"(?:[^"\\]|\\.)*"""".r ^^ { m =>
    val str = m.toString
    str.substring(1, str.length - 1)
  }

  // Single-Character tokens, used as delimiters
  def eqsign: Parser[Unit] = """=""".r ^^ { _ => () }
  def comma: Parser[Unit] = """,""".r ^^ { _ => () }

  // a single key/value pair for the challenge parameters
  def kvpair: Parser[(String, String)] = token ~ eqsign ~ (token | quotedString) ^^ { case key ~ _ ~ value => (key, value)}

  // A single challenge option
  def challenge: Parser[(String, Map[String, String])] = token ~ kvpair.* ^^ { case scheme ~ kvmap => (scheme, Map(kvmap:_*))}

  // The server may provice multiple comma separated challenges. This is to parse them
  def challengeList: Parser[List[(String, Map[String, String])]] = rep1sep(challenge, comma)
}

object BasicAuthParser extends BasicAuthParser {
  def main(args: Array[String]) = println(parse(challengeList, "Basic realm=\"foo bar\" test=foo, Digest realm=öns"))

  def parseChallenges(input: String): List[(String, Map[String, String])] = {
    parse(challengeList, input) match {
      case Success(matched, _) => matched
      case Failure(msg, _) => throw new RuntimeException(s"Could not parse HTTP Basic Challenge: $msg")
      case Error(msg, _) => throw new RuntimeException(s"Could not parse HTTP Basic Challenge: $msg")
    }
  }
}
