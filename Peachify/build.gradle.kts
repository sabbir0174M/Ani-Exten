version = 1

cloudstream {
    language = "en"
    // All of these properties are optional, you can safely remove them

    // description = "Add short description here"
    authors = listOf("sabbir0174M")
    /*
 * Status int as the following:
 * 0: Down
 * 1: Ok
 * 2: Slow
 * 3: Beta only
 * */
    status = 1 // will be 3 if unspecified
    tvTypes = listOf(
        "Movie",
        "TvSeries",
    )
    iconUrl = "https://www.google.com/s2/favicons?domain=peachify.top&sz=%size%"
}
