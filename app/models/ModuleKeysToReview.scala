package models

case class ModuleKeysToReview(sgl: Set[String], pav: Set[String]) {
  def contains(key: String) = sgl.contains(key) || pav.contains(key)
  def isSGLReview(keys: Set[String]) = keys.exists(sgl.contains)
  def isPAVReview(keys: Set[String]) = keys.exists(pav.contains)
}
