package at.chaosfield.packupdate.json

case class MultiMCPack(
                      components: Array[MultiMCComponent],
                      formatVersion: Int
                      )

case class MultiMCComponent(
                           important: Boolean,
                           uid: String,
                           version: String,
                           dependencyOnly: Boolean
                           )
