package providers

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

import models.ModuleKeysToReview

@Singleton()
final class ModuleKeysToReviewProvider @Inject() (config: ConfigReader) extends Provider[ModuleKeysToReview] {
  override def get(): ModuleKeysToReview =
    ModuleKeysToReview(
      config.moduleKeysToReviewFromPav.toSet
    )
}
