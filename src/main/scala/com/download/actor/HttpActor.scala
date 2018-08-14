package com.download.actor

import java.io.{File, InputStream}
import java.net.URL

import akka.actor.{Actor, ActorSystem, Props, Timers}
import akka.event.Logging
import akka.stream.javadsl.Sink
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, ThrottleMode}
import com.download.conf.ProviderConfig
import com.download.model.DestinationModel
import com.download.service.HttpHelper
import com.download.service.IOHelper._
import requests.Util.transferTo

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * This actor job to handle HTTP & HTTPS
  */
object HttpActor {
  def props(provider: ProviderConfig, destination: DestinationModel): Props = Props(new HttpActor(provider, destination))
}

class HttpActor(provider: ProviderConfig, destinationModel: DestinationModel) extends Actor with Timers {
  val log = Logging(context.system, this)
  import com.download.model.Reminder._
  timers.startSingleTimer(PeriodicKey, FirstTick, 500.millis)

  def receive = {
    case FirstTick ⇒
      timers.startSingleTimer(FirstRunKey, Download, 1.millis)
      timers.startPeriodicTimer(PeriodicKey, Download, provider.interval.second)
    case Download ⇒ {
      val url = HttpHelper.getUrl(provider)
      log.info(s"Scraping $url")
      val listOfUrls = HttpHelper.parse(url, provider.allowedExt, provider.username, provider.password)
      val currentDownloadDestination = DestinationModel.getProviderDestinations(provider.id, destinationModel)
      val filesOnDisk = listFilesOnDisk(currentDownloadDestination.finalDestination, currentDownloadDestination.tmpDestination).map(_.name)
      val filterExistingFiles = listOfUrls.filter(fileUrl => !filesOnDisk.contains(getFileNameFromUrl(fileUrl)))

      throttleAndSave(provider.maxConcurrentConnections,
        filterExistingFiles,
        currentDownloadDestination)
    }
  }

  private def throttleAndSave(concurrent : Int, listOfUrls: List[URL], currentDownloadDestination: DestinationModel): Unit = {
    implicit val runnableActor: ActorMaterializer = ActorMaterializer()
    implicit val ec: ExecutionContext = ActorSystem().dispatcher
    Source(listOfUrls)
      .throttle(concurrent, 5.seconds, concurrent, ThrottleMode.Shaping)
      .mapAsync(concurrent)(url => Future {
        val fileName = getFileNameFromUrl(url)
        requests.get.stream(url.toString)(
          onDownload = inputStream => {
            saveAndMoveStream(inputStream, fileName, currentDownloadDestination)
          }
        )
      })
      .runWith(Sink.foreach(_ => log.info("Done with downloading the file!")))
  }

  private def saveAndMoveStream(inputStream : InputStream, fileName: String, currentDownloadDestination : DestinationModel): Boolean ={
    log.info(s"Downloading $fileName into ${currentDownloadDestination.tmpDestination}")
    transferTo(inputStream, new java.io.FileOutputStream(s"${currentDownloadDestination.tmpDestination}/${fileName}"))
    log.info(s"Transfer done for $fileName moving into ${currentDownloadDestination.finalDestination}")
    new File(currentDownloadDestination.tmpDestination+ "/", fileName).renameTo(new File(currentDownloadDestination.finalDestination + "/", fileName))
  }

  private def getFileNameFromUrl(url: URL): String = {
    url.toString.substring( url.toString.lastIndexOf('/')+1, url.toString.length() )
  }
}