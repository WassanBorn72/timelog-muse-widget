package app.timelog.musewidget

internal object Destination {
    const val DEFAULT = "WassanBorn72/timelog-events"
    const val CODE = "WassanBorn72/timelog-muse-widget"
    fun isCode(repository: String) = repository.equals(CODE, ignoreCase = true)
    fun valid(repository: String) = Regex("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+").matches(repository) && !isCode(repository)
    fun configured(saved: String?) = if (saved == null || isCode(saved)) DEFAULT else saved
}
