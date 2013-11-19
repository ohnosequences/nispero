package ohnosequences.nispero.utils

import java.io.{InputStream, PrintWriter, File}
import java.util.zip.{ZipEntry, ZipFile}
import org.apache.commons.io.{IOUtils, FileUtils}
import scala.collection.JavaConversions._
import ohnosequences.awstools.s3.ObjectAddress
import net.liftweb.json.JsonParser.ParseException
import ohnosequences.awstools.ec2.Tag
import ohnosequences.awstools.autoscaling.AutoScaling
import ohnosequences.nispero.InstanceTags

object Utils {

  def copyAndReplace(files: List[File], outputDir: File, mapping: Map[String, String], ignorePrefix: String = "", mapIgnore: File => Boolean = (file => false)) {
    for (file <- files) {
      //println(file.getAbsolutePath)
      val newFile = new File(outputDir, file.getPath.replace(ignorePrefix, "")).getCanonicalFile
      //println(newFile.getPath)
      if (file.isDirectory) {

        newFile.mkdir()
      } else {

        if(mapIgnore(file)) {
         // println("ignoring: " + file.getPath)
          FileUtils.copyFile(file, newFile)
        } else {
          val content = replace(scala.io.Source.fromFile(file).mkString, mapping)

          //content.
          if(content.contains("$")) {
            println("warning: " + newFile.getPath + " contains $")
          }
          findPattern(content) match {
            case None => ()
            case Some((name, context)) => throw new Error("warning: " + name + " placeholder is free")
          }
          writeStringToFile(content, newFile)
        }

      }
    }
  }

  def findPattern(s: String): Option[(String, String)] = {
    val context = """.{0,5}\$([\w\-]+)\$.{0,5}""".r
    context.findFirstMatchIn(s).map {m => (m.group(1), m.matched)}
  }

  def writeStringToFile(s: String, file: File) {
    val writer = new PrintWriter(file)
    writer.print(s)
    writer.close()
  }

  def copyInputStreamToFile(stream: InputStream, file: File) {
    FileUtils.copyInputStreamToFile(stream, file)

  }

  def recursiveListFiles(file: File, exclude: List[File] = Nil, root: Boolean = true): List[File] =
    if(file.exists()) { root match {
      case false =>
        if (exclude.contains(file)) {
          Nil
        } else {
          val these = file.listFiles.toList
          these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles(_, exclude, false))
        }
      case true => file +: recursiveListFiles(file, exclude, false)
    }}
    else {
      List()
    }




  def zip(archive: File, files: List[File], nameMapping: Map[String, String]) = {
    import java.io.{ BufferedInputStream, FileInputStream, FileOutputStream }
    import java.util.zip.{ ZipEntry, ZipOutputStream }

    val zip = new ZipOutputStream(new FileOutputStream(archive))

    files.foreach {

      case file if !file.isDirectory => {

        val zipEntryName = nameMapping.keys.find(file.getPath.startsWith(_)) match {
          case None => file.getPath
          case Some(name) => file.getPath.replace(name, nameMapping(name))
        }

        zip.putNextEntry(new ZipEntry(zipEntryName))
        val in = new BufferedInputStream(new FileInputStream(file))
        var b = in.read()
        while (b > -1) {
          zip.write(b)
          b = in.read()
        }
        in.close()
        zip.closeEntry()
      }
      case _ =>
    }
    zip.close()
  }

  def unzip(zipFile: File, targetDir: File): List[File] = {
    val files = new java.util.ArrayList[File]()
    val zip = new ZipFile(zipFile)
    try {
      // zip = new ZipFile(zipFile);
      for (entry: ZipEntry <- zip.entries()) {
        val input = zip.getInputStream(entry)
        try {
          if (!targetDir.exists()) targetDir.mkdirs()
          val target = new File(targetDir, entry.getName)
          FileUtils.copyInputStreamToFile(input, target)
          files.add(target)
        } finally {
          IOUtils.closeQuietly(input)
        }
      }
      files.toList
    } finally {
      zip.close()
    }
  }

  def createTempDir(attempt: Int = 0): File = {
    val baseDir = FileUtils.getTempDirectory()
    val tmp: File = new File(baseDir, System.nanoTime() + "-" + attempt)

    if(tmp.mkdir()) {
      tmp
    } else if(attempt < 1000) {
      createTempDir(attempt + 1)
    } else {
      null
    }
  }

  def replace(s: String, mapping: Map[String, String]) = {
    var res = s
    for ((key, value) <- mapping) {
      res = res.replace(key, value)
    }
    res
  }

  def printInterval(intervalSecs: Long): String = {
    (intervalSecs / 60) + " min " + (intervalSecs % 60) + " sec"
  }


  def waitForResource[A](resource: => Option[A]) : Option[A] = {
    var iteration = 1
    var current: Option[A] = None
    val limit = 50

    do {
      current = resource
      iteration += 1
      Thread.sleep(1000)
    } while (current.isEmpty && iteration < limit)

    current
  }

  def tagAutoScalingGroup(as: AutoScaling, groupName: String, status: String) {
    as.createTags(groupName, InstanceTags.PRODUCT_TAG)
    as.createTags(groupName, Tag(InstanceTags.AUTO_SCALING_GROUP, groupName))
    as.createTags(groupName, Tag(InstanceTags.STATUS_TAG_NAME, status))
    as.createTags(groupName, Tag("Name", groupName))
  }
}