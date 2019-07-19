package at.chaosfield.packupdate.json.serializer

import at.chaosfield.packupdate.common.ComponentType
import org.json4s.CustomSerializer
import org.json4s.JsonAST.JString

object ComponentTypeSerializer extends CustomSerializer[ComponentType](format => (
  {
    case JString(string: String) => ComponentType.fromString(string) match {
      case Some(ty) => ty
      case None => throw new Exception(s"unknown component type $string")
    }
  },
  {
    case ty: ComponentType => JString(ty.stringValue)
  }
))
