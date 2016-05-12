package services

import java.io.{File, PrintWriter, FileOutputStream}
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.file.{Files, Paths}
import java.util.zip.{ZipEntry, ZipOutputStream}
import javax.inject.Inject
import com.amazonaws.services.lambda.AWSLambdaClient
import com.amazonaws.services.lambda.model._
import models.bots.Behavior
import play.api.Configuration
import play.api.libs.json.Json
import scala.reflect.io.{Path}
import sys.process._


class AWSLambdaServiceImpl @Inject() (val configuration: Configuration) extends AWSLambdaService {

  val blockingClient: AWSLambdaClient = new AWSLambdaClient(credentials)

  private def resultStringFor(payload: ByteBuffer): String = {
    val bytes = payload.array
    val jsonString = new java.lang.String( bytes, Charset.forName("UTF-8") )
    val maybeResult = (Json.parse(jsonString) \ "result").toOption
    maybeResult.map(_.toString.replaceAll("^\"|\"$", "")).getOrElse {
      "Hmm. Looks like something is wrong with your script. Try `learn` again."
    }
  }

  val API_BASE_URL = "apiBaseUrl"
  def apiBaseUrl: String = configuration.getString("application.apiBaseUrl").get

  def invoke(functionName: String, params: Map[String, String]): String = {
    val paramsWithApiBaseUrl = params ++ Map(API_BASE_URL -> apiBaseUrl)
    val invokeRequest =
      new InvokeRequest().
        withFunctionName(functionName).
        withInvocationType(InvocationType.RequestResponse).
        withPayload(Json.toJson(paramsWithApiBaseUrl).toString())
    val result = blockingClient.invoke(invokeRequest)
    resultStringFor(result.getPayload)
  }

  val requireRegex = """.*require\(['"]\s*(\S+)\s*['"]\).*""".r

  val alreadyIncludedModules = Array("dynamodb-doc")

  private def requiredModulesIn(code: String): Array[String] = {
    requireRegex.findAllMatchIn(code).flatMap(_.subgroups.headOption).toArray.diff(alreadyIncludedModules)
  }

  private def nodeCodeFor(code: String, params: Array[String], behavior: Behavior): String = {
    var fixedCode = "[“”]".r.replaceAllIn(code, "\"")
    "‘".r.replaceAllIn(fixedCode, "'")
    val paramsSupplied = params.indices.map(i => s"event.param$i")
    val withBuiltins = paramsSupplied ++ Array("onSuccess", "onError", "context")
    val paramString = withBuiltins.mkString(", ")
    s"""
      |exports.handler = function(event, context, callback) {
      |
      |   var context = {};
      |   context.teamId = "${behavior.team.id}";
      |
      |   context.db = {};
      |   context.db.putItemUrl = event.$API_BASE_URL + "/put_item";
      |   context.db.getItemUrl = event.$API_BASE_URL + "/get_item";
      |
      |   var fn = $fixedCode;
      |   var onSuccess = function(result) { callback(null, { "result": result }); };
      |   var onError = function(err) { callback(err); };
      |   fn($paramString);
      |}
    """.stripMargin
  }

  private def dirNameFor(functionName: String) = s"/tmp/$functionName"
  private def zipFileNameFor(functionName: String) = s"${dirNameFor(functionName)}.zip"

  private def createZipWithModulesFor(behavior: Behavior, code: String, params: Array[String]): Unit = {
    val dirName = dirNameFor(behavior.functionName)
    val path = Path(dirName)
    path.createDirectory()

    val writer = new PrintWriter(new File(s"$dirName/index.js"))
    writer.write(nodeCodeFor(code, params, behavior))
    writer.close()

    requiredModulesIn(code).foreach { moduleName =>
      Process(Seq("bash","-c",s"cd $dirName && npm install $moduleName")).!
    }

    Process(Seq("bash","-c",s"cd $dirName && zip -r ${zipFileNameFor(behavior.functionName)} *")).!
  }

  private def getZipFor(behavior: Behavior, code: String, params: Array[String]): ByteBuffer = {
    createZipWithModulesFor(behavior, code, params)
    val path = Paths.get(zipFileNameFor(behavior.functionName))
    ByteBuffer.wrap(Files.readAllBytes(path))
  }

  def deleteFunction(functionName: String): Unit = {
    val deleteFunctionRequest =
      new DeleteFunctionRequest().withFunctionName(functionName)
    try {
      blockingClient.deleteFunction(deleteFunctionRequest)
    } catch {
      case e: ResourceNotFoundException => Unit
    }
  }

  def deployFunctionFor(behavior: Behavior, code: String, params: Array[String]): Unit = {
    val functionName = behavior.functionName
    deleteFunction(functionName)
    val functionCode =
      new FunctionCode().
        withZipFile(getZipFor(behavior, code, params))
    val createFunctionRequest =
      new CreateFunctionRequest().
        withFunctionName(functionName).
        withCode(functionCode).
        withRole(configuration.getString("aws.role").get).
        withRuntime(com.amazonaws.services.lambda.model.Runtime.Nodejs43).
        withHandler("index.handler").
        withTimeout(10)

    blockingClient.createFunction(createFunctionRequest)
  }
}
