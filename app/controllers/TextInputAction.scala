package controllers

import play.api.mvc.{AbstractController, Result}

import java.nio.charset.StandardCharsets
import scala.concurrent.Future

trait TextInputAction { self: AbstractController =>

  def textInputAction(f: String => Future[Result]) = // TODO use this
    Action(parse.byteString).async { r =>
      val input = r.body.decodeString(StandardCharsets.UTF_8)
      f(input)
    }
}
