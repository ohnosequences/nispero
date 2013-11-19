package ohnosequences.nispero.bundles

import ohnosequences.statika._
import ohnosequences.statika.aws._

import ohnosequences.awstools.ec2.EC2
import ohnosequences.awstools.s3.S3
import ohnosequences.awstools.autoscaling.AutoScaling
import ohnosequences.awstools.sqs.SQS
import ohnosequences.awstools.sns.SNS
import ohnosequences.awstools.dynamodb.DynamoDB
import ohnosequences.nispero.AWSClients
import ohnosequences.typesets._


abstract class AWS(val configuration: Configuration) extends Bundle(configuration :~: ∅)  {

  val ec2 = EC2.create()

  val s3 = S3.create()

  val as = AutoScaling.create(ec2)

  val sqs = SQS.create()

  val sns = SNS.create()

  val dynamoDB = DynamoDB.create()

  val dynamoMapper = dynamoDB.createMapper

  val awsClients = AWSClients(
      ec2 = ec2,
      sqs = sqs,
      sns = sns,
      s3 = s3,
      autoScaling = as,
      dynamoDB = dynamoDB,
      dynamoMapper = dynamoMapper
  )

  val awsCredentials: AWSCredentials = RoleCredentials

  override def install[D <: AnyDistribution](distribution: D): InstallResults = {
    success("aws installed")
  }
}
