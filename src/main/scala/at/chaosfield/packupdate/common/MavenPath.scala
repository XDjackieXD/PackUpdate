package at.chaosfield.packupdate.common

import java.io.{File, FileNotFoundException}
import java.net.URI

class MavenPath(val domain: String, val name: String, val version: String, val classifier: Option[String], val extension: String = "jar") {

  def getFilePath: String = {
    val builder = new StringBuilder

    builder.append(domain.replace('.', '/'))
    builder.append("/")

    builder.append(name)
    builder.append("/")

    builder.append(version)
    builder.append("/")

    builder.append(name)
    builder.append("-")
    builder.append(version)
    classifier match {
      case Some(c) =>
        builder.append("-")
        builder.append(c)
      case None =>
    }
    builder.append(".")
    builder.append(extension)

    builder.toString
  }

  def getMavenPath: String = {
    val builder = new StringBuilder

    builder.append(domain)
    builder.append(":")

    builder.append(name)
    builder.append(":")

    builder.append(version)

    classifier match {
      case Some(c) =>
        builder.append(":")
        builder.append(c)
      case None =>
    }

    if (extension != "jar") {
      builder.append("@")
      builder.append(extension)
    }

    builder.toString
  }

  def getPom: MavenPath = new MavenPath(domain, name, version, classifier, "pom")

  def canEqual(other: Any): Boolean = other.isInstanceOf[MavenPath]

  override def equals(other: Any): Boolean = canEqual(other) && (other match {
    case p: MavenPath =>
      domain == p.domain && name == p.name && classifier == p.classifier && extension == p.extension
    case _ => false
  })

  def downloadTo(tryUrls: List[URI], dest: File, log: Log, progressCallback: (Int, Option[Int]) => Unit = (_, _) => ()): Unit = {
    var lastException: Option[Exception] = None
    for (url <- tryUrls) {
      try {
        FileManager.downloadWithHash(
          url
            .resolve(getFilePath)
            .toURL,
          dest,
          log,
          None,
          None, // TODO: Handle checksum
          progressCallback = progressCallback
        )
        return
      } catch {
        case e: FileNotFoundException =>
          lastException = Some(e)
          log.debug(s"File not found at $url, trying next...")
      }
    }
    throw lastException.get
  }

  override def hashCode(): Int = {
    domain.hashCode + name.hashCode + version.hashCode + classifier.hashCode() + extension.hashCode
  }
}

object MavenPath {
  private val parsingRegex = "^([\\w\\d-_\\.]+):([\\w\\d-_\\.]+):([\\w\\d-_\\.]+)(?::([\\w\\d-_\\.]+))?(?:@([\\w\\d-_]+))?$".r

  def fromString(path: String): MavenPath = {
    parsingRegex.findFirstMatchIn(path) match {
      case Some(m) =>
        new MavenPath(
          m.group(1),
          m.group(2),
          m.group(3),
          Option(m.group(4)),
          Option(m.group(5)).getOrElse("jar")
        )
      case None =>
        throw new RuntimeException(s"Invalid Maven Identifier: $path")
    }
  }
}