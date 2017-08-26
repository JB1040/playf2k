name := """F2k"""

version := "1.0-SNAPSHOT"
lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.12.2"

libraryDependencies += guice


// Test Database
libraryDependencies += javaJdbc
libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.41"
libraryDependencies += javaJpa
libraryDependencies += "com.h2database" % "h2" % "1.4.194"
libraryDependencies += "org.hibernate" % "hibernate-core" % "5.2.5.Final"
libraryDependencies ++= Seq(javaWs)
libraryDependencies += javaWs % "test"
libraryDependencies += "org.mockito" % "mockito-core" % "2.1.0" % "test"

// Testing libraries for dealing with CompletionStage...
libraryDependencies += "org.assertj" % "assertj-core" % "3.6.2" % Test
libraryDependencies += "org.awaitility" % "awaitility" % "2.0.0" % Test

// Make verbose tests
testOptions in Test := Seq(Tests.Argument(TestFrameworks.JUnit, "-a", "-v"))

EclipseKeys.preTasks := Seq(compile in Compile, compile in Test)
PlayKeys.externalizeResources := false