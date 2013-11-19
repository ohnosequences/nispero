package ohnosequences.nispero.bundles.console


import unfiltered.request._
import unfiltered.response._
import unfiltered.response.ResponseString
import unfiltered.Cycle

import org.clapper.avsl.Logger


import ohnosequences.nispero.utils.{JSON, Utils}


trait Users {
  def auth(u: String, p: String): Boolean
}

case class Auth(users: Users) {
  def apply[A,B](intent: Cycle.Intent[A,B]) =
    Cycle.Intent[A,B] {
      case req@BasicAuth(user, pass) if users.auth(user, pass) =>
        Cycle.Intent.complete(intent)(req)
      case _ =>
        Unauthorized ~> WWWAuthenticate("""Basic realm="/"""")
    }
}

class App(backend: BackEnd, mainPage: String, users: Users) extends unfiltered.filter.Plan {

  val logger = Logger(classOf[App])

  def responseJSON(any: Any): ResponseWriter = {
    ResponseString(JSON.toJson(any))
  }

  def intent = Auth(users) {

    case GET(Path("/status/inputQueue")) => responseJSON(backend.inputQueueStatus)

    case GET(Path("/status/outputQueue")) => responseJSON(backend.outputQueueStatus)

    case GET(Path("/status/errorQueue")) => responseJSON(backend.errorQueueStatus)

    case GET(Path("/status/successResults")) => responseJSON(backend.successMessages)

    case GET(Path("/status/errorResults")) => responseJSON(backend.failedMessages)

    case GET(Path("/tasks/removeAll")) => backend.removeAllTasks(); Ok

    case GET(Path("/tasks/initial")) => ResponseString(backend.initialTasks)

    case req@POST(Path("/tasks/add")) => {
      val body = scala.io.Source.fromInputStream(req.inputStream).mkString
      backend.addTasks(body)
      Ok
    }

    case GET(Path("/instances/status")) => responseJSON(backend.listGroupInstances)

    //todo remove it
    case GET(Path("/instances/requests/status")) => ResponseString("[]")

    case GET(Path(Seg("instances" :: "terminate" :: id :: Nil))) => backend.terminateInstance(id); Ok

    case GET(Path(Seg("instances" :: "ssh" :: id :: Nil))) => responseJSON(backend.getSSHCommand(id))

    case GET(Path(Seg("instances" :: "log" :: id :: Nil))) => ResponseString(backend.instanceLog(id))

    //todo remove it
    case GET(Path(Seg("instances" :: "requests" :: "cancel" :: id :: Nil))) => Ok

    case GET(Path(Seg("instances" :: "price" :: instanceType :: Nil))) =>
      ResponseString(backend.currentSpotPrice(instanceType))

    case GET(Path("/instances/stats")) => responseJSON(backend.farmHistory)

    case GET(Path("/autoScaling/groups")) => responseJSON(backend.autoScalingConfigs)

    case GET(Path(Seg("autoScaling" :: "groups" :: "delete" :: value :: Nil))) => backend.deleteAutoScalingGroup(value); Ok

    case GET(Path("/config")) => responseJSON(backend.configForConsole)

    case GET(Path("/undeploy")) => {
      backend.undeploy()
      ResponseString("undeployed")
    }

    case GET(Path(Seg("autoScaling" :: "group" :: "capacity" :: "set" :: value :: Nil))) => {
      backend.setGroupCapacity(value.toInt)
      Ok
    }

    case GET(Path("/autoScaling/group/state")) => responseJSON(backend.workersGroup)

    case GET(Path(Seg("autoScaling" :: "group" :: "capacity" :: "get" :: Nil))) => {
      ResponseString(backend.workersGroupCapacity.toString)
    }

    case GET(Path("/")) => HtmlCustom(mainPage)

    case GET(Path("/sandbox")) => {
      ResponseString("sandbox")
    }
  }
}

case class HtmlCustom(s: String) extends ComposeResponse(HtmlContent ~> ResponseString(s))

class FrontEnd(backend: BackEnd, password: String, port: Int, resourcesBucket: String) {

  val logger = Logger(this.getClass)

  def loadMainPage = {
    val webClientMapping = Map(
      "{bucket}" -> resourcesBucket
    )
    val main =  getClass.getResourceAsStream("/console/main.html")
    Utils.replace(scala.io.Source.fromInputStream(main).mkString, webClientMapping)
  }

  object adminOnly extends Users {
    def auth(u: String, p: String) = u.equals("nispero") && p.equals(password)
  }

  def printURL(domain: String, port: Int): String = port match {
    case 443 => "https://" + domain
    case 80 => "http://" + domain
    case p => domain + ":" + port
  }

  def run() {
    logger.info("console started")


    val dns = backend.currentAddress

    val managerLocation = printURL(dns, port)
    val message = "Nispero started \n" +
      "console location: "+  managerLocation + "\n" +
      "login: nispero\n" +
      "password: " + password + "\n" +
      "config:\n" + JSON.toJson(backend.configForConsole)
    val subject = "Nispero " + backend.configForConsole.resources.id + " started"


    backend.sendNotification(message, subject)


    val http = unfiltered.jetty.Https(port)
    http.filter(new App(backend, loadMainPage, adminOnly)).run({
      svr =>
        ()
    }, {
      svr =>
        logger.info("console finished")
    })
  }
}
