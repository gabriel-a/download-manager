package com.download.service

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.download.conf.{AppConf, ProviderConfig, ProviderProtocolType}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import org.scalatest.mockito.MockitoSugar

class MainServiceTest extends TestKit(ActorSystem(AppConf.DownloadManagerActorSystemName)) with MockitoSugar
  with Matchers
  with FlatSpecLike
  with BeforeAndAfterAll {

  override def afterAll {
    println("Stopping all actors")
    TestKit.shutdownActorSystem(system)
  }

  "All actors" should "be created with configuration and Unknown should not be added as actor" in {
    val appConf = AppConf.apply()
    val providerNames = getActorNames(appConf.providers.filter(p => ProviderProtocolType.isSupported(p.protocol)))
    val actors = GenerateActors.apply()
    assert(actors.size === 3)
    val actorPaths = actors.map(actor => ""+actor.path)
    assert(providerNames == actorPaths)
  }

  private def getActorNames(providers: Seq[ProviderConfig]): Seq[String] = {
    providers
      .map(provider => s"akka://${AppConf.DownloadManagerActorSystemName}/user/downloader-${provider.protocol.toString.toLowerCase}-${provider.id}".toLowerCase)
  }

}
