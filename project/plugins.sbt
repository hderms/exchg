resolvers ++= Seq(
  Classpaths.sbtPluginReleases,
  Resolver.url("sbt-scoverage repo", url("https://dl.bintray.com/sksamuel/sbt-plugins"))(Resolver.ivyStylePatterns),
  Classpaths.typesafeResolver,
  Resolver.typesafeRepo("releases")
)

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.1")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.7.0")


addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.1")
//For formatting
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")

addCompilerPlugin("org.psywerx.hairyfotr" %% "linter" % "0.1.14")
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")
addSbtPlugin("com.sksamuel.scapegoat" %% "sbt-scapegoat" % "1.0.4")
