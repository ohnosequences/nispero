package ohnosequences.nispero.bundles

import ohnosequences.statika._
import ohnosequences.awstools.sqs.{Message, Queue}
import ohnosequences.nispero.{Task, Undeployer}
import ohnosequences.nispero.manager._
import org.clapper.avsl.Logger
import net.liftweb.json.MappingException

import ohnosequences.awstools.sqs.Message
import ohnosequences.awstools.sqs.Queue
import ohnosequences.nispero.utils.JSON
import ohnosequences.typesets._


abstract class ControlQueueHandler(resourcesBundle: Resources, aws: AWS) extends Bundle(resourcesBundle :~: aws :~: ∅) {

  val logger = Logger(this.getClass)

  import aws.sqs

  def waitForTask(queue: Queue): Message = {

    logger.info("waiting for command")

    val MESSAGE_TIMEOUT = 5000

    queue.receiveMessage match {
      case Some(message) => message
      case None => {
        Thread.sleep(MESSAGE_TIMEOUT)
        waitForTask(queue)
      }
    }
  }

  def run() {
    val config = resourcesBundle.config
    val controlQueue = sqs.getQueueByName(resourcesBundle.resources.controlQueue).get
    val inputQueue =  sqs.getQueueByName(resourcesBundle.resources.inputQueue).get

    while(true) {
      try {
        val message = waitForTask(controlQueue)

        val command: RawCommand = JSON.parse[RawCommand](message.body)
        logger.info("received command: " + command)
        command match {
          case RawCommand("UnDeploy", reason: String) => {
            Undeployer.undeploy(aws.awsClients, config, reason)
          }
          case RawCommand("AddTasks", tasks: String) => {
            val parsedTasks = JSON.parse[List[Task]](tasks)
            parsedTasks.foreach { task =>
              inputQueue.sendMessage(JSON.toJson(task))
            }
          }
          case RawCommand("ChangeCapacity", n: String) => {
            aws.as.setDesiredCapacity(config.resources.workersGroup, n.toInt)
          }
        }
        controlQueue.deleteMessage(message)

      } catch {
        case t: Throwable => {
          logger.error("error during handling command from control queue")
          t.printStackTrace()
        }
      }
    }
  }

  override def install[D <: AnyDistribution](distribution: D): InstallResults = {
    success("ControlQueueHandler installed")
  }

}
