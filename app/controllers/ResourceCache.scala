package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext

import play.api.cache.Cached
import play.api.http.HeaderNames.*
import play.api.mvc.EssentialAction

/**
 * Server-side response cache for public resources, including core data and schedules.
 * Use `cache("resource", ttl)(action)` for reads and `cache.invalidate("resource")` after committed writes.
 * Invalidation covers all query variants and the dependencies below, but only in this application instance.
 * Use `Cached.unlessNoCache` instead when expiry alone is sufficient and browser caching is desirable.
 */
@Singleton
final class ResourceCache @Inject() (cached: Cached)(implicit ctx: ExecutionContext) {
  private var versions = Map.empty[String, Long]

  /**
   * Call after a successful write and any required view refresh. Advances resource and dependent cache versions;
   * old entries remain until expiry. Add dependencies here when a response embeds another editable resource.
   */
  def invalidate(entity: String): Unit = synchronized {
    val resource           = entity.toLowerCase
    val dependentResources = resource match {
      case "degrees" | "pos" | "studyprograms" | "specializations" => Set("studyprograms", "modules")
      case "identities"                                            => Set("modules", "scheduleentries", "bookings")
      case "rooms"                                                 => Set("scheduleentries", "bookings")
      case "teachingunits"                                         => Set("semesterplan")
      case _                                                       => Set.empty[String]
    }
    (dependentResources + resource).foreach { name =>
      versions = versions.updated(name, versions.getOrElse(name, 0L) + 1)
    }
  }

  /**
   * Caches 200 responses by resource version, HTTP method and full URI (including query parameters).
   * Wrap only public reads whose response does not vary by user, cookies or headers.
   * `Cache-Control: no-cache` bypasses the server cache without refreshing it. Responses use `no-store` and
   * omit ETag/Expires so browsers return to the server, where resource invalidation takes effect.
   */
  def apply(resource: String, duration: Duration)(action: EssentialAction): EssentialAction =
    EssentialAction { request =>
      val version = synchronized { versions.getOrElse(resource.toLowerCase, 0L) }
      // Capture the version once: an in-flight read must not populate the cache of a later write.
      val key = s"resource-cache:${resource.toLowerCase}:$version:${request.method}${request.uri}"
      cached
        .unlessNoCache(_ => key, 200, duration)(action)(
          request.withHeaders(request.headers.remove(IF_NONE_MATCH))
        )
        .map { result =>
          // Keep server caching; browsers must fetch again after a resource changes.
          result.withHeaders(CACHE_CONTROL -> "no-store").discardingHeader(ETAG).discardingHeader(EXPIRES)
        }
    }
}
