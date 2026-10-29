package helper

import scala.concurrent.Future

import git.api.GitApiConfig
import org.apache.pekko.util.ByteString
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.JsValue
import play.api.libs.ws.writeableOf_WsBody
import play.api.libs.ws.BodyWritable
import play.api.libs.ws.EmptyBody
import play.api.libs.ws.WSClient
import play.api.libs.ws.WSRequest
import play.api.libs.ws.WSResponse
import settings.SecretString

final class GitHttpFixture extends MockitoSugar {
  val client     = mock[WSClient]
  val config     = GitApiConfig(SecretString.unsafe("test-token"), "https://git.example/api/v4", 42)
  val projectUrl = "https://git.example/api/v4/projects/42"

  def respond(url: String, status: Int, json: JsValue): WSRequest = {
    val response = mock[WSResponse]
    when(response.status).thenReturn(status)
    when(response.json).thenReturn(json)
    when(response.body).thenReturn(json.toString)
    request(url, response)
  }

  def file(url: String, status: Int, content: String): WSRequest = {
    val response = mock[WSResponse]
    when(response.status).thenReturn(status)
    when(response.bodyAsBytes).thenReturn(ByteString(content))
    when(response.header("X-Gitlab-Last-Commit-Id")).thenReturn(None)
    request(url, response)
  }

  private def request(url: String, response: WSResponse): WSRequest = {
    val request = mock[WSRequest](RETURNS_SELF)
    when(client.url(url)).thenReturn(request)
    when(request.get()).thenReturn(Future.successful(response))
    when(request.put(EmptyBody)).thenReturn(Future.successful(response))
    when(request.post(any[JsValue])(using any[BodyWritable[JsValue]])).thenReturn(Future.successful(response))
    request
  }
}
