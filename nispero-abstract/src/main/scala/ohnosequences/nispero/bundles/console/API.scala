package ohnosequences.nispero.bundles.console

import ohnosequences.awstools.ec2.InstanceSpecs

// -> Manager
case class RunSpotSpecs(
  amount: Int,
  price: Double,
  instanceSpecs: InstanceSpecs
)

case class RunDemandSpecs(
  amount: Int,
  instanceSpecs: InstanceSpecs
)

// <- Manager
case class QueueState(amountMessages: Int)

case class InstanceState(instanceId: String, statusTag: String, state: String)

case class SpotRequestState(requestId: String, statusTag: String, state: String, status: String)




