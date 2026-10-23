package models

import play.api.Configuration

final case class ModuleKeysToReview(pav: Set[String]) {
  def contains(key: String)    = isPAVReview(key)
  def isPAVReview(key: String) = pav.contains(key)
}

object ModuleKeysToReview {
  def load(configuration: Configuration): ModuleKeysToReview =
    ModuleKeysToReview(configuration.get[Seq[String]]("moduleKeysToReview.pav").toSet)
}
