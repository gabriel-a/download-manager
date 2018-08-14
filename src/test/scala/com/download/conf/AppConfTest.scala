package com.download.conf

import java.nio.file.{Files, Paths}

import org.scalatest.FlatSpec


class AppConfTest extends FlatSpec {

  "Config" should "Parse the properties and Apply the directories" in {
    val appConf = AppConf.apply()

    val finalDestination = "/tmp/test-location"
    val tmpDestination = "/tmp/test-location-tmp"

    assert(appConf.destination.finalDestination === finalDestination)
    assert(appConf.destination.tmpDestination === tmpDestination)

    val finalDestinationExists = Files.exists(Paths.get(finalDestination))
    val tmpDestinationExists = Files.exists(Paths.get(finalDestination))
    assert(finalDestinationExists === true)
    assert(tmpDestinationExists === true)

    val listProviders = List(
      ProviderConfig("HTTP-1","https://ftp.openprogrammer.info", 443, ProviderProtocolType.HTTP,30,3,"/pass-protected/",List(".pdf", ".png"), "test", "test123"),
      ProviderConfig("HTTP-2","https://ftp.openprogrammer.info", 443, ProviderProtocolType.HTTP,30,3,"/pub/",List(".pdf", ".json"), "", ""),
      ProviderConfig("SFTP-1","ftp.openprogrammer.info", 22, ProviderProtocolType.SFTP,60,3,"/ftp",List(".pdf",".txt",".gz"), "test", "test123"),
      ProviderConfig("FTP-2","ftp.openprogrammer.info", 21, ProviderProtocolType.FTP,60,5,"/pub",List(".pdf",".txt",".gz"), "", ""),
      ProviderConfig("UNKNOWN-0","localhost", 0, ProviderProtocolType.UNKNOWN,1,1,"/test",List("*"), "", "")
    )

    assert(appConf.providers.length === 5)
    assert(appConf.providers === listProviders)

  }

  "A Link extension" should "Be in the extension list" in {
    val listOfAllowedExtensions = List(".pdf",".txt",".xls")
    val url = "https://www.google.com/test.txt"
    assert(AppConf.isAllowedExt(url, listOfAllowedExtensions) === true)
  }

  "This link" should "Be Not allowed" in {
    val listOfAllowedExtensions = List(".json")
    val url = "https://www.google.com/test.txt"
    assert(AppConf.isAllowedExt(url, listOfAllowedExtensions) === false)
  }

  "Any link" should "Should be allowed" in {
    val listOfAllowedExtensions = List("*")
    assert(AppConf.isAllowedExt("https://www.google.com/test.txt", listOfAllowedExtensions) === true)
    assert(AppConf.isAllowedExt("https://www.google.com/test1.zip", listOfAllowedExtensions) === true)
    assert(AppConf.isAllowedExt("https://www.google.com/whatever/test2.exe", listOfAllowedExtensions) === true)
  }

}
