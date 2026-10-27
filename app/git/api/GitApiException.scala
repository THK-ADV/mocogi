package git.api

final class GitApiException(val statusCode: Int, detail: String)
    extends RuntimeException(s"GitLab API returned HTTP $statusCode: $detail")
