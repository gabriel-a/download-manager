package com.download

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}

class Terminator(ref: ActorRef) extends Actor with ActorLogging {
  context watch ref
  def receive = {
    case Terminated(_) =>
      log.info("{} has terminated, shutting down", ref.path)
      context.system.terminate()
  }
}
