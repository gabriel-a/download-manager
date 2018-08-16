package com.download

import akka.actor.{ActorRef, ActorSystem}
import akka.event.jul.Logger
import com.download.conf.AppConf
import com.download.service.{GenerateActors, MainService}
import scala.concurrent.ExecutionContext.Implicits.global

object Main {

  private var mainService: MainService = GenerateActors
  val logger = Logger(this.getClass.getSimpleName)
  private val DownloadManagerActorSystemName = AppConf.DownloadManagerActorSystemName

  def main(args: Array[String]): Unit = {
    logger.info("Starting application...")
    val actors = mainService.apply()
    logger.info(s"${actors.size} Actors Created and Starting to pull data.")
  }

  def setMainService(injectedMainService: MainService): Unit = {
    mainService = injectedMainService
  }

  def killAllActors(): Unit = {
    logger.info("Terminating actors...")
    ActorSystem(DownloadManagerActorSystemName).terminate()
      .onComplete(_ => logger.info(s"Actor system $DownloadManagerActorSystemName terminated."))
    logger.info("Actors Terminated")
  }
}