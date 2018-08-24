package com.download.actor

import java.nio.file.Paths

import akka.NotUsed
import akka.actor.{Actor, Props, Timers}
import akka.event.Logging
import akka.stream.alpakka.ftp.FtpFile
import akka.stream.scaladsl.{FileIO, Source}
import akka.stream.{ActorMaterializer, IOResult, ThrottleMode}
import com.download.Logging
import com.download.conf.{AppConf, ProviderConfig}
import com.download.dto.FileOrDirModel
import com.download.model.{DestinationModel, FtpRemoteSettings}
import com.download.service.IOHelper._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Success
import com.download.model.Reminder._

object FtpActor {
  def props(provider: ProviderConfig, destinationModel: DestinationModel): Props =
    Props(new FtpActor(provider, destinationModel, new FtpRemoteSettings(provider, destinationModel)))
}

class FtpActor(val provider: ProviderConfig,
               val destinationModel: DestinationModel,
               val remoteSettings: FtpRemoteSettings) extends Actor with Timers with Logging {

  implicit val runnableActor: ActorMaterializer = ActorMaterializer()

  // you have two kinds of logging, is it intentional?
  val log = Logging(context.system, this)

  // I think you need to use preStart or postRestart for that
  timers.startSingleTimer(PeriodicKey, FirstTick, 1.millis)

  def receive = {
    case FirstTick ⇒
      timers.startSingleTimer(FirstRunKey, Download, 1.millis)
      timers.startPeriodicTimer(PeriodicKey, Download, provider.interval.second)
    case Download ⇒
      log.info(s"${provider.protocol} Download received")
      val currentDownloadDestination = DestinationModel.getProviderDestinations(provider.id, destinationModel)
      val throttleConnections = provider.maxConcurrentConnections - 1
      val filesOnDisk = listFilesOnDisk(currentDownloadDestination.finalDestination, currentDownloadDestination.tmpDestination)
      val filesOnRemoteServer = getListOfRemoteFiles(provider.basePath,filesOnDisk)

      throttleAndSave(filesOnRemoteServer,
        currentDownloadDestination,
        throttleConnections)
  }

  private def throttleAndSave(listOfSource : Source[FtpFile, NotUsed],
                              destination : DestinationModel,
                              throttleConnections: Int): Unit = {

    // why not inline it?
    listOfSource
      .throttle(elements = throttleConnections, per = 10.second, maximumBurst = throttleConnections, ThrottleMode.shaping)
      .runForeach(file => {
        log.info(s"Found a new file ${file.name}")
        saveAndMoveFile(file, destination)
      }).onComplete({
      case Success(value) =>
        log.info(s"Succeeded $value")
      case err =>
        // it says 'Will try to process again' but there's no difference
        // between Success branch and this one
        log.error(s"Error!! $err Will try to process again!")
    })
  }

  private def saveAndMoveFile(file: FtpFile, destination: DestinationModel): Unit ={
    remoteSettings.fromPath(file.path)
      .runWith(FileIO.toPath(Paths.get(destination.tmpDestination+ "/", file.name))).onComplete {
      case Success(IOResult(count, Success(_))) => fileProcessed(destination.tmpDestination, destination.finalDestination, file.name, count)
      case err =>
        log.info(s"Error downloading file ${file.name} from ftp server to $destination.tmpDestination - $err")
        removeDestination(s"${destination.tmpDestination}/${file.name}")
    }
  }

  private def getListOfRemoteFiles(path: String,
                                  // don't force it to be a list, Seq is a more generic option
                                   filesOnDisc: Seq[FileOrDirModel]): Source[FtpFile, NotUsed] =
     remoteSettings.ls(path)
      .filter(_.isFile)
      .filter(file => AppConf.isAllowedExt(file.name, provider.allowedExt))
       // exists is just a little faster
      .filter(file => !filesOnDisc.exists(_.name == file.name))

}