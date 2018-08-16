package com.download.service

import java.io.File

import akka.event.jul.Logger
import com.download.dto.FileOrDirModel
import org.apache.commons.io.FileDeleteStrategy

object IOHelper {
  val logger = Logger(this.getClass.getSimpleName)
  def createDirIfNotExists(path : String)  =
  {
    val directory = new File(path)
    if (!directory.exists){
      logger.info(s"$directory does not exists, creating new one")
      directory.mkdir()
    }
  }

  def listFilesOnDisk(destination: String, tmpDestination: String): List[FileOrDirModel] ={
    new File(destination).listFiles.filter(_.isFile).map(file => FileOrDirModel(file.getName, file.getTotalSpace, file.lastModified())).toList ::: new File(tmpDestination).listFiles.map(file => FileOrDirModel(file.getName, file.getTotalSpace, file.lastModified())).toList
  }

  def fileProcessed(fromDestination: String, toDestination: String, fileName: String, bytes: Long = 0L) = {
    val newFile = new File(toDestination + "/", fileName)
    new File(fromDestination+ "/", fileName).renameTo(newFile)
    logger.info(s"Download finished, $bytes bytes processed!")
  }

  def removeDestination(link: String) = {
    FileDeleteStrategy.FORCE.delete(new File(link))
  }
}
