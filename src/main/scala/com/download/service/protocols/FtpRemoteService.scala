package com.download.service.protocols

import java.io.PrintWriter
import java.net.InetAddress
import java.nio.file.Paths

import akka.NotUsed
import akka.actor.ActorRefFactory
import akka.stream.alpakka.ftp.FtpCredentials.{AnonFtpCredentials, NonAnonFtpCredentials}
import akka.stream.alpakka.ftp.scaladsl.{Ftp, Ftps, Sftp}
import akka.stream.alpakka.ftp.{FtpFile, _}
import akka.stream.scaladsl.{FileIO, Source}
import akka.stream.{ActorMaterializer, IOResult, ThrottleMode}
import akka.util.ByteString
import com.download.Logging
import com.download.conf.{AppConf, ProviderConfig, ProviderProtocolType}
import com.download.dto.FileOrDirModel
import com.download.model.DestinationModel
import com.download.service.DownloadManagerService
import com.download.service.IOHelper.{fileProcessed, listFilesOnDisk, removeDestination}
import org.apache.commons.net.PrintCommandListener
import org.apache.commons.net.ftp.FTPClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success

class FtpRemoteService(provider : ProviderConfig,
                       destinationModel: DestinationModel) extends Logging with DownloadManagerService {

  private val ftpSettings = getFtpSetting()
  private val sftpSettings = getSftpSettings()
  private val ftpsSettings = getFtpsSetting()

  def receive(implicit actorRefFactory: ActorRefFactory): Unit ={
    val currentDownloadDestination = DestinationModel.getProviderDestinations(provider.id, destinationModel)
    val throttleConnections = provider.maxConcurrentConnections - 1
    val filesOnDisk = listFilesOnDisk(currentDownloadDestination.finalDestination, currentDownloadDestination.tmpDestination)
    val filesOnRemoteServer = getListOfRemoteFiles(provider.basePath,filesOnDisk)

    throttleAndSave(filesOnRemoteServer,
      currentDownloadDestination,
      throttleConnections,
      actorRefFactory)
  }

  private def throttleAndSave(listOfSource : Source[FtpFile, NotUsed],
                              destination : DestinationModel,
                              throttleConnections: Int,
                              actorRefFactory: ActorRefFactory): Unit = {

    implicit val runnableActor: ActorMaterializer = ActorMaterializer()(actorRefFactory)

    listOfSource
      .throttle(elements = throttleConnections, per = 10.second, maximumBurst = throttleConnections, ThrottleMode.shaping)
      .runForeach(file => {
        logger.info(s"Found a new file ${file.name}")
        saveAndMoveFile(file, destination, actorRefFactory)
      }).onComplete({
      case Success(value) =>
        logger.info(s"Succeeded $value")
      case err =>
        logger.warning(s"Error!! $err Will try to process again!")
    })
  }

  private def saveAndMoveFile(file: FtpFile, destination: DestinationModel, actorRefFactory: ActorRefFactory): Unit ={
    implicit val runnableActor: ActorMaterializer = ActorMaterializer()(actorRefFactory)
    fromPath(file.path)
      .runWith(FileIO.toPath(Paths.get(destination.tmpDestination+ "/", file.name))).onComplete {
      case Success(IOResult(count, Success(_))) => fileProcessed(destination.tmpDestination, destination.finalDestination, file.name, count)
      case err =>
        logger.info(s"Error downloading file ${file.name} from ftp server to $destination.tmpDestination - $err")
        removeDestination(s"${destination.tmpDestination}/${file.name}")
    }
  }

  private def getListOfRemoteFiles(path: String,
                                   filesOnDisc: Seq[FileOrDirModel]): Source[FtpFile, NotUsed] =
    ls(path)
      .filter(_.isFile)
      .filter(file => AppConf.isAllowedExt(file.name, provider.allowedExt))
      .filter(file => !filesOnDisc.exists(_.name == file.name))

  private def fromPath(filePath: String): Source[ByteString, Future[IOResult]] = {
    provider.protocol match {
      case ProviderProtocolType.SFTP => Sftp.fromPath(filePath, sftpSettings)
      case ProviderProtocolType.FTPS => Ftps.fromPath(filePath, ftpsSettings)
      case ProviderProtocolType.FTP => Ftp.fromPath(filePath, ftpSettings)
    }
  }

  private def ls(path: String): Source[FtpFile, NotUsed] = {
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
