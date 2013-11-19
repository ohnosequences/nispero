package ohnosequences.nispero.bundles.console

import ohnosequences.nispero._
import scala.collection.mutable.ListBuffer
import org.clapper.avsl.Logger

import ohnosequences.nispero.Config
import ohnosequences.awstools.sqs.Message

import ohnosequences.nispero.Task

import ohnosequences.nispero.AWSClients
import ohnosequences.awstools.ec2._
import ohnosequences.awstools.s3.ObjectAddress
import ohnosequences.nispero.bundles.console.pojo.FarmState
import ohnosequences.awstools.autoscaling.AutoScalingGroup
import ohnosequences.nispero.manager.{RawCommand}
import ohnosequences.nispero.utils.JSON


class BackEnd(awsClients: AWSClients, config: Config, farmStateLogger: FarmStateLogger) {

  import awsClients._
  import ec2.{Instance, SpotInstanceRequest}

  val logger = Logger(this.getClass)

  val resources = config.resources

  def sendNotification(message: String, subject: String) {
    val notificationTopic = awsClients.sns.createTopic(config.notificationTopic)
    notificationTopic.publish(message, subject)
  }

  def inputQueueStatus: QueueState = {
    QueueState(sqs.createQueue(resources.inputQueue).getApproximateNumberOfMessages)
  }

  def outputQueueStatus: QueueState = {
    QueueState(sqs.createQueue(resources.outputQueue).getApproximateNumberOfMessages)
  }

  def errorQueueStatus: QueueState = {
    QueueState(sqs.createQueue(resources.errorQueue).getApproximateNumberOfMessages)
  }

  def successMessages: List[String] = queueMessages(resources.outputQueue)

  def failedMessages: List[String] = queueMessages(resources.errorQueue)

  def queueMessages(queueName: String): List[String] = {
    sqs.getQueueByName(queueName) match {
      case None => Nil
      case Some(outputQueue) => {
        var messages = ListBuffer[String]()
        var empty = false
        var i = 0
        while(!empty && i < 10) {
          val chuck = outputQueue.receiveMessages(10)
          if (chuck.isEmpty) {
            empty = true
          }
          messages ++= chuck.map(_.body)
          i += 1
        }
        messages.toList
      }
    }
  }

  def removeAllTasks() {
    logger.info("removing all tasks")
    val queue = sqs.getQueueByName(config.resources.inputQueue).get

    var messages = List[Message]()

    do {
      messages = queue.receiveMessages(10)
      messages.foreach{ message => queue.deleteMessage(message)}
    } while (!messages.isEmpty)
  }

  def initialTasks: String = {
    s3.readObject(config.initialTasks).getOrElse("unavailable")
  }

  def addTask(task: String) {
    sqs.createQueue(config.resources.inputQueue).sendMessage(task)
  }

  def addTasks(tasksString: String): Int = {
    val tasks = JSON.parse[List[Task]](tasksString)
    tasks.foreach { task: Task =>
      addTask(JSON.toJson(task))
    }
    tasks.size
  }

  def listGroupInstances: List[InstanceState] = {
    val workersGroupFilter = TagFilter(Tag(InstanceTags.AUTO_SCALING_GROUP, config.resources.workersGroup.name))
    val managerGroupFilter = TagFilter(Tag(InstanceTags.AUTO_SCALING_GROUP, config.managerConfig.groups._1.name))
    val consoleGroupFilter = TagFilter(Tag(InstanceTags.AUTO_SCALING_GROUP, config.managerConfig.groups._2.name))
    ec2.listInstancesByFilters(
      workersGroupFilter
    ).map(getInstanceState(_)) ++ ec2.listInstancesByFilters(
      managerGroupFilter
    ).map(getInstanceState(_)) ++ ec2.listInstancesByFilters(
      consoleGroupFilter
    ).map(getInstanceState(_))
  }

  def getInstanceState(instance: Instance) = InstanceState(
    instanceId = instance.getInstanceId,
    statusTag = instance.getTagValue(InstanceTags.STATUS_TAG_NAME) match {
      case None => "undefined"
      case Some(value) => value
    },
    state = instance.getState()
  )

  def terminateInstance(instanceId: String) {
    ec2.terminateInstance(instanceId)
  }

  def setGroupCapacity(capacity: Int) {
    autoScaling.setDesiredCapacity(config.resources.workersGroup, capacity)
  }

  def getSSHCommand(instanceId: String): Option[String] = ec2.getInstanceById(instanceId).flatMap(_.getSSHCommand())

  def instanceLog(instanceId: String): String = {
    s3.readObject(ObjectAddress(config.resources.bucket, instanceId)) match {
      case None => "unavailable"
      case Some(s) => s
    }
  }

  def currentAddress: String = {
    awsClients.ec2.getCurrentInstance.flatMap {_.getPublicDNS()}.getOrElse("<undefined>")
  }

  def currentSpotPrice(instanceType: String): String = {
    ec2.getCurrentSpotPrice(InstanceType.fromName(instanceType)).toString
  }

  def farmHistory: List[FarmState] = farmStateLogger.getFarmHistory

  def autoScalingConfigs: List[AutoScalingGroup] = {
    autoScaling.describeAutoScalingGroups()
  }

  def deleteAutoScalingGroup(name: String) {
    logger.info("Delete AutoScaling group: " + name)
    autoScaling.deleteAutoScalingGroup(name)
  }

  def configForConsole: Config = {
    config
  }

  def undeploy() {
    sendManagerCommand(RawCommand.UnDeploy("terminated from console"))
  }

  def workersGroup: Option[AutoScalingGroup] = {
    autoScaling.getAutoScalingGroupByName(config.resources.workersGroup.name)
  }

  def workersGroupCapacity: Int = {
    autoScaling.getAutoScalingGroupByName(config.resources.workersGroup.name) match {
      case None => -1
      case Some(group) => group.desiredCapacity
    }
  }

  def sendManagerCommand(command: RawCommand) {
    val c = RawCommand(command.command, command.arg)
    sqs.createQueue(resources.controlQueue).sendMessage(JSON.toJson(c))
  }

}
