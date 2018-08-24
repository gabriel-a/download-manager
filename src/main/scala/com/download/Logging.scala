package com.download

import akka.event.jul.Logger

trait Logging {
  protected val logger = Logger(this.getClass.getSimpleName)
}
