package database.repo

import database.table.ResponsibilityType.{ModuleManagement, Lecturer}
import database.table._
import git.GitFilePath
import parsing.types.ModuleRelation.{Child, Parent}
import parsing.types._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class MetadataRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    private implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  type GetResult = (Metadata, GitFilePath)

  type AddResult = (
      MetadataDbEntry,
      List[ResponsibilityDbEntry],
      List[AssessmentMethodMetadataDbEntry]
  )

  private val metadataTable = TableQuery[MetadataTable]

  private val responsibilityTable = TableQuery[ResponsibilityTable]

  private val personTable = TableQuery[PersonTable]

  private val assessmentMethodMetadataTable =
    TableQuery[AssessmentMethodMetadataTable]

  private val assessmentMethodTable =
    TableQuery[AssessmentMethodTable]

  def all(): Future[Seq[GetResult]] =
    fetchWithDependencies(metadataTable)

  def delete(path: GitFilePath): Future[Unit] = {
    def go(
        query: Query[MetadataTable, MetadataDbEntry, Seq],
        m: MetadataDbEntry
    ) =
      DBIO
        .seq(
          assessmentMethodMetadataTable.filter(_.metadata === m.id).delete,
          responsibilityTable.filter(_.metadata === m.id).delete,
          query.delete
        )
        .transactionally

    val query = metadataTable.filter(_.gitPath === path.value)

    db.run(
      for {
        exists <- query.result
        _ <-
          if (exists.isEmpty)
            DBIO.failed(
              new Throwable(s"no metadata with path ${path.value} found")
            )
          else if (exists.size > 1)
            DBIO.failed(
              new Throwable(
                s"more than one metadata objects with path ${path.value} are found"
              )
            )
          else go(query, exists.head)
      } yield ()
    )
  }

  def update(m: Metadata, path: GitFilePath): Future[AddResult] = {
    def go(query: Query[MetadataTable, MetadataDbEntry, Seq]) = {
      val mdb = toMetadataDbEntry(m, path)
      val rdb = responsibilityDbEntries(m)
      val adb = assessmentMethodMetadataDbEntries(m)

      (
        for {
          _ <- query.update(mdb)
          _ <- DBIO.seq(
            rdb.map { rdb =>
              for {
                _ <- responsibilityTable
                  .filter(_.metadata === rdb.metadata)
                  .delete
                _ <- responsibilityTable += rdb
              } yield ()
            }: _*
          )
          _ <- DBIO.seq(
            adb.map { adb =>
              for {
                _ <- assessmentMethodMetadataTable
                  .filter(_.metadata === adb.metadata)
                  .delete
                _ <- assessmentMethodMetadataTable += adb
              } yield ()
            }: _*
          )
        } yield (mdb, rdb, adb)
      ).transactionally
    }

    val query = existsQuery(m)

    db.run(
      for {
        exists <- query.result
        res <-
          if (exists.isEmpty)
            DBIO.failed(new Throwable(s"no metadata with id ${m.id} found"))
          else if (exists.size > 1)
            DBIO.failed(
              new Throwable(
                s"more than one metadata objects with id ${m.id} are found"
              )
            )
          else go(query)
      } yield res
    )
  }

  def create(
      m: Metadata,
      path: GitFilePath
  ): Future[AddResult] = {
    def go() = {
      val mdb = toMetadataDbEntry(m, path)
      val rdb = responsibilityDbEntries(m)
      val adb = assessmentMethodMetadataDbEntries(m)

      (
        for {
          a <- metadataTable returning metadataTable += mdb
          b <- DBIO.sequence(
            rdb.map(responsibilityTable returning responsibilityTable += _)
          )
          c <- DBIO.sequence(
            adb.map(
              assessmentMethodMetadataTable returning assessmentMethodMetadataTable += _
            )
          )
        } yield (a, b, c)
      ).transactionally
    }

    def alreadyExists() =
      DBIO.failed(new Throwable(s"metadata with id ${m.id} already exists"))

    db.run(
      for {
        exists <- existsQuery(m).result
        res <-
          if (exists.isEmpty) go()
          else alreadyExists()
      } yield res
    )
  }

  private def fetchWithDependencies(
      query: Query[MetadataTable, MetadataDbEntry, Seq]
  ): Future[Seq[GetResult]] = {
    val baseQ = for {
      ((((q, r), p), ap), am) <- query
        .joinLeft(responsibilityTable)
        .on(_.id === _.metadata)
        .joinLeft(personTable)
        .on((l, r) => l._2.map(_.person === r.abbrev).getOrElse(false))
        .joinLeft(assessmentMethodMetadataTable)
        .on(_._1._1.id === _.metadata)
        .filter(_._2.isDefined)
        .joinLeft(assessmentMethodTable)
        .on((l, r) =>
          l._2.map(_.assessmentMethod === r.abbrev).getOrElse(false)
        )
      mt <- q.moduleTypeFk
      lang <- q.languageFk
      se <- q.seasonFk
      st <- q.statusFk
      loc <- q.locationFk
    } yield ((q, mt, lang, se, st, loc), r, p, ap, am)

    val action = baseQ.result.map(_.groupBy(_._1._1.id).map { case (_, xs) =>
      val metadata = xs.head._1
      val resp = xs.flatMap(_._2)
      val ps = xs.flatMap(_._3)
      val (coord, lec) = resp.zip(ps).partitionMap { case (r, p) =>
        assert(r.person == p.abbrev) // TODO
        Either.cond(r.kind == ModuleManagement, p, p)
      }
      val aps = xs.flatMap(_._4)
      val ams = xs.flatMap(_._5)
      val amps = aps.zip(ams).map { a =>
        assert(a._1.assessmentMethod == a._2.abbrev) // TODO
        AssessmentMethodPercentage(a._2, a._1.percentage)
      }
      (
        toMetadata(metadata, coord.distinct, lec.distinct, amps.distinct),
        GitFilePath(metadata._1.gitPath)
      )
    }.toSeq)

    db.run(action.transactionally)
  }

  def exists(m: Metadata): Future[Boolean] =
    db.run(existsQuery(m).result.map(_.nonEmpty))

  private def existsQuery(
      m: Metadata
  ): Query[MetadataTable, MetadataDbEntry, Seq] =
    metadataTable.filter(_.id === m.id)

  private def toMetadataDbEntry(m: Metadata, path: GitFilePath) = {
    val (children, parent) = fromRelation(m.relation)
    MetadataDbEntry(
      m.id,
      path.value,
      m.title,
      m.abbrev,
      m.kind.abbrev,
      children,
      parent,
      m.credits.value,
      m.language.abbrev,
      m.duration,
      m.recommendedSemester,
      m.frequency.abbrev,
      m.workload.lecture,
      m.workload.seminar,
      m.workload.practical,
      m.workload.exercise,
      m.workload.projectSupervision,
      m.workload.projectWork,
      fromList(m.recommendedPrerequisites),
      fromList(m.requiredPrerequisites),
      m.status.abbrev,
      m.location.abbrev,
      fromList(m.po)
    )
  }

  private def toList(s: String): List[String] =
    if (s.isEmpty) Nil
    else s.split(",").toList

  private def fromList(xs: List[String]): String =
    xs.mkString(",")

  private def assessmentMethodMetadataDbEntries(
      m: Metadata
  ): List[AssessmentMethodMetadataDbEntry] =
    m.assessmentMethods.map(a =>
      AssessmentMethodMetadataDbEntry(
        m.id,
        a.assessmentMethod.abbrev,
        a.percentage
      )
    )

  private def responsibilityDbEntries(
      m: Metadata
  ): List[ResponsibilityDbEntry] = {
    val lecturers = m.responsibilities.lecturers.map(p =>
      ResponsibilityDbEntry(m.id, p.abbrev, Lecturer)
    )
    val coordinator = m.responsibilities.moduleManagement.map(p =>
      ResponsibilityDbEntry(m.id, p.abbrev, ModuleManagement)
    )
    lecturers ::: coordinator
  }

  private def toRelation(
      children: Option[String],
      parent: Option[String]
  ): Option[ModuleRelation] =
    children.map(toList _ andThen Parent.apply) orElse
      parent.map(Child.apply)

  private def toMetadata: (
      (MetadataDbEntry, ModuleType, Language, Season, Status, Location),
      Seq[Person],
      Seq[Person],
      Seq[AssessmentMethodPercentage]
  ) => Metadata = { case ((m, mt, lang, se, st, loc), coord, lec, amps) =>
    Metadata(
      m.id,
      m.title,
      m.abbrev,
      mt,
      toRelation(m.children, m.parent),
      ECTS(m.credits, Nil),
      lang,
      m.duration,
      m.recommendedSemester,
      se,
      Responsibilities(coord.toList, lec.toList),
      amps.toList,
      Workload(
        m.workloadLecture,
        m.workloadSeminar,
        m.workloadPractical,
        m.workloadExercise,
        m.workloadProjectSupervision,
        m.workloadProjectWork
      ),
      toList(m.recommendedPrerequisites),
      toList(m.requiredPrerequisites),
      st,
      loc,
      toList(m.po)
    )
  }

  private def fromRelation(
      relation: Option[ModuleRelation]
  ): (Option[String], Option[String]) =
    relation match {
      case Some(r) =>
        r match {
          case Parent(children) =>
            (Some(fromList(children)), Option.empty[String])
          case Child(parent) =>
            (Option.empty[String], Some(parent))
        }
      case None =>
        (Option.empty[String], Option.empty[String])
    }
}
