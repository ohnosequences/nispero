package ohnosequences.nispero.bundles

import ohnosequences.statika._
import ohnosequences.statika.aws._

import ohnosequences.nispero.{TasksProvider, InstanceTags}
import org.clapper.avsl.Logger

import ohnosequences.awstools.autoscaling.AutoScalingGroup
import ohnosequences.awstools.s3.ObjectAddress
import ohnosequences.nispero.utils.{JSON, Utils}
import ohnosequences.typesets._
import shapeless._

trait ManagerAux extends AnyAWSDistribution {
  type WA <: WorkerAux
  val worker: WA

  val resourcesBundle: Resources
  val logUploader: LogUploader
  val controlQueueHandler: ControlQueueHandler
  val terminationDaemon: TerminationDaemon
  val aws: AWS

  type Metadata = resourcesBundle.configuration.Metadata
  val metadata = resourcesBundle.configuration.metadata
  //val m: Metadata = resourcesBundle.configuration.metadata.asInstanceOf[Metadata] 

  type AMI = resourcesBundle.configuration.AMI
  val ami = resourcesBundle.configuration.ami

  //val m: ami.Metadata = resourcesBundle.configuration.metadata.asInstanceOf[ami.Metadata]

  //val metadata = m

  override type Deps = ControlQueueHandler :~: TerminationDaemon :~: Resources :~: LogUploader :~: AWS :~: ∅
  override val deps = controlQueueHandler :~: terminationDaemon :~: resourcesBundle :~: logUploader :~: aws

  override type Members = WA :~: ∅
  override val members = worker :~: ∅

  val logger = Logger(this.getClass)

  def uploadInitialTasks(taskProvider: TasksProvider, initialTasks: ObjectAddress) {
    try {
      logger.info("generating tasks")
      val tasks = taskProvider.tasks(aws.s3)

      logger.info("uploading initial tasks to S3")
      aws.s3.putWholeObject(initialTasks, JSON.toJson(tasks))


      logger.info("add initial tasks to SQS")
      val inputQueue = aws.sqs.createQueue(resourcesBundle.resources.inputQueue)

      tasks.foreach {
        task =>
          inputQueue.sendMessage(JSON.toJson(task))
      }
      aws.s3.putWholeObject(resourcesBundle.config.tasksUploaded, "")

    } catch {
      case t: Throwable => logger.error("error during uploading initial tasks"); t.printStackTrace()
    }
  }


  override def install[D <: AnyDistribution](distribution: D): InstallResults = {

    val config = resourcesBundle.config

    logger.info("manager is started")

    try {

      if (aws.s3.listObjects(config.tasksUploaded.bucket, config.tasksUploaded.key).isEmpty) {
        uploadInitialTasks(config.tasksProvider, config.initialTasks)
      } else {
        logger.warn("skipping uploading tasks")
      }

      logger.info("generating workers userScript")

      val workerUserScript = userScript(worker)
      // val workerUserScript = ami.userScript(metadata, this.fullName, worker.fullName)

      val workersGroup = aws.as.fixAutoScalingGroupUserData(config.resources.workersGroup, workerUserScript)

      logger.info("running workers auto scaling group")
      aws.as.createAutoScalingGroup(workersGroup)

      val groupName = config.resources.workersGroup.name

      Utils.waitForResource[AutoScalingGroup] {
        println("waiting for manager autoscalling")
        aws.as.getAutoScalingGroupByName(groupName)
      }

      logger.info("creating tags")
      Utils.tagAutoScalingGroup(aws.as, groupName, InstanceTags.INSTALLING.value)

      logger.info("starting termination daemon")
      terminationDaemon.TerminationDaemonThread.start()

      controlQueueHandler.run()

      success("manager installed")


    } catch {
      case t: Throwable => {
        t.printStackTrace()
        aws.ec2.getCurrentInstance.foreach(_.terminate())
        failure("manager fails")
      }
    }
  }
}

abstract class Manager[
W <: WorkerAux,
T <: HList : towerFor[ControlQueueHandler :~: TerminationDaemon :~: Resources :~: LogUploader :~: AWS :~: ∅]#is
](
   val controlQueueHandler: ControlQueueHandler,
   val terminationDaemon: TerminationDaemon,
   val resourcesBundle: Resources,
   val logUploader: LogUploader,
   val aws: AWS,
   val worker: W
   ) extends Bundle[ControlQueueHandler :~: TerminationDaemon :~: Resources :~: LogUploader :~: AWS :~: ∅, T](
  controlQueueHandler :~: terminationDaemon :~: resourcesBundle :~: logUploader :~: aws
) with ManagerAux {
  override type WA = W
}
