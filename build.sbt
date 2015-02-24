name := "recaptcha-v2-example"

version := "1.0"    

libraryDependencies ++= Seq(
  "com.nappin" %% "play-recaptcha" % "1.0" 
)

scalaVersion := "2.10.4"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

// Enable this if using a snapshot release of play-recaptcha
//resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
