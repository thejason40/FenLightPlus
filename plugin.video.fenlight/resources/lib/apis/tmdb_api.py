# -*- coding: utf-8 -*-
import datetime
import requests
from urllib.parse import unquote
from caches.meta_cache import cache_function
from caches.lists_cache import lists_cache_object
from caches.settings_cache import get_setting, set_setting
from modules.meta_lists import oscar_winners, years_tvshows
from modules.settings import get_meta_filter, tmdb_api_key
from modules.utils import make_thread_list_enumerate
from modules import kodi_utils
from modules.kodi_utils import make_session, tmdb_dict_removals, remove_keys, notification
# from modules.kodi_utils import logger

progress_dialog, get_icon, notification = kodi_utils.progress_dialog, kodi_utils.get_icon, kodi_utils.notification
sleep, confirm_dialog, ok_dialog, xbmc_monitor = kodi_utils.sleep, kodi_utils.confirm_dialog, kodi_utils.ok_dialog, kodi_utils.xbmc_monitor

EXPIRY_4_HOURS, EXPIRY_1_DAY, EXPIRY_1_WEEK = 4, 24, 168
base_url = 'https://api.themoviedb.org/3'
base_url_4 = 'https://api.themoviedb.org/4'

movies_append = 'external_ids,videos,credits,release_dates,alternative_titles,translations,images,keywords'
tvshows_append = 'external_ids,videos,credits,content_ratings,alternative_titles,translations,images,keywords'
empty_setting_check = (None, 'empty_setting', '')
session = make_session(base_url)
timeout = 20.0

def no_api_key():
	notification('Please set a valid TMDb API Key')
	return []

def movie_details(tmdb_id, api_key):
	try:
		url = '%s/movie/%s?api_key=%s&language=en&append_to_response=%s&include_image_language=en' % (base_url, tmdb_id, api_key, movies_append)
		return get_tmdb(url).json()
	except: return None

def tvshow_details(tmdb_id, api_key):
	try:
		url = '%s/tv/%s?api_key=%s&language=en&append_to_response=%s&include_image_language=en' % (base_url, tmdb_id, api_key, tvshows_append)
		return get_tmdb(url).json()
	except: return None

def episode_groups_data(tmdb_id):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'episode_groups_data_%s' % tmdb_id
	url = '%s/tv/%s/episode_groups?api_key=%s' % (base_url, tmdb_id, api_key)
	return cache_function(get_tmdb, string, url, expiration=EXPIRY_1_WEEK)

def episode_group_details(group_id):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'episode_groups_details_%s' % group_id
	url = '%s/tv/episode_group/%s?api_key=%s' % (base_url, group_id, api_key)
	return cache_function(get_tmdb, string, url, expiration=EXPIRY_1_WEEK)

def movie_set_details(collection_id, api_key):
	try:
		url = '%s/collection/%s?api_key=%s&language=en' % (base_url, collection_id, api_key)
		return get_tmdb(url).json()
	except: return None

def movie_external_id(external_source, external_id, api_key):
	try:
		string = 'movie_external_id_%s_%s' % (external_source, external_id)
		url = '%s/find/%s?api_key=%s&external_source=%s' % (base_url, external_id, api_key, external_source)
		result = cache_function(get_tmdb, string, url)
		result = result['movie_results']
		if result: return result[0]
		else: return None
	except: return None

def tvshow_external_id(external_source, external_id, api_key):
	try:
		string = 'tvshow_external_id_%s_%s' % (external_source, external_id)
		url = '%s/find/%s?api_key=%s&external_source=%s' % (base_url, external_id, api_key, external_source)
		result = cache_function(get_tmdb, string, url)
		result = result['tv_results']
		if result: return result[0]
		else: return None
	except: return None

def tmdb_movies_oscar_winners(page_no):
	return oscar_winners[page_no-1]

def tmdb_network_details(network_id):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_network_details_%s' % network_id
	url = '%s/network/%s?api_key=%s' % (base_url, network_id, api_key)
	return cache_function(get_tmdb, string, url, expiration=EXPIRY_1_WEEK)

def tmdb_keywords_by_query(query, page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_keywords_by_query_%s_%s' % (query, page_no)
	url = '%s/search/keyword?api_key=%s&query=%s&page=%s' % (base_url, api_key, query, page_no)
	return cache_function(get_tmdb, string, url, expiration=EXPIRY_1_WEEK)

def tmdb_movie_keywords(tmdb_id):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_movie_keywords_%s' % tmdb_id
	url = '%s/movie/%s/keywords?api_key=%s' % (base_url, tmdb_id, api_key)
	return cache_function(get_tmdb, string, url, expiration=EXPIRY_1_WEEK)

def tmdb_tv_keywords(tmdb_id):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_tv_keywords_%s' % tmdb_id
	url = '%s/tv/%s/keywords?api_key=%s' % (base_url, tmdb_id, api_key)
	return cache_function(get_tmdb, string, url, expiration=EXPIRY_1_WEEK)

def tmdb_movie_keyword_results(tmdb_id, page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_movie_keyword_results_%s_%s' % (tmdb_id, page_no)
	url = '%s/discover/movie?api_key=%s&language=en-US&with_keywords=%s&page=%s' % (base_url, api_key, tmdb_id, page_no)
	return lists_cache_object(get_data, string, url)

def tmdb_tv_keyword_results(tmdb_id, page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_tv_keyword_results_%s_%s' % (tmdb_id, page_no)
	url = '%s/discover/tv?api_key=%s&language=en-US&with_keywords=%s&page=%s' % (base_url, api_key, tmdb_id, page_no)
	return lists_cache_object(get_data, string, url)

def tmdb_movie_keyword_results_direct(query, page_no):
	if tmdb_api_key() in empty_setting_check: return no_api_key()
	try:
		results = tmdb_movie_keyword_results(tmdb_keywords_by_query(query, page_no)['results'][0]['id'], page_no)
		results['total_pages'] = 1
		return results
	except: return None

def tmdb_tv_keyword_results_direct(query, page_no):
	if tmdb_api_key() in empty_setting_check: return no_api_key()
	try:
		results = tmdb_tv_keyword_results(tmdb_keywords_by_query(query, page_no)['results'][0]['id'], page_no)
		results['total_pages'] = 1
		return results
	except: return None

def tmdb_company_id(query):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_company_id_%s' % query
	url = '%s/search/company?api_key=%s&query=%s' % (base_url, api_key, query)
	return cache_function(get_tmdb, string, url, expiration=EXPIRY_1_WEEK)

def tmdb_media_images(media_type, tmdb_id):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	if media_type in ('movie', 'movies'): media_type = 'movie'
	else: media_type = 'tv'
	string = 'tmdb_media_images_%s_%s' % (media_type, tmdb_id)
	url = '%s/%s/%s/images?include_image_language=en,null&api_key=%s' % (base_url, media_type, tmdb_id, api_key)
	return cache_function(get_tmdb, string, url, expiration=EXPIRY_1_WEEK)

def tmdb_media_videos(media_type, tmdb_id):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	if media_type in ('movie', 'movies'): media_type = 'movie'
	else: media_type = 'tv'
	string = 'tmdb_media_videos_%s_%s' % (media_type, tmdb_id)
	url = '%s/%s/%s/videos?api_key=%s' % (base_url, media_type, tmdb_id, api_key)
	return cache_function(get_tmdb, string, url, expiration=EXPIRY_1_WEEK)

def tmdb_movies_discover(query, page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	if '[current_date]' in query: query = query.replace('[current_date]', get_current_date())
	if '[random]' in query: query = query.replace('[random]', '')
	string = url = query + '&api_key=%s&page=%s' % (api_key, page_no)
	return lists_cache_object(get_tmdb, string, url, True, EXPIRY_1_DAY)

def tmdb_movies_popular(page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_movies_popular_%s' % page_no
	url = '%s/movie/popular?api_key=%s&language=en-US&region=US&with_original_language=en&page=%s' % (base_url, api_key, page_no)
	return lists_cache_object(get_data, string, url)

def tmdb_movies_popular_today(page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_movies_popular_today_%s' % page_no
	url = '%s/trending/movie/day?api_key=%s&language=en-US&region=US&with_original_language=en&page=%s' % (base_url, api_key, page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_DAY)

def tmdb_movies_blockbusters(page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_movies_blockbusters_%s' % page_no
	url = '%s/discover/movie?api_key=%s&language=en-US&region=US&with_original_language=en&sort_by=revenue.desc&page=%s' % (base_url, api_key, page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_WEEK)

def tmdb_movies_in_theaters(page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_movies_in_theaters_%s' % page_no
	url = '%s/movie/now_playing?api_key=%s&language=en-US&region=US&with_original_language=en&page=%s' % (base_url, api_key, page_no)
	return lists_cache_object(get_data, string, url)

def tmdb_movies_upcoming(page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	current_date, future_date = get_dates(31, reverse=False)
	string = 'tmdb_movies_upcoming_%s' % page_no
	url = '%s/discover/movie?api_key=%s&language=en-US&region=US&with_original_language=en&release_date.gte=%s&release_date.lte=%s&with_release_type=3|2|1&page=%s' \
							% (base_url, api_key, current_date, future_date, page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_DAY)

def tmdb_movies_latest_releases(page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	current_date, previous_date = get_dates(31, reverse=True)
	string = 'tmdb_movies_latest_releases_%s' % page_no
	url = '%s/discover/movie?api_key=%s&language=en-US&region=US&with_original_language=en&release_date.gte=%s&release_date.lte=%s&with_release_type=4|5|6&page=%s' \
							% (base_url, api_key, previous_date, current_date, page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_DAY)

def tmdb_movies_premieres(page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	current_date, previous_date = get_dates(31, reverse=True)
	string = 'tmdb_movies_premieres_%s' % page_no
	url = '%s/discover/movie?api_key=%s&language=en-US&region=US&with_original_language=en&release_date.gte=%s&release_date.lte=%s&with_release_type=1|3|2&page=%s' \
							% (base_url, api_key, previous_date, current_date, page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_DAY)

def tmdb_movies_highest_rated(page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_movies_highest_rated_%s' % page_no
	url = '%s/discover/movie?api_key=%s&language=en-US&region=US&with_original_language=en&sort_by=vote_average.desc&vote_count.gte=2000&release_date.lte=%s&page=%s' \
							% (base_url, api_key, get_current_date(), page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_WEEK)

def tmdb_movies_hidden_gems(page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_movies_hidden_gems_%s' % page_no
	# without_genres=10402 (Music) drops recorded concerts/live shows, which flood a low-vote gems list
	url = '%s/discover/movie?api_key=%s&language=en-US&region=US&with_original_language=en&without_genres=10402&sort_by=vote_average.desc&vote_average.gte=7.5&vote_count.gte=50&vote_count.lte=500&release_date.lte=%s&page=%s' \
							% (base_url, api_key, get_current_date(), page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_WEEK)

def tmdb_movies_quick_watches(page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_movies_quick_watches_%s' % page_no
	url = '%s/discover/movie?api_key=%s&language=en-US&region=US&with_original_language=en&without_genres=10402&sort_by=popularity.desc&with_runtime.lte=90&vote_average.gte=6.5&release_date.lte=%s&page=%s' \
							% (base_url, api_key, get_current_date(), page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_WEEK)

def tmdb_movies_genres(genre_id, page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_movies_genres_%s_%s' % (genre_id, page_no)
	url = '%s/discover/movie?api_key=%s&with_genres=%s&language=en-US&region=US&with_original_language=en&release_date.lte=%s&page=%s' \
			% (base_url, api_key, genre_id, get_current_date(), page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_WEEK)

def tmdb_movies_languages(language, page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_movies_languages_%s_%s' % (language, page_no)
	url = '%s/discover/movie?api_key=%s&language=en-US&with_original_language=%s&release_date.lte=%s&page=%s' \
			% (base_url, api_key, language, get_current_date(), page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_WEEK)

def tmdb_movies_certifications(certification, page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_movies_certifications_%s_%s' % (certification, page_no)
	url = '%s/discover/movie?api_key=%s&language=en-US&region=US&with_original_language=en&certification_country=US&certification=%s&sort_by=%s&release_date.lte=%s&page=%s' \
							% (base_url, api_key, certification, 'popularity.desc', get_current_date(), page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_WEEK)

def tmdb_movies_year(year, page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_movies_year_%s_%s' % (year, page_no)
	url = '%s/discover/movie?api_key=%s&language=en-US&region=US&with_original_language=en&certification_country=US&primary_release_year=%s&page=%s' \
							% (base_url, api_key, year, page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_WEEK)

def tmdb_movies_decade(decade, page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_movies_decade_%s_%s' % (decade, page_no)
	start = '%s-01-01' % decade
	end = get_dates(2)[0] if decade == '2020' else '%s-12-31' % str(int(decade) + 9)
	url = '%s/discover/movie?api_key=%s&language=en-US&region=US&with_original_language=en&primary_release_date.gte=%s' \
			'&primary_release_date.lte=%s&page=%s' % (base_url, api_key, start, end, page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_WEEK)

def tmdb_movies_providers(provider, page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_movies_providers_%s_%s' % (provider, page_no)
	url = '%s/discover/movie?api_key=%s&watch_region=US&with_watch_providers=%s&vote_count.gte=100&page=%s' % (base_url, api_key, provider, page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_WEEK)
	
def tmdb_movies_providers_uk(provider, page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_movies_providers_UK_%s_%s' % (provider, page_no)
	url = '%s/discover/movie?api_key=%s&watch_region=GB&with_watch_providers=%s&vote_count.gte=100&page=%s' % (base_url, api_key, provider, page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_WEEK)

def tmdb_movies_recommendations(tmdb_id, page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_movies_recommendations_%s_%s' % (tmdb_id, page_no)
	url = '%s/movie/%s/recommendations?api_key=%s&language=en-US&region=US&with_original_language=en&page=%s' % (base_url, tmdb_id, api_key, page_no)
	return lists_cache_object(get_data, string, url)

def tmdb_movies_search(query, page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	meta_filter = get_meta_filter()
	string = 'tmdb_movies_search_%s_%s_%s' % (query, meta_filter, page_no)
	url = '%s/search/movie?api_key=%s&language=en-US&include_adult=%s&query=%s&page=%s' % (base_url, api_key, meta_filter, query, page_no)
	return lists_cache_object(get_data, string, url)

def tmdb_movies_companies(company_id, page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_movies_companies_%s_%s' % (company_id, page_no)
	url = '%s/discover/movie?api_key=%s&language=en-US&region=US&with_original_language=en&with_companies=%s&page=%s' \
							% (base_url, api_key, company_id, page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_WEEK)

def tmdb_movies_reviews(tmdb_id, page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	url = '%s/movie/%s/reviews?api_key=%s&page=%s' % (base_url, tmdb_id, api_key, page_no)
	return get_tmdb(url).json()

def tmdb_tv_discover(query, page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	if '[current_date]' in query: query = query.replace('[current_date]', get_current_date())
	if '[random]' in query: query = query.replace('[random]', '')
	string = url = query + '&api_key=%s&page=%s' % (api_key, page_no)
	return lists_cache_object(get_tmdb, string, url, True, EXPIRY_1_DAY)

def tmdb_tv_popular(page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_tv_popular_%s' % page_no
	url = '%s/tv/popular?api_key=%s&language=en-US&region=US&with_original_language=en&page=%s' % (base_url, api_key, page_no)
	return lists_cache_object(get_data, string, url)

def tmdb_tv_popular_today(page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_tv_popular_today_%s' % page_no
	url = '%s/trending/tv/day?api_key=%s&language=en-US&region=US&with_original_language=en&page=%s' % (base_url, api_key, page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_DAY)

def tmdb_tv_premieres(page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	current_date, previous_date = get_dates(31, reverse=True)
	string = 'tmdb_tv_premieres_%s' % page_no
	url = '%s/discover/tv?api_key=%s&language=en-US&region=US&with_original_language=en&include_null_first_air_dates=false&first_air_date.gte=%s&first_air_date.lte=%s&page=%s' \
							% (base_url, api_key, previous_date, current_date, page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_DAY)

def tmdb_tv_airing_today(page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_tv_airing_today_%s' % page_no
	url = '%s/tv/airing_today?api_key=%s&language=en-US&region=US&with_original_language=en&page=%s' % (base_url, api_key, page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_DAY)

def tmdb_tv_on_the_air(page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_tv_on_the_air_%s' % page_no
	url = '%s/tv/on_the_air?api_key=%s&language=en-US&region=US&with_original_language=en&page=%s' % (base_url, api_key, page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_DAY)

def tmdb_tv_upcoming(page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	current_date, future_date = get_dates(31, reverse=False)
	string = 'tmdb_tv_upcoming_%s' % page_no
	url = '%s/discover/tv?api_key=%s&language=en-US&region=US&with_original_language=en&first_air_date.gte=%s&first_air_date.lte=%s&page=%s' \
							% (base_url, api_key, current_date, future_date, page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_DAY)

def tmdb_tv_genres(genre_id, page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_tv_genres_%s_%s' % (genre_id, page_no)
	url = '%s/discover/tv?api_key=%s&with_genres=%s&language=en-US&region=US&with_original_language=en&include_null_first_air_dates=false&first_air_date.lte=%s&page=%s' \
							% (base_url, api_key, genre_id, get_current_date(), page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_WEEK)

def tmdb_tv_languages(language, page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_tv_languages_%s_%s' % (language, page_no)
	url = '%s/discover/tv?api_key=%s&language=en-US&include_null_first_air_dates=false&with_original_language=%s&first_air_date.lte=%s&page=%s' \
							% (base_url, api_key, language, get_current_date(), page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_WEEK)

def tmdb_tv_networks(network_id, page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_tv_networks_%s_%s' % (network_id, page_no)
	url = '%s/discover/tv?api_key=%s&language=en-US&region=US&with_original_language=en&include_null_first_air_dates=false&with_networks=%s&first_air_date.lte=%s&page=%s' \
							% (base_url, api_key, network_id, get_current_date(), page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_WEEK)

def tmdb_tv_providers(provider, page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_tv_providers_%s_%s' % (provider, page_no)
	url = '%s/discover/tv?api_key=%s&watch_region=US&with_watch_providers=%s&include_null_first_air_dates=false&vote_count.gte=100&first_air_date.lte=%s&page=%s' \
				% (base_url, api_key, provider, get_current_date(), page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_WEEK)
	
def tmdb_tv_providers_uk(provider, page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_tv_providers_UK_%s_%s' % (provider, page_no)
	url = '%s/discover/tv?api_key=%s&watch_region=GB&with_watch_providers=%s&include_null_first_air_dates=false&vote_count.gte=100&first_air_date.lte=%s&page=%s' \
				% (base_url, api_key, provider, get_current_date(), page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_WEEK)

def tmdb_tv_year(year, page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_tv_year_%s_%s' % (year, page_no)
	url = '%s/discover/tv?api_key=%s&language=en-US&region=US&with_original_language=en&include_null_first_air_dates=false&first_air_date_year=%s&page=%s' \
							% (base_url, api_key, year, page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_WEEK)

def tmdb_tv_decade(decade, page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_tv_decade_%s_%s' % (decade, page_no)
	start = '%s-01-01' % decade
	end = get_dates(2)[0] if decade == '2020' else '%s-12-31' % str(int(decade) + 9)
	url = '%s/discover/tv?api_key=%s&language=en-US&region=US&with_original_language=en&include_null_first_air_dates=false&first_air_date.gte=%s' \
			'&first_air_date.lte=%s&page=%s' % (base_url, api_key, start, end, page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_WEEK)

def tmdb_tv_recommendations(tmdb_id, page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_tv_recommendations_%s_%s' % (tmdb_id, page_no)
	url = '%s/tv/%s/recommendations?api_key=%s&language=en-US&region=US&with_original_language=en&page=%s' % (base_url, tmdb_id, api_key, page_no)
	return lists_cache_object(get_data, string, url)

def tmdb_tv_search(query, page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	meta_filter = get_meta_filter()
	string = 'tmdb_tv_search_%s_%s_%s' % (query, meta_filter, page_no)
	url = '%s/search/tv?api_key=%s&language=en-US&include_adult=%s&query=%s&page=%s' % (base_url, api_key, meta_filter, query, page_no)
	return lists_cache_object(get_data, string, url)

def tmdb_tv_reviews(tmdb_id, page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	url = '%s/tv/%s/reviews?api_key=%s&page=%s' % (base_url, tmdb_id, api_key, page_no)
	return get_tmdb(url).json()

def tmdb_tv_highest_rated(page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_tv_highest_rated_%s' % page_no
	url = '%s/discover/tv?api_key=%s&language=en-US&include_null_first_air_dates=false&sort_by=vote_average.desc&vote_count.gte=1000&first_air_date.lte=%s&page=%s' \
							% (base_url, api_key, get_current_date(), page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_WEEK)

def tmdb_tv_hidden_gems(page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_tv_hidden_gems_%s' % page_no
	url = '%s/discover/tv?api_key=%s&language=en-US&with_original_language=en&include_null_first_air_dates=false&sort_by=vote_average.desc&vote_average.gte=7.5&vote_count.gte=50&vote_count.lte=500&first_air_date.lte=%s&page=%s' \
							% (base_url, api_key, get_current_date(), page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_WEEK)

def tmdb_tv_marathon(page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_tv_marathon_%s' % page_no
	url = '%s/discover/tv?api_key=%s&language=en-US&include_null_first_air_dates=false&with_status=3&sort_by=popularity.desc&vote_average.gte=7&vote_count.gte=200&first_air_date.lte=%s&page=%s' \
							% (base_url, api_key, get_current_date(), page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_WEEK)

def tmdb_anime_popular(page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_tv_anime_popular_%s' % page_no
	url = '%s/discover/tv?api_key=%s&with_keywords=210024&page=%s' % (base_url, api_key, page_no)
	return lists_cache_object(get_data, string, url)

def tmdb_anime_popular_recent(page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_tv_anime_popular_recent_%s' % page_no
	url = '%s/discover/tv?api_key=%s&with_keywords=210024&sort_by=first_air_date.desc&include_null_first_air_dates=false&first_air_date_year=%s&page=%s' \
							% (base_url, api_key, years_tvshows[0]['id'], page_no)
	return lists_cache_object(get_data, string, url)

def tmdb_anime_premieres(page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	current_date, previous_date = get_dates(93, reverse=True)
	string = 'tmdb_anime_premieres_%s' % page_no
	url = '%s/discover/tv?api_key=%s&with_keywords=210024&include_null_first_air_dates=false&first_air_date.gte=%s&first_air_date.lte=%s&page=%s' \
							% (base_url, api_key, previous_date, current_date, page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_DAY)

def tmdb_anime_upcoming(page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	current_date, future_date = get_dates(93, reverse=False)
	string = 'tmdb_anime_upcoming_%s' % page_no
	url = '%s/discover/tv?api_key=%s&with_keywords=210024&first_air_date.gte=%s&first_air_date.lte=%s&sort_by=first_air_date.asc&page=%s' \
							% (base_url, api_key, current_date, future_date, page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_DAY)

def tmdb_anime_on_the_air(page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	current_date, future_date = get_dates(7, reverse=False)
	string = 'tmdb_anime_on_the_air_%s' % page_no
	url = '%s/discover/tv?api_key=%s&with_keywords=210024&air_date.gte=%s&air_date.lte=%s&page=%s' \
							% (base_url, api_key, current_date, future_date, page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_DAY)

def tmdb_anime_highest_rated(page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_anime_highest_rated_%s' % page_no
	url = '%s/discover/tv?api_key=%s&with_keywords=210024&include_null_first_air_dates=false&sort_by=vote_average.desc&vote_count.gte=100&first_air_date.lte=%s&page=%s' \
							% (base_url, api_key, get_current_date(), page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_WEEK)

def tmdb_anime_hidden_gems(page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_anime_hidden_gems_%s' % page_no
	url = '%s/discover/tv?api_key=%s&with_keywords=210024&include_null_first_air_dates=false&sort_by=vote_average.desc&vote_average.gte=7.5&vote_count.gte=50&vote_count.lte=500&first_air_date.lte=%s&page=%s' \
							% (base_url, api_key, get_current_date(), page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_WEEK)

def tmdb_anime_marathon(page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_anime_marathon_%s' % page_no
	url = '%s/discover/tv?api_key=%s&with_keywords=210024&include_null_first_air_dates=false&with_status=3&sort_by=popularity.desc&vote_average.gte=7&vote_count.gte=50&first_air_date.lte=%s&page=%s' \
							% (base_url, api_key, get_current_date(), page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_WEEK)

def tmdb_anime_genres(genre_id, page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_anime_genres_%s_%s' % (genre_id, page_no)
	url = '%s/discover/tv?api_key=%s&with_keywords=210024&with_genres=%s&include_null_first_air_dates=false&first_air_date.lte=%s&page=%s' \
							% (base_url, api_key, genre_id, get_current_date(), page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_WEEK)

def tmdb_anime_providers(provider, page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_anime_providers_%s_%s' % (provider, page_no)
	url = '%s/discover/tv?api_key=%s&with_keywords=210024&watch_region=US&with_watch_providers=%s&include_null_first_air_dates=false&first_air_date.lte=%s&page=%s' \
				% (base_url, api_key, provider, get_current_date(), page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_WEEK)

def tmdb_anime_year(year, page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_anime_year_%s_%s' % (year, page_no)
	url = '%s/discover/tv?api_key=%s&with_keywords=210024&include_null_first_air_dates=false&first_air_date_year=%s&page=%s' % (base_url, api_key, year, page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_WEEK)

def tmdb_anime_decade(decade, page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_anime_decade_%s_%s' % (decade, page_no)
	start = '%s-01-01' % decade
	end = get_dates(2)[0] if decade == '2020' else '%s-12-31' % str(int(decade) + 9)
	url = '%s/discover/tv?api_key=%s&with_keywords=210024&include_null_first_air_dates=false&first_air_date.gte=%s' \
			'&first_air_date.lte=%s&page=%s' % (base_url, api_key, start, end, page_no)
	return lists_cache_object(get_data, string, url, expiration= EXPIRY_1_WEEK)

def tmdb_anime_search(query, page_no):
	def _process(foo):
		data = get_data(url)
		if data['results']:
			threads = list(make_thread_list_enumerate(_anime_checker, data['results']))
			[i.join() for i in threads]
			anime_results.sort(key=lambda k: k[0])
			data['results'] = [i[1] for i in anime_results]
		return data
	def _anime_checker(count, item):
		try: keywords = tmdb_tv_keywords(item['id'])
		except: return
		if any([x['id'] == 210024 for x in keywords['results']]): anime_results_append((count, item))
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	meta_filter = get_meta_filter()
	string = 'tmdb_anime_search_%s_%s_%s' % (query, meta_filter, page_no)
	url = '%s/search/tv?api_key=%s&include_adult=%s&query=%s&page=%s' % (base_url, api_key, meta_filter, query, page_no)
	anime_results = []
	anime_results_append = anime_results.append
	return lists_cache_object(_process, string, url)

def tmdb_popular_people(page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_people_popular_%s' % page_no
	url = '%s/person/popular?api_key=%s&language=en&page=%s' % (base_url, api_key, page_no)
	return cache_function(get_tmdb, string, url, EXPIRY_1_DAY)

def tmdb_trending_people_day(page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_people_trending_day_%s' % page_no
	url = '%s/trending/person/day?api_key=%s&page=%s' % (base_url, api_key, page_no)
	return cache_function(get_tmdb, string, url, EXPIRY_1_DAY)

def tmdb_trending_people_week(page_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_people_trending_week_%s' % page_no
	url = '%s/trending/person/week?api_key=%s&page=%s' % (base_url, api_key, page_no)
	return cache_function(get_tmdb, string, url, EXPIRY_1_WEEK)

def tmdb_people_full_info(actor_id):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	string = 'tmdb_people_full_info_%s' % actor_id
	url = '%s/person/%s?api_key=%s&language=en&append_to_response=external_ids,combined_credits,images,tagged_images' % (base_url, actor_id, api_key)
	return cache_function(get_tmdb, string, url, expiration=EXPIRY_1_WEEK)

def tmdb_people_info(query, page_no=1):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	meta_filter = get_meta_filter()
	string = 'tmdb_people_info_%s_%s_%s' % (query, meta_filter, page_no)
	url = '%s/search/person?api_key=%s&language=en&include_adult=%s&query=%s&page=%s' % (base_url, api_key, meta_filter, query, page_no)
	return cache_function(get_tmdb, string, url, expiration=EXPIRY_4_HOURS)

def season_episodes_details(tmdb_id, season_no):
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	try:
		url = '%s/tv/%s/season/%s?api_key=%s&language=en&append_to_response=credits' % (base_url, tmdb_id, season_no, api_key)
		return get_tmdb(url).json()
	except: return None

def get_dates(days, reverse=True):
	current_date = get_current_date(return_str=False)
	if reverse: new_date = (current_date - datetime.timedelta(days=days)).strftime('%Y-%m-%d')
	else: new_date = (current_date + datetime.timedelta(days=days)).strftime('%Y-%m-%d')
	return str(current_date), new_date

def get_current_date(return_str=True):
	if return_str: return str(datetime.date.today())
	else: return datetime.date.today()

def get_reviews_data(media_type, tmdb_id):
	def builder(media_type, tmdb_id):
		reviews_list, all_data = [], []
		template = '[B]%02d. %s%s[/B][CR][CR]%s'
		media_type = 'movie' if media_type in ('movie', 'movies') else 'tv'
		function = tmdb_movies_reviews if media_type  == 'movie' else tmdb_tv_reviews
		next_page, total_pages = 1, 1
		while next_page <= total_pages:
			data = function(tmdb_id, next_page)
			all_data += data['results']
			total_pages = data['total_pages']
			next_page = data['page'] + 1
		if all_data:
			for count, item in enumerate(all_data, 1):
				try:
					user = item['author'].upper()
					rating = item['author_details'].get('rating')
					if rating: rating = ' - %s/10' % str(rating).split('.')[0]
					else: rating = ''
					content = template % (count, user, rating, item['content'])
					reviews_list.append(content)
				except: pass
		return reviews_list
	string, url = 'tmdb_%s_reviews_%s' % (media_type ,tmdb_id), [media_type, tmdb_id]
	return cache_function(builder, string, url, json=False, expiration=EXPIRY_1_WEEK)

def get_data(url):
	data = get_tmdb(url).json()
	data['results'] = [remove_keys(i, tmdb_dict_removals) for i in data['results']]
	return data

def get_tmdb(url):
	try: response = session.get(url, timeout=timeout)
	except: response = None
	return response

def auth():
	access_token = None
	account_id = None
	t_o = 5
	read_token = get_setting('tmdb_read_access_token')
	head = {'Authorization': f"Bearer {read_token}"}
	url = '%s/auth/request_token' % (base_url_4)
	response = requests.post(url, headers=head, timeout=t_o).json()
	if not response['success']: return
	url = 'https://www.themoviedb.org/auth/access?request_token=%s' % response['request_token']
	try:
		tiny_url, qr_icon = kodi_utils.make_qr(url)
	except: pass
	
	content = 'Authorize TMDB Account[CR]Scan the QR code or navigate to: [B]%s[/B]' % tiny_url
	progressDialog = progress_dialog('TMDB Authorize', qr_icon)
	progressDialog.update(content, 0)
	data = {'request_token': response['request_token']}
	url = '%s/auth/access_token' % (base_url_4)
	
	# Give the user more time to approve the request
	timeout_counter = 0
	max_timeout = 120  # Give the user 2 minutes to approve
	
	while not progressDialog.iscanceled() and not access_token and timeout_counter < max_timeout:
		sleep(1000)  # Wait 1 second between attempts
		timeout_counter += 1
		
		# Update progress dialog with countdown
		remaining = max_timeout - timeout_counter
		progressDialog.update(f"{content}\n\nTime remaining: {remaining} seconds", int((timeout_counter / max_timeout) * 100))
		
		# Only check for the token every 5 seconds to reduce API calls
		if timeout_counter % 5 == 0:
			try: 
				response = requests.post(url, json=data, headers=head, timeout=t_o).json()
			except Exception as e:
				kodi_utils.logger('request exception', str(e))
				continue
			
			# Check if authorization was successful
			if response.get('success', False) == True and 'access_token' in response and 'account_id' in response:
				try:
					access_token = str(response['access_token'])
					account_id = str(response['account_id'])
					break  # Success! Exit the loop
				except Exception as e:
					kodi_utils.logger('extraction error', str(e))
	
	progressDialog.close()
	
	# Check if we got the tokens
	if access_token:
		set_setting('tmdb.access_token', access_token)
		set_setting('tmdb.account_id', account_id)
		ok_dialog(text='Success')
	else:
		# If the user canceled or time ran out
		if progressDialog.iscanceled():
			ok_dialog(text='Authorization canceled')
		else:
			ok_dialog(text='Authorization timed out. Please try again.')

def unauth():
	read_token = get_setting('tmdb_read_access_token')
	access_token = get_setting('tmdb.access_token')
	head = {'Authorization': f"Bearer {read_token}"}
	data = {'access_token': access_token}
	url = '%s/auth/access_token' % (base_url_4)
	response = requests.delete(url, json=data, headers=head).json()
	set_setting('tmdb.access_token', 'empty_setting')
	set_setting('tmdb.account_id', 'empty_setting')
	ok_dialog(text='Success')
	
def get_lists(list_type, page = 1):
	read_token = get_setting('tmdb_read_access_token')
	access_token = get_setting('tmdb.access_token')
	head = {'Authorization': f"Bearer {access_token}"}
	account_id = get_setting('tmdb.account_id')
	url = f'{base_url_4}/account/{account_id}/lists?page={page}'

	try:
		response = requests.get(url, headers=head)
		response.raise_for_status()
		json_data = response.json()
	except Exception as e:
		kodi_utils.logger('TMDB API ERROR', f"{type(e).__name__}: {str(e)}")
		return []

	if json_data.get('page', 0) > 0:
		return json_data
	else:
		return []

def tmdb_list_items(list_id, page_no):
	string = 'tmdb_list_%s_%s' % (list_id, page_no)
	params = {'list_id': list_id, 'page_no': page_no}
	return lists_cache_object(get_list_items, string, params)

def get_list_items(params):
	list_id = params.get('list_id')
	page_no = params.get('page_no')
	access_token = get_setting('tmdb.access_token')
	head = {'Authorization': f'Bearer {access_token}'}
	url = f'{base_url_4}/list/{list_id}?page={page_no}'
	response = requests.get(url, headers=head).json()
	return response
	
def new_tmdb_list(keep_open = False, params = '', default_text = ''):
	list_title = kodi_utils.kodi_dialog().input('', defaultt = default_text)
	if not list_title:
		notification('Cancelled', 3000)
		return
	list_name = unquote(list_title)
	access_token = get_setting('tmdb.access_token')
	payload = {
		"name": list_title,
		"iso_3166_1": "US",
		"iso_639_1": "en"
		}
	head = {
		'accept': 'application/json',
		'content-type': 'application/json',
		'Authorization': f'Bearer {access_token}'
		}
	url = f'{base_url_4}/list'
	response = requests.post(url, json=payload, headers=head).json()
	if response.get('success'):
		return_params = { 'list_title': list_title, 'list_id': response.get('id') }
		if not default_text == '':
			return return_params
		notification('List Created', 1500)
		kodi_utils.kodi_refresh()
		if keep_open:
			run_plugin = 'RunPlugin(%s)'
			kodi_utils.execute_builtin(run_plugin % kodi_utils.build_url(params))
		return return_params
	else:
		notification('Error', 3000)
		kodi_utils.logger('TMDB New List Error', response.get('status_message'))

def del_tmdb_list(list_id):
	if not kodi_utils.confirm_dialog(): 
		notification('Cancelled', 3000)
		return
	access_token = get_setting('tmdb.access_token')
	head = {
		'accept': 'application/json',
		'content-type': 'application/json',
		'Authorization': f'Bearer {access_token}'
		}
	url = f'{base_url_4}/list/{list_id}'
	response = requests.delete(url, headers=head).json()
	if response.get('success'):
		notification('Success', 3000)
		kodi_utils.kodi_refresh()
	else:
		notification('Error', 3000)
		kodi_utils.logger('TMDB Delete List Error', response.get('status_message'))	
		
def clear_tmdb_list(list_id, list_name):
	if not kodi_utils.confirm_dialog(text='This will delete all contents from the list \'%s\'. Are you sure?' % list_name): 
		notification('Cancelled', 3000)
		return
	access_token = get_setting('tmdb.access_token')
	head = {
		'accept': 'application/json',
		'Authorization': f'Bearer {access_token}'
		}
	url = f'{base_url_4}/list/{list_id}/clear'
	response = requests.get(url, headers=head).json()
	if response.get('success'):
		from caches.lists_cache import lists_cache
		lists_cache.delete_tmdb_list(list_id)
		notification('Success', 3000)
		kodi_utils.kodi_refresh()
	else:
		notification('Error', 3000)
		kodi_utils.logger('TMDB Clear List Error', response.get('status_message'))
		
def clear_list_cache(list_id, list_name):
	if not kodi_utils.confirm_dialog(text='Delete the cache for the list \'%s\'?' % list_name): 
		return notification('Cancelled', 3000)
	from caches.lists_cache import lists_cache
	if lists_cache.delete_tmdb_list(list_id): return notification('Success', 3000)
		
def trakt_to_tmdb(choice, list_id, tmdb_list_name, confirm = True):
	from apis import trakt_api
	import xbmcgui
	
	media_type = ''
	progress = xbmcgui.DialogProgress()
	progress.create('Processing Trakt List', 'Please wait...')
	if not isinstance(choice, str):
		list_ids = choice.get('ids')
		list_type = choice.get('list_type')
		with_auth = list_type == 'my_lists'
		trakt_items = trakt_api.get_trakt_list_contents(list_type, choice.get('user'), list_ids.get('slug'), with_auth)
	elif '_tv' in choice:
		media_type = 'show'
	elif '_movies' in choice:
		media_type = 'movie'
	if '_collection' in choice:
		trakt_items = trakt_api.trakt_fetch_collection_watchlist('collection', media_type)
	elif '_watchlist' in choice:
		trakt_items = trakt_api.trakt_fetch_collection_watchlist('watchlist', media_type)
	elif '_favor' in choice:
		trakt_items = trakt_api.trakt_favorites(media_type, '')
		
	total_items = len(trakt_items)
	
	if progress.iscanceled():
		progress.close()
		notification('Cancelled', 3000)
		return
	
	progress.close()
	
	if confirm:
		if not kodi_utils.confirm_dialog(text='Import %s items to the list \'%s\'?' % (str(total_items), tmdb_list_name)): 
			return notification('Cancelled', 3000)

	tmdb_items = []
	skipped_items = 0
	progress.create('Processing Trakt List', 'Please wait...')
	
	for index, i in enumerate(trakt_items):
		# Update progress bar (calculate percentage)
		percent = int((index / float(total_items)) * 100)
		progress.update(percent, f'Processing item {index+1} of {total_items}')
		
		# Check if user cancelled
		if progress.iscanceled():
			progress.close()
			notification('Cancelled', 3000)
			return
		
		try:
			# Skip seasons and episodes - they can't be imported to TMDB
			if i.get('type') in ['season', 'episode']:
				skipped_items += 1
				continue
				
			# For movies and shows only
			if media_type == '':
				media_type2 = i['type']
			else: 
				media_type2 = media_type
				
			if media_type2 == 'show':
				media_type2 = 'tv'
				
			media_ids = i.get('media_ids')
			if not media_ids:
				# Handle alternative formats
				if 'tmdb_id' in i:
					tmdb_id = i['tmdb_id']
				else:
					continue
			else:
				tmdb_id = media_ids.get('tmdb')
			
			if tmdb_id:
				tmdb_items.append({'media_type': media_type2, 'media_id': tmdb_id})
				
		except Exception as e:
			kodi_utils.logger('Error', f'Failed to process item: {str(e)}')
			continue
	
	# Close progress dialog
	progress.close()
		
	if tmdb_items:

		try:
			add_remove('add', tmdb_items, list_id, True)
			notification(f'Added {len(tmdb_items)} items to list', 3000)
			kodi_utils.kodi_refresh()
		except Exception as e:
			kodi_utils.logger('TMDB Error', str(e))
			notification('Error adding to TMDB list', 3000)
	else:
		notification('No valid items found to add', 3000)
	
def update_list(payload, list_id):
	access_token = get_setting('tmdb.access_token')
	head = {
		'accept': 'application/json',
		'content-type': 'application/json',
		'Authorization': f'Bearer {access_token}'
		}
	url = f'{base_url_4}/list/{list_id}'
	response = requests.put(url, json=payload, headers=head).json()
	if response.get('success'):
		notification('Success', 3000)
		kodi_utils.kodi_refresh()
	else:
		notification('Error', 3000)
		kodi_utils.logger('TMDB Set Backdrop Error', response.get('status_message'))

def rename_list(list_id):
	list_title = kodi_utils.kodi_dialog().input('')
	if not list_title:
		notification('Cancelled', 3000)
		return
	payload = { 'name': list_title }
	return update_list(payload, list_id)

def set_list_backdrop(list_id, backdrop_url):
	backdrop = str(backdrop_url).split('/')[-1]
	payload = { 'backdrop_path': '/%s' % backdrop }
	return update_list(payload, list_id)

def check_item_exists(item_params, list_params):
	access_token = get_setting('tmdb.access_token')
	head = {
		'accept': 'application/json',
		'Authorization': f'Bearer {access_token}'
		}
	media_type, media_id = item_params.get('media_type'), item_params.get('tmdb_id')
	list_id = list_params.get('list_id')
	url = f'{base_url_4}/list/{list_id}/item_status?media_id={media_id}&media_type={media_type}'
	response = requests.get(url, headers=head).json()
	if response.get('success') and response.get('status_message') == 'Success.':
		exists = True
	else:
		exists = False
	return exists

from math import ceil

def chunk_list(lst, chunk_size):
	return [lst[i:i + chunk_size] for i in range(0, len(lst), chunk_size)]

def add_remove(add_or_remove, items, list_id, show_progress=False):
	if add_or_remove not in ('add', 'remove'):
		kodi_utils.logger('TMDB Add_Remove Error', f"Invalid value for 'add_or_remove': {add_or_remove}")
		return notification('Error', 1500)

	access_token = get_setting('tmdb.access_token')
	head = {
		'accept': 'application/json',
		'content-type': 'application/json',
		'Authorization': f'Bearer {access_token}'
	}

	url = f'{base_url_4}/list/{list_id}/items'

	progress_dialog = None
	if show_progress:
		import xbmcgui
		import threading
		import time

		progress_dialog = xbmcgui.DialogProgressBG()
		progress_dialog.create('TMDB List Update', 'Preparing to send requests...')

		api_complete = threading.Event()
		progress = 0
		
		def update_progress():
			phase_messages = [
				'Sending request to TMDB...',
				'Waiting for response...',
				'Still waiting for response...',
				'This might take a moment...',
				'TMDB is processing your request...'
			]
			index = 0
			
			while not api_complete.is_set():
				progress_dialog.update(progress, message=phase_messages[index])
				if not index == 4: index = (index + 1) % len(phase_messages)
				for _ in range(20):
					if api_complete.is_set(): break
					time.sleep(0.25)

		update_thread = threading.Thread(target=update_progress)
		update_thread.daemon = True
		update_thread.start()

	try:
		chunks = chunk_list(items, 100)
		total_chunks = len(chunks)

		for idx, chunk in enumerate(chunks, 1):
			payload = {'items': chunk}
			try:
				if add_or_remove == 'add':
					response = requests.post(url, json=payload, headers=head, timeout=120)
				else:
					response = requests.delete(url, json=payload, headers=head, timeout=120)


				if response.status_code not in (200, 201):
					continue  # Skip this chunk and keep going

				if response.text:
					data = response.json()
					if not data.get('success'):
						continue

			except requests.exceptions.RequestException as e:
				kodi_utils.logger('TMDB Chunk Network Error', f'Chunk {idx}: {str(e)}')
				continue  # Skip this chunk on network error

			if response.text:
				data = response.json()
				if not data.get('success'):
					if progress_dialog:
						progress_dialog.close()
					notification('TMDB Response Error', 3000)
					return False

			if show_progress:
				progress = int((idx / total_chunks) * 100)
				progress_dialog.update(progress, message=f'Processing chunk {idx} of {total_chunks}...')

		if progress_dialog:
			progress_dialog.update(100, message='Complete!')
			time.sleep(0.5)
			progress_dialog.close()

		from caches.lists_cache import lists_cache
		lists_cache.delete_tmdb_list(list_id)

		notification('Success', 3000)
		kodi_utils.kodi_refresh()
		return True

	except requests.exceptions.RequestException as e:
		if show_progress:
			api_complete.set()
			progress_dialog.close()
		notification('Network Error', 3000)
		kodi_utils.logger('TMDB Add_Remove Network Error', str(e))
		kodi_utils.kodi_refresh()
		return False
	except Exception as e:
		if show_progress:
			api_complete.set()
			progress_dialog.close()
		notification('Error Processing Response', 3000)
		kodi_utils.logger('TMDB Add_Remove Error', str(e))
		return False
	finally:
		if show_progress:
			api_complete.set()
