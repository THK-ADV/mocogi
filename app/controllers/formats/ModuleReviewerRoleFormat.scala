package controllers.formats

import models.ModuleReviewerRole
import play.api.libs.json.Format

trait ModuleReviewerRoleFormat {
  implicit val moduleReviewerRoleFormat: Format[ModuleReviewerRole] =
    Format.of[String].bimap(ModuleReviewerRole.apply, _.id)
}
