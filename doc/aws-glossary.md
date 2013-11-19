# AWS glossary

We will often refer here to some notions that are specific to Amazon Web Services (AWS).
Here you will find a small glossary, for getting more detailed information visit the [AWS documentation website](http://aws.amazon.com/documentation).

## AWS services

Amazon provide a lot of different web services, but *nispero* uses only a few of them:

* **AWS EC2** — provides virtual machines (called _instances_), which you can run under virtually any operating system.
* **AWS Auto scaling** — provides flexible scenarios for launching and managing a number of instances with _identical configuration_.
* **AWS S3** — a cloud storage service
* **AWS SQS** — a queue service
* **AWS SNS** — a notification service
* **AWS DynamoDB** — simple NoSQL database; more concretely, a key-value store

### EC2 and auto scaling

* **instance** — virtual machine at Amazon cloud
* **instance type** — hardware configuration of the instance (see http://aws.amazon.com/ec2/instance-types/)
* **auto scaling group** — set of instances with the same configuration. Its number can be managed according to different metrics; the Auto scaling service tries to keep the number of instances in the group constant (the **desired size** parameter). This means that if one of instance will terminated, another one will launched automatically.*Nispero* launches every instance as a member of an auto scaling group, which together with the immutable specification of behaviour makes *nispero* pretty fault-tolerant with respect to instance failures.
* **key pair** — keys used for establishing `ssh` connection to EC2 instances


### S3

* **object** — essentially a file stored in the Amazon Cloud
* **bucket** — a container for objects

### SQS

* **queue** — temporary storage with queue-like put/pull semantics
* **message** — an element of queue
* **visibility timeout** — time period within which a received message will be invisible to all other queue clients. See [visibility-timeout](visibility-timeout.md).

### SNS

* **topic** — endpoint for delivering notifications, you can subscribe to it a SQS queue, a e-mail account, a phone number...

### DynamoDB

* **table** — the basic unit of that contains items
* **item** - a list of key-values
