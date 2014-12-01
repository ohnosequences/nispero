package ohnosequences.nispero.worker

import ohnosequences.nispero._
import ohnosequences.awstools.sqs.Message
import ohnosequences.awstools.sns.Topic
import ohnosequences.awstools.sqs.Queue
import org.clapper.avsl.Logger
import java.io.File
import ohnosequences.nispero.utils.{JSON, Utils}
import scala.concurrent.Future


class InstructionsExecutor(config: Config, instructions: Instructions, val awsClients: AWSClients) {

  val MESSAGE_TIMEOUT = 5000

  import awsClients._

  val logger = Logger(this.getClass)

  val instance = ec2.getCurrentInstance

  @volatile var stopped = false

  def waitForTask(queue: Queue): Message = {

    var message: Option[Message] = queue.receiveMessage

    while(message.isEmpty) {
      logger.info("InstructionsExecutor wait for task")
      instance.foreach(_.createTag(InstanceTags.IDLE))
      Thread.sleep(MESSAGE_TIMEOUT)
      message = queue.receiveMessage
    }

    message.get
  }

  def waitForResult(futureResult: Future[TaskResult], message: Message): (TaskResult, Int) = {
    val startTime = System.currentTimeMillis()
    val step = 500

    def timeSpent(): Int = {
      val currentTime = System.currentTimeMillis()
      ((currentTime - startTime) / 1000).toInt
    }

    var stopWaiting = false

    var it = 1

    var taskResult: TaskResult = Failure("internal error during waiting for task result")

    while(!stopWaiting) {
      it += 1;

      if(timeSpent() > config.taskProcessTimeout) {
        stopWaiting = true
        taskResult = Failure("timeout: " + timeSpent + " > visibilityTimeoutLimit")
        terminate()
      } else {
        futureResult.value match {
          case None => {
            logger.info("solving task: " + Utils.printInterval(timeSpent()))
            if(it % 60==0) {
              message.changeVisibilityTimeout((step / 1000) * 2)
            }
            
            Thread.sleep(step)
          }
          case Some(scala.util.Success(r)) => stopWaiting = true; taskResult = r
          case Some(scala.util.Failure(t)) => stopWaiting = true; taskResult = Failure("future error: " + t.getMessage)
        }
      }
    }
    (taskResult, timeSpent())
  }

  def terminate() {
    stopped = true
    instance.foreach(_.createTag(InstanceTags.FINISHING))
    logger.info("terminating")

    instance.foreach(_.terminate())

  }


  def run() {

    logger.info("InstructionsExecutor started at " + instance.map(_.getInstanceId))

    val inputQueue = sqs.getQueueByName(config.resources.inputQueue).get
    val outputTopic = sns.createTopic(config.resources.outputTopic)
    val errorTopic = sns.createTopic(config.resources.errorTopic)

    while(!stopped) {
      var taskId = ""
      var lastTimeSpent = 0
      try {
        val message = waitForTask(inputQueue)

        instance.foreach(_.createTag(InstanceTags.PROCESSING))
        logger.info("InstructionsExecutor: received message " + message)
        val task = JSON.parse[Task](message.body)
        taskId = task.id

        logger.info("InstructionsExecutor processing message")

        instructions.execute(s3, task, new File(config.workersDir))  
        import scala.concurrent.ExecutionContext.Implicits._
        val futureResult = scala.concurrent.future {
          instructions.execute(s3, task, new File(config.workersDir))  
        }

        val (taskResult, timeSpent) = waitForResult(futureResult, message)
        lastTimeSpent = timeSpent

        logger.info("task result: " + taskResult)

        val taskResultDescription = TaskResultDescription(
          id = task.id,
          message = taskResult.message,
          instanceId = instance.map(_.getInstanceId()),
          time = timeSpent
        )

        logger.info("publishing result to topic")

        taskResult match {
          case Success(msg) => {
            outputTopic.publish(JSON.toJson(taskResultDescription.copy(message = msg)))
            logger.info("InstructionsExecutor deleting message with from input queue")
            inputQueue.deleteMessage(message)
          }
          case Failure(msg) => {
            errorTopic.publish(JSON.toJson(taskResultDescription.copy(message = msg)))
          }
        }
      } catch {
        case e: Throwable =>  {
          logger.error("fatal error instance will terminated")
          e.printStackTrace()
          val taskResultDescription = TaskResultDescription(
            id = taskId,
            message = e.getMessage,
            instanceId = instance.map(_.getInstanceId()),
            time = lastTimeSpent
          )
          errorTopic.publish(JSON.toJson(taskResultDescription))
          terminate()
        }
      }
    }
  }

}
