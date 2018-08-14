package com.download.service

import java.io.File
import java.nio.file.{Files, Paths}

import com.download.conf.AppConf
import com.download.model.DestinationModel
import org.scalatest.FlatSpec

class IOHelperTest extends FlatSpec {

  "A directory" should "should be created and removed" in {
    val appConf = AppConf.apply()
    val newDirLoc = s"${appConf.destination.tmpDestination}/test-dir-1"

    val exists = Files.exists(Paths.get(newDirLoc))
    assert(exists === false)

    IOHelper.createDirIfNotExists(newDirLoc)
    assert(Files.exists(Paths.get(newDirLoc)) === true)

    IOHelper.removeDestination(newDirLoc)
    assert(Files.exists(Paths.get(newDirLoc)) === false)
  }

  "A directory" should "should be created and removed with files" in {
    val appConf = AppConf.apply()
    val resourcePath = new File("src/test/resources/test-files")
    assert(resourcePath.isDirectory, true)

    val newDestination = DestinationModel.getProviderDestinations("test-dir-2", appConf.destination)
    assert(Files.exists(Paths.get(newDestination.finalDestination)) === true)
    assert(Files.exists(Paths.get(newDestination.tmpDestination)) === true)

    //Prepare files in tmp
    Files.copy(Paths.get(resourcePath.getPath+ "/", "pdf-sample.pdf"), Paths.get(newDestination.tmpDestination+ "/", "pdf-sample.pdf"))
    Files.copy(Paths.get(resourcePath.getPath+ "/", "pdf-sample-1.pdf"), Paths.get(newDestination.tmpDestination+ "/", "pdf-sample-1.pdf"))


    IOHelper.fileProcessed(newDestination.tmpDestination,newDestination.finalDestination,"pdf-sample.pdf")
    assert(Files.exists(Paths.get(s"${newDestination.finalDestination}/pdf-sample.pdf")) === true)


    val filesOnDisk = IOHelper.listFilesOnDisk(newDestination.finalDestination, newDestination.tmpDestination)
    assert(filesOnDisk.size === 2)

    IOHelper.removeDestination(newDestination.tmpDestination)
    assert(Files.exists(Paths.get(newDestination.tmpDestination)) === false)

    IOHelper.removeDestination(newDestination.finalDestination)
    assert(Files.exists(Paths.get(newDestination.finalDestination)) === false)
  }

}
