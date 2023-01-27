package controllers.formats

import models.UserBranch
import play.api.libs.json.{Format, Json}

trait UserBranchFormat {
  implicit val userBranchFmt: Format[UserBranch] =
    Json.format[UserBranch]
}
