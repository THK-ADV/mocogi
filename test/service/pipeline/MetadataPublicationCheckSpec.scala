package service.pipeline

import java.util.UUID

import scala.concurrent.duration.*
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cli.MarkdownCLI
import database.repo.core.*
import helper.*
import models.ModuleCore
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import org.scalatest.OptionValues
import org.scalatestplus.mockito.MockitoSugar
import parsing.metadata.MetadataCompositeParser
import parsing.metadata.THKV1Parser
import printing.yaml.ModuleYamlPrinter
import service.ModuleService

final class MetadataPublicationCheckSpec
    extends AnyFunSuite
    with Matchers
    with EitherValues
    with OptionValues
    with MockitoSugar
    with FakeLocations
    with FakeLanguages
    with FakeStatus
    with FakeAssessmentMethod
    with FakeModuleTypes
    with FakeSeasons
    with FakeIdentities
    with FakePOs
    with FakeSpecializations {

  private given ExecutionContext = ExecutionContext.parasitic

  private val parent         = UUID.fromString("00000000-0000-0000-0000-000000000001")
  private val child          = UUID.fromString("00000000-0000-0000-0000-000000000002")
  private val other          = UUID.fromString("00000000-0000-0000-0000-000000000003")
  private val publishedChild = ModuleCore(child, "Published child", "CHILD")

  private def await[A](result: Future[A]): A = Await.result(result, 5.seconds)

  private def module(id: UUID, children: List[UUID] = Nil, prerequisites: List[UUID] = Nil): Print = {
    val relation =
      if children.isEmpty then "" else "relation:\n  children:\n" + children.map(id => s"    - module.$id\n").mkString
    val required =
      if prerequisites.isEmpty then ""
      else "recommended_prerequisites:\n  modules:\n" + prerequisites.map(id => s"    - module.$id\n").mkString
    Print(s"""---v1s
             |id: $id
             |title: Module $id
             |abbreviation: TEST
             |type: type.module
             |${relation}ects: 5
             |language: lang.de
             |duration: 1
             |frequency: season.ws
             |responsibilities:
             |  module_management: person.ald
             |  lecturers: person.abe
             |assessment_methods_mandatory:
             |  - method: assessment.written-exam
             |first_examiner: person.ald
             |second_examiner: person.abe
             |exam_phases: exam_phase.none
             |workload:
             |  lecture: 36
             |  seminar: 0
             |  practical: 18
             |  exercise: 18
             |  project_supervision: 0
             |  project_work: 0
             |${required}status: status.active
             |location: location.gm
             |po_mandatory:
             |  - study_program: study_program.inf1
             |    recommended_semester: 3
             |---
             |
             |## (de) Angestrebte Lernergebnisse:
             |
             |Programmieren lernen
             |
             |## (en) Learning Outcome:
             |
             |Learn to code
             |
             |## (de) Modulinhalte:
             |
             |Algorithmen
             |
             |## (en) Module Content:
             |
             |Algorithms
             |
             |## (de) Lehr- und Lernmethoden (Medienformen):
             |
             |Vorlesung
             |
             |## (en) Teaching and Learning Methods:
             |
             |Lecture
             |
             |## (de) Empfohlene Literatur:
             |
             |## (en) Recommended Reading:
             |
             |## (de) Besonderheiten:
             |
             |## (en) Particularities:
             |""".stripMargin)
  }

  private class Fixture(
      published: Seq[ModuleCore] = Nil,
      relations: Map[UUID, Set[UUID]] = Map.empty,
      previewOnly: Seq[ModuleCore] = Nil
  ) {
    val modules                 = mock[ModuleService]
    val markdown                = mock[MarkdownCLI]
    private val locations       = mock[LocationRepository]
    private val languages       = mock[LanguageRepository]
    private val statuses        = mock[StatusRepository]
    private val assessments     = mock[AssessmentMethodRepository]
    private val types           = mock[ModuleTypeRepository]
    private val seasons         = mock[SeasonRepository]
    private val identities      = mock[IdentityRepository]
    private val pos             = mock[PORepository]
    private val specializations = mock[SpecializationRepository]

    when(locations.list()).thenReturn(Future.successful(fakeLocations))
    when(languages.list()).thenReturn(Future.successful(fakeLanguages))
    when(statuses.list()).thenReturn(Future.successful(fakeStatus))
    when(assessments.all()).thenReturn(Future.successful(fakeAssessmentMethod))
    when(types.list()).thenReturn(Future.successful(fakeModuleTypes))
    when(seasons.list()).thenReturn(Future.successful(fakeSeasons))
    when(identities.list()).thenReturn(Future.successful(fakeIdentities))
    when(pos.list()).thenReturn(Future.successful(fakePOs))
    when(specializations.list()).thenReturn(Future.successful(fakeSpecializations))
    when(modules.allModuleCoreWithRelations()).thenReturn(Future.successful((published, relations)))
    when(modules.allNewlyCreated()).thenReturn(Future.successful(previewOnly))
    when(markdown.lint(any[Seq[(Option[UUID], String)]])).thenReturn(Right(Nil))

    private val parser = new MetadataParsingService(
      new MetadataCompositeParser(Set(new THKV1Parser)),
      locations,
      languages,
      statuses,
      assessments,
      types,
      seasons,
      identities,
      pos,
      specializations,
      summon[ExecutionContext]
    )
    val pipeline = new MetadataPipeline(parser, modules, mock[ModuleYamlPrinter], markdown, summon[ExecutionContext])
  }

  test("published modules resolve both children and prerequisites") {
    val f     = new Fixture(published = Seq(publishedChild))
    val print = module(parent, children = List(child), prerequisites = List(child))

    val result = await(f.pipeline.publicationCheck(Seq(print), Set.empty)).value

    result.map(_._1) shouldBe Seq(print)
    val metadata = result.head._2.metadata
    metadata.relation.value.children.toList shouldBe List(publishedChild)
    metadata.prerequisites.recommended.value.modules shouldBe List(publishedChild)
    result.head._2.enContent.learningOutcome shouldBe "Learn to code"
    verify(f.modules, never()).allNewlyCreated()
  }

  for reverse <- List(false, true) do
    test(s"co-selected modules resolve each other independently of input order (reverse=$reverse)") {
      val f      = new Fixture
      val prints = Seq(module(parent, children = List(child)), module(child, prerequisites = List(parent)))
      val input  = if reverse then prints.reverse else prints

      val result   = await(f.pipeline.publicationCheck(input, Set.empty)).value
      val metadata = result.map(_._2.metadata).map(m => m.id -> m).toMap

      result.map(_._1) shouldBe input
      metadata(parent).relation.value.children.toList.map(_.id) shouldBe List(child)
      metadata(child).prerequisites.recommended.value.modules.map(_.id) shouldBe List(parent)
    }

  test("preview-only references pass fullCheck but fail publicationCheck when not selected") {
    val f     = new Fixture(previewOnly = Seq(publishedChild))
    val input = Seq(module(parent, children = List(child), prerequisites = List(child)))

    val errors = await(f.pipeline.publicationCheck(input, Set.empty)).left.value

    errors should have size 1
    errors.head shouldBe a[PipelineError.Validator]
    errors.head.metadata shouldBe Some(parent)
    errors.head.getMessage should include(child.toString)
    errors.head.getMessage should include("module relation")
    errors.head.getMessage should include("recommended prerequisites")
    verify(f.modules, never()).allNewlyCreated()
    await(f.pipeline.fullCheck(input)).value.map(_._2.metadata.id) shouldBe Seq(parent)
  }

  test("a published module selected for deletion can no longer satisfy a reference") {
    val f     = new Fixture(published = Seq(publishedChild))
    val input = Seq(module(parent, children = List(child)))

    await(f.pipeline.publicationCheck(input, Set.empty)).value should have size 1
    val errors = await(f.pipeline.publicationCheck(input, Set(child))).left.value

    errors should have size 1
    errors.head shouldBe a[PipelineError.Validator]
    errors.head.metadata shouldBe Some(parent)
    errors.head.getMessage should include(child.toString)
  }

  test("two selected parents cannot claim the same child even when each is valid alone") {
    val f      = new Fixture(published = Seq(publishedChild))
    val first  = module(parent, children = List(child))
    val second = module(other, children = List(child))

    await(f.pipeline.publicationCheck(Seq(first), Set.empty)).value should have size 1
    await(f.pipeline.publicationCheck(Seq(second), Set.empty)).value should have size 1
    val errors = await(f.pipeline.publicationCheck(Seq(first, second), Set.empty)).left.value

    errors should have size 2
    errors.map(_.metadata).toSet shouldBe Set(Some(parent), Some(other))
    all(errors.map(_.getMessage)) should include("multiple parents")
  }

  test("the published relation graph prevents stealing a child from an unselected parent") {
    val f = new Fixture(
      published = Seq(publishedChild, ModuleCore(other, "Existing parent", "OTHER")),
      relations = Map(other -> Set(child))
    )

    val errors = await(f.pipeline.publicationCheck(Seq(module(parent, children = List(child))), Set.empty)).left.value

    errors should have size 1
    errors.head.metadata shouldBe Some(parent)
    errors.head.getMessage should include("multiple parents")
    errors.head.getMessage should include(other.toString)
  }

  test("replacing the old parent's relation permits moving a child in the same batch") {
    val f = new Fixture(
      published = Seq(publishedChild, ModuleCore(other, "Old parent", "OTHER")),
      relations = Map(other -> Set(child))
    )
    val input = Seq(module(parent, children = List(child)), module(other))

    for prints <- Seq(input, input.reverse) do {
      val result = await(f.pipeline.publicationCheck(prints, Set.empty)).value.map(_._2.metadata)
      result.find(_.id == other).value.relation shouldBe None
      result.find(_.id == parent).value.relation.value.children.toList shouldBe List(publishedChild)
    }
  }

  test("parse, content, lint and validation failures accumulate without losing valid inputs") {
    val f             = new Fixture
    val malformed     = Print("---v1s\nid: not-a-uuid\n---")
    val brokenContent = Print(module(child).value.replace("## (en) Learning Outcome:", "## Wrong heading:"))
    val unresolved    = module(parent, children = List(child))
    val valid         = module(other)
    when(f.markdown.lint(any[Seq[(Option[UUID], String)]]))
      .thenReturn(Right(Seq(None -> "Malformed Markdown", Some(other) -> "Invalid heading level")))

    val errors =
      await(f.pipeline.publicationCheck(Seq(malformed, brokenContent, unresolved, valid), Set.empty)).left.value

    errors should have size 5
    errors.collect { case e: PipelineError.Parser => e.metadata }.toSet shouldBe Set(None, Some(child))
    errors.collect { case e: PipelineError.Printer => e.metadata }.toSet shouldBe Set(None, Some(other))
    errors.collect { case e: PipelineError.Validator => e.metadata } shouldBe Seq(Some(parent))
    errors.find(_.isInstanceOf[PipelineError.Validator]).value.getMessage should include(child.toString)
    errors.map(_.getMessage).mkString should include("Invalid heading level")
    val inputs = ArgumentCaptor.forClass(classOf[Seq[(Option[UUID], String)]])
    verify(f.markdown).lint(inputs.capture())
    inputs.getValue should contain theSameElementsAs Seq(
      None         -> malformed.value,
      Some(child)  -> brokenContent.value,
      Some(parent) -> unresolved.value,
      Some(other)  -> valid.value
    )
  }

  test("lint alone can reject an otherwise valid module and preserves its ID and diagnostic") {
    val f = new Fixture
    when(f.markdown.lint(any[Seq[(Option[UUID], String)]]))
      .thenReturn(Right(Seq(Some(parent) -> "MD001: heading levels must increase by one")))

    val errors = await(f.pipeline.publicationCheck(Seq(module(parent)), Set.empty)).left.value

    errors should have size 1
    errors.head shouldBe a[PipelineError.Printer]
    errors.head.metadata shouldBe Some(parent)
    errors.head.getMessage should include("MD001: heading levels must increase by one")
  }

  test("a failed lint process rejects the batch with its diagnostic and no invented module ID") {
    val f = new Fixture
    when(f.markdown.lint(any[Seq[(Option[UUID], String)]]))
      .thenReturn(Left("markdownlint exited with code 2"))

    val errors = await(f.pipeline.publicationCheck(Seq(module(parent)), Set.empty)).left.value

    errors should have size 1
    errors.head shouldBe a[PipelineError.Printer]
    errors.head.metadata shouldBe None
    errors.head.getMessage should include("markdownlint exited with code 2")
  }
}
