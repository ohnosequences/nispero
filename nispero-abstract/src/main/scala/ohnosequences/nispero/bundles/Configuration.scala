package ohnosequences.nispero.bundles

import ohnosequences.statika._
import ohnosequences.nispero.Config
import ohnosequences.statika.aws.{SbtMetadata, AnyAMI, AnyMetadata}
import ohnosequences.awstools.s3.ObjectAddress
import ohnosequences.typesets._

abstract class Configuration extends Bundle() {

  type Metadata <: AnyMetadata
  val metadata: Metadata

  type AMI <: AnyAMI.of[Metadata]
  val ami: AMI

  val config: Config

  def generateId(metadata: SbtMetadata): String = {
    val name = metadata.artifact.replace(".", "")
    val version = metadata.version.replace(".", "")
    (name + version).toLowerCase
  }

  def getAddress(url: String): ObjectAddress = {
    val s3url = """s3://(.+)/(.+)""".r
    url match {
      case s3url(bucket, key) => ObjectAddress(bucket, key)
      case _ => throw new Error("wrong fat jar url, check your publish settings")
    }
  }

  override def install[D <: AnyDistribution](distribution: D): InstallResults = {
    success("configuration installed")
  }
}
