// Comment out to get more information during initialization
logLevel := Level.Warn

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.8")

// Shows a dependency graph, like maven (run "dependencyTree")
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.2")