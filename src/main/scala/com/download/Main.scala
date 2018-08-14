package com.download

import akka.event.jul.Logger
import com.download.service.GenerateActors

object Main {
  def main(args: Array[String]): Unit = {
    val logger = Logger(this.getClass.getSimpleName)
    val actors = GenerateActors.apply()
    logger.info(s"${actors.size} Actor(s) Created and started working!")
  }
}
