name := "eventuate-learn"

version := "1.0"

scalaVersion := "2.11.8"

resolvers += "Eventuate Releases" at "https://dl.bintray.com/rbmhtechnology/maven"

libraryDependencies += "com.rbmhtechnology" %% "eventuate-core" % "0.8.1"

libraryDependencies += "com.rbmhtechnology" %% "eventuate-crdt" % "0.8.1"

libraryDependencies += "com.rbmhtechnology" %% "eventuate-log-leveldb" % "0.8.1"

libraryDependencies += "com.rbmhtechnology" %% "eventuate-log-cassandra" % "0.8.1"

libraryDependencies += "com.rbmhtechnology" %% "eventuate-adapter-stream" % "0.8.1"