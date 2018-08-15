package com.download

import akka.actor.{ActorSystem, Props}
import com.download.service.MainService
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.ExecutionContext

class MainTest extends FlatSpec with MockitoSugar {
  "Main App" should "Create actors" in {
    val mockService = mock[MainService]
    val system = ActorSystem(s"system-test-actor-1")
    val actorRefList = List(system.actorOf(Props.empty), system.actorOf(Props.empty))
    when(mockService.apply()).thenReturn(actorRefList)

    Main.setMainService(mockService)
    Main.main(Array(""))

    verify(mockService, times(1)).apply()
    actorRefList.foreach(actor => {
      system.stop(actor)
    })
    implicit val ec: ExecutionContext = ActorSystem().dispatcher
    system.terminate().onComplete(_ => println("Terminated the actors"))
  }
}
