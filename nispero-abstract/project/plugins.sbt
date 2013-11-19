resolvers ++= Seq(
  "Era7 Releases"       at "http://releases.era7.com.s3.amazonaws.com",
  "Era7 Snapshots"      at "http://snapshots.era7.com.s3.amazonaws.com"
)

addSbtPlugin("ohnosequences" % "nice-sbt-settings" % "0.3.1")

