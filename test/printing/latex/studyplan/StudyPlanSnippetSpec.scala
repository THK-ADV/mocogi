package printing.latex.studyplan

import java.util.Locale
import java.util.UUID

import scala.collection.mutable.ListBuffer

import cats.data.NonEmptyList
import models.*
import models.core.ExamPhases.ExamPhase
import models.core.IDLabel
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import parsing.types.ModuleContent
import play.api.i18n.DefaultMessagesApi
import play.api.i18n.Lang
import service.artifact.modulecatalog.ModuleCatalogGenericModuleOccurrence
import service.artifact.modulecatalog.ModuleCatalogSemesterSelection
import service.artifact.modulecatalog.ModuleCatalogStudyPlanConfig
import service.artifact.modulecatalog.ModuleCatalogWarning
import service.artifact.modulecatalog.StudyPlanSection

final class StudyPlanSnippetSpec extends AnyWordSpec with Matchers {

  private val currentPO = "po1"
  private val module1   = UUID.fromString("10000000-0000-0000-0000-000000000001")
  private val module2   = UUID.fromString("10000000-0000-0000-0000-000000000002")
  private val generic1  = UUID.fromString("10000000-0000-0000-0000-000000000003")

  private val messagesApi = new DefaultMessagesApi(
    Map(
      "de" -> Map(
        "latex.module_catalog.study_plan.headline"               -> "Studienverlaufsplan",
        "latex.module_catalog.study_plan.sections"               -> "Studienabschnitte",
        "latex.module_catalog.study_plan.semester_assignment"    -> "Leistungspunkte und Semesterzuordnung",
        "latex.module_catalog.study_plan.column.module"          -> "Module",
        "latex.module_catalog.study_plan.column.pv"              -> "PV",
        "latex.module_catalog.study_plan.column.cp"              -> "CP",
        "latex.module_catalog.study_plan.pv.yes"                 -> "TN",
        "latex.module_catalog.study_plan.pv.no"                  -> "-",
        "latex.module_catalog.study_plan.footer.total"           -> "Summe Leistungspunkte",
        "latex.module_catalog.study_plan.header.continuation"    -> "Studienverlaufsplan (fortgesetzt)",
        "latex.module_catalog.study_plan.unassigned"             -> "Nicht zugeordnet",
        "latex.module_catalog.study_plan.unassigned.explanation" -> "Diesen Modulen wurde kein Semester zugeordnet",
        "latex.module_catalog.study_plan.section.unassigned"     -> "Weitere Module",
        "latex.module_catalog.study_plan.base"                   -> "Basisstudium",
        "latex.module_catalog.study_plan.specialization"         -> "Schwerpunkt {0}"
      )
    )
  )

  private def emptyContent: ModuleContent = ModuleContent("", "", "", "", "")

  private def protocol(
      id: UUID,
      title: String,
      moduleType: String = "module",
      mandatory: List[ModulePOMandatoryProtocol]
  ): ModuleProtocol =
    ModuleProtocol(
      Some(id),
      MetadataProtocol(
        title,
        title.take(3).toUpperCase,
        moduleType,
        5.0,
        "de",
        1,
        "ws",
        ModuleWorkload(0, 0, 0, 0, 0, 0),
        "active",
        "gm",
        None,
        None,
        NonEmptyList.one("nn"),
        NonEmptyList.one("nn"),
        ModuleAssessmentMethodsProtocol(List(ModuleAssessmentMethodEntryProtocol("exam", None, Nil))),
        Examiner.NN,
        NonEmptyList.one(ExamPhase.none.id),
        ModulePrerequisitesProtocol(None, None),
        ModulePOProtocol(mandatory, Nil),
        Nil,
        None,
        None
      ),
      emptyContent,
      emptyContent
    )

  private def render(
      modules: Vector[ModuleProtocol],
      config: ModuleCatalogStudyPlanConfig = ModuleCatalogStudyPlanConfig.empty,
      specializations: List[IDLabel] = Nil,
      isPreview: Boolean = true
  ): (String, List[ModuleCatalogWarning]) = {
    val warnings = ListBuffer.empty[ModuleCatalogWarning]
    val snippet  = StudyPlanSnippet(
      currentPO,
      modules.map(module => module.id.get -> module.metadata),
      NonEmptyList.fromList(config.sections),
      config.semesterSelections,
      config.genericModuleOccurrences,
      specializations,
      warning => warnings += warning,
      isPreview,
      messagesApi
    )

    given lang: Lang = Lang(Locale.GERMANY)
    val builder      = new StringBuilder()
    snippet.print(using lang, builder)
    builder.toString -> warnings.toList
  }

  private def occurrencesOf(value: String, needle: String): Int =
    value.sliding(needle.length).count(_ == needle)

  "StudyPlanSnippet" should {
    "use configured semester selections without fallback warnings" in {
      val module = protocol(
        module1,
        "Ambiguous Module",
        mandatory = List(ModulePOMandatoryProtocol(currentPO, None, List(3, 5)))
      )

      val (output, warnings) = render(
        Vector(module),
        ModuleCatalogStudyPlanConfig(Nil, List(ModuleCatalogSemesterSelection(module1, 5)), Nil)
      )

      output should include("Ambiguous Module")
      (output should not).include("3 & 4 & 5")
      warnings.map(_.code) should not contain "multiple_recommended_semesters"
    }

    "warn when multiple recommended semesters fall back to the minimum" in {
      val module = protocol(
        module1,
        "Ambiguous Module",
        mandatory = List(ModulePOMandatoryProtocol(currentPO, None, List(3, 5)))
      )

      val (_, warnings) = render(Vector(module))

      warnings.map(_.code) should contain("multiple_recommended_semesters")
    }

    "replace a generic module default row with configured duplicate occurrences" in {
      val module = protocol(
        generic1,
        "Generic Module",
        moduleType = "generic_module",
        mandatory = List(ModulePOMandatoryProtocol(currentPO, None, List(5)))
      )

      val (output, warnings) = render(
        Vector(module),
        ModuleCatalogStudyPlanConfig(
          Nil,
          Nil,
          List(ModuleCatalogGenericModuleOccurrence(generic1, 5, 2))
        )
      )

      occurrencesOf(output, s"\\hyperref[sec:${generic1.toString}]") shouldBe 2
      warnings.map(_.code) should not contain "generic_module_default_occurrence"
    }

    "warn when a generic module uses the default single occurrence" in {
      val module = protocol(
        generic1,
        "Generic Module",
        moduleType = "generic_module",
        mandatory = List(ModulePOMandatoryProtocol(currentPO, None, List(5)))
      )

      val (_, warnings) = render(Vector(module))

      warnings.map(_.code) should contain("generic_module_default_occurrence")
    }

    "render base and specialization tables separately" in {
      val base = protocol(
        module1,
        "Base Module",
        mandatory = List(ModulePOMandatoryProtocol(currentPO, None, List(1)))
      )
      val specialization = protocol(
        module2,
        "Specialization Module",
        mandatory = List(ModulePOMandatoryProtocol(currentPO, Some("po1_spec"), List(2)))
      )

      val (output, _) = render(
        Vector(base, specialization),
        specializations = List(IDLabel("po1_spec", "Data", "Data"))
      )

      output should include("Basisstudium")
      output should include("Schwerpunkt Data")
      output should include("Base Module")
      output should include("Specialization Module")
    }

    "keep duplicate generic rows when grouping into manual sections" in {
      val module = protocol(
        generic1,
        "Generic Module",
        moduleType = "generic_module",
        mandatory = List(ModulePOMandatoryProtocol(currentPO, None, List(1)))
      )

      val (output, _) = render(
        Vector(module),
        ModuleCatalogStudyPlanConfig(
          List(StudyPlanSection(1, "First Year")),
          Nil,
          List(ModuleCatalogGenericModuleOccurrence(generic1, 1, 2))
        )
      )

      occurrencesOf(output, s"\\hyperref[sec:${generic1.toString}]") shouldBe 2
    }
  }
}
