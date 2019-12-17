package at.chaosfield.packupdate.common

import java.io.File
import java.net.URL

import scala.collection.immutable.HashSet

case class MainConfig(
                       minecraftDir: File,
                       remoteUrl: URL,
                       packSide: PackSide,
                       acceptEula: Boolean = false,
                       debugFlags: HashSet[DebugFlag]
                     )
