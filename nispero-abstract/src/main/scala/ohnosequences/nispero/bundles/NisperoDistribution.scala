package ohnosequences.nispero.bundles

import ohnosequences.statika._
import ohnosequences.statika.aws._
import ohnosequences.nispero.bundles.console.Console
import ohnosequences.typesets._


abstract class NisperoDistribution
[M <: ManagerAux, C <: Console](val manager: M, val console: C)
  extends Bundle() with NisperoDistributionAux {
  type MA = M
  type CA = C

}

trait NisperoDistributionAux extends AnyAWSDistribution {
  type MA <: ManagerAux
  val manager: MA

  type CA <: Console
  val console: CA

  override type Members = MA :~: CA :~: ∅
  override val members = manager :~: console :~: ∅

  type Metadata = manager.resourcesBundle.configuration.Metadata
  val metadata = manager.resourcesBundle.configuration.metadata
  //val m: Metadata = resourcesBundle.configuration.metadata.asInstanceOf[Metadata]

  type AMI = manager.resourcesBundle.configuration.AMI
  val ami = manager.resourcesBundle.configuration.ami

  override type Deps = ∅
  override val deps = ∅

  override def install[D <: AnyDistribution](distribution: D): InstallResults = {
    success("nispero distribution installed")
  }
}