name := "exchg"

organization := "com.github.hderms"

version := "0.1"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
    "org.scalatest"   %% "scalatest"    % "2.2.4"   % "test,it",
    "org.scalacheck"  %% "scalacheck"   % "1.12.5"      % "test,it",
    "com.typesafe.akka" %% "akka-persistence" % "2.4.11",
    "org.iq80.leveldb"            % "leveldb"          % "0.7",
    "org.fusesource.leveldbjni"   % "leveldbjni-all"   % "1.8",
"com.github.nikita-volkov" % "sext" % "0.2.4"
)

scalacOptions ++= List("-feature","-deprecation", "-unchecked", "-Xlint")

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-l", "org.scalatest.tags.Slow", "-u","target/junit-xml-reports", "-oD", "-eS")


