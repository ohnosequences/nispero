package ohnosequences.nispero.bundles

import ohnosequences.statika._
import ohnosequences.nispero.worker.InstructionsExecutor
import ohnosequences.typesets._
import shapeless._

abstract class Worker[I <: InstructionsAux, T <: HList : towerFor[I :~: Resources :~: LogUploader :~: AWS :~: ∅]#is]
(val instructions: I, val resourcesBundle: Resources, val logUploader: LogUploader, val aws: AWS)
  extends Bundle[I :~: Resources :~: LogUploader :~: AWS :~: ∅, T](instructions :~: resourcesBundle :~: logUploader :~: aws :~: ∅) with WorkerAux {
  override type IA = I
}


trait WorkerAux extends AnyBundle {

  type IA <: InstructionsAux
  val instructions: IA
  val resourcesBundle: Resources
  val aws: AWS
  val logUploader: LogUploader
  override type Deps = IA :~: Resources :~: LogUploader :~: AWS :~: ∅
  override val deps = instructions :~: resourcesBundle :~: logUploader :~: aws :~: ∅

  override def install[D <: AnyDistribution](distribution: D): InstallResults = {
    val config = resourcesBundle.config
    val instructionsExecutor = new InstructionsExecutor(config, instructions.instructions, aws.awsClients)
    instructionsExecutor.run()
    success("worker installed")
  }
}