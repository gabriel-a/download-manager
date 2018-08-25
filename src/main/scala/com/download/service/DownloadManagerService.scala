package com.download.service

import akka.actor.ActorRefFactory

trait DownloadManagerService {
  def receive(implicit actorRefFactory: ActorRefFactory)
}
