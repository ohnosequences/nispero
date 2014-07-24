## Nispero environment setup for Amazon Linux

> this instructions also can used for other linux distributions with Java pre-installed (a JRE).

#### Requirements

* an AWS account
* keys for your AWS account, saved in a properties file under the path "~/credentials.nispero" (for example `/home/ec2-user/credentials`):

```
accessKey = <your access key>
#for example: accessKey = DKIAIG23IDH2AEPBEFVA

secretKey = <your secret key>
#for example: secretKey = QZpGhgq6i4+m+TRXJ0W8nYmRJY3ejr5p5DQULTci
```

> you can use other location for credentials file, but in this case you will have to put it as last parameter to  the *nispero* command-line tool

#### install SBT

```bash
cd ~
curl http://dl.bintray.com/sbt/rpm/sbt-0.13.5.rpm > sbt.rpm
sudo yum install sbt.rpm -y
```

#### install conscript

```bash
curl https://raw.github.com/n8han/conscript/master/setup.sh | sh
cs n8han/giter8

```

#### install nispero command line

```
cs ohnosequences/nisperoCLI -b super-cli
```



