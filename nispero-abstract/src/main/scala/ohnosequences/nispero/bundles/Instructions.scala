package ohnosequences.nispero.bundles

import ohnosequences.statika._
import ohnosequences.typesets._
import shapeless._



trait InstructionsAux extends AnyBundle {

  val instructions: ohnosequences.nispero.Instructions

  override def install[D <: AnyDistribution](distribution: D): InstallResults = {
    success("instructions installed")
  }
}

abstract class Instructions[D <: TypeSet : ofBundles, T <: HList : towerFor[D]#is](deps: D)
  extends Bundle[D, T](deps) with InstructionsAux
