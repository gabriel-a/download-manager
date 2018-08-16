package integration

import java.io.File

import com.download.Main
import com.download.conf.{AppConf, ProviderProtocolType}
import com.download.model.DestinationModel
import com.download.service.IOHelper
import org.scalatest._
import org.awaitility.Awaitility._
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.TimeUnit.MILLISECONDS

private class MainApplicationTest extends FunSpec with PrivateMethodTester {
  describe("It must start the application") {
    it("It must create directories & download files") {
      println("Came to create the Integration test")
      Main.main(Array(""))
      var totalTest = 0
      val app = AppConf.apply()
      await.atMost(500, MILLISECONDS).until(() => new File(app.destination.finalDestination).exists())
      app.providers
        .filter(p => ProviderProtocolType.isSupported(p.protocol))
        .foreach(provider => {
          provider.id match {
            case "FTP-1" => {
              val localFtpLocation = DestinationModel.getProviderDestinations (provider.id, app.destination)
              await.atMost(5, SECONDS).until(() => new File (localFtpLocation.finalDestination).listFiles (_.isFile).size == 2)
              assert (new File (localFtpLocation.tmpDestination).listFiles (_.isFile).size == 0)
              assert (new File (localFtpLocation.finalDestination).listFiles (_.isFile).size == 2)
              totalTest += 1
              IOHelper.removeDestination(localFtpLocation.tmpDestination)
              IOHelper.removeDestination(localFtpLocation.finalDestination)
            }
            case "SFTP-1" => {
              val localSftpLocation = DestinationModel.getProviderDestinations (provider.id, app.destination)
              await.atMost(5, SECONDS).until(() => new File (localSftpLocation.finalDestination).listFiles (_.isFile).size == 3)
              assert (new File (localSftpLocation.tmpDestination).listFiles (_.isFile).size == 0)
              assert (new File (localSftpLocation.finalDestination).listFiles (_.isFile).size == 3)
              totalTest += 1
              IOHelper.removeDestination(localSftpLocation.tmpDestination)
              IOHelper.removeDestination(localSftpLocation.finalDestination)
            }
            case "HTTP-1" => {
              val localHttpLocation = DestinationModel.getProviderDestinations (provider.id, app.destination)
              await.atMost(5, SECONDS).until(() => new File (localHttpLocation.finalDestination).listFiles (_.isFile).size == 2)
              assert (new File (localHttpLocation.tmpDestination).listFiles (_.isFile).size == 0)
              assert (new File (localHttpLocation.finalDestination).listFiles (_.isFile).size == 2)
              totalTest += 1
              IOHelper.removeDestination(localHttpLocation.tmpDestination)
              IOHelper.removeDestination(localHttpLocation.finalDestination)
            }
          }
        })
      assert((totalTest == 3) === true)
      Main.killAllActors()
      //Cleanup
      IOHelper.removeDestination(app.destination.tmpDestination)
      IOHelper.removeDestination(app.destination.finalDestination)
    }
  }
}

class ApplicationTests extends Suites(new MainApplicationTest) with BeforeAndAfterAll {

  object SftpObject {
    private val _instance = new SFtpServerMock(9022)
    def instance() = _instance
  }

  object FtpObject {
    private val _instance = new FtpServerMock(9999, "test","test123", "/")
    def instance() = _instance
  }

  object HttpObject {
    private val _instance = new HttpServerMock(8083)
    def instance() = _instance
  }

  override def beforeAll() {
    println("Before all")
    FtpObject.instance().start()
    SftpObject.instance().start()
    HttpObject.instance().start()
  }

  override def afterAll() {
    println("After all")
    FtpObject.instance().stop()
    SftpObject.instance().stop()
    HttpObject.instance().stop()
    super.afterAll
  }
}
