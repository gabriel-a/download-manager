package com.download.conf

object ProviderProtocolType extends Enumeration {
  type ProtocolType = Value
  val HTTP, FTP, FTPS, SFTP, UNKNOWN = Value

  def isSupported(d: ProtocolType): Boolean = ! (d == UNKNOWN)

  def withNameWithDefault(name: String): Value =
    values.find(s => name.equalsIgnoreCase(s.toString)).getOrElse(UNKNOWN)
}
