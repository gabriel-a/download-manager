package com.download.model

import java.io.PrintWriter
import java.net.InetAddress

import akka.NotUsed
import akka.event.jul.Logger
import akka.stream.IOResult
import akka.stream.alpakka.ftp.FtpCredentials.{AnonFtpCredentials, NonAnonFtpCredentials}
import akka.stream.alpakka.ftp.scaladsl.{Ftp, Ftps, Sftp}
import akka.stream.alpakka.ftp._
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.download.Logging
import com.download.conf.{ProviderConfig, ProviderProtocolType}
import org.apache.commons.net.PrintCommandListener
import org.apache.commons.net.ftp.FTPClient

import scala.concurrent.Future

class FtpRemoteSettings(provider : ProviderConfig, destinationModel: DestinationModel) extends Logging {

  val ftpSettings = getFtpSetting()
  val sftpSettings = getSftpSettings()
  val ftpsSettings = getFtpsSetting()

  def fromPath(filePath: String): Source[ByteString, Future[IOResult]] = {
    provider.protocol match {
      case ProviderProtocolType.SFTP => Sftp.fromPath(filePath, sftpSettings)
      case ProviderProtocolType.FTPS => Ftps.fromPath(filePath, ftpsSettings)
      case ProviderProtocolType.FTP => Ftp.fromPath(filePath, ftpSettings)
    }
  }

  def ls(path: String): Source[FtpFile, NotUsed] = {
    provider.protocol match {
      case ProviderProtocolType.SFTP => Sftp.ls(path, sftpSettings, _=>false)
      case ProviderProtocolType.FTPS => Ftps.ls(path, ftpsSettings, _=>false)
      case ProviderProtocolType.FTP => Ftp.ls(path, ftpSettings, _=>false)
    }
  }

  private def getFtpSetting(): FtpSettings = {
    FtpSettings(
      InetAddress.getByName(provider.host),
      provider.port,
      credentials = getAuthentication(provider.username, provider.password),
      binary = true,
      passiveMode = true,
      configureConnection = (ftpClient: FTPClient) => {
        ftpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out), true))
      }
    )
  }

  private def getFtpsSetting(): FtpsSettings = {
    FtpsSettings(
      InetAddress.getByName(provider.host),
      provider.port,
      credentials = getAuthentication(provider.username, provider.password),
      binary = true,
      passiveMode = true,
      configureConnection = (ftpClient: FTPClient) => {
        ftpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out), true))
      }
    )
  }

  private def getSftpSettings(): SftpSettings = {
    SftpSettings(
      InetAddress.getByName(provider.host),
      provider.port,
      getAuthentication(provider.username, provider.password),
      strictHostKeyChecking = false
    )
  }

  private def getAuthentication(username: String, password: String) : FtpCredentials = {
    if (username.isEmpty && password.isEmpty)
      AnonFtpCredentials
    else
      NonAnonFtpCredentials(username, password)
  }
}
