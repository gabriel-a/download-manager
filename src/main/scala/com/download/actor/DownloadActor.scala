package com.download.actor

import akka.actor.{Actor, Props, Timers}
import com.download.Logging
import com.download.model.Reminder._
import com.download.service.DownloadManagerService

import scala.concurrent.duration._

object DownloadActor {
  def props(duration: Int, downloadManagerSettings: DownloadManagerService):
  Props = Props(new DownloadActor(duration, downloadManagerSettings))
}

class DownloadActor(duration: Int, downloadManagerSettings: DownloadManagerService)
  extends Actor with Timers with Logging {

  override def preStart(): Unit = {
    timers.startSingleTimer(FirstRunKey, Download, 1.millis)
    timers.startPeriodicTimer(PeriodicKey, Download, duration.second)
  }

  def receive = {
    case Download ⇒
      downloadManagerSettings.receive
  }

}
