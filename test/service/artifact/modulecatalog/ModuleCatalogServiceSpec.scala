package service.artifact.modulecatalog

import java.time.LocalDate
import java.util.UUID

import cats.data.NonEmptyList
import models.*
import models.core.Degree
import models.core.ExamPhases.ExamPhase
import models.core.IDLabel
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import parsing.types.ModuleContent

final class ModuleCatalogServiceSpec extends AnyWordSpec with Matchers {

  private val currentPO = "po1"
  private val module1   = UUID.fromString("00000000-0000-0000-0000-000000000001")
  private val module2   = UUID.fromString("00000000-0000-0000-0000-000000000002")
  private val generic1  = UUID.fromString("00000000-0000-0000-0000-000000000003")
  private val generic2  = UUID.fromString("00000000-0000-0000-0000-000000000004")

  private def emptyContent: ModuleContent = ModuleContent("", "", "", "", "")

  private def protocol(
      id: UUID,
      title: String,
      moduleType: String = "module",
      mandatory: List[ModulePOMandatoryProtocol] = Nil,
      optional: List[ModulePOOptionalProtocol] = Nil
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
        ModulePOProtocol(mandatory, optional),
        Nil,
        None,
        None
      ),
      emptyContent,
      emptyContent
    )

  private def preview(module: ModuleProtocol): (ModuleProtocol, LocalDate) =
    module -> LocalDate.of(2026, 1, 1)

  private def config(
      moduleSelection: ModuleCatalogModuleSelectionConfig = ModuleCatalogModuleSelectionConfig.empty,
      studyPlan: ModuleCatalogStudyPlanConfig = ModuleCatalogStudyPlanConfig.empty
  ): ModuleCatalogConfig =
    ModuleCatalogConfig(moduleSelection, studyPlan)

  private val poOnly = Seq(
    StudyProgramView(
      "inf",
      "Informatik",
      "Computer Science",
      "INF",
      POCore(currentPO, 1),
      Degree("bsc", "Bachelor", "", "Bachelor", ""),
      None
    )
  )

  private val poWithSpecialization = poOnly.appended(
    poOnly.head.copy(specialization = Some(IDLabel("po1_spec", "Spec", "Spec")))
  )

  "ModuleCatalogService.applyModuleSelection" should {
    "preserve all PO modules for an empty selection" in {
      val first  = protocol(module1, "First", mandatory = List(ModulePOMandatoryProtocol(currentPO, None, List(1))))
      val second = protocol(
        module2,
        "Second",
        optional = List(ModulePOOptionalProtocol(currentPO, None, generic1, partOfCatalog = true, List(2)))
      )
      val modules = Vector(preview(first), preview(second))

      ModuleCatalogService.applyModuleSelection(
        currentPO,
        modules,
        ModuleCatalogModuleSelectionConfig.empty
      ) shouldBe modules
    }

    "remove globally excluded modules" in {
      val first  = protocol(module1, "First", mandatory = List(ModulePOMandatoryProtocol(currentPO, None, List(1))))
      val second = protocol(module2, "Second", mandatory = List(ModulePOMandatoryProtocol(currentPO, None, List(2))))

      val filtered = ModuleCatalogService.applyModuleSelection(
        currentPO,
        Vector(preview(first), preview(second)),
        ModuleCatalogModuleSelectionConfig(List(module2), Nil)
      )

      filtered.map(_._1.id.get) shouldBe Vector(module1)
    }

    "remove only the selected elective relationship and keep remaining PO relationships" in {
      val option = protocol(
        module1,
        "Option",
        optional = List(
          ModulePOOptionalProtocol(currentPO, None, generic1, partOfCatalog = true, List(5)),
          ModulePOOptionalProtocol(currentPO, None, generic2, partOfCatalog = true, List(6)),
          ModulePOOptionalProtocol("other-po", None, generic1, partOfCatalog = true, List(7))
        )
      )

      val filtered = ModuleCatalogService.applyModuleSelection(
        currentPO,
        Vector(preview(option)),
        ModuleCatalogModuleSelectionConfig(Nil, List(ModuleCatalogExcludedElectiveOption(generic1, module1)))
      )

      val remainingOptional = filtered.head._1.metadata.po.optional
      remainingOptional.map(relation => relation.po -> relation.instanceOf) shouldBe List(
        currentPO  -> generic2,
        "other-po" -> generic1
      )
    }

    "drop only the selected option after removing its last relation to the current PO" in {
      val selectedOption = protocol(
        module1,
        "Selected option",
        optional = List(ModulePOOptionalProtocol(currentPO, None, generic1, partOfCatalog = true, List(5)))
      )
      val retainedOption = protocol(
        module2,
        "Retained option",
        optional = List(ModulePOOptionalProtocol(currentPO, None, generic1, partOfCatalog = true, List(5)))
      )

      val filtered = ModuleCatalogService.applyModuleSelection(
        currentPO,
        Vector(preview(selectedOption), preview(retainedOption)),
        ModuleCatalogModuleSelectionConfig(Nil, List(ModuleCatalogExcludedElectiveOption(generic1, module1)))
      )

      filtered shouldBe Vector(preview(retainedOption))
    }
  }

  "ModuleCatalogService.validateConfig" should {
    "accept an empty config" in {
      val module = protocol(
        module1,
        "Default",
        mandatory = List(ModulePOMandatoryProtocol(currentPO, None, List(1)))
      )

      noException should be thrownBy {
        ModuleCatalogService.validateConfig(currentPO, Vector(module), poOnly, ModuleCatalogConfig.empty)
      }
    }

    "accept valid module-selection and study-plan overrides" in {
      val generic = protocol(
        generic1,
        "Generic",
        moduleType = "generic_module",
        mandatory = List(ModulePOMandatoryProtocol(currentPO, None, List(3)))
      )
      val mandatory = protocol(
        module1,
        "Mandatory",
        mandatory = List(ModulePOMandatoryProtocol(currentPO, None, List(3, 5)))
      )
      val option = protocol(
        module2,
        "Option",
        optional = List(ModulePOOptionalProtocol(currentPO, None, generic1, partOfCatalog = true, List(3)))
      )
      val valid = config(
        moduleSelection = ModuleCatalogModuleSelectionConfig(
          Nil,
          List(ModuleCatalogExcludedElectiveOption(generic1, module2))
        ),
        studyPlan = ModuleCatalogStudyPlanConfig(
          List(StudyPlanSection(3, "First section")),
          List(ModuleCatalogSemesterSelection(module1, 5)),
          List(ModuleCatalogGenericModuleOccurrence(generic1, 3, 2))
        )
      )

      noException should be thrownBy {
        ModuleCatalogService.validateConfig(currentPO, Vector(generic, mandatory, option), poOnly, valid)
      }
    }

    "reject unknown module references in every config block" in {
      val invalid = config(
        moduleSelection = ModuleCatalogModuleSelectionConfig(
          List(module1),
          List(ModuleCatalogExcludedElectiveOption(module1, module2))
        ),
        studyPlan = ModuleCatalogStudyPlanConfig(
          Nil,
          List(ModuleCatalogSemesterSelection(generic1, 1)),
          List(ModuleCatalogGenericModuleOccurrence(generic2, 1, 1))
        )
      )

      val error = intercept[ModuleCatalogConfigException] {
        ModuleCatalogService.validateConfig(currentPO, Vector.empty, poOnly, invalid)
      }

      error.getMessage should include("excludedModuleIds references modules outside PO")
      error.getMessage should include("excludedElectiveOptions references modules outside PO")
      error.getMessage should include("semesterSelections references modules outside PO")
      error.getMessage should include("genericModuleOccurrences references modules outside PO")
    }

    "reject invalid elective-option definitions" in {
      val regular = protocol(
        module1,
        "Regular",
        mandatory = List(ModulePOMandatoryProtocol(currentPO, None, List(1)))
      )
      val option = protocol(
        module2,
        "Option",
        mandatory = List(ModulePOMandatoryProtocol(currentPO, None, List(1))),
        optional = List(ModulePOOptionalProtocol("other-po", None, module1, partOfCatalog = true, List(1)))
      )
      val invalid = config(
        moduleSelection = ModuleCatalogModuleSelectionConfig(
          Nil,
          List(ModuleCatalogExcludedElectiveOption(module1, module2))
        )
      )

      val error = intercept[ModuleCatalogConfigException] {
        ModuleCatalogService.validateConfig(currentPO, Vector(regular, option), poOnly, invalid)
      }

      error.getMessage should include(s"genericModuleId $module1 is not a generic module")
      error.getMessage should include(s"missing relationship $module2 -> $module1")
    }

    "reject duplicate semester selections" in {
      val module = protocol(
        module1,
        "Duplicate",
        mandatory = List(ModulePOMandatoryProtocol(currentPO, None, List(1, 2)))
      )
      val invalid = config(
        studyPlan = ModuleCatalogStudyPlanConfig(
          Nil,
          List(
            ModuleCatalogSemesterSelection(module1, 1),
            ModuleCatalogSemesterSelection(module1, 2)
          ),
          Nil
        )
      )

      val error = intercept[ModuleCatalogConfigException] {
        ModuleCatalogService.validateConfig(currentPO, Vector(module), poOnly, invalid)
      }

      error.getMessage should include(s"duplicate module id $module1")
    }

    "reject selected semesters outside the module recommendations" in {
      val module = protocol(
        module1,
        "Ambiguous",
        mandatory = List(ModulePOMandatoryProtocol(currentPO, None, List(3, 5)))
      )
      val invalid = config(
        studyPlan = ModuleCatalogStudyPlanConfig(
          Nil,
          List(ModuleCatalogSemesterSelection(module1, 4)),
          Nil
        )
      )

      val error = intercept[ModuleCatalogConfigException] {
        ModuleCatalogService.validateConfig(currentPO, Vector(module), poOnly, invalid)
      }

      error.getMessage should include("expected one of 3, 5")
    }

    "reject semester selections for non-mandatory modules" in {
      val module = protocol(
        module1,
        "Optional",
        optional = List(ModulePOOptionalProtocol(currentPO, None, generic1, partOfCatalog = true, List(3)))
      )
      val invalid = config(
        studyPlan = ModuleCatalogStudyPlanConfig(
          Nil,
          List(ModuleCatalogSemesterSelection(module1, 3)),
          Nil
        )
      )

      val error = intercept[ModuleCatalogConfigException] {
        ModuleCatalogService.validateConfig(currentPO, Vector(module), poOnly, invalid)
      }

      error.getMessage should include(s"module $module1 is not mandatory")
    }

    "reject manual sections for POs with specializations" in {
      val invalid = config(
        studyPlan = ModuleCatalogStudyPlanConfig(List(StudyPlanSection(3, "Grundlagen")), Nil, Nil)
      )

      val error = intercept[ModuleCatalogConfigException] {
        ModuleCatalogService.validateConfig(currentPO, Vector.empty, poWithSpecialization, invalid)
      }

      error.getMessage should include("sections cannot be used")
    }

    "reject study-plan references to globally excluded modules" in {
      val module = protocol(
        module1,
        "Excluded",
        mandatory = List(ModulePOMandatoryProtocol(currentPO, None, List(1)))
      )
      val invalid = config(
        moduleSelection = ModuleCatalogModuleSelectionConfig(List(module1), Nil),
        studyPlan = ModuleCatalogStudyPlanConfig(
          Nil,
          List(ModuleCatalogSemesterSelection(module1, 1)),
          Nil
        )
      )

      val error = intercept[ModuleCatalogConfigException] {
        ModuleCatalogService.validateConfig(currentPO, Vector(module), poOnly, invalid)
      }

      error.getMessage should include("studyPlan references excluded modules")
    }

    "reject generic occurrences for non-generic modules and non-positive counts" in {
      val module = protocol(
        module1,
        "Regular",
        mandatory = List(ModulePOMandatoryProtocol(currentPO, None, List(1)))
      )
      val invalid = config(
        studyPlan = ModuleCatalogStudyPlanConfig(
          Nil,
          Nil,
          List(ModuleCatalogGenericModuleOccurrence(module1, 1, 0))
        )
      )

      val error = intercept[ModuleCatalogConfigException] {
        ModuleCatalogService.validateConfig(currentPO, Vector(module), poOnly, invalid)
      }

      error.getMessage should include("not a generic module")
      error.getMessage should include("positive count")
    }

    "reject generic occurrences without mandatory semester data or with an invalid semester" in {
      val optionalGeneric = protocol(
        generic1,
        "Optional generic",
        moduleType = "generic_module",
        optional = List(ModulePOOptionalProtocol(currentPO, None, generic2, partOfCatalog = true, List(3)))
      )
      val mandatoryGeneric = protocol(
        generic2,
        "Mandatory generic",
        moduleType = "generic_module",
        mandatory = List(ModulePOMandatoryProtocol(currentPO, None, List(5)))
      )
      val invalid = config(
        studyPlan = ModuleCatalogStudyPlanConfig(
          Nil,
          Nil,
          List(
            ModuleCatalogGenericModuleOccurrence(generic1, 3, 1),
            ModuleCatalogGenericModuleOccurrence(generic2, 4, 1)
          )
        )
      )

      val error = intercept[ModuleCatalogConfigException] {
        ModuleCatalogService.validateConfig(currentPO, Vector(optionalGeneric, mandatoryGeneric), poOnly, invalid)
      }

      error.getMessage should include(s"module $generic1 is not mandatory")
      error.getMessage should include(s"module $generic1 has no recommended semesters")
      error.getMessage should include(s"module $generic2 uses semester 4, expected one of 5")
    }
  }

  "ModuleCatalogService.configOptions" should {
    "build generic elective groups from preview modules" in {
      val generic = protocol(
        generic1,
        "Generic Pool",
        moduleType = "generic_module",
        mandatory = List(ModulePOMandatoryProtocol(currentPO, None, List(5)))
      )
      val option = protocol(
        module1,
        "Concrete Option",
        optional = List(ModulePOOptionalProtocol(currentPO, None, generic1, partOfCatalog = true, List(5)))
      )

      val options = ModuleCatalogService.configOptions(currentPO, Vector(generic, option), poOnly)

      options.modules.map(_.id).toSet shouldBe Set(generic1, module1)
      options.genericElectiveGroups.map(_.genericModuleId) shouldBe List(generic1)
      options.genericElectiveGroups.head.optionCandidates.map(_.moduleId) shouldBe List(module1)
    }
  }

  "ModuleCatalogService.diagnosticsSnippets" should {
    "create diagnostics only for previews with warnings" in {
      val warning = ModuleCatalogWarning("code", "message", Some(module1))

      ModuleCatalogService.diagnosticsSnippets(isPreview = true, List(warning)) should have size 1
      ModuleCatalogService.diagnosticsSnippets(isPreview = true, Nil) shouldBe Nil
      ModuleCatalogService.diagnosticsSnippets(isPreview = false, List(warning)) shouldBe Nil
    }
  }
}
