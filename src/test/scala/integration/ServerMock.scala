package integration

import java.io.{ByteArrayInputStream}
import java.util

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.jcraft.jsch.{ChannelSftp, JSch}
import org.mockftpserver.fake.filesystem.{FileEntry, UnixFakeFileSystem}
import org.mockftpserver.fake.{FakeFtpServer, UserAccount}
import software.sham.sftp.MockSftpServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._

trait ServerMock{
  def start()
  def addFiles()
  def stop()
}

class FtpServerMock(ftpPort: Int, ftpUserName: String, ftpPassword: String, directory: String) extends ServerMock{
    private val fakeFtpServer : FakeFtpServer = new FakeFtpServer()

    def start() : Unit = {
      fakeFtpServer.setServerControlPort(ftpPort)
      fakeFtpServer.addUserAccount(new UserAccount(ftpUserName, ftpPassword, directory))
      fakeFtpServer.start()
      println(s"FTP server status ${fakeFtpServer.getSystemStatus()}")
      addFiles()
    }

    def addFiles(): Unit ={
        val fileSystem = new UnixFakeFileSystem
        fileSystem.add(new FileEntry("/file1.txt", "Test 1"))
        fileSystem.add(new FileEntry("/file1.test", "Test 1"))
        fileSystem.add(new FileEntry("/file-pdf.pdf", "Test 2"))
        fakeFtpServer.setFileSystem(fileSystem)
        println(s"Ftp files added")
    }

    def stop(): Unit ={
      println("Stopping servers")
      fakeFtpServer.stop()
    }
}

class SFtpServerMock(sftpPort: Int) extends ServerMock{
  private val sshd : MockSftpServer = new MockSftpServer(sftpPort)

  def start() : Unit = {
    addFiles()
  }

  def addFiles(): Unit ={
    val jsch = new JSch
    val config = new util.Hashtable[String, String]
    config.put("StrictHostKeyChecking", "no")
    JSch.setConfig(config)
    val session = jsch.getSession("tester", "localhost", sftpPort)
    session.setPassword("testing")
    session.connect
    val channel = session.openChannel("sftp")
    channel.connect
    val sftpChannel = channel.asInstanceOf[ChannelSftp]
    sftpChannel.put(new ByteArrayInputStream("some file contents 1".getBytes), "file-sftp.txt")
    sftpChannel.put(new ByteArrayInputStream("some file contents 2".getBytes), "file-1-sftp.txt")
    sftpChannel.put(new ByteArrayInputStream("some file contents 3".getBytes), "file-2-sftp.pdf")
    sftpChannel.put(new ByteArrayInputStream("some file contents 4".getBytes), "file-2-sftp.notsupported")

    if (sftpChannel.isConnected) {
      sftpChannel.exit
      println("Disconnected channel")
    }
    if (session.isConnected) {
      session.disconnect
      println("Disconnected session")
    }
  }

  def stop(): Unit ={
    sshd.stop()
  }
}

class HttpServerMock(httpPort: Int) extends ServerMock {
  var wireMockServer: WireMockServer = new WireMockServer(
    wireMockConfig()
      .withRootDirectory("src/test/resources/test-files")
      .port(httpPort)
  )

  override def start(): Unit = {
    wireMockServer.start()
    WireMock.configureFor("localhost", httpPort)
    addFiles()
  }

  override def addFiles(): Unit = {
    wireMockServer.stubFor(get(urlEqualTo("/testing"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "text/plain")
        .withBodyFile("my-resources.html")))

    wireMockServer.stubFor(get(urlEqualTo("/testing/myfile.xml"))
      .willReturn(aResponse()
        .withBodyFile("myfile.xml")))

    wireMockServer.stubFor(get(urlEqualTo("/testing/myfile.json"))
      .willReturn(aResponse()
        .withBody("Literal text to put in the body")))
  }

  override def stop(): Unit = {
    wireMockServer.stop()
  }
}