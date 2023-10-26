package controllers.formats

import models.UniversityRole
import play.api.libs.json.Format

trait ModuleReviewerRoleFormat {
  implicit val moduleReviewerRoleFormat: Format[UniversityRole] =
    Format.of[String].bimap(UniversityRole.apply, _.id)
}
