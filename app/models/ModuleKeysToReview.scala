package models

case class ModuleKeysToReview(sgl: Set[String], pav: Set[String]) {
  def contains(key: String) = isSGLReview(key) || isPAVReview(key)

  def isSGLReview(key: String) = sgl.contains(key)

  def isPAVReview(key: String) = pav.contains(key)

  // TODO
//  def keyFromRole(key: String, role: UniversityRole) =
//    keysForRole(role).contains(key)
//
//  private def keysForRole(role: UniversityRole) =
//    role match {
//      case UniversityRole.SGL => sgl
//      case UniversityRole.PAV => pav
//    }
}
