package controllers

import play.api.Logging
import play.api.libs.json.JsString
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}

@Singleton
class GitWebhookController @Inject() (cc: ControllerComponents)
    extends AbstractController(cc)
    with Logging {

  def onPushEvent() = Action(parse.json) { r =>
    logger.debug(r.body.toString())
    logger.info(r.body.toString())
    println(r.body.toString())
    logger.debug(r.headers.toString())
    logger.info(r.headers.toString())
    println(r.headers.toString())
    Ok(JsString("Alles gucci"))
  }
}
