package ohnosequences.nispero

import ohnosequences.awstools.s3.ObjectAddress
import ohnosequences.awstools.autoscaling.AutoScalingGroup

/**
 * configuration for nispero
 * @param managerConfig configuration for Manager and Console
 * @param email e-mail address for notifications
 * @param terminationConditions termination conditions
 * @param resources configuration of names for resources: queues, topics, buckets
 * @param workersDir working directory dor worker
 * @param tasksProvider task provider (@see https://github.com/ohnosequences/nispero/blob/master/doc/tasks-providers.md)
 * @param taskProcessTimeout maximum time for processing task
 */
case class Config(
                   managerConfig: ManagerConfig,
                   email: String,
                   terminationConditions: TerminationConditions,
                   resources: Resources,
                   workersDir: String,
                   tasksProvider: TasksProvider = EmptyTasks,
                   jarAddress: ObjectAddress,
                   taskProcessTimeout: Int = 60 * 60 * 10 // 10 hours
                   ) {

  def initialTasks = ObjectAddress(resources.bucket, "initialTasks")

  def tasksUploaded = ObjectAddress(resources.bucket, "tasksUploaded")

  def notificationTopic: String = {
    "nisperoNotificationTopic" + email.replace("@", "").replace("-", "").replace(".", "")
  }

}


/**
 * configuration of resources
 * @param id unique name of nispero instance
 * @param inputQueue name of queue with tasks
 * @param controlQueue name of queue for interaction with Manager
 * @param outputTopic name of topic for tasks result notifications
 * @param outputQueue name of queue with tasks results notifications (will be subscribed to outputTopic)
 * @param errorTopic name of topic for errors
 * @param errorQueue name of queue with errors (will be subscribed to errorTopic)
 * @param bucket name of bucket for logs and console static files
 * @param workersStateTable name of DynamoDB table with workers statistics
 * @param workersGroup configuration of worker group
 */
case class Resources(
                      id: String
                      )(

                      val inputQueue: String = "nisperoInputQueue" + id,

                      val controlQueue: String = "nisperoControlQueue" + id,

                      val outputQueue: String = "nisperoOutputQueue" + id,
                      val outputTopic: String = "nisperoOutputTopic" + id,

                      val errorTopic: String = "nisperoErrorQueue" + id,
                      val errorQueue: String = "nisperoErrorTopic" + id,

                      val bucket: String = "nisperobucket" + id.replace("_", "-"),

                      val workersStateTable: String = "nisperoworkersStateTable" + id,
                      val workersGroup: AutoScalingGroup
                      )

/**
 * configuration for Manager and Console
 * @param port http port for Console
 * @param groups configuration of auto scaling for Manager and Console
 * @param password password for Console
 */
case class ManagerConfig(
                          port: Int = 443,
                          groups: (AutoScalingGroup, AutoScalingGroup),
                          password: String
                          )

/**
 * configuration of termination conditions
 * @param terminateAfterInitialTasks if true nispero will terminate after solving all initial tasks
 * @param errorsThreshold if true nispero will terminate after errorQueue will contain more unique messages then threshold
 * @param timeout if true nispero will terminate after this timeout reached. Time units are sceonds.
 */
case class TerminationConditions(
                                  terminateAfterInitialTasks: Boolean,
                                  errorsThreshold: Option[Int] = None,
                                  timeout: Option[Int] = None
                                  )
