package models

case class ModuleKeysToReview(pav: Set[String]) {
  def contains(key: String) = isPAVReview(key)
  def isPAVReview(key: String) = pav.contains(key)
}
