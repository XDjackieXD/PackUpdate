package at.chaosfield.packupdate.json

case class MultiMCMetadata(
        formatVersion: Int,
        versions: Array[MultiMCMetaVersion]
        )

case class MultiMCMetaVersion(
                             version: String
                             )
