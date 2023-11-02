package models

case class ModuleKeysToReview(sgl: Set[String], pav: Set[String]) {
  def contains(key: String) = isSGLReview(key) || isPAVReview(key)

  def isSGLReview(key: String) = sgl.contains(key)

  def isPAVReview(key: String) = pav.contains(key)
}
