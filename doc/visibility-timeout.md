# Visibility timeout

To deliver tasks to instructions *nispero* uses Amazon SQS queues.

There are three basic operations:

* put message to queue
* receive message/messages from queue
* delete message from queue

SQS is designed so as to be possible to access one queue from a big amount of clients. For example, in *nispero* system all *workers* periodically try to receive new messages with tasks from the same queue.
To prevent all possible concurrency issues Amazon SQS uses a very simple feature — a *visibility timeout*. After receiving a message it becomes invisible for all further requests during this *visibility timeout*.

## Message visibility timeouts in *nispero*

*Nispero* uses visibility timeouts for the *input queue* (queue for input tasks); once a *worker* starts processing a new task it becomes invisible for all other *workers*. To prevent issues with a message reappearing due to visibility timeout even if the processing is going as expected, every worker continuously extends this timeout. So it won't appear again until this *worker* finish processing or stops/fails.

Worker will stop extending this timeout in two cases:

* execution of task is finished
* *taskProcessTimeout* is exceeded
