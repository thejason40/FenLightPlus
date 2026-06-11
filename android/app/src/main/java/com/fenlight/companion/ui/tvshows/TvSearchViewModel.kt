package com.fenlight.companion.ui.tvshows

import android.app.Application
import com.fenlight.companion.ui.media.MediaSearchViewModel
import com.fenlight.companion.ui.media.TvCatalog

class TvSearchViewModel(application: Application) :
    MediaSearchViewModel(application, ::TvCatalog)
