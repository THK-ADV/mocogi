package tasks

import play.api.inject.*
import providers.ModuleReviewNotifierProvider
import service.mail.ModuleReviewNotifier

class NotifierModule
    extends SimpleModule(
      bind(classOf[ModuleReviewNotifier]).toProvider(classOf[ModuleReviewNotifierProvider]).eagerly()
    )
