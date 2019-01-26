organization in ThisBuild := "com.github.dmgcodevil"
scalaVersion in ThisBuild := "2.12.8"


lazy val annotations = project.settings(
  name := "closable-annotations",
)

lazy val plugin = project.settings(
  name := "closable-compiler-plugin",
  libraryDependencies += "org.scala-lang" % "scala-compiler" % "2.12.8"
)

lazy val example = project.settings(
  skip in publish := true,
  autoCompilerPlugins := true,
  libraryDependencies += compilerPlugin("com.github.dmgcodevil" %% "closable-compiler-plugin" % version.value),
  libraryDependencies += "com.github.dmgcodevil" %% "closable-annotations" % version.value,
  libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.25",
  libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.25",
  mainClass := Some("com.github.dmgcodevil.closable.example.App")
)