package app.applister.data.model

enum class SortMode(val displayNameResId: Int) {
    NAME_ASC(app.applister.R.string.sort_name_asc),
    NAME_DESC(app.applister.R.string.sort_name_desc),
    INSTALL_DATE_NEWEST(app.applister.R.string.sort_install_date_newest),
    INSTALL_DATE_OLDEST(app.applister.R.string.sort_install_date_oldest),
    UPDATE_DATE_NEWEST(app.applister.R.string.sort_updated_newest),
    UPDATE_DATE_OLDEST(app.applister.R.string.sort_updated_oldest),
    SIZE_LARGEST(app.applister.R.string.sort_size_largest),
    SIZE_SMALLEST(app.applister.R.string.sort_size_smallest),
    PACKAGE_NAME(app.applister.R.string.sort_package_name);

    companion object {
        fun fromIndex(index: Int): SortMode = entries.getOrElse(index) { NAME_ASC }
    }
}
