package com.download.service

import akka.actor.ActorRefFactory

trait DownloadManagerSettings {
  def receive(implicit actorRefFactory: ActorRefFactory)
}
