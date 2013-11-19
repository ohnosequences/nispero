# Nispero configuration

## General

- `email` Email for receiving notifications
- `tasksProvider`[Task provider](tasks-provider.md) for the project.
- `workersDir` Working directory for *workers*, by default it will be the mount point for the instance's ephemeral storage.
- `jarAddress` (automatically generated) Address of the artifact for this *nispero* project.
- `taskProcessTimeout` Maximum time for processing a task. See the [visibility timeout](visibility-timeout.md) section for more information.

## Resources

- `resources/id` (automatically generated) This parameter is used for generating names for all resources. For example, if this parameter set to "42" default name for *input queue* will be "nisperoInputQueue42". By default it is generated automatically using the artifact name and version.
- `resources/inputQueue`, `resources/outputQueue`, `resources/errorQueue` (automatically generated) Names for the SQS queues that are used in *nispero*. These parameters are optional as they're generated automatically.
-``resources/outputTopic`, `resources/errorTopic`, `resources/notificationTopic` (automatically generated) Names for the SQS topics used in *nispero*. These parameters are optional as they're generated automatically.
- `resources/bucket` Name for the S3 bucket used in *nispero*. This parameter is optional as it's generated automatically.
- `resources/workersStateTable` (automatically generated) Name for the DynamoDB table that isused in *nispero*. This parameter is optional as it's generated automatically.

- `resources/workersGroup` Configuration parameters for the *workers* auto scaling group.

## *manager* and *console* configuration

- `managerConfig/port` HTTP port for the REST API and *console*. This port will be automatically open to all in the security group of the *console* specification.
- `managerConfig/password` Password for the web console, generated automatically.
- `managerConfig/groups` Configuration for the *console* and *manage* auto scaling groups.

## Termination conditions

Termination conditions allow you to force *nispero* to stop after some events.

- `terminationConditions/terminateAfterInitialTasks` If the value of this parameter is set to true, *nispero* will be terminated (by *termination daemon*) once all tasks from the initial task list will be solved (that is: will appear in the *output queue*).
- `terminationConditions/timeout` This is an optional parameter; if set *nispero* will terminate after this timeout is reached. The time unit is seconds.
- `terminationConditions/errorsThreshold`	This is an optional parameter; if set *nispero* will terminate once the `errorQueue` will contain more unique messages than this threshold.

## Example

See example of nispero prject configuration [here](examples.md#configuration).

