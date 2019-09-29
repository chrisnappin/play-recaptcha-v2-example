// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository 
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.7.3")

// Shows a dependency graph, like maven (run "dependencyTree")
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.2")