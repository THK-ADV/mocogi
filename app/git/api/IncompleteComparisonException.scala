package git.api

/** GitLab aborted a comparison of two revisions and returned only some of the changed files. */
final class IncompleteComparisonException
    extends RuntimeException("GitLab konnte den Vergleich von main und preview nicht vollständig liefern")
