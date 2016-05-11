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

  def invoke(functionName: String, params: Map[String, String]): String = {
    var invokeRequest =
      new InvokeRequest().
        withFunctionName(functionName).
        withInvocationType(InvocationType.RequestResponse)
    if (params.nonEmpty) {
      invokeRequest = invokeRequest.withPayload(Json.toJson(params).toString())
    }
    val result = blockingClient.invoke(invokeRequest)
    resultStringFor(result.getPayload)
  }

  val requireRegex = """.*require\(['"]\s*(\S+)\s*['"]\).*""".r

  private def requiredModulesIn(code: String): Iterator[String] = {
    requireRegex.findAllMatchIn(code).flatMap(_.subgroups.headOption)
  }

  private def nodeCodeFor(code: String, params: Array[String], behavior: Behavior): String = {
    var fixedCode = "[“”]".r.replaceAllIn(code, "\"")
    "‘".r.replaceAllIn(fixedCode, "'")
    val paramsSupplied = params.indices.map(i => s"event.param$i")
    val withCallbacks = paramsSupplied ++ Array("onSuccess", "onError")
    val paramString = withCallbacks.mkString(", ")
    s"""
      |exports.handler = function(event, context, callback) {
      |   var Ellipsis = {};
      |   Ellipsis.teamId = "${behavior.team.id}";
      |   Ellipsis.db = {};
      |   Ellipsis.db.itemsTable = "${AWSDynamoDBConstants.ITEMS_TABLE_NAME}";
      |   Ellipsis.db.putItemUrl = "https://05f7c2f1.ngrok.io/put_item";
      |   Ellipsis.db.getItemUrl = "https://05f7c2f1.ngrok.io/get_item";
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
