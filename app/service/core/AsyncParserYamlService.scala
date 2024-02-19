package service.core

import database.repo.Repository

import scala.concurrent.Future

trait AsyncParserYamlService[A] extends YamlService[A] {
  def repo: Repository[A, A, _]

  override def createOrUpdateMany(xs: Seq[A]): Future[Seq[A]] =
    repo.createOrUpdateMany(xs)

  override def all(): Future[Seq[A]] =
    repo.all()
}
