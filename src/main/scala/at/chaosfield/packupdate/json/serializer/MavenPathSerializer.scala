package at.chaosfield.packupdate.json.serializer

import at.chaosfield.packupdate.common.{FileHash, MavenPath}
import org.json4s.CustomSerializer
import org.json4s.JsonAST.JString

object MavenPathSerializer extends CustomSerializer[MavenPath](format => (
  {
    case JString(string: String) => MavenPath.fromString(string)
  },
  {
    case path: MavenPath => JString(path.getMavenPath)
  }
))
