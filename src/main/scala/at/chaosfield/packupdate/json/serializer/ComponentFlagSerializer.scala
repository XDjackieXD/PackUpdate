package at.chaosfield.packupdate.json.serializer

import at.chaosfield.packupdate.common.ComponentFlag
import org.json4s.CustomSerializer
import org.json4s.JsonAST.JString

object ComponentFlagSerializer extends CustomSerializer[ComponentFlag](format => (
  {
    case JString(string: String) => ComponentFlag.fromString(string) match {
      case Some(flag) => flag
      case None => throw new Exception(s"unknown component flag $string")
    }
  },
  {
    case ty: ComponentFlag => JString(ty.internalName)
  }
))
