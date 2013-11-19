# Script executor

## Introduction

The *Script executor* is a particular implementation of instructions which comes bundled with *nispero*. These instructions allows the execution of arbitrary programs or scripts using *nispero*.
To use the *script executor* you need to define two scripts: the *configuration script* and the *instructions script*.


## configuration script

The *Configuration script* will run on worker instances before running any tasks, So that it is possible to use it for environment configuration: installing additional software or downloading some common data needed for all tasks.

## instructions script

The *Instructions script* will run for every task after downloading input objects, so this script should contain the actual work that you want to perform.


## Worker life-cycle

1. *worker* starts.
2. *script executor* launches configure script.

For every task:

1. *script executor* retrieves input objects and put it to subdirectory `input` of working directory.

For example for this task:

```json
{
  "id": "task1",
  "inputObjects": {
    "text": {
      "bucket": "nispero",
      "key": "text1"
    },
    "patterns": {
      "bucket": "nispero",
      "key":"patterns1"
    }
  },
  "outputObjects": {
    "output":{
      "bucket":"nispero",
      "key":"output1"
    }
  }
}
```

*script executor* will create two files: "input/text1" for "s3:///nispero/text1" and "input/patterns1" for "s3:///nispero/patterns1".

2. *script executor* launches instructions script. Input objects of the task will be accessible to the script under the path "input/<name>"  for example ("input/text1"). Script should store all results in "output" directory using output object names  (for the example task above, it has only one output object "output"; in this case the script should store its result in the file "output/output1").

3. *script executor* uploads the files in the "output" directory to corresponding S3 objects. For the example task "output/output1" will be uploaded to "s3:///nispero/output1".

4. *script executor* reads the content of the *message* file and generates task result like this:

```
{
  "id": "task1",
  "message": <message>,
  "instanceId": ...
  "time": ...
}
```

* depending on the exit code of the instructions script, *script executor* will either publish this task result to the *output topic* (for successful zero code) or to the *error topic*.


## Example usage

```scala
case object instructions extends ohnosequences.nispero.bundles.ScriptExecutor() {
  val metadata = metadataProvider.generateMetadata[this.type, configuration.metadata.type](this.toString, configuration.metadata)

  val instructionsScript =
"""
grep -f input/patterns input/text > output/output
echo "success" > message
"""

  val configureScript =
"""
echo "configuring..."
"""
}
```