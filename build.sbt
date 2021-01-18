import scalariform.formatter.preferences._
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

val scala212 = "2.12.10"
val scala213 = "2.13.1"


resolvers in Global += Resolver.url("upstartcommerce", url("https://upstartcommerce.bintray.com/nochannel"))(Resolver.ivyStylePatterns)

lazy val bintraySettings: Seq[Setting[_]] = Seq(
  bintrayOmitLicense := true,
  bintrayOrganization := Some("upstartcommerce"),
  bintrayRepository := "generic",
  bintrayReleaseOnPublish in ThisBuild := false,
  publishMavenStyle := false)

lazy val notPublishSettings = Seq(
  publishArtifact := false,
  skip in publish := true,
  bintrayOmitLicense := false,
  bintrayRelease := {},
  publishLocal := {},
  publish := {}
)

lazy val commonSettings = Seq(
  organization := "com.upstartcommerce",
  scalaVersion := scala212,
  version := "4.0.4",
  description := "Json diff/patch library",
  licenses ++= Seq("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"), ("Apache-2.0", url("https://bintray.com/upstartcommerce"))),
  homepage := Some(url("https://github.com/gnieh/diffson")),
  parallelExecution := false,
  scalacOptions ++= PartialFunction.condOpt(CrossVersion.partialVersion(scalaVersion.value)) {
    case Some((2, n)) if n >= 13 =>
      Seq(
        "-Ymacro-annotations"
      )
  }.toList.flatten,
  libraryDependencies ++=
    (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, v)) if v <= 12 =>
        Seq(
          compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
        )
      case _ =>
        // if scala 2.13.0 or later, macro annotations merged into scala-reflect
        Nil
    }),
  addCompilerPlugin("org.typelevel" % "kind-projector" % "0.10.3" cross CrossVersion.binary),
  scalariformAutoformat := true,
  scalariformPreferences := {
    scalariformPreferences.value
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentConstructorArguments, true)
      .setPreference(MultilineScaladocCommentsStartOnFirstLine, true)
  },
  coverageExcludedPackages := "<empty>;.*Test.*",
  scalacOptions in (Compile,doc) ++= Seq("-groups", "-implicits"),
  autoAPIMappings := true,
  scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")) ++ Seq(
    scalariformPreferences := {
    scalariformPreferences.value
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentConstructorArguments, true)
      .setPreference(MultilineScaladocCommentsStartOnFirstLine, true)
      .setPreference(DanglingCloseParenthesis, Prevent)
    })

lazy val diffson = project.in(file("."))
  .enablePlugins(ScoverageSbtPlugin)
  .settings(commonSettings: _*)
  .settings(notPublishSettings: _*)
  .settings(
    name := "diffson",
    packagedArtifacts := Map(),
  )
  .aggregate(core.jvm, core.js, sprayJson, circe.jvm, circe.js, playJson.jvm, playJson.js, testkit.jvm, testkit.js)

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure).in(file("core"))
  .enablePlugins(ScoverageSbtPlugin, ScalaUnidocPlugin)
  .settings(commonSettings: _*)
  .settings(bintraySettings)
  .settings(
    name := "diffson-core",
    crossScalaVersions := Seq(scala212, scala213),
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %%% "scala-collection-compat" % "2.1.4",
      "org.typelevel"  %%% "cats-core"  % "2.1.1",
      "io.estatico"    %%% "newtype"    % "0.4.3",
      "org.scalatest"  %%% "scalatest"  % "3.1.1" % Test,
      "org.scalacheck" %%% "scalacheck" % "1.14.3"      % Test
    ),
  )
  .jsSettings(coverageEnabled := false)

lazy val testkit = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full).in(file("testkit"))
  .enablePlugins(ScoverageSbtPlugin)
  .settings(commonSettings: _*)
  .settings(notPublishSettings: _*)
  .settings(
    name := "diffson-testkit",
    crossScalaVersions := Seq(scala212, scala213),
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest" % "3.1.1",
      "org.scalacheck" %%% "scalacheck" % "1.14.3")
  )
  .jsSettings(coverageEnabled := false)
  .dependsOn(core)

lazy val sprayJson = project.in(file("sprayJson"))
  .enablePlugins(ScoverageSbtPlugin)
  .settings(commonSettings: _*)
  .settings(notPublishSettings: _*)
  .settings(
    name := "diffson-spray-json",
    crossScalaVersions := Seq(scala212, scala213),
    libraryDependencies += "io.spray" %%  "spray-json" % "1.3.5")
  .dependsOn(core.jvm, testkit.jvm % Test)

lazy val playJson = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full).in(file("playJson"))
  .enablePlugins(ScoverageSbtPlugin)
  .settings(commonSettings: _*)
  .settings(bintraySettings)
  .settings(
    name := "diffson-play-json",
    libraryDependencies += "com.typesafe.play" %%% "play-json" % "2.8.1",
    crossScalaVersions := Seq(scala212, scala213),
  )
  .jsSettings(coverageEnabled := false)
  .dependsOn(core, testkit % Test)

val circeVersion = "0.13.0"
lazy val circe = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full).in(file("circe"))
  .enablePlugins(ScoverageSbtPlugin)
  .settings(commonSettings: _*)
  .settings(notPublishSettings: _*)
  .settings(
    name := "diffson-circe",
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core"    % circeVersion,
      "io.circe" %%% "circe-parser"  % circeVersion,
      "io.circe" %%% "circe-generic" % circeVersion % Test
    ),
    crossScalaVersions := Seq(scala212, scala213))
  .jsSettings(
    coverageEnabled := false,
    libraryDependencies += "io.circe" %%% "not-java-time" % "0.2.0"
  )
  .dependsOn(core, testkit % Test)
