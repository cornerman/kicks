addSbtPlugin("org.scala-js"  % "sbt-scalajs"         % "1.14.0")
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.21.1")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.0.0")

addSbtPlugin("org.scalablytyped.converter" % "sbt-converter" % "1.0.0-beta43+4-ae10c353-SNAPSHOT")

addSbtPlugin("org.typelevel" % "sbt-tpolecat" % "0.5.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")

addSbtPlugin("com.eed3si9n"                 % "sbt-assembly"         % "0.14.10")
addSbtPlugin("com.disneystreaming.smithy4s" % "smithy4s-sbt-codegen" % "0.18.5")
addSbtPlugin("io.spray"                     % "sbt-revolver"         % "0.10.0")

addSbtPlugin("com.github.cornerman"                     % "sbt-quillcodegen"         % "0.1.1")

// for reading npmDependencies from package.json
//libraryDependencies ++= Seq("com.lihaoyi" %% "upickle" % "2.0.0")

resolvers ++= Seq(
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  "Sonatype OSS Snapshots S01" at "https://s01.oss.sonatype.org/content/repositories/snapshots", // https://central.sonatype.org/news/20210223_new-users-on-s01/
)
