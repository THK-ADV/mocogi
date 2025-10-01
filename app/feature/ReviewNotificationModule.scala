package feature

import play.api.inject.*
import providers.ReviewNotificationSchedulerProvider
import service.notification.ReviewNotificationScheduler

class ReviewNotificationModule
    extends SimpleModule(
      bind(classOf[ReviewNotificationScheduler]).toProvider(classOf[ReviewNotificationSchedulerProvider]).eagerly()
    )
