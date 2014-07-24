## Nispero usage

### Installation

[Amazon Linux](installation-amazon-linux.md)

[Windows](installation-windows.md)

### Updating nispero

If you have already installed *nispero* and want to update to new version type:

```bash
cs ohnosequences/nisperoCLI/<version>
```

### Usage

#### Create project

To start work with *nispero* you should create a *nispero project* — [SBT project](http://www.scala-sbt.org/)
containing all settings such as amount workers, [task provider](tasks-providers.md), [instructions](overview.md#instructions).
The following command will create ready to use template for *nispero* project:

```bash
nispero create ohnosequences/nispero.g8
```

This command will ask you to fill these settings:

* **name** — name of your *nispero* project. Only latin letters, digits and underscores ("_") are allowed (the name will converted automatically to this format otherwise)
* **email** — email for *nispero* notifications
* **bucketsSuffix** — should be specified only one suffix of an S3 bucket for publishing artifacts
* **resolver-accessKey** — generated automatically, AWS access key for publishing
* **resolver-secretKey** — generated automatically, AWS secret key for publishing
* **password** — generated automatically, password for *console*

After filling all fields the command will generate a directory with a *nispero* project with same name as the **name** field.

> note that a nispero project is nothing more than a sbt project, with configured dependencies, publishing and versioning

#### Setup your configuration

All configuration is stored in the Scala file "<project name>/src/main/scala/configuration.scala". In this specification you can change:

* [instructions](overview.md#instructions)
* [tasks provider](tasks-providers.md)
* specifications for worker instances
* amount of workers

**see also:** [nispero configuration](config.md)


#### Prepare you scripts

The default *nispero* project uses [script executor](script-executor.md) for instructions. To use it you should provide two scripts:

* *configure script* — configure worker instance
* *run script* — solve tasks

**see also:** [script executor](script-executor.md)

#### Publish

Before running your *nispero* project you should compile it and publish artifacts to the special S3 bucket. To do it type:

```bash
nispero publish
```

in your project directory.

#### Run

```bash
nispero run
```

> Also it is possible to use `sbt` commands "nispero-publish" and "nispero-run" for this:

```
> sbt
$ sbt
[info] Loading global plugins ...
> nispero-publish
[info] Loading global plugins ...
...
> nispero-run
...
```

#### Nispero Console

Every *nispero* project runs together with a *nispero console*, that can be used for monitoring and managing *nispero*. To access the *console* follow the instructions from the e-mail that will be send to the e-mail account you provided before.

> Because nispero console uses self-signed HTTPs certificate, you browser can show you security alert.

**see also:** [nispero console](console.md)

#### SSH into instances

Using the *console* you can also get the ssh command needed to login to the instance and see logs:

```
ssh -i nispero.pem ec2-user@ec2-46-137-10-122.eu-west-1.compute.amazonaws.com
sudo tail -f /root/log.txt
```

For ssh connections you need a special file containing an AWS keypair ("keyName" parameter in configuration); *Nispero* will generate it for you and save in a directory within the project if a keypair named "keyName" doesn't exist in your AWS account.

#### Manual undeploy

If you can't get access to the console, you can undeploy your *nispero* by running the `nispero undeploy` command in the project folder of the running *nispero* instance. Before executing this command make sure that the version of your running *nispero* instance coincides with the version found in "version.sbt".




