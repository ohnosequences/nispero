## Nispero environment setup for Windows 7 or higher

#### Requirements

* AWS account
* keys for your AWS account, saved in a properties file under the path "~/credentials.nispero" (for example `C:\Users\Administrator\credentials`):

```
accessKey = <your access key>
#for example: accessKey = AKIAIG23IDH2AEPBEFVA

secretKey = <your secret key>
#for example: secretKey = AZpGhgq6i4+m+TRXJ0W8nYmRJY3ejr5p5DQULTci
```

#### Java

Go to http://java.com/en/download/index.jsp and download installation package for your system and launch it. Once the installation finishes ensure that you have the `java` command in your path. For this open "Command Prompt" and type:

```bash
java
```

If you see a message like this:

```
'java` is not recognized as an internal or external command,
operable program or batch file.
```

you need add the directory with your Java (usually it is stored at: "C:\Program Files (x86)\Java\jre7\bin" or "C:\Program Files\Java\jre7\bin") as part of the `PATH`.

For that go to "My Computer" context menu -> "Properties" -> "Change Settings" -> "Advanced" -> "Environment Variables" and there look for the variable "PATH" and add to it ";<path to java>" (for example ";C:\Program Files (x86)\Java\jre7\bin").

Reopen your terminal.

#### install SBT

Go to http://www.scala-sbt.org/0.13/tutorial/Installing-sbt-on-Windows.html and install the MSI package from there.

#### install conscript

* go to https://github.com/n8han/conscript 
* then download and run "conscript runnable jar"
* add ";C:\Users\%USERNAME%\bin" to you PATH variable (see Java section)

#### install nispero command line

```
#### install nispero command line

```
cs ohnosequences/nisperoCLI/<version>
```

for example

```
cs ohnosequences/nisperoCLI/v2.1.0
```

```


