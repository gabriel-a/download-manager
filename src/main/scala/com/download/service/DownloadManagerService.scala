package com.download.service

import akka.actor.ActorRefFactory

trait DownloadManagerService {
  def download(implicit actorRefFactory: ActorRefFactory)
}
