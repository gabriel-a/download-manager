package integration

import java.io.File

import com.download.Main
import com.download.conf.{AppConf, ProviderProtocolType}
import com.download.model.DestinationModel
import com.download.service.IOHelper
import org.scalatest._

class MainApplicationTest extends FunSpec with MustMatchers {
  describe("It must start the application") {
    it("It must create directories & download files") {
      Main.main(Array(""))
      Thread.sleep(5000)
      var totalTest = 0
      val app = AppConf.apply()
      app.providers
        .filter(p => ProviderProtocolType.isSupported(p.protocol))
        .foreach(provider => {
          provider.id match {
            case "FTP-1" => {
              val currentDestination = DestinationModel.getProviderDestinations (provider.id, app.destination)
              assert (new File (currentDestination.tmpDestination).listFiles (_.isFile).size == 0)
              assert (new File (currentDestination.finalDestination).listFiles (_.isFile).size == 2)
              totalTest += 1
              IOHelper.removeDestination(currentDestination.tmpDestination)
              IOHelper.removeDestination(currentDestination.finalDestination)
            }
          }
        })
      assert((totalTest > 0) === true)
      Main.killAllActors()
    }
  }
}

class ApplicationTests extends Suites(new MainApplicationTest) with BeforeAndAfterAll {
  val ftpServerMock = new FtpServerMock(9999, 8006,"test","test123", "/")

  override def beforeAll() {
    println("Before all")
    ftpServerMock.start()
    ftpServerMock.addFiles()
  }

  // Delete the temp file
  override def afterAll() {
    println("After all")
    ftpServerMock.stop()
  }
}
