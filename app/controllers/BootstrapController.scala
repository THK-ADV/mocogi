package controllers

import auth.AuthorizationAction
import controllers.actions.{AdminCheck, PermissionCheck}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.mvc.{AbstractController, ControllerComponents}
import slick.jdbc.JdbcProfile

import java.io.File
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.io.Source

@Singleton
final class BootstrapController @Inject() (
    cc: ControllerComponents,
    auth: AuthorizationAction,
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with HasDatabaseConfigProvider[JdbcProfile]
    with AdminCheck
    with PermissionCheck {

  import profile.api._

  def createViews() =
    auth andThen isAdmin async { _ =>
      val source = Source.fromFile(new File("conf/sql/views.sql"))
      val inserts = source.mkString
      source.close()
      db.run(sqlu"#$inserts").map(_ => NoContent)
    }
}
