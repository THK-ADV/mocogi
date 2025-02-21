package controllers

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.io.Source

import auth.AuthorizationAction
import auth.Role.Admin
import controllers.actions.PermissionCheck
import controllers.actions.RoleCheck
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import slick.jdbc.JdbcProfile

@Singleton
final class BootstrapController @Inject() (
    cc: ControllerComponents,
    auth: AuthorizationAction,
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with HasDatabaseConfigProvider[JdbcProfile]
    with RoleCheck
    with PermissionCheck {

  import profile.api.*

  def createViews() =
    auth.andThen(hasRole(Admin)).async { _ =>
      val source  = Source.fromFile(new File("conf/sql/views.sql"))
      val inserts = source.mkString
      source.close()
      db.run(sqlu"#$inserts").map(_ => NoContent)
    }
}
