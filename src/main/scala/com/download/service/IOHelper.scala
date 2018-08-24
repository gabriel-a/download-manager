package com.download.service

import java.io.File

import com.download.Logging
import com.download.dto.FileOrDirModel
import org.apache.commons.io.FileDeleteStrategy

object IOHelper extends Logging {

  def createDirIfNotExists(path: String) = {
    val directory = new File(path)
    if (!directory.exists) {
      logger.info(s"$directory does not exists, creating new one")
      directory.mkdir()
    }
  }

  def listFilesOnDisk(destination: String, tmpDestination: String): Seq[FileOrDirModel] = {
    new File(destination).listFiles
      .filter(_.isFile)
      .map(file => FileOrDirModel(file.getName, file.getTotalSpace, file.lastModified())
      ) ++
      new File(tmpDestination).listFiles
        .map(file => FileOrDirModel(file.getName, file.getTotalSpace, file.lastModified()))
  }

  def fileProcessed(fromDestination: String, toDestination: String, fileName: String, bytes: Long = 0L) = {
    // I think you don't need a '/' for this constructor
    val newFile = new File(toDestination, fileName)
    new File(fromDestination, fileName).renameTo(newFile)
    logger.info(s"Download finished, $bytes bytes processed!")
  }

  def removeDestination(link: String) = {
    FileDeleteStrategy.FORCE.delete(new File(link))
  }
}
