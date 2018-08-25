package com.download.service.protocols

import java.io.{File, InputStream}
import java.net.URL

import akka.actor.{ActorRefFactory, ActorSystem}
import akka.stream.javadsl.Sink
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, ThrottleMode}
import com.download.Logging
import com.download.conf.ProviderConfig
import com.download.model.DestinationModel
import com.download.service.{DownloadManagerService, HttpHelper}
import com.download.service.IOHelper.listFilesOnDisk
import requests.Util.transferTo

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class HttpRemoteService(provider : ProviderConfig,
                        destinationModel: DestinationModel) extends Logging with DownloadManagerService{

  override def receive(implicit actorRefFactory: ActorRefFactory): Unit = {
    val url = HttpHelper.getUrl(provider)
    logger.info(s"Scraping $url")
    val listOfUrls = HttpHelper.parse(url, provider.allowedExt, provider.username, provider.password)
    val currentDownloadDestination = DestinationModel.getProviderDestinations(provider.id, destinationModel)
    val filesOnDisk = listFilesOnDisk(currentDownloadDestination.finalDestination, currentDownloadDestination.tmpDestination).map(_.name)
    val filterExistingFiles = listOfUrls.filter(fileUrl => !filesOnDisk.contains(getFileNameFromUrl(fileUrl)))

    throttleAndSave(provider.maxConcurrentConnections,
      filterExistingFiles,
      currentDownloadDestination,
      actorRefFactory)
  }

  private def throttleAndSave(concurrent : Int, listOfUrls: Seq[URL],
                              currentDownloadDestination: DestinationModel,
                              actorRefFactory: ActorRefFactory): Unit = {
    implicit val runnableActor: ActorMaterializer = ActorMaterializer()(actorRefFactory)
    implicit val ec: ExecutionContext = ActorSystem().dispatcher
    Source(listOfUrls.toList)
      .throttle(concurrent, 5.seconds, concurrent, ThrottleMode.Shaping)
      .mapAsync(concurrent)(url => Future {
        val fileName = getFileNameFromUrl(url)
        requests.get.stream(url.toString)(
          onDownload = inputStream => {
            saveAndMoveStream(inputStream, fileName, currentDownloadDestination)
          }
        )
      })
      .runWith(Sink.foreach(_ => logger.info("Done with downloading the file!")))
  }

  private def saveAndMoveStream(inputStream : InputStream, fileName: String, currentDownloadDestination : DestinationModel): Boolean ={
    logger.info(s"Downloading $fileName into ${currentDownloadDestination.tmpDestination}")
    transferTo(inputStream, new java.io.FileOutputStream(s"${currentDownloadDestination.tmpDestination}/$fileName"))
    logger.info(s"Transfer done for $fileName moving into ${currentDownloadDestination.finalDestination}")
    new File(currentDownloadDestination.tmpDestination+ "/", fileName).renameTo(new File(currentDownloadDestination.finalDestination + "/", fileName))
  }

  private def getFileNameFromUrl(url: URL): String = {
    url.toString.substring( url.toString.lastIndexOf('/')+1, url.toString.length() )
  }
}
