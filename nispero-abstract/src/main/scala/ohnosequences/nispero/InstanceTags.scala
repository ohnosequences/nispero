package ohnosequences.nispero

import ohnosequences.awstools.ec2.Tag

object InstanceTags {

  val PRODUCT_TAG = Tag("product", "nispero")

  val STATUS_TAG_NAME = "status"

  //for instances
  val RUNNING = Tag(STATUS_TAG_NAME, "running")
  val INSTALLING = Tag(STATUS_TAG_NAME, "installing")
  val IDLE = Tag(STATUS_TAG_NAME, "idle")
  val PROCESSING = Tag(STATUS_TAG_NAME, "processing")
  val FINISHING = Tag(STATUS_TAG_NAME, "finishing")
  val FAILED = Tag(STATUS_TAG_NAME, "failed")

  val AUTO_SCALING_GROUP = "autoScalingGroup"

  val BUCKET = "bucket"


}