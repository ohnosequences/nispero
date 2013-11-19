# Tasks providers

Preparing tasks for *nispero* can be big deal: splitting files, uploading to S3, generating tasks definition file.
But many of these things can be atomized with *tasks providers*. `TasksProvider is simple interface with only one method:

```scala
def tasks(s3: S3): List[Task]
```

that will be called by *manager* during deploying *nispero*.
Every particular *tasks provider* should implement logic for generating and uploading tasks.
In most cases *tasks provider* split some input data into chunks, upload it to S3 and then generate *nispero* task
for every chunk.

## Build-in tasks provider
*Nispero* has several build-in tasks provider.

### Tasks list

If you already have definition in your tasks with uploaded input objects you can store task definition as JSON list
(see example [here](examples.md#task-list-of-text-search-problem))
and then you one of following tasks providers.

### S3Tasks

`S3Tasks` retrieve tasks from S3 object:

```scala
tasksProvider = new S3Tasks(ObjectAddress("team1-test-bucket", "small.fasta.tasks"))
```
This provider useful if you already have prepared tasks list definition uploaded to S3.


### ResourceTasks

Takes tasks from JVM resources (every file in directory `src/main/resources` become a JVM resource):

```scala
//"/taskslist" refers to "src/main/resources/taskslist"
tasksProvider = new ResourceTasks("/taskslist")
```
This provider useful if you have task list, but don't want to upload it.

### General purpose

#### EmptyTasks

This tasks provider upload empty task list definition. Remind that you always can upload tasks after launching of
*nispero* using [console](console.md#tasks).


#### Composing tasks provider
It is possible to compose tasks providers (concatenate tasks lists) using `~` method:

```scala
tasksProvider = (new S3Tasks(ObjectAddress("team1-test-bucket", "small1.fasta"))) ~ (new S3Tasks(ObjectAddress("team1-test-bucket", "small2.fasta")))
```


### Bioinformatics related tasks provider

#### FastaTasks

`FastaTasks` takes [FASTA](http://en.wikipedia.org/wiki/FASTA_format) file for input, split it to chunks by `n` reads, upload chunks to S3,
and generate task list based on `template`:


```scala
	tasksProvider = FastaTasks(
	  fasta = ObjectAddress("team1-test-bucket", "small.fasta"),
	  output = ObjectAddress("team1-test-bucket", "chunks/$counter$.fasta"),
	  n = 2,
	  template = 
"""
{
  "id": "task$counter$",
  "inputObjects": {
	"input1": {
	  "bucket": "team1-test-bucket",
	  "key": "chunks/$counter$.fasta"
	  }
  },
  "outputObjects": {
	"output1":{
	  "bucket": "team1-test-bucket",
	  "key": "results/$counter$.xml"
	}
  }
}
"""
	)
```

#### FastasTasks

`FastasTasks` takes "directory" with FASTA files in S3 and produce tasks for every FASTA file using `FastaTasks`
and then merge results to one task list. You can use `$sample$` variable in template
to distinguish chunks from different FASTA files:

```scala
	tasksProvider = FastasTasks(
	  fastaPrefix = ObjectAddress("team1-test-bucket", "reads/"),
	  output = ObjectAddress("team1-test-bucket", "chunks/$sample$_$counter$.fasta"),
	  n = 2,
	  template = 
"""
{
  "id": "task_$sample$_$counter$",
  "inputObjects": {
	"input1": {
	  "bucket": "team1-test-bucket",
	  "key": "chunks/$sample$_$counter$.fasta"
	  }
  },
  "outputObjects": {
	"output1":{
	  "bucket": "team1-test-bucket",
	  "key": "results/$sample$_$counter$.xml"
	}
  }
}
"""
	)
```

