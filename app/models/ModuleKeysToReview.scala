package models

case class ModuleKeysToReview(sgl: Set[String], pav: Set[String]) {
  def contains(key: String) = sgl.contains(key) || pav.contains(key)

  def isSGLReview(keys: Set[String]) = keys.exists(sgl.contains)

  def isPAVReview(keys: Set[String]) = keys.exists(pav.contains)

  def keyFromRole(key: String, role: UniversityRole) =
    keysForRole(role).contains(key)

  private def keysForRole(role: UniversityRole) =
    role match {
      case UniversityRole.SGL => sgl
      case UniversityRole.PAV => pav
    }
}
