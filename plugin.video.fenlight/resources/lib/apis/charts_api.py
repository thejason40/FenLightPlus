# -*- coding: utf-8 -*-
from caches.main_cache import main_cache
from modules.kodi_utils import make_session
# from modules.kodi_utils import logger

base_url = 'https://raw.githubusercontent.com/thejason40/FenLightPlus/main/data/top/%s.json'
timeout = 10.0
session = make_session('https://raw.githubusercontent.com')
EXPIRY_15_MINUTES, EXPIRY_6_HOURS, EXPIRY_1_WEEK = 0.25, 6, 168
type_dict = {'movie': 'movie', 'tvshow': 'series'}

def get_index():
	return _get_data('index')

def get_country(country):
	return _get_data(country)

def charts_movies(key_id, page_no):
	return _chart_results(key_id, 'movie')

def charts_tvshows(key_id, page_no):
	return _chart_results(key_id, 'tvshow')

def _chart_results(key_id, media_type):
	country, service = key_id.split(':')
	items = get_country(country).get('services', {}).get(service, {}).get('items', [])
	items = sorted([i for i in items if i.get('type') == type_dict[media_type] and i.get('tmdbId')], key=lambda k: k['typeRank'])
	return {'results': [{'id': int(i['tmdbId'].split('/')[-1])} for i in items], 'page': 1, 'total_pages': 1}

def _get_data(name):
	string = 'streaming_charts_%s' % name
	data = main_cache.get(string)
	if data is not None: return data
	try:
		response = session.get(base_url % name, timeout=timeout)
		if response.status_code != 200: raise Exception()
		data = response.json()
	except:
		data = main_cache.get('%s_stale' % string) or {}
		main_cache.set(string, data, expiration=EXPIRY_15_MINUTES)
		return data
	main_cache.set(string, data, expiration=EXPIRY_6_HOURS)
	main_cache.set('%s_stale' % string, data, expiration=EXPIRY_1_WEEK)
	return data
