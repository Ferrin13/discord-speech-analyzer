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
  "com.sedmelluq" % "lavaplayer" % "1.3.16",
  "edu.cmu.sphinx" % "sphinx4-core" %"5prealpha-SNAPSHOT"
)