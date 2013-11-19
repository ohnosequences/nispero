package ohnosequences.nispero.bundles

import ohnosequences.statika._
import ohnosequences.typesets._
import ohnosequences.nispero.{Names}

import org.clapper.avsl.Logger

abstract class Resources(val configuration: Configuration, aws: AWS) extends Bundle(configuration :~: aws :~: ∅) {

  val resources = configuration.config.resources

  val config = configuration.config

  override def install[D <: AnyDistribution](distribution: D): InstallResults = {

    val config = configuration.config

    val logger = Logger(this.getClass)

    import aws._

    logger.info("installing resources")

    logger.info("creating error topic: " + resources.errorTopic)
    val errorTopic = sns.createTopic(resources.errorTopic)
    logger.info("creating error queue: " + resources.errorQueue)
    val errorQueue = sqs.createQueue(resources.errorQueue)
    logger.info("subscribing error queue to error topic")
    errorTopic.subscribeQueue(errorQueue)

    logger.info("creating input queue: " + resources.inputQueue)
    val inputQueue = sqs.createQueue(resources.inputQueue)

    logger.info("creating control queue: " + resources.controlQueue)
    sqs.createQueue(resources.controlQueue)

    logger.info("creating output topic: " + resources.outputTopic)
    val outputTopic = sns.createTopic(resources.outputTopic)
    logger.info("creating output queue: " + resources.outputQueue)
    val outputQueue = sqs.createQueue(resources.outputQueue)
    logger.info("subscribing output queue to output topic")
    outputTopic.subscribeQueue(outputQueue)

    logger.info("creating notification topic: " + config.notificationTopic)
    val topic = sns.createTopic(config.notificationTopic)

    if (!topic.isEmailSubscribed(config.email)) {
      logger.info("subscribing " + config.email + " to notification topic")
      topic.subscribeEmail(config.email)
      logger.info("please confirm subscription")
    }

    logger.info("creating bucket " + resources.bucket)
    aws.s3.createBucket(config.resources.bucket)

    logger.info("creating farm state table")
    dynamoDB.createTable(config.resources.workersStateTable, Names.Tables.WORKERS_STATE_HASH_KEY, Names.Tables.WORKERS_STATE_RANGE_KEY)

    success("resources bundle finished")

  }

}
