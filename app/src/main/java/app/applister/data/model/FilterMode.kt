package app.applister.data.model

enum class FilterMode(val displayNameResId: Int) {
    ALL(app.applister.R.string.filter_all),
    USER(app.applister.R.string.filter_user),
    SYSTEM(app.applister.R.string.filter_system)
}
