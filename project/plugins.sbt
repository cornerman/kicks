addSbtPlugin("org.typelevel"    % "sbt-tpolecat" % "0.5.0")
addSbtPlugin("org.scalameta"    % "sbt-scalafmt" % "2.5.2")
addSbtPlugin("com.timushev.sbt" % "sbt-rewarn"   % "0.1.3")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")
addSbtPlugin("io.spray"     % "sbt-revolver" % "0.10.0")

addSbtPlugin("com.disneystreaming.smithy4s" % "smithy4s-sbt-codegen" % "0.18.5")
addSbtPlugin("com.github.cornerman"         % "sbt-quillcodegen"     % "0.1.5")

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.0.0")
addSbtPlugin("org.scala-js"       % "sbt-scalajs"              % "1.12.0")

//TODO: new release after https://github.com/ScalablyTyped/Converter/issues/583
addSbtPlugin("org.scalablytyped.converter" % "sbt-converter" % "1.0.0-beta43+13-92885d6d-SNAPSHOT")

ThisBuild / resolvers ++= Seq(
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  "Sonatype OSS Snapshots S01" at "https://s01.oss.sonatype.org/content/repositories/snapshots",
)
