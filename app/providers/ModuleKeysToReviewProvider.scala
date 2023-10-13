package providers

import models.ModuleKeysToReview

import javax.inject.{Inject, Provider, Singleton}

@Singleton()
final class ModuleKeysToReviewProvider @Inject() (config: ConfigReader)
    extends Provider[ModuleKeysToReview] {
  override def get(): ModuleKeysToReview =
    ModuleKeysToReview(
      config.moduleKeysToReviewFromSgl.toSet,
      config.moduleKeysToReviewFromPav.toSet
    )
}
