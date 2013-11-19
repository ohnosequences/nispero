package ohnosequences.nispero

import ohnosequences.awstools.ec2.EC2
import ohnosequences.awstools.sqs.SQS
import ohnosequences.awstools.sns.SNS
import ohnosequences.awstools.s3.S3
import ohnosequences.awstools.dynamodb.{DynamoObjectMapper, DynamoDB}

import ohnosequences.awstools.autoscaling.AutoScaling
import com.amazonaws.auth.AWSCredentialsProvider


case class AWSClients(ec2: EC2, sqs: SQS, sns: SNS, s3: S3, autoScaling: AutoScaling, dynamoDB: DynamoDB, dynamoMapper: DynamoObjectMapper)

object AWSClients {
  def fromProvider(provider: AWSCredentialsProvider) = {
    val ec2 = EC2.create(provider)
    val s3 = S3.create(provider)
    val sqs = SQS.create(provider)
    val sns = SNS.create(provider)
    val as = AutoScaling.create(provider, ec2)
    val dynamoDB = DynamoDB.create(provider)
    val dynamoMapper = dynamoDB.createMapper

    AWSClients(
      ec2 = ec2,
      sqs = sqs,
      sns = sns,
      s3 = s3,
      autoScaling = as,
      dynamoDB = dynamoDB,
      dynamoMapper = dynamoMapper
    )
  }
}

