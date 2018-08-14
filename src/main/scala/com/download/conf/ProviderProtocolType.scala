package com.download.conf

object ProviderProtocolType extends Enumeration {
  type ProtocolType = Value
  val HTTP, FTP, FTPS, SFTP, UNKNOWN = Value

  def isSupported(d: ProtocolType) = ! (d == UNKNOWN)

  def withNameWithDefault(name: String): Value =
    values.find(_.toString.toLowerCase == name.toLowerCase()).getOrElse(UNKNOWN)
}
