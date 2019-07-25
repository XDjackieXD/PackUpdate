package at.chaosfield.packupdate.generator

import java.io.File
import java.net.URL

import at.chaosfield.packupdate.common.Util

object PackGenerator {

  def run(url: URL, out: File): Unit = {
    
  }

  private def generateMultiMCConfig(launchCommand: Array[String]) = {
    Map("PreLaunchCommand" -> Util.unparseCommandLine(launchCommand))
  }

}
