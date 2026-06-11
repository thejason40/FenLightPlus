package com.fenlight.companion.ui.tvshows

import android.app.Application
import com.fenlight.companion.ui.media.MediaHomeViewModel
import com.fenlight.companion.ui.media.TvCatalog

class TvViewModel(application: Application) :
    MediaHomeViewModel(application, ::TvCatalog)
