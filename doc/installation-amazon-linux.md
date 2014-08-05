## Nispero environment setup for Amazon Linux

> this instructions also can used for other linux distributions with Java pre-installed (a JRE).

#### Requirements

* an AWS account
* keys for your AWS account, saved in a properties file under the path "~/nispero.credentials" (for example `/home/ec2-user/nispero.credentials`):

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
yum install -y http://dl.bintray.com/sbt/rpm/sbt-0.13.5.rpm
```

#### install conscript

```bash
wget https://raw.github.com/n8han/conscript/master/setup.sh
chmod +x ./setup.sh
./setup.sh
```

#### install nispero command line

```
cs ohnosequences/nisperoCLI/<version>
```

for example

```
cs ohnosequences/nisperoCLI/v2.1.0
```

