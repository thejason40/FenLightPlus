package com.fenlight.companion.ui.movies

import android.app.Application
import com.fenlight.companion.ui.media.MediaHomeViewModel
import com.fenlight.companion.ui.media.MovieCatalog

class MovieViewModel(application: Application) :
    MediaHomeViewModel(application, ::MovieCatalog)
