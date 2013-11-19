package ohnosequences.nispero

import ohnosequences.awstools.s3.{S3, ObjectAddress}
import java.io.{PrintWriter, File}
import net.liftweb.json.JsonParser.ParseException
import scala.collection.mutable.ListBuffer
import ohnosequences.nispero.utils.JSON

abstract class TasksProvider {  p =>
  def tasks(s3: S3): List[Task]

  def readTasks(tasksString: String): List[Task] = {
    try {
      val tasks = JSON.parse[List[Task]](tasksString)
      tasks
    } catch {
      case t: ParseException => {
        println("error during parsing initial tasks")
        t.printStackTrace()
        List[Task]()
      }
    }
  }

  def ~(q: TasksProvider) = new TasksProvider {
    def tasks(s3: S3): List[Task] = p.tasks(s3) ++ q.tasks(s3)
  }
}

object TasksProvider {
  def flatten(qs: List[TasksProvider]): TasksProvider = new TasksProvider {
    def tasks(s3: S3): List[Task] =  qs.flatMap(_.tasks(s3))
  }
}

case class S3Tasks(tasks: ObjectAddress) extends TasksProvider {
  override def tasks(s3: S3) = {
    s3.readObject(tasks) match {
      case None => println("can't read content from " + tasks); List[Task]()
      case Some(s) => readTasks(s)
    }
  }
}



case class StringTasks(tasks: String) extends TasksProvider {
  override def tasks(s3: S3) = {
    readTasks(tasks)
  }
}

case object EmptyTasks extends TasksProvider {
  override def tasks(s3: S3) = List[Task]()
}


case class ResourceTasks(resource: String) extends TasksProvider {
  override def tasks(s3: S3) = {
    JSON.parseFromResource[List[Task]](resource) match {
      case None => List[Task]()
      case Some(l) => l
    }
  }
}

case class FastaTasks(fasta: ObjectAddress,
                      output: ObjectAddress,
                      n: Int,
                      template: String,
                      counterTemplate: String = "$counter$") extends TasksProvider {

  //todo output folder
  val tempFile = new File("temp.fasta")

  def constructOutputAddress(address: ObjectAddress, counter: Int) = {
    ObjectAddress (
      bucket = output.bucket.replace(counterTemplate, counter.toString),
      key = output.key.replace(counterTemplate, counter.toString)
    )
  }


  def putChunk(s3: S3, writer: PrintWriter, counter: Int) {
    val addr = constructOutputAddress(output, counter)
   // println("putting " + tempFile.getAbsolutePath + " to " + addr)
    writer.close()
    s3.createBucket(addr.bucket)
    s3.putObject(addr, tempFile)
   // writer = new java.io.PrintWriter(tempFile)

  }



  def generate(template: String, n: Int): List[Task] = {
    val result = ListBuffer[Task]()

    for (i <- 1 to n) {
      val task: Task = JSON.parse[Task](template.replace(counterTemplate, i.toString))
      result += task
    }

    result.toList
  }



  override def tasks(s3: S3): List[Task] = {

    val is = s3.getObjectStream(fasta)
    val inputSource = scala.io.Source.fromInputStream(is)

    var counter = 1
    var smallCounter = 0
    var writer = new java.io.PrintWriter(tempFile)



    inputSource.getLines().foreach{ line =>
      if(line.startsWith(">")) {
        smallCounter += 1
        if(smallCounter >= n) {
          smallCounter = 0
          //println(line)
          putChunk(s3, writer, counter)
          counter += 1
          writer = new java.io.PrintWriter(tempFile)
        }
      }
      writer.println(line)
    }

    putChunk(s3, writer, counter)
    generate(template, counter)
  }
}


case class FastasTasks(fastaPrefix: ObjectAddress,
                      output: ObjectAddress,
                      n: Int,
                      template: String,
                      sampleTemplate: String = "$sample$") extends TasksProvider {

  override def tasks(s3: S3): List[Task] = {
    s3.listObjects(fastaPrefix.bucket, fastaPrefix.key).flatMap { objectAddress =>
      val name = objectAddress.key.replace(fastaPrefix.key, "")

      if (!name.isEmpty) {
        println("generating tasks from " + objectAddress)

        val sampleName = name.split("""\.""")(0)

       // println("sampleName =  " + sampleName)
        val fastaTasks = FastaTasks(
          fasta = objectAddress,
          output = output.copy(key = output.key.replace(sampleTemplate, sampleName)),
          template = template.replace(sampleTemplate, sampleName),
          n = n
        )
        fastaTasks.tasks(s3)
      } else {
        List[Task]()
      }
    }

  }
}