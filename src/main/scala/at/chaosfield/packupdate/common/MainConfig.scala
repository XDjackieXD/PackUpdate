package at.chaosfield.packupdate.common

import java.io.File
import java.net.URL

case class MainConfig(
                       minecraftDir: File,
                       remoteUrl: URL,
                       packSide: PackSide,
                       acceptEula: Boolean = false
                     )
