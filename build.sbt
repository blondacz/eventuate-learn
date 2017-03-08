name := "eventuate-learn"

version := "1.0"

scalaVersion := "2.11.8"

scalaVersion := "2.11.8"

resolvers += "Eventuate Releases" at "https://dl.bintray.com/rbmhtechnology/maven"
// for snapshots
//resolvers += "OJO Snapshots" at "https://oss.jfrog.org/oss-snapshot-local"

// for releases
resolvers += "OJO Releases" at "https://oss.jfrog.org/oss-release-local"

libraryDependencies += "com.rbmhtechnology" %% "eventuate-core" % "0.8.1"

libraryDependencies += "com.rbmhtechnology" %% "eventuate-crdt" % "0.8.1"

libraryDependencies += "com.rbmhtechnology" %% "eventuate-log-leveldb" % "0.8.1"

libraryDependencies += "com.rbmhtechnology" %% "eventuate-log-cassandra" % "0.8.1"

libraryDependencies += "com.rbmhtechnology" %% "eventuate-adapter-stream" % "0.8.1"
// https://mvnrepository.com/artifact/net.manub/scalatest-embedded-kafka_2.11
libraryDependencies += "net.manub" % "scalatest-embedded-kafka_2.11" % "0.11.0"
libraryDependencies += "com.typesafe.akka" %% "akka-stream-kafka" % "0.13"
libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.13.4"
libraryDependencies += "com.twitter" % "chill-akka_2.11" % "0.9.1"


libraryDependencies += "com.rbmhtechnology.eventuate-tools" %% "kamon-metrics" % "0.8.2"
libraryDependencies +=  "io.kamon" %% "kamon-log-reporter" % "0.6.3"
libraryDependencies +=  "io.kamon" %% "kamon-akka" % "0.6.3"
//libraryDependencies +=  "io.kamon" %% "kamon-system-metrics" % "0.6.3"
libraryDependencies += "org.aspectj" % "aspectjweaver" % "1.8.10"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.1"
libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.2.3"



// Bring the sbt-aspectj settings into this build
aspectjSettings

// Here we are effectively adding the `-javaagent` JVM startup
// option with the location of the AspectJ Weaver provided by
// the sbt-aspectj plugin.
javaOptions in run <++= AspectjKeys.weaverOptions in Aspectj

// We need to ensure that the JVM is forked for the
// AspectJ Weaver to kick in properly and do it's magic.
fork in run := true