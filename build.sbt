name := "recaptcha-v2-example"

version := "2.5"

libraryDependencies ++= Seq(
  "com.nappin" %% "play-recaptcha" % "2.5" notTransitive(),
  guice,
  ws,
  specs2 % Test,
  "org.mockito" % "mockito-core" % "1.+" % Test 
)

scalaVersion := "2.13.6"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

// Enable this if using a snapshot release of play-recaptcha
//resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
