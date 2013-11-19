package ohnosequences.nispero

import ohnosequences.awstools.s3.{ObjectAddress, S3}
import java.io.File

case class Task(id: String, inputObjects: Map[String, ObjectAddress], outputObjects: Map[String, ObjectAddress])

trait Instructions {
  def execute(s3: S3, task: Task, workingDir: File = new File(".")): TaskResult
}

sealed abstract class TaskResult {
  val message: String
}
case class Success(message: String) extends TaskResult
case class Failure(message: String) extends TaskResult

case class TaskResultDescription(
  id: String,
  message: String,
  instanceId: Option[String],
  time: Int
)