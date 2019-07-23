package at.chaosfield.packupdate.json

import java.net.URI

// https://launchermeta.mojang.com/mc/game/version_manifest.json
case class VanillaVersionSpec(
                             versions: VanillaVersion
                             )

case class VanillaVersion(
                         id: String,
                         url: URI
                         )

case class VanillaVersionManifest(
                                 downloads: VanillaDownloadInfo
                                 )

case class VanillaDownloadInfo(
                              server: VanillaDownloadDetails
                              )

case class VanillaDownloadDetails(
                                 sha1: String,
                                 size: Int,
                                 url: URI
                                 )
