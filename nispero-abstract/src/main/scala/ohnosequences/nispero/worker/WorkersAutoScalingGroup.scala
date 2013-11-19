package ohnosequences.nispero.worker


import ohnosequences.awstools.ec2.InstanceSpecs
import ohnosequences.awstools.autoscaling._

object WorkersAutoScalingGroup {
  def apply(instanceSpecs: InstanceSpecs,
            version: String,
            desiredCapacity: Int = 1,
            minSize: Int = 0,
            maxSize: Int = 10,
            purchaseModel: PurchaseModel = SpotAuto) = AutoScalingGroup(
    name = "nisperoWorkersGroup" + version,
    minSize = minSize,
    maxSize = maxSize,
    desiredCapacity = desiredCapacity,
    launchingConfiguration = LaunchConfiguration(
      name = "nisperoWorkersLaunchConfiguration" + version,
      instanceSpecs = instanceSpecs,
      purchaseModel = purchaseModel
    )
  )
}
