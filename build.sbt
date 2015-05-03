name := "flowlib"

organization := "com.bgsig"

versionWithGit

git.baseVersion := "0.9"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.1" % "test"

libraryDependencies += "org.scalaz" %% "scalaz-concurrent" % "7.1.0" % "test"

publishLocal := { 
  def depends = 
    s""""${organization.value}" %% "${name.value}" % "${version.value}""""
  IO.write(file("dependency"), s"libraryDependencies += $depends\n")
  publishLocal.value 
}
