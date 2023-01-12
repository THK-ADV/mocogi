package controllers.json

import database.table.UserBranch
import play.api.libs.json.{Format, Json}

trait UserBranchFormat {
  implicit val userBranchFmt: Format[UserBranch] =
    Json.format[UserBranch]
}
