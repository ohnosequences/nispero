package ohnosequences.nispero.manager

import ohnosequences.awstools.ec2.InstanceSpecs
import ohnosequences.awstools.autoscaling._

object ManagerAutoScalingGroups {
  def apply(instanceSpecs: InstanceSpecs,
            version: String,
            purchaseModel: PurchaseModel = SpotAuto): (AutoScalingGroup, AutoScalingGroup) = {

    val managerGroup = AutoScalingGroup(
      name = "nisperoManagerGroup" + version,
      minSize = 1,
      maxSize = 1,
      desiredCapacity = 1,

      launchingConfiguration = LaunchConfiguration(
        name = "nisperoManagerLaunchConfiguration" + version,
        instanceSpecs = instanceSpecs,
        purchaseModel = purchaseModel
      )
    )

    val consoleGroup = AutoScalingGroup(
      name = "nisperoConsoleGroup" + version,
      minSize = 1,
      maxSize = 1,
      desiredCapacity = 1,

      launchingConfiguration = LaunchConfiguration(
        name = "nisperoConsoleLaunchConfiguration" + version,
        instanceSpecs = instanceSpecs,
        purchaseModel = purchaseModel
      )
    )

    (managerGroup, consoleGroup)
  }
}


