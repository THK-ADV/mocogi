package controllers

import play.api.mvc.AbstractController

trait SimpleYamlController[A] extends YamlController[A, A] {
  self: AbstractController =>

}
