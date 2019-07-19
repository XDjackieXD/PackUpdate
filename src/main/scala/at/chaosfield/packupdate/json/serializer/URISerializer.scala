package at.chaosfield.packupdate.json.serializer

import java.net.URI

import org.json4s.CustomSerializer
import org.json4s.JsonAST.JString

object URISerializer extends CustomSerializer[URI](format => (
  {
    case JString(string: String) => new URI(string)
  },
  {
    case uri: URI => JString(uri.toASCIIString)
  }
))
