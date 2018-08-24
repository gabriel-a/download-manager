package com.download.service

import akka.actor.{ActorRef, ActorSystem, Props}
import com.download.actor.{FtpActor, HttpActor}
import com.download.conf.{AppConf, ProviderConfig, ProviderProtocolType}
import com.download.model.DestinationModel
import com.download.{Logging, Terminator}

trait MainService {
  def apply(): Seq[ActorRef] = GenerateActors.apply()
}

object GenerateActors extends MainService with Logging {

  override def apply(): Seq[ActorRef] = prepareApplicationAndCreateActors(AppConf.apply())

  private def prepareApplicationAndCreateActors(appConf: AppConf) = {
    createActors(appConf.providers, appConf.destination)
  }


  private def createActors(providers: Seq[ProviderConfig], destination: DestinationModel): Seq[ActorRef] = {
    providers
      .filter(provider => ProviderProtocolType.isSupported(provider.protocol))
      .map(provider => {
        val actorName = getActorName(provider)
        val system = ActorSystem(AppConf.DownloadManagerActorSystemName)
        val actorProp = provider.protocol match {
          case ProviderProtocolType.FTP | ProviderProtocolType.FTPS | ProviderProtocolType.SFTP => {
            logger.info(s"Start FTP/FTPS/SFTP Actor")
            FtpActor.props(provider, destination)
          }
          case ProviderProtocolType.HTTP => {
            logger.info(s"Start HTTP Actor")
            HttpActor.props(provider, destination)
          }
        }
        val actorRef = system.actorOf(actorProp, actorName)
        registerTermination(system, actorRef, actorName)
      })
  }

  private def registerTermination(systemRef: ActorSystem, actorRef: ActorRef, actorName: String): ActorRef = {
    systemRef.actorOf(Props(classOf[Terminator], actorRef), s"terminator-$actorName")
    actorRef
  }

  private def getActorName(provider: ProviderConfig) : String  = {
    s"downloader-${provider.protocol}-${provider.id}".toLowerCase
  }
}
