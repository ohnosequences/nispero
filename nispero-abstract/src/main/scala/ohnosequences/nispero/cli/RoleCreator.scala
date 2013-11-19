package ohnosequences.nispero.cli

import com.amazonaws.services.identitymanagement.model._
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient

object RoleCreator {

  def roleExists(role: String, iam: AmazonIdentityManagementClient): Boolean = {
    try {
      iam.getRole(new GetRoleRequest()
        .withRoleName(role)
      )
      true
    } catch {
      case e: Throwable => false
    }

  }

  def createGodRole(role : String, iam: AmazonIdentityManagementClient) = {

    val a = """{"Version":"2008-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":["ec2.amazonaws.com"]},"Action":["sts:AssumeRole"]}]}"""
    val p = """{"Statement":[{"Effect":"Allow","Action":"*","Resource":"*"}]}"""


    try {
      iam.deleteRolePolicy(new DeleteRolePolicyRequest()
        .withRoleName(role)
        .withPolicyName(role)
      )
    } catch {
      case e: NoSuchEntityException => ()
    }

    try {
      iam.removeRoleFromInstanceProfile(new RemoveRoleFromInstanceProfileRequest()
        .withInstanceProfileName(role)
        .withRoleName(role)
      )
    } catch {
      case e: NoSuchEntityException => ()
    }

    try {
      iam.deleteInstanceProfile(new DeleteInstanceProfileRequest()
        .withInstanceProfileName(role)
      )
    } catch {
      case e: NoSuchEntityException => ()
    }

    try {
      iam.deleteRole(new DeleteRoleRequest()
        .withRoleName(role)
      )
    } catch {
      case e: NoSuchEntityException => ()
    }

    try {
      iam.createInstanceProfile(new CreateInstanceProfileRequest()
        .withInstanceProfileName(role)
      )
    } catch {
      case e: EntityAlreadyExistsException => () //println("already exist")
    }

    try {
      iam.createRole(new CreateRoleRequest()
        .withRoleName(role)
        .withAssumeRolePolicyDocument(a)
      )
    } catch {
      case e: EntityAlreadyExistsException => () //println("already exist")
    }

    try {
      iam.putRolePolicy(new PutRolePolicyRequest()
        .withPolicyName(role)
        .withRoleName(role)
        .withPolicyDocument(p)
      )
    } catch {
      case e: EntityAlreadyExistsException => () //println("already exist")
    }

    iam.addRoleToInstanceProfile(new AddRoleToInstanceProfileRequest()
      .withInstanceProfileName(role)
      .withRoleName(role)
    )

    val arn = iam.getInstanceProfile(new GetInstanceProfileRequest()
      .withInstanceProfileName(role)
    ).getInstanceProfile.getArn
    arn
//
//    println(arn)

  }


}

