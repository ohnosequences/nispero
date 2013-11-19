package ohnosequences.nispero.bundles


import ohnosequences.nispero._
import ohnosequences.awstools.s3.{S3}
import java.io.File
import org.clapper.avsl.Logger
import ohnosequences.awstools.s3.LoadingManager
import ohnosequences.nispero.Failure
import ohnosequences.nispero.Task
import org.apache.commons.io.{FileUtils}
import ohnosequences.nispero.utils.Utils
import ohnosequences.typesets._
import shapeless._


trait ScriptExecutorAux extends ohnosequences.nispero.bundles.InstructionsAux {

  val configureScript: String

  val instructionsScript: String

  val logger = Logger(this.getClass)

  val instructions = new ohnosequences.nispero.Instructions {

    var loadManager: LoadingManager = null

    val scriptname = "script.sh"

    def execute(s3: S3, task: Task, workingDir: File): TaskResult = {
      try {

        if (loadManager == null) {
          logger.info("creating download manager")
          loadManager = s3.createLoadingManager()
        }

        workingDir.mkdir()

        val APP_DIR = new File(workingDir, "scriptExecutor")
        logger.info("cleaning working directory: " + APP_DIR.getAbsolutePath)
        FileUtils.deleteDirectory(APP_DIR)

        logger.info("creating working directory: " + APP_DIR.getAbsolutePath)
        APP_DIR.mkdir()

        val scriptFile = new File(APP_DIR, scriptname)

        logger.info("writing script to " + scriptFile.getAbsolutePath)
        Utils.writeStringToFile(fixLineEndings(instructionsScript), scriptFile)

        //download input objects
        val inputObjects = new File(APP_DIR, "input")
        inputObjects.mkdir()
        inputObjects.listFiles().foreach(_.delete())


        val outputObjects = new File(APP_DIR, "output")
        outputObjects.mkdir()
        outputObjects.listFiles().foreach(_.delete())

        for ((name, objectAddress) <- task.inputObjects) {
          logger.info("trying to retrieve input object " + objectAddress)
          loadManager.download(objectAddress, new File(inputObjects, name))
          logger.info("success")
        }

        logger.info("running instructions script in " + APP_DIR.getAbsolutePath)
        val p = sys.process.Process(Seq("bash", "-x", scriptname), APP_DIR).run()

        val result = p.exitValue()

        val messageFile = new File(APP_DIR, "message")

        val message = if (messageFile.exists()) {
          scala.io.Source.fromFile(messageFile).mkString
        } else {
          logger.warn("couldn't found message file")
          ""
        }

        if (result != 0) {
          logger.error("script finished with non zero code: " + result)
          if (message.isEmpty) {
            Failure("script finished with non zero code: " + result)
          } else {
            Failure(message)
          }
        } else {
          logger.info("start.sh script finished, uploading results")
          for ((name, objectAddress) <- task.outputObjects) {
            val outputFile = new File(outputObjects, name)
            if (outputFile.exists()) {
              logger.info("trying to publish output object " + objectAddress)
              loadManager.upload(objectAddress, outputFile)
              logger.info("success")
            } else {
              logger.warn("warning: file " + outputFile.getAbsolutePath + " doesn't exists!")
            }
          }
          Success(message)
        }
      } catch {
        case e: Throwable => {
          e.printStackTrace()
          Failure(e.getMessage)
        }
      }
    }
  }


  def fixLineEndings(s: String): String = s.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n")

  import ohnosequences.statika._

  override def install[D <: AnyDistribution](distribution: D): InstallResults = {
    val configureScriptName = "configure.sh"

    Utils.writeStringToFile(fixLineEndings(configureScript), new File(configureScriptName))

    logger.info("running configure script")

    sys.process.Process(Seq("bash", "-x", configureScriptName)).! match {
      case 0 => success("configure.sh finished")
      case code => failure("configure.sh fails with error code: " + code)
    }
  }
}


import ohnosequences.statika._

abstract class ScriptExecutor[D <: TypeSet : ofBundles, T <: HList : towerFor[D]#is](deps: D = ∅)
  extends Bundle[D, T](deps) with ScriptExecutorAux

