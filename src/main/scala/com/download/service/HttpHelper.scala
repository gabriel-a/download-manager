package com.download.service

import java.net.URL

import akka.event.jul.Logger
import com.download.Logging
import com.download.conf.{AppConf, ProviderConfig}
import org.apache.commons.validator.routines.UrlValidator
import org.jsoup.Jsoup

import scala.collection.JavaConverters._


object HttpHelper extends Logging {

  val urlValidator = new UrlValidator()

  def isValid(url: String): Boolean = {
    if (url.contains("localhost")) true
    else urlValidator.isValid(url)
  }

  def parse(url: String, allowedExtensions: Seq[String], username: String, password: String): Seq[URL] = {
    if (isValid(url)) {
      import org.apache.commons.net.util.Base64
      val authString = username + ":" + password
      val encodedString = new String(Base64.encodeBase64(authString.getBytes))
      val response = Jsoup.connect(url)
        .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
        .header("Authorization", "Basic " + encodedString)
        .followRedirects(true)
        .execute()

      response.parse().getElementsByTag("a")
        .asScala
        // .view makes it a lazy stream as Java 8 streams are, so it will
        // not create a collection on every new .map or .filter call.
        .view
        .map(e => e.attr("href"))
        .filter(s => isValid(getLink(url, s)) && AppConf.isAllowedExt(getLink(url, s), allowedExtensions))
        .map(link => new URL(getLink(url, link)))
        .distinct
    } else {
      logger.warning(s"URL $url is not valid! Please fix it, returning an empty list!")
      Seq[URL]()
    }
  }

  def getLink(url: String, pathToFile: String): String = {
    if (isValid(pathToFile)) {
      pathToFile
    } else {
      val cleanUrl = if (url.endsWith("/")) url else url + "/"
      val cleanPath = if (pathToFile.startsWith("/")) pathToFile.replace("/", "") else pathToFile
      cleanUrl + cleanPath
    }
  }

  def getUrl(provider : ProviderConfig) : String = {
    s"${provider.host}${getPort(provider.port)}/${provider.basePath.replaceFirst("/","")}"
  }

  // just to make it a little more logical
  private def getPort(port: Int): String = {
    port match {
      case 80 | 443 => ""
      case _ => s":$port"
    }
  }

}
