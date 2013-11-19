package ohnosequences.nispero

import org.clapper.avsl.Logger


object Undeployer {


  def undeploy(awsClients: AWSClients, config: Config, reason: String) {
    import awsClients._

    val logger = Logger(this.getClass())
    logger.info("termination due to " + reason)
    logger.info("send notification")
    try {
      val message = "termination due to " + reason
      val subject = "Nispero " + config.resources.id + " terminated"
      val notificationTopic = sns.createTopic(config.notificationTopic)
      notificationTopic.publish(message, subject)
    } catch {
      case t: Throwable => logger.error("error during sending notification" + t.getMessage)
    }


    logger.info("deleting workers group")
    awsClients.autoScaling.deleteAutoScalingGroup(config.resources.workersGroup)


    try {
      logger.info("deleting bucket " + config.resources.bucket)
      awsClients.s3.deleteBucket(config.resources.bucket)
    } catch {
      case t: Throwable => logger.error("error during deleting bucket: " + t.getMessage)
    }

    try {
      sqs.getQueueByName(config.resources.errorQueue).foreach(_.delete())
    } catch {
      case t: Throwable => logger.error("error during deleting error queue " + t.getMessage)
    }

    try {
      sns.createTopic(config.resources.errorTopic).delete()
    } catch {
      case t: Throwable => logger.error("error during deleting error topic " + t.getMessage)
    }

    try {
      sqs.getQueueByName(config.resources.outputQueue).foreach(_.delete())
    } catch {
      case t: Throwable => logger.error("error during deleting output queue " + t.getMessage)
    }

    try {
      sns.createTopic(config.resources.outputTopic).delete()
    } catch {
      case t: Throwable => logger.error("error during deleting output topic " + t.getMessage)
    }

    try {
      sqs.getQueueByName(config.resources.inputQueue).foreach(_.delete())
    } catch {
      case t: Throwable => logger.error("error during deleting input queue " + t.getMessage)
    }

    try {
      sqs.getQueueByName(config.resources.controlQueue).foreach(_.delete())
    } catch {
      case t: Throwable => logger.error("error during deleting control queue " + t.getMessage)
    }



    try {
      dynamoDB.deleteTable(config.resources.workersStateTable)
    } catch {
      case t: Throwable => logger.error("error during deleting workers state table: " + t.getMessage)
    }

    try {
      logger.info("delete console group")
      awsClients.autoScaling.deleteAutoScalingGroup(config.managerConfig.groups._2)
    } catch {
      case t: Throwable => logger.info("error during deleting console group: " + t.getMessage)
    }

    try {
      logger.info("delete manager group")
      awsClients.autoScaling.deleteAutoScalingGroup(config.managerConfig.groups._1)
    } catch {
      case t: Throwable => logger.info("error during deleting console group: " + t.getMessage)
    }

    logger.info("undeployed")
  }

}
