package ohnosequences.nispero.bundles.console

import ohnosequences.statika._

import ohnosequences.nispero.bundles._
import org.clapper.avsl.Logger
import ohnosequences.nispero.bundles.console.pojo.{FarmStatePojo, FarmState}
import ohnosequences.nispero.{Names, InstanceTags}
import java.util.Date
import java.text.SimpleDateFormat
import ohnosequences.awstools.dynamodb.NumericValue
import ohnosequences.awstools.ec2.TagFilter
import ohnosequences.awstools.ec2.RequestStateFilter
import ohnosequences.awstools.ec2.InstanceStateFilter
import ohnosequences.awstools.ec2.Tag
import ohnosequences.typesets._


abstract class FarmStateLogger(resourcesBundle: Resources, aws: AWS) extends Bundle(resourcesBundle :~: aws :~: ∅) {

  val logger = Logger(this.getClass)

  val TIMEOUT = 30000

  import aws._

  val format: SimpleDateFormat = new SimpleDateFormat("HH:mm:ss")

  def formatDate(date: Date) = {
    format.format(date)
  }

  def getFarmState: FarmState = {

    val groupFilter = TagFilter(Tag(InstanceTags.AUTO_SCALING_GROUP, resourcesBundle.resources.workersGroup.name))

    val installing = ec2.listInstancesByFilters(
      groupFilter,
      TagFilter(InstanceTags.INSTALLING),
      InstanceStateFilter("running")
    ).size

    val idle = ec2.listInstancesByFilters(
      groupFilter,
      TagFilter(InstanceTags.IDLE),
      InstanceStateFilter("running")
    ).size

    val processing = ec2.listInstancesByFilters(
      groupFilter,
      TagFilter(InstanceTags.PROCESSING),
      InstanceStateFilter("running")
    ).size

    FarmState(
      date = formatDate(new Date()),
      timestamp = new Date().getTime,
      idleInstances = idle,
      processingInstances = processing,
      installingInstances = installing
    )
  }

  def getFarmHistory: List[FarmState] = {

    //5 hours
    val interval = 1000 * 60L * 60 * 5

    val currentTimestamp = System.currentTimeMillis()
    val fromTimestamp = currentTimestamp - interval

    dynamoMapper.queryRangeInterval(classOf[FarmStatePojo],
      resourcesBundle.resources.workersStateTable,
      Names.Tables.WORKERS_STATE_HASH_KEY_VALUE,
      NumericValue(fromTimestamp),
      NumericValue(currentTimestamp)
    ).map {
      pojo => FarmState.fromPojo(pojo)
    }
  }

  object FarmStateLoggerThread extends Thread("FarmStateLoggerThread") {
    override def run() {
      while (true) {
        val farmState = getFarmState
        logger.info(farmState)
        try {
          dynamoMapper.save(resourcesBundle.resources.workersStateTable, farmState.toPojo)
        } catch {
          case t: Throwable => logger.error("couldn't save farm state: " + t)
        }
        Thread.sleep(TIMEOUT)

      }
    }
  }

  override def install[D <: AnyDistribution](distribution: D): InstallResults = {
    FarmStateLoggerThread.start()
    success("FarmStateLoggerAux finished")
  }

}
