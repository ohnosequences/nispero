# Nispero overview

## Basic notions

As already mentioned, *nispero* is a system for scaling/parallelizing stateless computations using Amazon Web Services. To use it you should basically define two things: *instructions* (that represent your computations) and *tasks* (input data for these computations). The basic principle here is that *instructions* are exactly the same for all *tasks* and don't depend on them.

## Instructions

There are different ways to define *instructions*. The simplest one is to use the built-in *nispero* [script executor](script-executor.md). It can wrap an arbitrary shell script into *instructions*. 

Another way to define instructions is to implement the simple [Instructions interface](https://github.com/ohnosequences/nispero/blob/master/nispero-abstract/src/main/scala/ohnosequences/nispero/Task.scala#L8).

*Instructions* are wrapped as [*statika*](https://github.com/ohnosequences/statika) bundles. It simplify dependence management and allows reuse existing components.

- **see also:** [statika web site](https://github.com/ohnosequences/statika)

## Tasks

Every task contains three fields:

- *id* — unique identifier of tasks.
- *inputObjects* — named set of addresses pointing to S3 objects, these objects should contain the input data required by instructions.
* *outputObjects* — named set of addresses to S3 objects, that will be used for storing the results of executing instructions.

*Nispero* uses a *JSON* representation of a task, called the *task description*. An example of a task description:

``` json
{
  "id": "task1",
  "inputObjects": {
    "app": {
      "bucket": "nispero",
      "key": "app.zip"
    },
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

This description defines a task with id `task1`, with three input objects: `app` ("s3:///nispero/app.zip"),
`text` ("s3:///nispero/text1") and `pattern` ("s3:///nispero/pattern1")
and one output object `output` ("s3:///nispero/output1").

## Task providers

Sometimes what you have as input is not a priori partitioned into tasks; to simplify this process [tasks providers](tasks-providers.md) can be used.

## Workers

*Instructions* are run on EC2 instances by a special application called *worker*.
This application prepares all the necessary environment for executing instructions: establishes connection to AWS services, retrieves tasks, publishes results. We will also call *workers* the instances that host the *worker* application.

## Manager

The *Manager* is a monitoring and management system for *nispero*. As in the case of *workers* we will call instances that host this application also *managers*.

## Console

The *Console* is a web interface for the *manager*. See [console documentation](console.md) for details.
