package com.fenlight.companion.ui.movies

import android.app.Application
import com.fenlight.companion.ui.media.MediaSearchViewModel
import com.fenlight.companion.ui.media.MovieCatalog

class MovieSearchViewModel(application: Application) :
    MediaSearchViewModel(application, ::MovieCatalog)
