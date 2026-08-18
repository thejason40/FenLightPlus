# -*- coding: utf-8 -*-
import datetime
from caches.lists_cache import lists_cache, lists_cache_object
from modules.settings import tmdb_api_key
from modules.kodi_utils import make_session, addon_version
from modules.utils import make_thread_list, paginate_list
from apis.tmdb_api import get_tmdb, get_current_date, no_api_key, empty_setting_check, base_url as tmdb_base_url, EXPIRY_1_DAY
# from modules.kodi_utils import logger

base_url = 'https://moviesthisday.com'
by_day_url = '%s/movies/by-day/%s'
session = make_session(base_url)
timeout = 10.0
PAGE_LIMIT = 20
FALLBACK_YEARS = 50

def get_popularity(item):
	try: return float(item.get('popularity') or 0)
	except: return 0.0

def valid_date(year, month_day):
	try:
		month, day = [int(i) for i in month_day.split('-')]
		datetime.date(year, month, day)
		return True
	except: return False

def service_results(month_day):
	try:
		headers = {'User-Agent': 'FenLight+ %s' % addon_version()}
		response = session.get(by_day_url % (base_url, month_day), headers=headers, timeout=timeout)
		if response.status_code != 200: return []
		items = response.json().get('results') or []
	except: return []
	results = []
	for item in items:
		try: results.append((int(item['id']), get_popularity(item)))
		except: pass
	return results

def tmdb_results(month_day):
	# fallback path. /discover has no month/day filter, only a contiguous range, so it costs one call per year
	api_key = tmdb_api_key()
	if api_key in empty_setting_check: return no_api_key()
	current_year = get_current_date(return_str=False).year
	years = [i for i in range(current_year - FALLBACK_YEARS + 1, current_year + 1) if valid_date(i, month_day)]
	results = []
	def worker(year):
		date = '%s-%s' % (year, month_day)
		url = '%s/discover/movie?api_key=%s&language=en-US&with_original_language=en&primary_release_date.gte=%s' \
				'&primary_release_date.lte=%s&sort_by=popularity.desc' % (tmdb_base_url, api_key, date, date)
		# get_tmdb, not get_data, which strips popularity, the sort key
		try: results.extend([(int(i['id']), get_popularity(i)) for i in get_tmdb(url).json()['results']])
		except: pass
	threads = list(make_thread_list(worker, years))
	[i.join() for i in threads]
	return results

def get_movies_on_this_day(month_day):
	results = service_results(month_day) or tmdb_results(month_day)
	results = sorted(results, key=lambda i: i[1], reverse=True)
	return [{'id': i[0]} for i in results]

def moviesthisday_movies_on_this_day(page_no):
	month_day = get_current_date(return_str=False).strftime('%m-%d')
	# MM-DD belongs in the key, else the list keeps serving yesterday's films past midnight
	string = 'moviesthisday_movies_on_this_day_%s' % month_day
	results = lists_cache_object(get_movies_on_this_day, string, [month_day], False, EXPIRY_1_DAY)
	if not results:
		lists_cache.delete(string) # a momentary outage shouldn't blank the list for a full day
		return {'results': [], 'total_pages': 0, 'page': page_no}
	try: items, total_pages = paginate_list(results, page_no, PAGE_LIMIT)
	except: return {'results': [], 'total_pages': 0, 'page': page_no}
	return {'results': items, 'total_pages': total_pages, 'page': page_no}
