package integration

import org.mockftpserver.fake.FakeFtpServer
import org.mockftpserver.fake.UserAccount
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem
import org.mockftpserver.fake.filesystem.FileEntry

class FtpServerMock(ftpPort: Int, sftpPort: Int, userName: String, password: String, directory: String) {
    var fakeFtpServer : FakeFtpServer = new FakeFtpServer()

    def start() : Unit = {

      fakeFtpServer.setServerControlPort(ftpPort)
      fakeFtpServer.addUserAccount(new UserAccount(userName, password, directory))
      fakeFtpServer.start()
      println(s"FTP server status ${fakeFtpServer.getSystemStatus()}")
    }

    def addFiles(): Unit ={
      val fileSystem = new UnixFakeFileSystem
      fileSystem.add(new FileEntry("/file1.txt", "Test 1"))
      fileSystem.add(new FileEntry("/file1.test", "Test 1"))
      fileSystem.add(new FileEntry("/file-pdf.pdf", "Test 2"))
      fakeFtpServer.setFileSystem(fileSystem)
    }

    def stop(): Unit ={
      fakeFtpServer.stop()
    }
}
