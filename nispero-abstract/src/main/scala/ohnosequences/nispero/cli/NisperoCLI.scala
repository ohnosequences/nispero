package ohnosequences.nispero.cli

import org.eclipse.jgit.api.Git
import java.io.{IOException, FileInputStream, File}
import org.apache.commons.io.FileUtils
import ohnosequences.awstools.ec2.{Tag, EC2}
import com.amazonaws.auth.{AWSCredentialsProvider, InstanceProfileCredentialsProvider, PropertiesCredentials}
import org.clapper.avsl.Logger
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest
import scala.collection.JavaConversions._
import java.util.Properties
import com.amazonaws.internal.StaticCredentialsProvider
import com.amazonaws.AmazonClientException
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient
import ohnosequences.nispero.utils.Utils

case class Exit(code: Int) extends xsbti.Exit


class NisperoCLI extends xsbti.AppMain {
  def run(config: xsbti.AppConfiguration) = {
    NisperoCLI.main(config.arguments)
    Exit(0)
  }
}

object NisperoCLI {

  def createSecurityGroup(ec2: EC2, securityGroup: String, port: Int): Option[String] = {
    logger.info("creating security group: " + securityGroup)
    ec2.createSecurityGroup(securityGroup)

    val sg = Utils.waitForResource(ec2.ec2.describeSecurityGroups(new DescribeSecurityGroupsRequest()
      .withGroupNames(securityGroup)
    ).getSecurityGroups.headOption)

    //val sgID = sg.get.getGroupId

    if(sg.isEmpty) {
      logger.error("couldn't create security group: " + securityGroup)
      System.exit(1)
    }

    //enable ssh connection
    logger.info("enabling ports")
    ec2.enablePortForGroup(securityGroup, 22)
    ec2.enablePortForGroup(securityGroup, port)

    sg.map(_.getGroupId)

  }

  val logger = Logger(this.getClass)

  val sbtCommand = if (System.getProperty("os.name").toLowerCase.contains("win")) {
    "sbt.bat"
  } else {
    "sbt"
  }

  def accountSetup(ec2: EC2, iam: AmazonIdentityManagementClient): Map[String, String] = {
    val securityGroup = "nispero"
    val iamRole = "nispero"
    val keyName = "nispero"

    val port = 443
    val bucketsSuffix = "bucketsSuffix"


    val id = createSecurityGroup(ec2, securityGroup, port)

    val bucketsSuffixValue = ec2.ec2.describeSecurityGroups(new DescribeSecurityGroupsRequest()
      .withGroupNames(securityGroup)
    ).getSecurityGroups.head.getTags.find(_.getKey.equals(bucketsSuffix)).map(_.getValue)



    val fixedBucketsSuffix = bucketsSuffixValue match {
      case None => {
        logger.info("bucketsSuffix isn't configured")
        println("type suffix for artifacts buckets: ")
        val newBucketsSuffixValue = readLine()
        ec2.createTags(id.get, List(Tag(bucketsSuffix, newBucketsSuffixValue)))
        newBucketsSuffixValue
      }
      case Some(v) => logger.info("bucketsSuffix is " + v); v
    }

    logger.info("creating key pair: " + keyName)
    ec2.createKeyPair(keyName, Some(new File(keyName + ".pem")))

    logger.info("creating IAM role: " + iamRole)

    if(!RoleCreator.roleExists(iamRole, iam)){
      RoleCreator.createGodRole(iamRole, iam)
    }


  //  val instanceProfileARN = "instanceProfileARN"

    Map(
      bucketsSuffix -> fixedBucketsSuffix
      //instanceProfileARN -> arn
    )


    //ec2.createTags()
  }



  def defaultPrinter(key: String, value: String) = key + " [" + value + "]: "

  def createMapping(defaultProps: File,
                    predef: Map[String, String],
                    propertyPrinter: (String, String) => String = defaultPrinter
                   ): Map[String, String] = {

    var result = Map[String, String]()
    val properties: Properties = new Properties()
    properties.load(new FileInputStream(defaultProps))

    logger.info("please specify the settings for nispero project:")

    properties.foreach { case (name, value) =>

      val fixedValue = predef.get(name) match {
        case None => value
        case Some(predefValue) => predefValue
      }

     // var actualValue
      //logger.info("print here")
      println(propertyPrinter(name, fixedValue))
      val actualValue = readLine() match {
        case "" => fixedValue
        case v => v
      }


      result += ("$" + name + "$" -> actualValue)
    }
    result
  }

  def retrieveCredentialsProviderFromFile(file: File): (AWSCredentialsProvider, Option[String])= {
    logger.info("retrieving credentials from: " + file.getPath)
    (new StaticCredentialsProvider(new PropertiesCredentials(file)), Some(file.getPath))
  }

  def retrieveCredentialsProvider(args: List[String]): (AWSCredentialsProvider, Option[String]) = {
    args match {
      case Nil => {
        logger.info("trying to retrieve IAM credentials")
        val iamProvider = new InstanceProfileCredentialsProvider()
        try {
          //iamProvider.getCredentials
          //stupid test
          val ec2 = EC2.create(iamProvider)
          ec2.createSecurityGroup("nispero")
          logger.info("retrieved AIM role credentials")
          (iamProvider, None)
        } catch {
          case e: AmazonClientException => {
            val defaultLocation = System.getProperty("user.home")
            val file = new File(defaultLocation, "credentials")
            retrieveCredentialsProviderFromFile(file)
          }
        }
      }
      case head :: tail => retrieveCredentialsProviderFromFile(new File(head))
    }
  }

  def createNispero(credentialsProvider: AWSCredentialsProvider, path: Option[String], tag: Option[String], url: String) {
    val ec2 = EC2.create(credentialsProvider)

    val resolverCredentials = path match {
      case Some(p) => {
        val cred = credentialsProvider.getCredentials
        (cred.getAWSAccessKeyId, cred.getAWSSecretKey)
      }
      case None => ("", "")
    }

    val resolverKeys = Map(
      "resolver-accessKey" -> resolverCredentials._1,
      "resolver-secretKey" -> resolverCredentials._2
    )

    val hideCredentialsPrinter: (String, String) => String = { case (key, value) =>
      if (key.toLowerCase.contains("key")) {
        path match {
          case None => defaultPrinter(key, "use role credentials")
          case Some(p) => defaultPrinter(key, "retrieved from " + p)
        }
      } else {
        defaultPrinter(key, value)
      }
    }

    val iam = new AmazonIdentityManagementClient(credentialsProvider)
    val predef = accountSetup(ec2, iam) ++ resolverKeys ++ Map(
      "password" -> java.lang.Long.toHexString((Math.random() * 100000000000L).toLong)
    )
    fetch(tag, url, predef, hideCredentialsPrinter)
  }

  def main(args: Array[String]) {
    val argsList = args.toList
    argsList match {
      case "create" :: url :: tag :: tail =>  {
        val p = retrieveCredentialsProvider(tail)
        createNispero(p._1, p._2, Some(tag), url)
      }
      case "create" :: tail => {
        val p = retrieveCredentialsProvider(tail)
        createNispero(p._1, p._2, Some("1.0.0"), "https://github.com/ohnosequences/nispero.g8.git")  
      }
      case "publish" :: Nil => publishNispero()
      case "run" :: tail => runNispero(tail.headOption)
      case "undeploy" :: tail => undeployNispero(tail.headOption)
      case _ => println("supported commands: create, run, publish, undeploy")
    }
  }

  def checkDirectory() {
    val file = new File("nispero")
    if(!file.exists()) {
      logger.error("change working dir to nispero project")
      System.exit(1)
    }
  }

  def runNispero(path: Option[String]) {


    checkDirectory()
    import scala.sys.process._
    path match {
      case Some(p) => Seq(sbtCommand, "run " + path).!
      case None => Seq(sbtCommand, "run").!
    }
    println("")
  }

  def undeployNispero(path: Option[String]) {
    checkDirectory()
    import scala.sys.process._
    path match {
      case Some(p) => Seq(sbtCommand, "run undeploy " + path).!
      case None => Seq(sbtCommand, "run undeploy").!
    }
    println("")
  }

  def publishNispero() {
    checkDirectory()

    import scala.sys.process._
    Seq(sbtCommand, "nispero-publish").!
    println("")
  }

  def fixName(name: String): String = {
    val res = new StringBuilder()
    name.split("[^a-zA-Z0-9]+").foreach { s =>
      if(!res.isEmpty) {
        res.append("_")
      }
      res.append(s.toLowerCase)
//      if(res.isEmpty) {
//        res.append(s.toLowerCase)
//      } else {
//        val pref = s.substring(0, 1).toUpperCase()
//        val suf = s.substring(1).toLowerCase()
//        res.append(pref + suf)
//      }
    }
    var fixedName = res.toString()
    if(!fixedName.matches("[a-z].+")) {
      fixedName = "the" + fixedName
    }
    if(!res.toString().equals(name)) {
      logger.warn("name converted to " + fixedName)
    }
    fixedName
  }

  def fetch(tag: Option[String], url: String, predef: Map[String, String], propertyPrinter: (String, String) => String) {

    val dst = Utils.createTempDir()

    logger.info("cloning template from repository")

    clone(url, tag, dst)

    val props = new File(dst, "src/main/g8/default.properties")
    val preMapping = createMapping(props, predef, propertyPrinter)

    val mapping = preMapping + ("$name$" -> fixName(preMapping("$name$")))



    //remove properties file
    props.delete()

    val dst2 = new File(mapping("$name$"))


    val t = new File(dst, "src/main/g8")
    val files = Utils.recursiveListFiles(t)


    FileUtils.deleteDirectory(dst2)
    dst.mkdir()

    val mapIgnore = {file: File =>
      !(file.getName.endsWith(".scala") || file.getName.endsWith(".sbt"))
    }

    Utils.copyAndReplace(files, dst2, mapping, t.getPath, mapIgnore)

    try {
      FileUtils.deleteDirectory(dst)
    } catch {
      case e: IOException => logger.warn("unable to delete: " + dst.getPath)
    }

  }

  def clone(url: String, tag: Option[String], dst: File) {
    try {
      FileUtils.deleteDirectory(dst)
    } catch {
      case e: IOException => logger.warn("unable to delete: " + dst.getPath)
    }

    val cmd = Git.cloneRepository()
      .setDirectory(dst)
      .setURI(url)
    val repo = cmd.call()

    tag match {
      case None => ()
      case Some(t) => {
        repo.checkout()
          .setName("tags/" + t)
          .call()
      }
    }
  }

}
