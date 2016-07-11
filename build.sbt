name := "recaptcha-v2-example"

version := "2.0"    

libraryDependencies ++= Seq(
  "com.nappin" %% "play-recaptcha" % "2.0" notTransitive(),
  ws,
  specs2 % Test,
  "org.mockito" % "mockito-core" % "1.+" % Test 
)

scalaVersion := "2.11.7"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

// needed for Play 2.4
//routesGenerator := InjectedRoutesGenerator

// Enable this if using a snapshot release of play-recaptcha
//resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
