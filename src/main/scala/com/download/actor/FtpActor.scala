package com.download.actor

import java.nio.file.Paths

import akka.NotUsed
import akka.actor.{Actor, Props, Timers}
import akka.event.Logging
import akka.stream.alpakka.ftp.FtpFile
import akka.stream.scaladsl.{FileIO, Source}
import akka.stream.{ActorMaterializer, IOResult, ThrottleMode}
import com.download.conf.{AppConf, ProviderConfig}
import com.download.dto.FileOrDirModel
import com.download.model.{DestinationModel, FtpRemoteSettings}
import com.download.service.IOHelper._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Success

object FtpActor {
  def props(provider: ProviderConfig, destinationModel: DestinationModel): Props = Props(new FtpActor(provider, destinationModel))
}

class FtpActor(val provider: ProviderConfig,
               val destinationModel: DestinationModel) extends Actor with Timers {

  val log = Logging(context.system, this)
  import com.download.model.Reminder._
  timers.startSingleTimer(PeriodicKey, FirstTick, 1.millis)
  implicit val runnableActor: ActorMaterializer = ActorMaterializer()

  def receive = {
    case FirstTick ⇒
      timers.startSingleTimer(FirstRunKey, Download, 1.millis)
      timers.startPeriodicTimer(PeriodicKey, Download, provider.interval.second)
    case Download ⇒
      log.info(s"${provider.protocol} Download received")
      val currentDownloadDestination = DestinationModel.getProviderDestinations(provider.id, destinationModel)
      val throttleConnections = provider.maxConcurrentConnections - 1
      val filesOnDisk = listFilesOnDisk(currentDownloadDestination.finalDestination, currentDownloadDestination.tmpDestination)
      val remoteSettings = new FtpRemoteSettings(provider, destinationModel)
      val filesOnRemoteServer = getListOfRemoteFiles(provider.basePath,filesOnDisk,remoteSettings)

      throttleAndSave(filesOnRemoteServer,
        currentDownloadDestination,
        throttleConnections,
        remoteSettings)
  }

  private def throttleAndSave(listOfSource : Source[FtpFile, NotUsed],
                              destination : DestinationModel,
                              throttleConnections: Int,
                              remoteSettings: FtpRemoteSettings): Unit ={
    val runnable: Source[FtpFile, NotUsed] = listOfSource

    runnable
      .throttle(elements = throttleConnections, per = 10.second, maximumBurst = throttleConnections, ThrottleMode.shaping)
      .runForeach(file => {
        log.info(s"Found a new file ${file.name}")
        saveAndMoveFile(file, destination, remoteSettings)
      }).onComplete({
      case Success(value) =>
        log.info(s"Succeeded $value")
      case err =>
        log.error(s"Error!! $err Will try to process again!")
    })
  }

  private def saveAndMoveFile(file: FtpFile, destination: DestinationModel, remoteSettings : FtpRemoteSettings): Unit ={
    remoteSettings.fromPath(file.path)
      .runWith(FileIO.toPath(Paths.get(destination.tmpDestination+ "/", file.name))).onComplete {
      case Success(IOResult(count, Success(_))) => fileProcessed(destination.tmpDestination, destination.finalDestination, file.name, count)
      case err =>
        log.info(s"Error downloading file ${file.name} from ftp server to $destination.tmpDestination - $err")
        removeDestination(s"${destination.tmpDestination}/${file.name}")
    }
  }

  private def getListOfRemoteFiles(path: String,
                                   filesOnDisc: List[FileOrDirModel],
                                   remoteSettings: FtpRemoteSettings): Source[FtpFile, NotUsed] =
     remoteSettings.ls(path)
      .filter(_.isFile)
      .filter(file => AppConf.isAllowedExt(file.name, provider.allowedExt))
      .filter(file => !filesOnDisc.map(_.name).contains(file.name))

}