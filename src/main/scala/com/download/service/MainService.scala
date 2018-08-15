package com.download.service

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.jul.Logger
import com.download.Terminator
import com.download.actor.{FtpActor, HttpActor}
import com.download.conf.{AppConf, ProviderConfig, ProviderProtocolType}
import com.download.model.DestinationModel

import scala.collection.mutable.ListBuffer

trait MainService {
  def apply() : List[ActorRef] = GenerateActors.apply()
}

object GenerateActors extends MainService{
  val logger = Logger(this.getClass.getSimpleName)
  override def apply(): List[ActorRef] = prepareApplicationAndCreateActors(AppConf.apply())

  private def prepareApplicationAndCreateActors(appConf : AppConf) ={
    createActors(appConf.providers, appConf.destination)
  }

  private def createActors(providers: List[ProviderConfig], destination: DestinationModel): List[ActorRef] ={
    var actors = ListBuffer[ActorRef]()
    providers
      .filter(provider => ProviderProtocolType.isSupported(provider.protocol))
      .foreach(provider => {
        val actorName = getActorName(provider)
        val system = ActorSystem(AppConf.DownloadManagerActorSystemName)
        provider.protocol match {
          case ProviderProtocolType.FTP | ProviderProtocolType.FTPS | ProviderProtocolType.SFTP => {
            logger.info(s"Start FTP/FTPS/SFTP Actor")
            val ftpActor = system.actorOf(FtpActor.props(provider, destination), actorName)
            registerTermination(system, ftpActor, actorName)
            actors += ftpActor
          }
          case ProviderProtocolType.HTTP => {
            logger.info(s"Start HTTP Actor")
            val httpActor = system.actorOf(HttpActor.props(provider, destination), actorName)
            registerTermination(system, httpActor, actorName)
            actors += httpActor
          }
        }
    })
    actors.toList
  }

  private def registerTermination(systemRef: ActorSystem, actorRef: ActorRef, actorName: String): Unit ={
    systemRef.actorOf(Props(classOf[Terminator], actorRef), s"terminator-$actorName")
  }

  private def getActorName(provider: ProviderConfig) : String  = {
    s"downloader-${provider.protocol}-${provider.id}".toLowerCase
  }
}
