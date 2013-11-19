# Examples

## Tasks lists

### Tasks list of a text search problem

``` json
[
  {
    "id": "id001",
    "inputObjects": {
      "pattern": {
        "bucket": "testNisperoBuncket",
        "key": "pattern1"
      },
      "text": {
        "bucket": "testNisperoBuncket",
        "key": "text"
      }
    },
    "outputObjects":{
      "output": {
        "bucket": "testNisperoBuncket",
        "key": "occurrences2"
      }
    }
  }, {
    "id": "id002",
    "inputObjects": {
      "pattern": {
        "bucket":"testNisperoBuncket",
        "key": "pattern2"
      },
      "text": {
        "bucket":"testNisperoBuncket",
        "key": "text"
      }
    },
    "outputObjects": {
      "output": {
        "bucket": "testNisperoBuncket",
        "key": "occurrences2"
      }
    }
  }
]
```

## Configuration
Example of nispero project configuration:

```scala
case object configuration extends Configuration {

  val metadata = new generated.metadata.daaaaam()  
  val version = generateId(metadata)
  
  type AMI = AMI149f7863.type
  val ami = AMI149f7863

  val specs = InstanceSpecs(
    instanceType = InstanceType.C1Medium,
    amiId = ami.id,
    securityGroups = List("nispero"),
    keyName = "nispero",
    instanceProfile = Some("nispero")
  )

  val config = Config(

    email = "user@mail.com",

    managerConfig = ManagerConfig(
      groups = ManagerAutoScalingGroups(
        instanceSpecs = specs.copy(instanceType = InstanceType.C1Medium),
        version = version,
        purchaseModel = SpotAuto
      ),
      password = "1604c27ca7"
    ),

    tasksProvider = EmptyTasks,

    //sets working directory to ephemeral storage
    workersDir =  "/media/ephemeral0",

    //maximum time for processing task
    taskProcessTimeout = 60 * 60 * 1000,

    resources = Resources(
      id = version
    )(
      workersGroup = WorkersAutoScalingGroup(
        desiredCapacity = 1,
        version = version,
        instanceSpecs = specs.copy(
          deviceMapping = Map("/dev/xvdb" -> "ephemeral0")
        )
      )
    ),

    terminationConditions = TerminationConditions(
      terminateAfterInitialTasks = false
    ),

    jarAddress = getAddress(metadata.artifactUrl)
  )
}
```
