package controllers

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class ArtifactRoutesSpec extends AnyWordSpec with Matchers {
  "artifact routes" should {
    "identify their resources by PO only" in {
      routes.ModuleCatalogController.allGenericModulesForPO("inf_inf2").url shouldBe
        "/moduleCatalogs/inf_inf2/genericModules"
      routes.ModuleCatalogController.generate("inf_inf2").url shouldBe "/moduleCatalogs/inf_inf2"
      routes.ModuleCatalogController.uploadIntroFile("inf_inf2").url shouldBe "/moduleCatalogIntros/inf_inf2"
      routes.ExamListsController.getPreview("inf_inf2").url shouldBe "/examLists/preview/inf_inf2"
      routes.ExamListsController.replace("inf_inf2").url shouldBe "/examLists/inf_inf2"
      routes.ExamLoadController.generateExamLoad("inf_inf2").url shouldBe "/examLoad/inf_inf2"
    }
  }
}
