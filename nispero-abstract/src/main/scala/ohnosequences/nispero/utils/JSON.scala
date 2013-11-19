package ohnosequences.nispero.utils

import net.liftweb.json._
import net.liftweb.json.JsonAST.JValue
import net.liftweb.json.JsonAST.JString
import net.liftweb.json.TypeInfo
import ohnosequences.awstools.ec2.InstanceType
import java.io.File
import net.liftweb.json.JsonParser.ParseException


object JSON {


  def toJson(r: Any): String = {

    r match {
      case None => "{}"
      case Some(s) => {
        toJson(s)
      }

      case o => {
        import net.liftweb.json.JsonAST._
        import net.liftweb.json.Extraction._
        import net.liftweb.json.Printer._

        implicit val formats = net.liftweb.json.DefaultFormats + InstanceTypeSerializer

        pretty(render(decompose(o)))
      }

    }
  }

  object InstanceTypeSerializer extends Serializer[InstanceType] {

    val Class = classOf[InstanceType]

    def deserialize(implicit format: Formats):
    PartialFunction[(TypeInfo, JValue), InstanceType] = {
      case (TypeInfo(Class, _), json) => json match {
        case JString(s) => InstanceType.fromName(s)
        case _ => throw new MappingException("wrong instance type")
      }
    }

    def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
      case instanceType:InstanceType  => JString(instanceType.toString)
    }
  }


  def parse[T](s: String)(implicit mf: scala.reflect.Manifest[T]): T = {
    val json = JsonParser.parse(s)

    implicit val formats = net.liftweb.json.DefaultFormats + InstanceTypeSerializer
    json.extract[T]
  }

  def parseFromResource[T](name: String)(implicit mf: scala.reflect.Manifest[T]): Option[T] = {
    val r = getClass.getResourceAsStream(name)
    if (r == null) {
      println("couldn't fount resource: " + name)
      None
    } else {
      val s = scala.io.Source.fromInputStream(r).mkString
      try {
        Some(parse[T](s))
      } catch {
        case t: ParseException => {
          println("error during parsing initial tasks")
          t.printStackTrace()
          None
        }
      }
    }
  }

}
