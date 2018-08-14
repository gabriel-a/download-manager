package com.download.service

import com.download.conf.{ProviderConfig, ProviderProtocolType}
import org.apache.commons.validator.routines.UrlValidator
import org.scalatest.FlatSpec

class HttpHelperTest extends FlatSpec {

  val urlValidator = new UrlValidator()

  "A Link" should "should be valid" in {
    val link = HttpHelper.isValid("https://www.google.com")
    assert(link === true)
  }

  "A Link" should "should be Invalid" in {
    val link = HttpHelper.isValid("www.google")
    assert(link === false)
  }

  "A Link" should "return a full valid url" in {
    val link = HttpHelper.getLink("https://www.google.com", "/test")
    assert(urlValidator.isValid(link) === true)
  }

  "A Link" should "return a full valid url if path does not have /" in {
    val link = HttpHelper.getLink("https://www.google.com", "test")
    assert(urlValidator.isValid(link) === true)
  }

  "If Path to file is valid url" should "return a full path to file url" in {
    val link = HttpHelper.getLink("https://www.google.com", "https://www.example.com/test")
    assert(urlValidator.isValid(link) === true)
    assert(link === "https://www.example.com/test")
  }

  "A Source url" should " return without username and password with one backslash" in {
    val provider = ProviderConfig("test", "https://www.google.com", 8443, ProviderProtocolType.HTTP, 0, 0, "/search", List("*"), "", "")
    val url = HttpHelper.getUrl(provider)
    assert(url === "https://www.google.com:8443/search")
  }

  "A Source url" should " return without username and password with one backslash and no port 443" in {
    val provider = ProviderConfig("test", "https://www.google.com", 443, ProviderProtocolType.HTTP, 0, 0, "/search", List("*"), "", "")
    val url = HttpHelper.getUrl(provider)
    assert(url === "https://www.google.com/search")
  }

  "A Source url" should " return without username and password with one backslash and no port 80" in {
    val provider = ProviderConfig("test", "http://www.google.com", 80, ProviderProtocolType.HTTP, 0, 0, "/search", List("*"), "", "")
    val url = HttpHelper.getUrl(provider)
    assert(url === "http://www.google.com/search")
  }

  "A Source url" should " return without username and password" in {
    val provider = ProviderConfig("test", "https://www.google.com", 8443, ProviderProtocolType.HTTP, 0, 0, "search", List("*"), "", "")
    val url = HttpHelper.getUrl(provider)
    assert(url === "https://www.google.com:8443/search")
  }

  "Url parsing " should " return a valid url" in {
    val url = "https://ftp.openprogrammer.info/pass-protected/"
    val allowed = List(".test")
    val username = "test"
    val password = "test123"

    val listOfFiles = HttpHelper.parse(url, allowed, username, password)
    assert(listOfFiles.size === 1)
  }

  "Url parsing " should " return an in-valid url (Empty list)" in {
    val url = "http://localhost/"
    val allowed = List("*")
    val username = ""
    val password = ""

    val listOfFiles = HttpHelper.parse(url, allowed, username, password)
    assert(listOfFiles.size === 0)
  }

}
