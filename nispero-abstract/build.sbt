Nice.scalaProject

name := "nispero-abstract"

description := "nispero abstract code"

organization := "ohnosequences"

libraryDependencies += "ohnosequences" % "aws-scala-tools_2.10" % "0.4.2"

libraryDependencies += "commons-io" % "commons-io" % "2.4"

libraryDependencies += "net.liftweb" % "lift-json_2.10" % "2.5"

libraryDependencies += "ohnosequences" % "statika_2.10" % "1.0.0"

libraryDependencies += "ohnosequences" % "aws-statika_2.10" % "1.0.0"

libraryDependencies += "com.bacoder.jgit" % "org.eclipse.jgit" % "3.1.0-201309071158-r"

libraryDependencies += "org.scala-sbt" % "launcher-interface" % "0.13.0" % "provided"

libraryDependencies += "ohnosequences" % "amazon-linux-ami_2.10" % "0.14.0"

libraryDependencies += "net.databinder" %% "unfiltered-filter" % "0.6.5"

libraryDependencies += "net.databinder" %% "unfiltered-jetty" % "0.6.5"

libraryDependencies += "org.clapper" %% "avsl" % "1.0.1"

dependencyOverrides += "commons-codec" % "commons-codec" % "1.6"

dependencyOverrides += "org.scala-lang" % "scala-compiler" % "2.10.3"


