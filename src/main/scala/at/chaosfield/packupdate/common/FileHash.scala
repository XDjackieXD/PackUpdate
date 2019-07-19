package at.chaosfield.packupdate.common

import java.io.{File, FileInputStream}

import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils

class FileHash(data: Array[Byte]) {

  def this(data: String) = this(Hex.decodeHex(data))

  def hex: String = Hex.encodeHexString(data)
  def binary: Array[Byte] = data

  def canEqual(other: Any): Boolean = other match {
    case _: FileHash | _: String | _: Array[Byte] => true
    case _ => false
  }

  override def equals(o: Any): Boolean = this.canEqual(o) && (o match {
    case other: FileHash => data sameElements other.binary
    case other: String => data sameElements Hex.decodeHex(other)
    case other: Array[Byte] => data sameElements other
    case _ => false
  })

  override def toString: String = hex
  override def hashCode(): Int = data.hashCode
}

object FileHash {
  final val Invalid = new FileHash(new Array[Byte](20))

  def forFile(file: File): FileHash =
    new FileHash(DigestUtils.sha256(new FileInputStream(file)))
}
