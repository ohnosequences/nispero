package ohnosequences.nispero.bundles

import ohnosequences.statika._
import java.io.File
import ohnosequences.awstools.s3.{ObjectAddress, S3}
import ohnosequences.awstools.ec2.EC2
import org.clapper.avsl.Logger
import ohnosequences.typesets._

abstract class LogUploader(resourcesBundle: Resources, aws: AWS) extends Bundle(resourcesBundle :~: aws :~: ∅) {

  override def install[D <: AnyDistribution](distribution: D): InstallResults = {
    try {
      val logFile = new File("/root/log.txt")

      val logger = Logger(this.getClass)

      val bucket = resourcesBundle.resources.bucket

      aws.ec2.getCurrentInstanceId match {
        case Some(id) => {
          val logUploader = new Thread(new Runnable {
            def run() {
              while(true) {
                try {
                  if(aws.s3.getBucket(bucket).isEmpty) {
                      logger.warn("bucket " + bucket + " doesn't exist")
                    } else {
                      aws.s3.putObject(ObjectAddress(bucket, id), logFile)  
                    }
                  
                  Thread.sleep(1000 * 30)
                } catch {
                  case t: Throwable => logger.error("log upload fails() " + t.toString);
                }
              }
            }
          }, "logUploader")
          logUploader.setDaemon(true)
          logUploader.start()
          success("logUploader started")
        }
        case None => failure("can't obtain instanceId")
      }
    }
  }
}
