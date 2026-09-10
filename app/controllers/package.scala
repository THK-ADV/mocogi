import scala.concurrent.duration.Duration

import play.api.cache.Cached
import play.api.http.HeaderNames.CACHE_CONTROL
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.mvc.EssentialAction
import play.api.mvc.RequestHeader

package object controllers {

  extension (cached: Cached) {

    /**
     * Use for reads where time-based expiry is sufficient: caches the chosen status with Play's ETag/Expires
     * and conditional 304 responses. The key must distinguish every response variant (URI, language, etc.).
     * `Cache-Control: no-cache` runs the action directly, bypassing both cache reads and writes; it does not
     * evict or refresh an existing entry. Use `ResourceCache` for public reads needing write invalidation
     * and server-only caching. Never put authorization inside a shared cached action: cache hits skip it.
     */
    def unlessNoCache(key: RequestHeader => String, status: Int, duration: Duration)(
        action: EssentialAction
    ): EssentialAction = {
      val cachedAction = cached.status(key, status, duration)(action)
      EssentialAction { rh =>
        val noCache =
          rh.headers.getAll(CACHE_CONTROL).iterator.flatMap(_.split(',')).exists(_.trim.equalsIgnoreCase("no-cache"))
        if noCache then action(rh)
        else cachedAction(rh)
      }
    }
  }

  implicit def listReads[A](implicit reads: Reads[A]): Reads[List[A]] =
    Reads.list(reads)

  given Writes[Throwable] = t =>
    Json.obj(
      "type"    -> "exception",
      "message" -> t.getMessage
    )

  given Writes[Exception] = e =>
    Json.obj(
      "type"    -> "exception",
      "message" -> e.getMessage
    )

  extension (self: RequestHeader) {
    def isExtended: Boolean =
      self
        .getQueryString("extend")
        .flatMap(_.toBooleanOption)
        .getOrElse(false)
  }

  object MimeTypes {
    val PDF  = "application/pdf"
    val WORD = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
  }
}
