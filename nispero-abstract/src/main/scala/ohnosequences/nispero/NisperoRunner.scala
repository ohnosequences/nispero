package ohnosequences.nispero

import ohnosequences.statika.aws._
import ohnosequences.nispero.bundles.{NisperoDistributionAux}

import com.amazonaws.AmazonServiceException
import org.clapper.avsl.Logger
import ohnosequences.awstools.ec2.{EC2, Tag}
import ohnosequences.awstools.autoscaling.{AutoScalingGroup, AutoScaling}
import ohnosequences.awstools.sns.SNS
import java.io.File
import com.amazonaws.auth.{InstanceProfileCredentialsProvider, PropertiesCredentials, AWSCredentialsProvider}
import com.amazonaws.internal.StaticCredentialsProvider
import com.amazonaws.AmazonClientException
import ohnosequences.awstools.s3.S3
import ohnosequences.nispero.utils.Utils


abstract class NisperoRunner(nisperoDistribution: NisperoDistributionAux) {

  val config = nisperoDistribution.manager.resourcesBundle.config

  def compilerChecks(): Unit

  def main(args: Array[String]) {
    args.toList match {
      case "undeploy" :: "-m" :: msg :: tail => undeploy(msg, tail)

      case "undeploy" :: tail => undeploy("manual undeploy", tail)

      case list => runNispero(args.toList, nisperoDistribution)
    }
  }



  def retrieveCredentialsProviderFromFile(file: File): (AWSCredentialsProvider, Option[String]) = {
    logger.info("retrieving credentials from: " + file.getPath)
    (new StaticCredentialsProvider(new PropertiesCredentials(file)), Some(file.getPath))
  }

  def retrieveCredentialsProvider(args: List[String]): (AWSCredentialsProvider, Option[String]) = {
    args.reverse match {
      case Nil => {
        logger.info("trying to retrieve IAM credentials")
        val iamProvider = new InstanceProfileCredentialsProvider()
        try {
          val ec2 = EC2.create(iamProvider)
          ec2.createSecurityGroup("nispero")
          (iamProvider, None)
        } catch {
          case e: AmazonClientException => {
            val defaultLocation = System.getProperty("user.home")
            val file = new File(defaultLocation, "credentials")
            retrieveCredentialsProviderFromFile(file)
          }
        }
      }
      case head :: tail => retrieveCredentialsProviderFromFile(new File(head))
    }
  }

  val logger = Logger(this.getClass)



  //deploy nispero
  def runNispero(args: List[String], nisperoDistribution: NisperoDistributionAux) {

    val credentialsProvider = retrieveCredentialsProvider(args)._1
    val ec2 = EC2.create(credentialsProvider)
    val as = AutoScaling.create(credentialsProvider, ec2)
    val sns = SNS.create(credentialsProvider)
    val s3 = S3.create(credentialsProvider)



    if(!checkConfig(config, ec2, s3)) {
      return
    }

    s3.createBucket(config.resources.bucket)

    logger.info("creating notification topic: " + config.notificationTopic)

    val topic = sns.createTopic(config.notificationTopic)

    if (!topic.isEmailSubscribed(config.email)) {
      logger.info("subscribing " + config.email + " to notification topic")
      topic.subscribeEmail(config.email)
      logger.info("please confirm subscription")
    }

    logger.info("generating userScript for manager")

    //val managerUserData = nisperoDistribution.ami.userScript(nisperoDistribution.metadata, nisperoDistribution.fullName, nisperoDistribution.manager.fullName)
    val managerUserData = nisperoDistribution.userScript(nisperoDistribution.manager)

    logger.info("running manager auto scaling group")
    val managerGroup = as.fixAutoScalingGroupUserData(config.managerConfig.groups._1, managerUserData)
    as.createAutoScalingGroup(managerGroup)
    logger.info("creating tags")
    Utils.tagAutoScalingGroup(as, managerGroup.name, "manager")

    logger.info("generating userScript for console")

    //val consoleUserData = nisperoDistribution.ami.userScript(nisperoDistribution.metadata, nisperoDistribution.fullName, nisperoDistribution.console.fullName)
    val consoleUserData = nisperoDistribution.userScript(nisperoDistribution.console)

    logger.info("running manager auto scaling group")
    val consoleGroup = as.fixAutoScalingGroupUserData(config.managerConfig.groups._2, consoleUserData)
    as.createAutoScalingGroup(consoleGroup)
    logger.info("creating tags")
    Utils.tagAutoScalingGroup(as, consoleGroup.name, "console")
  }

  def checkConfig(c: Config, ec2: EC2, s3: S3): Boolean = {

      val (min, d, max) = (
        c.resources.workersGroup.minSize,
        c.resources.workersGroup.desiredCapacity,
        c.resources.workersGroup.maxSize
      )

      if (d < min || d > max) {
        logger.error("desired capacity should be in interval [minSize, maxSize]")
        return false
      }

      val keyPair = c.resources.workersGroup.launchingConfiguration.instanceSpecs.keyName
      if(!ec2.isKeyPairExists(keyPair)) {
        logger.error("key pair: " + keyPair + " doesn't exists")
        return false
      }

    try {
      s3.getObjectStream(config.jarAddress) match {
        case null => logger.error("artifact isn't uploaded"); false
        case _ => true
      }
    } catch {
      case s3e: AmazonServiceException if s3e.getStatusCode==301 => true
      case t: Throwable  => {
        logger.error("artifact isn't uploaded: " + config.jarAddress + " " + t)
        false
      }
    }
  }

  def undeploy(message: String, args: List[String]) {
    val awsClients = AWSClients.fromProvider(retrieveCredentialsProvider(args)._1)
    Undeployer.undeploy(awsClients, config, message)
  }

}
