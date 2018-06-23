import sbt.Keys.sourceManaged
import sbt.{file, _}
import sbtprotoc.ProtocPlugin.autoImport.PB

object ProtobufUtil {
  /**
    * This method sets up where are the .proto files can be found for the projects and the
    * related params (like what would be the language to apply)
    *
    * Usable example is in the project:
    *   https://github.com/scalapb/ScalaPB/tree/master/examples
    * Documentation:
    *   https://trueaccord.github.io/ScalaPB/sbt-settings.html
    *
    * @param projectFolder
    * @param forJava
    * @param forServer
    * @return
    */
  def scalapbSettings(projectFolder: String, forJava: Boolean = false, forServer: Boolean = false) = {

    val f = file(s"$projectFolder/src/main/protobuf")

    require(f.exists(), s"The specified folder dir is not exists! [$projectFolder]")
    require(f.isDirectory, s"The specified path is not a folder! [$projectFolder]")

    val protoSources = PB.protoSources in Compile := Seq(file(s"$projectFolder/src/main/protobuf"))
    val pVersion = PB.protocVersion := "-v300"

    val pbgen = forJava match {
      case true =>
        PB.targets in Compile := {
          Seq(
            scalapb.gen(javaConversions = true, grpc = forServer, singleLineToProtoString = true) -> (sourceManaged in Compile).value,
            PB.gens.java("3.3.1") -> (sourceManaged in Compile).value
          )
        }
      case false =>
        PB.targets in Compile := {
          Seq( scalapb.gen(javaConversions = false, grpc = forServer, singleLineToProtoString = true) -> (sourceManaged in Compile).value )
        }
    }

    Seq(pVersion,protoSources).:+(pbgen)
  }
}
