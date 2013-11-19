![nispero](nispero.png)

## what?

*Nispero* gives you an easy way of scaling stateless computations using [Amazon Web Services](http://aws.amazon.com/).

## how?

To use *nispero* first of all you need an [Amazon AWS account](http://aws.amazon.com/);
*Nispero* will create and manage for you the infrastructure needed for executing your tasks.

You can start with these documents:

* [AWS glossary](aws-glossary.md) in case you're not familiar with AWS
* [nispero overview](overview.md) for a general introduction to *nispero*
* [nispero architecture](architecture.md) for a description of how *nispero* works

If you just want to start working with *nispero* read [nispero usage](usage.md).

### additional resources:

* [script executor](script-executor.md)
* [nispero configuration](config.md)
* [nispero console](console.md)
* [tasks providers](tasks-providers.md)
* [SQS visibility timeout](visibality-timeout.md)


## nispero design notes

A key property of *nispero* design is that it doesn't contain any state that is not managed through AWS: input tasks, results, *workers* amount — all this data is stored in Amazon AWS resources such as queues and S3 objects.

This makes *nispero* very safe and fault-tolerant: for example, in the case of *worker* or/and *manager* instance failure you don't lose any data. Another important feature of *nispero* is that it uses only auto scaling groups for launching instances.

