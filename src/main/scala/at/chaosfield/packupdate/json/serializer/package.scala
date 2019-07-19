package at.chaosfield.packupdate.json

import org.json4s.{DefaultFormats, Formats}

package object serializer {
  def formats: Formats =
    DefaultFormats + ComponentFlagSerializer + URISerializer + ComponentTypeSerializer + FileHashSerializer
}
