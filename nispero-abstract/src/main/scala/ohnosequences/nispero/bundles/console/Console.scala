package ohnosequences.nispero.bundles.console

import ohnosequences.statika._
import ohnosequences.nispero.bundles.{AWS, LogUploader, Resources}
import java.io.File
import org.apache.commons.io.FileUtils
import org.clapper.avsl.Logger
import ohnosequences.nispero.utils.Utils
import ohnosequences.typesets._

abstract class Console(resourcesBundle: Resources, logUploader: LogUploader, farmStateLogger: FarmStateLogger, aws: AWS)
  extends Bundle(resourcesBundle :~: logUploader :~: farmStateLogger :~: aws :~: ∅) {

  val logger = Logger(this.getClass)

  override def install[D <: AnyDistribution](distribution: D): InstallResults = {

    val awsClients = aws.awsClients

    val config = resourcesBundle.config

    logger.info("deploying console data")

    val resources = List(
      "/console/bootstrap.min.js",
      "/console/bootstrap.css",
      "/console/main.css",
      "/console/glyphicons-halflings-white.png",
      "/console/glyphicons-halflings.png",
      "/console/jquery-1.8.3.min.js",
      "/console/d3.v3.js"
    )


    val tmpDir = Utils.createTempDir()
    resources.foreach { resource =>
      logger.info("write " + resource + " to file")
      val f = new File(tmpDir, resource.replace("/console/", ""))
      val i = getClass.getResourceAsStream(resource)
      FileUtils.copyInputStreamToFile(i, f)
    }

    logger.info("preparing SSL certificate ")
    val keytoolConf = new File("keytoolConf")
    val i = getClass.getResourceAsStream("/keytoolConf")
    FileUtils.copyInputStreamToFile(i, keytoolConf)

    import scala.sys.process._

    "cat keytoolConf" #| "keytool -keystore keystore -alias jetty -genkey -keyalg RSA -storepass password" !

    System.setProperty("jetty.ssl.keyStore", "keystore")
    System.setProperty("jetty.ssl.keyStorePassword", "password")

    val bucket = aws.s3.createBucket(config.resources.bucket)
    tmpDir.listFiles().foreach(bucket.putObject(_, public = true))

    val backEnd = new BackEnd(
      awsClients = awsClients,
      config = config,
      farmStateLogger
    )

    val frontEnd = new FrontEnd(
      backend = backEnd,
      password = config.managerConfig.password,
      port = config.managerConfig.port,
      resourcesBucket = config.resources.bucket
    )

    frontEnd.run()


    success("console installed")
  }

}
