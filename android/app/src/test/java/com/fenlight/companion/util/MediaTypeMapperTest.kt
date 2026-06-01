package com.fenlight.companion.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaTypeMapperTest {

    @Test
    fun traktKey_maps_show_and_tv_to_shows() {
        assertEquals("shows", MediaTypeMapper.traktKey("show"))
        assertEquals("shows", MediaTypeMapper.traktKey("tv"))
    }

    @Test
    fun traktKey_maps_everything_else_to_movies() {
        assertEquals("movies", MediaTypeMapper.traktKey("movie"))
        assertEquals("movies", MediaTypeMapper.traktKey(""))
    }

    @Test
    fun tmdbType_maps_show_and_tv_to_tv() {
        assertEquals("tv", MediaTypeMapper.tmdbType("show"))
        assertEquals("tv", MediaTypeMapper.tmdbType("tv"))
    }

    @Test
    fun tmdbType_maps_everything_else_to_movie() {
        assertEquals("movie", MediaTypeMapper.tmdbType("movie"))
        assertEquals("movie", MediaTypeMapper.tmdbType("anything"))
    }
}
