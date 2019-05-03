name := "CS436FinalProject"

version := "0.1"

scalaVersion := "2.12.8"

resolvers ++= Seq(
  "Dv8tion releases" at "http://jcenter.bintray.com",
  "Shpinx4 releases" at "https://oss.sonatype.org/content/repositories/snapshots"
)

libraryDependencies ++= Seq(
  "net.dv8tion" % "JDA" % "3.8.3_462",
  "org.slf4j" % "slf4j-simple" % "1.6.4",
  "org.apache.commons" % "commons-io" % "1.3.2",
  "com.google.cloud" % "google-cloud-speech" % "0.80.0-beta",
  "com.google.cloud" % "google-cloud-storage" % "1.70.0",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.9.2",
  "com.typesafe" % "config" % "1.3.4"
)