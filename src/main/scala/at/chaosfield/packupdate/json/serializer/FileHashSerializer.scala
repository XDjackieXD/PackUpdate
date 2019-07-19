package at.chaosfield.packupdate.json.serializer

import at.chaosfield.packupdate.common.FileHash
import org.json4s.CustomSerializer
import org.json4s.JsonAST.JString

object FileHashSerializer extends CustomSerializer[FileHash](format => (
  {
    case JString(string: String) => new FileHash(string)
  },
  {
    case hash: FileHash => JString(hash.hex)
  }
))
