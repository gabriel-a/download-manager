package com.download.service

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.download.conf.{AppConf, ProviderConfig, ProviderProtocolType}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

class GenerateActorsTest extends TestKit(ActorSystem("MySpec"))
  with Matchers
  with FlatSpecLike
  with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "All actors" should "be created with configuration" in {
    val appConf = AppConf.apply()
    val providerNames = getActorNames(appConf.providers.filter(p => ProviderProtocolType.isSupported(p.protocol)))
    val actors = GenerateActors.apply()

    //Unknown should not be added as actor
    assert(actors.size === 4)

    val actorPaths = actors.map(actor => ""+actor.path)
    assert(providerNames == actorPaths)
  }

  private def getActorNames(providers: List[ProviderConfig]): List[String] = {
    providers
      .map(provider => s"akka://system-downloader-${provider.protocol.toString.toLowerCase}-${provider.id}/user/downloader-${provider.protocol.toString.toLowerCase}-${provider.id}".toLowerCase)

  }

}
