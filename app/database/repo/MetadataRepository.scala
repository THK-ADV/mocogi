package database.repo

import database.table._
import git.GitFilePath
import parsing.types._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import validator.{Metadata, Module, ModuleRelation, Workload}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class MetadataOutput(
    id: UUID,
    gitPath: String,
    title: String,
    abbrev: String,
    moduleType: String,
    ects: Double,
    language: String,
    duration: Int,
    season: String,
    workload: Workload,
    status: String,
    location: String,
    participants: Option[Participants],
    moduleRelation: Option[ModuleRelation]
)

trait MetadataRepository {
  def create(metadata: Metadata, path: GitFilePath): Future[Metadata]
  def all(): Future[Seq[MetadataOutput]]
  def allIdsAndAbbrevs(): Future[Seq[(UUID, String)]]
}

@Singleton
final class MetadataRepositoryImpl @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    private implicit val ctx: ExecutionContext
) extends MetadataRepository
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  type GetResult = (ParsedMetadata, GitFilePath)

  type AddResult = (
      MetadataDbEntry,
      List[ResponsibilityDbEntry],
      List[AssessmentMethodMetadataDbEntry]
  )

  private val metadataTable = TableQuery[MetadataTable]

  private val ectsFocusAreaContributionTable =
    TableQuery[ECTSFocusAreaContributionTable]

  private val moduleRelationTable =
    TableQuery[ModuleRelationTable]

  private val responsibilityTable = TableQuery[ResponsibilityTable]

  private val personTable = TableQuery[PersonTable]

  private val assessmentMethodMetadataTable =
    TableQuery[AssessmentMethodMetadataTable]

  private val assessmentMethodTable =
    TableQuery[AssessmentMethodTable]

  /*def all(): Future[Seq[GetResult]] =
    Future.successful(Nil)*/
  // fetchWithDependencies(metadataTable)

  def delete(path: GitFilePath): Future[Unit] = Future.unit
  /*{
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
  }*/

  def update(m: ParsedMetadata, path: GitFilePath): Future[AddResult] =
    Future.failed(new Throwable("currently unsupported"))
  /*
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
  }*/

  /*{
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
  }*/

  /*private def fetchWithDependencies(
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
  }*/

  def exists(m: ParsedMetadata): Future[Boolean] =
    Future.successful(true)

  // db.run(existsQuery(m).result.map(_.nonEmpty))

  /*  private def existsQuery(
      m: Metadata
  ): Query[MetadataTable, MetadataDbEntry, Seq] =
    metadataTable.filter(_.id === m.id)*/

  /*private def toMetadataDbEntry(m: Metadata, path: GitFilePath) = {
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
      m.frequency.abbrev,
      m.workload.lecture,
      m.workload.seminar,
      m.workload.practical,
      m.workload.exercise,
      m.workload.projectSupervision,
      m.workload.projectWork,
      fromList(
        m.recommendedPrerequisites.map(_.modules) getOrElse Nil
      ), // TODO use all fields
      fromList(
        m.requiredPrerequisites.map(_.modules) getOrElse Nil
      ), // TODO use all fields
      m.status.abbrev,
      m.location.abbrev,
      fromList(m.poMandatory.map(_.studyProgram)) // TODO
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
    m.assessmentMethodsMandatory.map(a =>
      AssessmentMethodMetadataDbEntry(
        m.id,
        a.method.abbrev,
        a.percentage // TODO use all fields
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
      se,
      Responsibilities(coord.toList, lec.toList),
      amps.toList.map(a => AssessmentMethodEntry(a.assessmentMethod, a.percentage, Nil)), // TODO use all fields
      Nil, // TODO add support
      Workload(
        m.workloadLecture,
        m.workloadSeminar,
        m.workloadPractical,
        m.workloadExercise,
        m.workloadProjectSupervision,
        m.workloadProjectWork
      ),
      Some(PrerequisiteEntry("", toList(m.recommendedPrerequisites), Nil)),
      Some(PrerequisiteEntry("", toList(m.requiredPrerequisites), Nil)),
      st,
      loc,
      toList(m.poMandatory).map(po => POMandatory(po, Nil, Nil)), // TODO
      Nil,
      None,
      Nil,
      Nil,
      Nil
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
   */

  /*
  responsibilities: Responsibilities,
  assessmentMethods: AssessmentMethods,
  prerequisites: Prerequisites,
  validPOs: POs,
  participants: Option[Participants],
  competences: List[Competence],
  globalCriteria: List[GlobalCriteria],
  taughtWith: List[Module]
   */
  override def create(metadata: Metadata, path: GitFilePath) = {
    val dbEntry = MetadataDbEntry(
      metadata.id,
      path.value,
      metadata.title,
      metadata.abbrev,
      metadata.kind.abbrev,
      metadata.ects.value,
      metadata.language.abbrev,
      metadata.duration,
      metadata.season.abbrev,
      metadata.workload,
      metadata.status.abbrev,
      metadata.location.abbrev,
      metadata.participants.map(_.min),
      metadata.participants.map(_.max)
    )

    val result = for {
      _ <- metadataTable += dbEntry
      _ <- ectsFocusAreaContributionTable ++= contributionsToFocusAreas(
        metadata
      )
      _ <- moduleRelationTable ++= moduleRelations(metadata)
    } yield metadata

    db.run(result)
  }

  private def moduleRelations(metadata: Metadata): List[ModuleRelationDbEntry] =
    metadata.relation.fold(List.empty[ModuleRelationDbEntry]) {
      case ModuleRelation.Parent(children) =>
        children.map(child =>
          ModuleRelationDbEntry(
            metadata.id,
            ModuleRelationType.Parent,
            child.id
          )
        )
      case ModuleRelation.Child(parent) =>
        List(
          ModuleRelationDbEntry(
            metadata.id,
            ModuleRelationType.Child,
            parent.id
          )
        )
    }

  private def contributionsToFocusAreas(metadata: Metadata) =
    metadata.ects.contributionsToFocusAreas.map(c =>
      ECTSFocusAreaContributionDbEntry(
        metadata.id,
        c.focusArea.abbrev,
        c.ectsValue,
        c.description
      )
    )

  override def all() =
    retrieve(metadataTable)

  private def retrieve(
      query: Query[MetadataTable, MetadataDbEntry, Seq]
  ): Future[Seq[MetadataOutput]] =
    db.run(
      query
        .joinLeft(moduleRelationTable)
        .on(_.id === _.module)
        .result
        .map(_.groupBy(_._1).map { case (m, deps) =>
          val participants = for {
            min <- m.participantsMin
            max <- m.participantsMax
          } yield Participants(min, max)
          val relations = deps.flatMap(_._2)
          val relation = relations.headOption.map { r => // TODO test
            r.relationType match {
              case ModuleRelationType.Parent =>
                ModuleRelation
                  .Parent(
                    relations.map(d => Module(d.module, "???")).toList
                  ) // TODO replace ??? with real data
              case ModuleRelationType.Child =>
                ModuleRelation.Child(Module(r.module, "???"))
            }
          }
          MetadataOutput(
            m.id,
            m.gitPath,
            m.title,
            m.abbrev,
            m.moduleType,
            m.ects,
            m.language,
            m.duration,
            m.season,
            m.workload,
            m.status,
            m.location,
            participants,
            relation
          )
        }.toSeq)
    )

  override def allIdsAndAbbrevs() =
    db.run(
      metadataTable
        .map(m => (m.id, m.abbrev))
        .result
    )
}
