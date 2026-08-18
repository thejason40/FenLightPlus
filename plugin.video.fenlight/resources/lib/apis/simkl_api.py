# -*- coding: utf-8 -*-

import time
import requests
from threading import Lock
from caches import simkl_cache
from caches.settings_cache import get_setting, set_setting
from caches.main_cache import cache_object
from modules import kodi_utils, settings
from modules.metadata import movie_meta_external_id, tvshow_meta_external_id
from modules.utils import make_thread_list, timedelta, copy2clip, jsondate_to_datetime as js2date

sleep = kodi_utils.sleep
logger, notification, xbmc_player, confirm_dialog = kodi_utils.logger, kodi_utils.notification, kodi_utils.xbmc_player, kodi_utils.confirm_dialog
progress_dialog, kodi_refresh = kodi_utils.progress_dialog, kodi_utils.kodi_refresh
simkl_user_active, simkl_client = settings.simkl_user_active, settings.simkl_client
tmdb_api_key, show_unaired_watchlist = settings.tmdb_api_key, settings.show_unaired_watchlist
simkl_watched_cache = simkl_cache.simkl_watched_cache
simkl_data_cache = simkl_cache.simkl_cache
cache_simkl_object = simkl_cache.cache_simkl_object
clear_all_simkl_cache_data = simkl_cache.clear_all_simkl_cache_data
empty_setting_check = (None, 'empty_setting', '')
API_ENDPOINT = 'https://api.simkl.com/%s'
PIN_PAGE = 'https://simkl.com/pin/%s'
APP_NAME = 'FenLightPlus'
res_formats = ('%Y-%m-%dT%H:%M:%SZ', '%Y-%m-%dT%H:%M:%S.%fZ', '%Y-%m-%dT%H:%M:%S.000Z')
placeholder_date = '1970-01-01T00:00:01Z'
EPOCH = '1970-01-01T00:00:00Z'  # date_from for a full pull
TV_FLAGS = {'episode_watched_at': 'yes', 'include_all_episodes': 'yes'}
timeout = 60
MOVIE_STATUSES = ('plantowatch', 'completed', 'dropped')
SHOW_STATUSES = ('watching', 'plantowatch', 'hold', 'dropped', 'completed')
SYNC_THROTTLE_SECONDS = 900
SYNC_STAMP_KEY = 'simkl_last_activities_check'
STATUS_RAW_KEY = 'simkl_raw_status_%s'

_rate_lock = Lock()
_last_request = [0.0]
MIN_REQUEST_GAP = 1.0

def _rate_gate():
	with _rate_lock:
		wait = MIN_REQUEST_GAP - (time.time() - _last_request[0])
		if wait > 0: sleep(int(wait * 1000))
		_last_request[0] = time.time()

def addon_version():
	try: return get_setting('fenlight.addon_version', '1.0.0')
	except: return '1.0.0'

def no_client_key():
	notification('Please set a valid Simkl Client ID Key')
	return None

def _base_params():
	return {'client_id': simkl_client(), 'app-name': APP_NAME, 'app-version': addon_version()}

def call_simkl(path, params=None, data=None, is_delete=False, with_auth=True, method=None, is_scrobble=False):
	CLIENT_ID = simkl_client()
	if CLIENT_ID in empty_setting_check: return no_client_key()
	if params is None: params = {}
	call_params = dict(params); call_params.update(_base_params())
	headers = {'Content-Type': 'application/json', 'User-Agent': '%s/%s' % (APP_NAME, addon_version()), 'simkl-api-key': CLIENT_ID}
	if with_auth:
		token = get_setting('fenlight.simkl.token')
		if token and token not in empty_setting_check: headers['Authorization'] = 'Bearer ' + token
	def send_query():
		_rate_gate()
		try:
			if method == 'post' or data is not None:
				return requests.post(API_ENDPOINT % path, params=call_params, json=data, headers=headers, timeout=timeout)
			if method == 'delete' or is_delete:
				return requests.delete(API_ENDPOINT % path, params=call_params, headers=headers, timeout=timeout)
			return requests.get(API_ENDPOINT % path, params=call_params, headers=headers, timeout=timeout)
		except Exception as e:
			logger('Simkl Error', str(e))
			return None
	backoffs = (1, 2, 4, 8, 16)
	attempt = 0
	while True:
		response = send_query()
		if response is None: return None
		status_code = response.status_code
		if status_code == 409 and is_scrobble:
			return {'result': 'OK'}
		if status_code == 401:
			if with_auth and xbmc_player().isPlaying() is False:
				if confirm_dialog(heading='Authorize Simkl', text='You must authenticate with Simkl. Do you want to authenticate now?') and simkl_authenticate():
					headers['Authorization'] = 'Bearer ' + get_setting('fenlight.simkl.token')
					response = send_query()
					if response is None: return None
					status_code = response.status_code
				else: return None
			else: return None
		if status_code == 400:
			try: body = response.json()
			except: body = {}
			if isinstance(body, dict) and 'rate_limit' in str(body).lower() and attempt == 0:
				attempt += 1
				sleep(3000)
				continue
		if status_code == 429 or 500 <= status_code < 600:
			if attempt < len(backoffs):
				sleep(backoffs[attempt] * 1000)
				attempt += 1
				continue
			return None
		if status_code == 204: return {'result': 'OK'}
		try: response.encoding = 'utf-8'
		except: pass
		try: return response.json()
		except: return None

def simkl_get_pin():
	CLIENT_ID = simkl_client()
	if CLIENT_ID in empty_setting_check: return no_client_key()
	return call_simkl('oauth/pin', with_auth=False)

def simkl_get_pin_token(pin_info):
	result = None
	try:
		user_code = str(pin_info['user_code'])
		expires_in = int(pin_info.get('expires_in', 900))
		interval = int(pin_info.get('interval', 5))
		try: copy2clip(user_code)
		except Exception as e: logger('SIMKL ERROR 1', e)
		content = '[CR]Scan the QR Code or navigate to: [B]%s[/B][CR]Enter the following code: [B]%s[/B]' % ('simkl.com/pin', user_code)
		tiny_url, qr_icon = kodi_utils.make_qr(PIN_PAGE % user_code)
		progressDialog = progress_dialog('Simkl Authorize', qr_icon)
		progressDialog.update(content, 0)
		start = time.time()
		time_passed = 0
		while not progressDialog.iscanceled() and time_passed < expires_in:
			sleep(max(interval, 1) * 1000)
			poll = call_simkl('oauth/pin/%s' % user_code, with_auth=False)
			if isinstance(poll, dict):
				if poll.get('result') == 'OK' and poll.get('access_token'):
					result = poll
					break
				if 'device_code' in poll and not poll.get('access_token'):
					break
			time_passed = time.time() - start
			progressDialog.update(content, int(100 * time_passed / expires_in))
		try: progressDialog.close()
		except Exception as e: logger('SIMKL ERROR 3', e)
	except Exception as e: logger('SIMKL ERROR 4', e)
	return result

def simkl_authenticate(dummy=''):
	pin_info = simkl_get_pin()
	if not pin_info or not pin_info.get('user_code'): return notification('Simkl Error Authorizing', 3000) or False
	token = simkl_get_pin_token(pin_info)
	if token and token.get('access_token'):
		set_setting('simkl.token', token['access_token'])
		if settings.tracking_provider() != 'trakt' or confirm_dialog(heading='Active Tracker',
				text='Make Simkl your active tracker now?[CR]Choose No to keep using Trakt for tracking.'):
			set_setting('watched_indicators', '2')
		sleep(1000)
		try:
			user = simkl_get_user()
			username = ''
			if isinstance(user, dict):
				username = (user.get('user', {}) or {}).get('name') or (user.get('account', {}) or {}).get('id') or ''
			set_setting('simkl.user', str(username) if username else 'Simkl')
		except: set_setting('simkl.user', 'Simkl')
		kodi_utils.set_property('fenlight.simkl.user', get_setting('fenlight.simkl.user'))
		notification('Simkl Account Authorized', 3000)
		if not _offer_trakt_import(): simkl_sync_activities(force_update=True)
		return True
	notification('Simkl Error Authorizing', 3000)
	return False

def _offer_trakt_import():
	try:
		if settings.trakt_user_active() and confirm_dialog(heading='Import Trakt data', text='Import your Trakt data to Simkl?'):
			return bool(import_from_trakt({}))
	except Exception as e: logger('SIMKL IMPORT ERROR', e)
	return False

def simkl_revoke_authentication(dummy=''):
	set_setting('simkl.user', 'empty_setting')
	set_setting('simkl.token', '')
	if settings.tracking_provider() == 'simkl':
		if settings.trakt_user_active() and confirm_dialog(heading='Active Tracker',
				text='Switch tracking to Trakt?[CR]Choose No to use built-in tracking.'):
			set_setting('watched_indicators', '1')
		else: set_setting('watched_indicators', '0')
	kodi_utils.set_property('fenlight.simkl.user', 'empty_setting')
	clear_all_simkl_cache_data(silent=True, refresh=False)
	notification('Simkl Account Authorization Reset', 3000)

def simkl_get_user():
	return call_simkl('users/settings', method='post')

def _ids_object(ids):
	out = {}
	if not isinstance(ids, dict): return out
	for k in ('simkl', 'simkl_id', 'tmdb', 'imdb', 'tvdb', 'traktslug', 'slug', 'anidb', 'mal'):
		v = ids.get(k)
		if v not in (None, '', 'None'): out[k] = v
	return out

def get_simkl_movie_id(ids):
	if ids.get('tmdb'): return ids['tmdb']
	api_key = tmdb_api_key()
	if ids.get('imdb'):
		try: return movie_meta_external_id('imdb_id', ids['imdb'], api_key)['id']
		except: pass
	return None

def get_simkl_tvshow_id(ids):
	if ids.get('tmdb'): return ids['tmdb']
	api_key = tmdb_api_key()
	if ids.get('imdb'):
		try: return tvshow_meta_external_id('imdb_id', ids['imdb'], api_key)['id']
		except: pass
	if ids.get('tvdb'):
		try: return tvshow_meta_external_id('tvdb_id', ids['tvdb'], api_key)['id']
		except: pass
	return None

def simkl_get_activity():
	return call_simkl('sync/activities')

def simkl_all_items(media_type, date_from=None, extended=None, extra=None):
	params = {}
	if extended: params['extended'] = extended
	if date_from: params['date_from'] = date_from
	if extra: params.update(extra)
	result = call_simkl('sync/all-items/%s' % media_type, params=params)
	if not isinstance(result, dict): return None
	for key in (media_type, 'shows', 'movies', 'anime'):
		if isinstance(result.get(key), list): return result[key]
	return []

def simkl_all_items_combined(date_from, extended=None, extra=None):
	params = {'date_from': date_from}
	if extended: params['extended'] = extended
	if extra: params.update(extra)
	result = call_simkl('sync/all-items', params=params)
	return result if isinstance(result, dict) else None

def _first_watched_at(item, fallback):
	return item.get('last_watched_at') or item.get('watched_at') or fallback

def _norm_ts(ts):
	if not isinstance(ts, str) or not ts: ts = placeholder_date
	if ts.endswith('Z') and '.' not in ts: return ts[:-1] + '.000Z'
	return ts

def _show_episode_rows(item):
	show = item.get('show') or item
	tmdb_id = get_simkl_tvshow_id(_ids_object(show.get('ids', {})))
	if not tmdb_id: return None, []
	title = show.get('title', '')
	rows = []
	for s in item.get('seasons') or []:
		season_no = s.get('number')
		for e in s.get('episodes', []) or []:
			tvdb = e.get('tvdb') or {}
			s_final = tvdb.get('season', season_no)
			e_final = tvdb.get('episode', e.get('number'))
			if s_final in (None, 0) or e_final is None: continue
			watched_at = _norm_ts(e.get('watched_at') or _first_watched_at(item, placeholder_date))
			rows.append(('episode', str(tmdb_id), s_final, e_final, watched_at, title))
	return tmdb_id, rows

def _movie_watched_row(item):
	movie = item.get('movie') or item
	tmdb_id = get_simkl_movie_id(_ids_object(movie.get('ids', {})))
	if not tmdb_id: return None, None, False
	row = ('movie', str(tmdb_id), '', '', _norm_ts(_first_watched_at(item, placeholder_date)), movie.get('title', ''))
	return tmdb_id, row, item.get('status') == 'completed'

def simkl_indicators_movies(act=None):
	items = simkl_all_items('movies')
	if items is None: return  # transient failure — keep the existing rows and cache
	rows = []
	for item in items:
		tmdb_id, row, watched = _movie_watched_row(item)
		if tmdb_id and watched: rows.append(row)
	simkl_watched_cache.set_bulk_movie_watched(rows)
	_seed_status_raw('movies', items, act)

def simkl_indicators_tv(act=None):
	shows = simkl_all_items('shows', extended='full', extra=TV_FLAGS)
	anime = simkl_all_items('anime', extended='full_anime_seasons', extra=TV_FLAGS)
	_seed_status_raw('shows', shows, act)
	_seed_status_raw('anime', anime, act)
	if shows is None or anime is None: return  # partial failure — don't rebuild rows from half a library
	rows = []
	for item in shows + anime:
		_tmdb, ep_rows = _show_episode_rows(item)
		if ep_rows: rows.extend(ep_rows)
	simkl_watched_cache.set_bulk_tvshow_watched(rows)

def simkl_delta_sync(watermark, act=None):
	data = simkl_all_items_combined(watermark, extended='full_anime_seasons', extra=TV_FLAGS) or {}
	_merge_status_raw_from_delta(data, act)
	for item in (data.get('shows') or []) + (data.get('anime') or []):
		tmdb_id, ep_rows = _show_episode_rows(item)
		if tmdb_id is None: continue
		if not ep_rows and item.get('watched_episodes_count'): continue
		simkl_watched_cache.replace_show(tmdb_id, ep_rows)
	for item in (data.get('movies') or []):
		tmdb_id, row, watched = _movie_watched_row(item)
		if tmdb_id is None: continue
		if watched: simkl_watched_cache.upsert_movie(row)
		else: simkl_watched_cache.remove_movie(tmdb_id)

def simkl_playback_progress():
	result = call_simkl('sync/playback')
	if isinstance(result, list): return result
	if isinstance(result, dict):
		items = []
		for k in ('movies', 'shows', 'anime'):
			if isinstance(result.get(k), list): items.extend(result[k])
		if items: return items
		if result.get('type') or result.get('movie') or result.get('show'): return [result]
	return []

def simkl_progress_movies(progress_info):
	def _process(item):
		movie = item.get('movie') or item
		ids = _ids_object(movie.get('ids', {}))
		tmdb_id = get_simkl_movie_id(ids)
		if not tmdb_id: return
		try: pct = round(float(item.get('progress', 0)), 1)
		except: pct = 0
		paused_at = _norm_ts(item.get('paused_at', ''))
		insert_append(('movie', str(tmdb_id), '', '', str(pct), 0, paused_at, item.get('id', 0), movie.get('title', '')))
	insert_list = []
	insert_append = insert_list.append
	items = [i for i in progress_info if _pb_type(i) == 'movie' and _pb_progress(i) > 1]
	if not items: return
	threads = list(make_thread_list(_process, items))
	[i.join() for i in threads]
	simkl_watched_cache.set_bulk_movie_progress(insert_list)

def _pb_type(item):
	if item.get('type'): return 'movie' if item['type'] in ('movie', 'movies') else 'episode'
	return 'movie' if item.get('movie') else 'episode'

def _pb_progress(item):
	try: return float(item.get('progress', 0))
	except: return 0

def simkl_progress_tv(progress_info):
	def _process(item):
		show = item.get('show') or {}
		ids = _ids_object(show.get('ids', {}))
		tmdb_id = get_simkl_tvshow_id(ids)
		if not tmdb_id: return
		ep = item.get('episode') or {}
		try:
			season = int(ep.get('season', 0))
			number = int(ep.get('number', 0))
		except: return
		if season <= 0 or number <= 0: return
		pct = round(_pb_progress(item), 1)
		insert_append(('episode', str(tmdb_id), season, number, str(pct), 0, _norm_ts(item.get('paused_at', '')), item.get('id', 0), show.get('title', '')))
	insert_list = []
	insert_append = insert_list.append
	items = [i for i in progress_info if _pb_type(i) == 'episode' and _pb_progress(i) > 1]
	if not items: return
	threads = list(make_thread_list(_process, items))
	[i.join() for i in threads]
	simkl_watched_cache.set_bulk_tvshow_progress(insert_list)

def _timestamp(date_time_str):
	for fmt in res_formats:
		try: return int(time.mktime(js2date(date_time_str, fmt).timetuple()))
		except: continue
	return 0

def _compare(latest, cached):
	try: return _timestamp(latest) > _timestamp(cached)
	except: return True

def _section(activities, key):
	return activities.get(key, {}) if isinstance(activities, dict) else {}

def _sync_throttled():
	try:
		last = float(simkl_data_cache.get(SYNC_STAMP_KEY) or 0)
		return (time.time() - last) < SYNC_THROTTLE_SECONDS
	except: return False

def simkl_sync_activities(force_update=False, bypass_throttle=False):
	if force_update: clear_all_simkl_cache_data(silent=True, refresh=False)
	if not simkl_user_active() and not force_update: return 'no account'
	if not force_update and not bypass_throttle and _sync_throttled(): return 'not needed'
	try: simkl_data_cache.set(SYNC_STAMP_KEY, time.time())
	except: pass
	try: latest = simkl_get_activity()
	except: return 'failed'
	if not isinstance(latest, dict): return 'failed'
	_activities_memo[0], _activities_memo[1] = time.time(), latest
	previous = simkl_data_cache.get('simkl_get_activity')
	is_initial = not isinstance(previous, dict)
	cached = simkl_cache.default_activities() if is_initial else previous
	if not force_update and not _compare(latest.get('all', ''), cached.get('all', '')):
		simkl_data_cache.set('simkl_get_activity', latest)
		return 'not needed'
	refresh_movies_watched = refresh_tv_watched = refresh_playback = False
	l_movies, c_movies = _section(latest, 'movies'), _section(cached, 'movies')
	l_tv, c_tv = _section(latest, 'tv_shows'), _section(cached, 'tv_shows')
	l_anime, c_anime = _section(latest, 'anime'), _section(cached, 'anime')
	for l, c in ((l_movies, c_movies),):
		for key in ('completed', 'all'):
			if _compare(l.get(key, ''), c.get(key, '')): refresh_movies_watched = True
	for l, c in ((l_tv, c_tv), (l_anime, c_anime)):
		for key in ('watching', 'completed', 'hold', 'dropped', 'all'):
			if _compare(l.get(key, ''), c.get(key, '')): refresh_tv_watched = True

	refresh_playback = force_update
	for l, c in ((l_tv, c_tv), (l_anime, c_anime), (l_movies, c_movies)):
		if _compare(l.get('playback', ''), c.get('playback', '')): refresh_playback = True
	removed = (_compare(l_tv.get('removed_from_list', ''), c_tv.get('removed_from_list', ''))
			or _compare(l_movies.get('removed_from_list', ''), c_movies.get('removed_from_list', ''))
			or _compare(l_anime.get('removed_from_list', ''), c_anime.get('removed_from_list', '')))
	if force_update or is_initial:
		simkl_indicators_movies(latest)
		simkl_indicators_tv(latest)
	elif removed:
		if simkl_removals_reconcile(latest):
			simkl_delta_sync(cached.get('all'), latest)
		else:
			simkl_indicators_movies(latest)
			simkl_indicators_tv(latest)
	elif refresh_movies_watched or refresh_tv_watched:
		simkl_delta_sync(cached.get('all'), latest)
	if refresh_playback:
		progress_info = simkl_playback_progress()
		simkl_progress_movies(progress_info)
		simkl_progress_tv(progress_info)
	simkl_data_cache.set('simkl_get_activity', latest)
	from caches.simkl_cache import clear_simkl_calendar
	clear_simkl_calendar()
	return 'success'

def _payload_ids(media_id, key='tmdb', extra_ids=None):
	ids = {key: media_id}
	if extra_ids:
		for k, v in extra_ids.items():
			if v not in (None, '', 'None'): ids[k] = v
	return ids

def _write_rejected(result):
	try:
		not_found = result.get('not_found') if isinstance(result, dict) else None
		if not isinstance(not_found, dict): return False
		return any(not_found.get(k) for k in ('movies', 'shows', 'episodes', 'anime'))
	except: return False

def simkl_mark_watched(action, media, media_id, tvdb_id=0, season=None, episode=None, key='tmdb', watched_at=None, show_seasons=None):
	url = 'sync/history' if action == 'mark_as_watched' else 'sync/history/remove'
	ids = _payload_ids(media_id, key)
	if media == 'movies':
		item = {'ids': ids}
		if watched_at: item['watched_at'] = watched_at
		data = {'movies': [item]}
	elif media == 'episode':
		ep = {'number': int(episode)}
		if watched_at: ep['watched_at'] = watched_at
		data = {'shows': [{'ids': ids, 'seasons': [{'number': int(season), 'episodes': [ep]}]}]}
	elif media == 'shows':
		if url == 'sync/history': data = {'shows': [{'ids': ids}]}
		else:
			if show_seasons is None: show_seasons = simkl_watched_cache.get_watched_seasons(media_id)
			if not show_seasons: return True  # nothing watched on Simkl — nothing to unmark
			data = {'shows': [{'ids': ids, 'seasons': [{'number': int(s)} for s in show_seasons]}]}
	else:  # season
		data = {'shows': [{'ids': ids, 'seasons': [{'number': int(season)}]}]}
	result = call_simkl(url, data=data)
	if result is None or _write_rejected(result):
		logger('simkl', '%s rejected (key=%s) payload=%s response=%s' % (url, key, data, result))
		# Fall back to the tvdb id for shows if tmdb didn't resolve.
		if media != 'movies' and tvdb_id not in (0, '0', None) and key != 'tvdb':
			return simkl_mark_watched(action, media, tvdb_id, 0, season, episode, 'tvdb', watched_at, show_seasons)
		return False
	_echo_watched_status(action, media, media_id, key)
	return True

def simkl_add_to_list(media_type, media_id, to='plantowatch', key='tmdb', remove=False):
	url = 'sync/history/remove' if remove else 'sync/add-to-list'
	bucket = 'movies' if media_type in ('movie', 'movies') else 'shows'
	if bucket == 'movies' and to not in MOVIE_STATUSES: to = 'plantowatch'
	item = {'ids': _payload_ids(media_id, key)}
	if not remove: item['to'] = to
	data = {bucket: [item]}
	result = call_simkl(url, data=data)
	if result is None: return notification('Error', 3000) or False
	notification('Success', 3000)
	# Local echo instead of a sync-after-write; on an echo miss the delta merge refreshes the raw caches.
	if not _echo_list_change(bucket, media_id, key, None if remove else to, remove):
		try: simkl_delta_sync(_status_watermark())
		except: pass
	return result

def simkl_scrobble(action, media, media_id, percent, season=None, episode=None, key='tmdb'):
	url = 'scrobble/%s' % action
	if media in ('movie', 'movies'):
		data = {'movie': {'ids': _payload_ids(media_id, key)}, 'progress': float(percent)}
	else:
		data = {'show': {'ids': _payload_ids(media_id, key)}, 'episode': {'season': int(season), 'number': int(episode)}, 'progress': float(percent)}
	return call_simkl(url, data=data, is_scrobble=True)

def _echo_pause(result, media, media_id, percent, season=None, episode=None):
	# Write the scrobble/pause response straight into the progress cache
	if not isinstance(result, dict): return
	resume_id = result.get('id')
	if not resume_id: return
	node = result.get('movie') or result.get('show') or result.get('anime') or {}
	title = node.get('title', '') or ''
	try: pct = round(float(result.get('progress', percent)), 1)
	except: pct = percent
	last_played = time.strftime('%Y-%m-%dT%H:%M:%S.000Z', time.gmtime())  # no paused_at in the response
	if media in ('movie', 'movies'):
		simkl_watched_cache.upsert_progress(('movie', str(media_id), '', '', str(pct), 0, last_played, resume_id, title))
	else:
		ep = result.get('episode') or {}
		try:
			s, n = int(ep.get('season', season)), int(ep.get('number', episode))
		except: s, n = int(season), int(episode)
		simkl_watched_cache.upsert_progress(('episode', str(media_id), s, n, str(pct), 0, last_played, resume_id, title))

def _echo_list_change(bucket, media_id, key, to, remove, only_if=None):
	types = ('movies',) if bucket == 'movies' else ('shows', 'anime')
	target, found = str(media_id), False
	for t in types:
		cached = simkl_data_cache.get(STATUS_RAW_KEY % t)
		if not (isinstance(cached, dict) and isinstance(cached.get('items'), list)): continue
		kept, hit, changed = [], False, False
		for i in cached['items']:
			node = i.get('movie') or i.get('show') or i
			ids = _ids_object(node.get('ids', {}) or {})
			if str(ids.get(key, '')) != target:
				kept.append(i)
				continue
			hit = True
			if remove:
				changed = True
				continue
			if only_if is not None and i.get('status') != only_if:
				kept.append(i)
				continue
			if i.get('status') != to:
				i = dict(i)
				i['status'] = to
				changed = True
			kept.append(i)
		if not hit: continue
		found = True
		if not changed: continue
		simkl_data_cache.set(STATUS_RAW_KEY % t, {'ts': cached.get('ts'), 'removed_ts': cached.get('removed_ts'), 'items': kept})
	return found

def _echo_watched_status(action, media, media_id, key):
	try:
		if action != 'mark_as_watched': return
		if media == 'movies': _echo_list_change('movies', media_id, key, 'completed', False)
		elif media == 'shows': _echo_list_change('shows', media_id, key, 'completed', False)
		else: _echo_list_change('shows', media_id, key, 'watching', False, only_if='plantowatch')
	except: pass

def _status_watermark():
	# Last-known activities stamp, used as date_from for a targeted delta.
	try:
		cached = simkl_data_cache.get('simkl_get_activity')
		if isinstance(cached, dict): return cached.get('all') or EPOCH
	except: pass
	return EPOCH

def simkl_progress(action, media, media_id, percent, season=None, episode=None, resume_id=None, refresh_tracker=False):
	if action == 'clear_progress':
		try:
			if resume_id and int(resume_id) > 0: call_simkl('sync/playback/%s' % int(resume_id), is_delete=True)
		except: pass
		return
	try: result = simkl_scrobble('pause', media, media_id, percent, season, episode)
	except: result = None
	# Local echo instead of a re-sync.
	try: _echo_pause(result, media, media_id, percent, season, episode)
	except: pass
	if refresh_tracker: simkl_sync_activities()

def simkl_official_status(media_type):
	# Double-scrobble guard, analog of trakt_official_status
	try:
		if not kodi_utils.addon_installed('script.simkl'): return True
		if not kodi_utils.addon_enabled('script.simkl'): return True
		simkl_addon = kodi_utils.addon('script.simkl')
		try:
			if simkl_addon.getSetting('token') in ('', None): return True
		except: return True
		try:
			if simkl_addon.getSetting('autoscrobble') == 'false': return True
		except: pass
		return False
	except: pass
	return True

def _is_unaired(item):
	try:
		total = int(item.get('total_episodes_count') or 0)
		not_aired = int(item.get('not_aired_episodes_count') or 0)
		if total > 0: return not_aired >= total
	except: pass
	return False

_activities_memo = [0.0, None]
ACTIVITIES_MEMO_SECONDS = 300

def _current_activities():
	now = time.time()
	if _activities_memo[1] is not None and now - _activities_memo[0] < ACTIVITIES_MEMO_SECONDS:
		return _activities_memo[1]
	if _sync_throttled():
		cached = simkl_data_cache.get('simkl_get_activity')
		if isinstance(cached, dict):
			_activities_memo[0], _activities_memo[1] = now, cached
			return cached
	act = simkl_get_activity()
	if isinstance(act, dict): _activities_memo[0], _activities_memo[1] = now, act
	return act

_SECTION_FOR_TYPE = {'shows': 'tv_shows', 'anime': 'anime', 'movies': 'movies'}

def _status_cache_state(t, act):
	ts, removed = _act_stamps(t, act)
	return simkl_data_cache.get(STATUS_RAW_KEY % t), ts, removed

def _node_simkl_id(item):
	node = item.get('movie') or item.get('show') or item
	ids = node.get('ids') or {}
	return ids.get('simkl') or ids.get('simkl_id')

def _merge_status_items(cached_items, delta_items):
	merged = {}
	for n, i in enumerate(cached_items): merged[str(_node_simkl_id(i) or 'c%s' % n)] = i
	for i in delta_items:
		key = _node_simkl_id(i)
		if key is not None: merged[str(key)] = i
	return list(merged.values())

def _strip_item(item):
	if isinstance(item, dict) and 'seasons' in item:
		item = dict(item)
		item.pop('seasons', None)
	return item

def _act_stamps(t, act):
	section = act.get(_SECTION_FOR_TYPE.get(t, t)) if isinstance(act, dict) else None
	ts = section.get('all') if isinstance(section, dict) else None
	removed = section.get('removed_from_list') if isinstance(section, dict) else None
	return ts, removed

def _seed_status_raw(t, items, act=None):
	if items is None: return
	act = act if isinstance(act, dict) else _current_activities()
	ts, removed = _act_stamps(t, act)
	simkl_data_cache.set(STATUS_RAW_KEY % t, {'ts': ts, 'removed_ts': removed, 'items': [_strip_item(i) for i in items]})

def _merge_status_raw_from_delta(delta, act=None):
	if not isinstance(delta, dict): return
	act = act if isinstance(act, dict) else _current_activities()
	for t in ('shows', 'anime', 'movies'):
		cached = simkl_data_cache.get(STATUS_RAW_KEY % t)
		if not (isinstance(cached, dict) and 'items' in cached): continue
		ts, removed = _act_stamps(t, act)
		merged = _merge_status_items(cached['items'], [_strip_item(i) for i in (delta.get(t) or [])])
		simkl_data_cache.set(STATUS_RAW_KEY % t, {'ts': ts, 'removed_ts': removed, 'items': merged})

def simkl_removals_reconcile(act):
	result = call_simkl('sync/all-items', params={'extended': 'simkl_ids_only'})
	if not isinstance(result, dict): return False
	ok = True
	for t in ('shows', 'anime', 'movies'):
		cached = simkl_data_cache.get(STATUS_RAW_KEY % t)
		if not (isinstance(cached, dict) and 'items' in cached):
			ok = False
			continue
		keep = set()
		for i in (result.get(t) or []):
			sid = _node_simkl_id(i)
			if sid is not None: keep.add(str(sid))
		kept_items = []
		for i in cached['items']:
			sid = _node_simkl_id(i)
			if sid is not None and str(sid) not in keep:
				node = i.get('movie') or i.get('show') or i
				tmdb_id = _ids_object(node.get('ids', {})).get('tmdb')
				if tmdb_id:
					if t == 'movies': simkl_watched_cache.remove_movie(tmdb_id)
					else: simkl_watched_cache.remove_show(tmdb_id)
				continue
			kept_items.append(i)
		_ts, removed_ts = _act_stamps(t, act)
		simkl_data_cache.set(STATUS_RAW_KEY % t, {'ts': cached.get('ts'), 'removed_ts': removed_ts, 'items': kept_items})
	return ok

def _status_all_items(t):
	act = _current_activities()
	cached, ts, removed = _status_cache_state(t, act)
	if isinstance(cached, dict) and 'items' in cached and (ts is None or cached.get('ts') == ts):
		return cached['items']
	if isinstance(cached, dict) and 'items' in cached and cached.get('ts') and cached.get('removed_ts') == removed:
		stale = []
		for dt in ('shows', 'anime', 'movies'):
			d_cached, d_ts, d_removed = _status_cache_state(dt, act)
			if not (isinstance(d_cached, dict) and 'items' in d_cached and d_cached.get('ts')): continue
			if d_cached.get('ts') == d_ts: continue  # already fresh
			if d_cached.get('removed_ts') != d_removed: continue  # needs a full refetch instead
			stale.append((dt, d_cached, d_ts, d_removed))
		if stale:
			delta = simkl_all_items_combined(min(i[1]['ts'] for i in stale))
			if delta is not None:
				result = None
				for dt, d_cached, d_ts, d_removed in stale:
					merged = _merge_status_items(d_cached['items'], [_strip_item(i) for i in (delta.get(dt) or [])])
					simkl_data_cache.set(STATUS_RAW_KEY % dt, {'ts': d_ts, 'removed_ts': d_removed, 'items': merged})
					if dt == t: result = merged
				if result is not None: return result
	items = simkl_all_items(t)
	if items is not None:
		simkl_data_cache.set(STATUS_RAW_KEY % t, {'ts': ts, 'removed_ts': removed, 'items': [_strip_item(i) for i in items]})
		return items
	if isinstance(cached, dict): return cached.get('items', [])
	return []

def simkl_status_list(status, media_type, dummy_arg=None):
	types = ('shows', 'anime') if media_type in ('tvshow', 'tvshows', 'shows', 'tv') else ('movies',)
	filter_unaired = status == 'plantowatch' and not show_unaired_watchlist()
	results = []
	for t in types:
		for i in _status_all_items(t):
			if i.get('status') != status: continue
			if filter_unaired and _is_unaired(i): continue
			node = i.get('movie') or i.get('show') or i
			ids = _ids_object(node.get('ids', {}))
			results.append({'media_ids': {'tmdb': ids.get('tmdb', ''), 'imdb': ids.get('imdb', ''), 'tvdb': ids.get('tvdb', ''), 'simkl': ids.get('simkl', '')}, 'title': node.get('title', '')})
	return results

def simkl_watching(media_type, page_no=1):
	return simkl_status_list('watching', media_type)

def simkl_plantowatch(media_type, page_no=1):
	return simkl_status_list('plantowatch', media_type)

def simkl_completed(media_type, page_no=1):
	return simkl_status_list('completed', media_type)

def simkl_hold(media_type, page_no=1):
	return simkl_status_list('hold', media_type)

def simkl_dropped(media_type, page_no=1):
	return simkl_status_list('dropped', media_type)

def simkl_get_my_calendar(recently_aired, current_date):
	def _process(dummy):
		try:
			shows_by_simkl = {}
			for status in ('watching', 'plantowatch'):
				for row in simkl_status_list(status, 'tvshow') or []:
					sid = str(row.get('media_ids', {}).get('simkl') or '')
					if sid: shows_by_simkl[sid] = row
			if not shows_by_simkl: return []
			data = []
			for feed in ('tv', 'anime'):
				try:
					resp = requests.get('https://data.simkl.in/calendar/v2/%s.json' % feed, params=_base_params(),
										headers={'User-Agent': '%s/%s' % (APP_NAME, addon_version())}, timeout=timeout)
					payload = resp.json()
				except: payload = {}
				calendar = payload.get('calendar', []) if isinstance(payload, dict) else (payload or [])
				for entry in calendar:
					try:
						row = shows_by_simkl.get(str(entry.get('simkl_id') or ''))
						if not row: continue
						ep = entry.get('episode', {}) or {}
						season_no = int(ep.get('season', 0) or 0)
						ep_no = ep.get('episode')
						if season_no <= 0 or ep_no is None: continue
						mids = row.get('media_ids', {})
						data.append({'sort_title': '%s s%s e%s' % (row.get('title', ''), str(season_no).zfill(2), str(ep_no).zfill(2)),
									'media_ids': {'tmdb': mids.get('tmdb', ''), 'imdb': mids.get('imdb', ''), 'tvdb': mids.get('tvdb', '')},
									'season': season_no, 'episode': ep_no, 'first_aired': entry.get('date')})
					except: pass
			return [i for n, i in enumerate(data) if i not in data[n + 1:]]
		except: return []
	start, finish = simkl_calendar_days(recently_aired, current_date)
	string = 'simkl_get_my_calendar_%s_%s' % (start, finish)
	return cache_simkl_object(_process, string, 'dummy')

def simkl_calendar_days(recently_aired, current_date):
	if recently_aired: return (current_date - timedelta(days=14)).strftime('%Y-%m-%d'), '14'
	previous_days = int(get_setting('fenlight.simkl.calendar_previous_days', '0'))
	future_days = int(get_setting('fenlight.simkl.calendar_future_days', '7'))
	start = (current_date - timedelta(days=previous_days)).strftime('%Y-%m-%d')
	return start, str(previous_days + future_days)

def _trakt_ids_to_simkl(ids):
	out = {}
	if not isinstance(ids, dict): return out
	mapping = {'imdb': 'imdb', 'tmdb': 'tmdb', 'tvdb': 'tvdb', 'slug': 'traktslug'}
	for tk, sk in mapping.items():
		v = ids.get(tk)
		if v not in (None, '', 'None'): out[sk] = v
	return out

def import_from_trakt(params):
	if not simkl_user_active(): return notification('Authorize Simkl first', 3000)
	if not settings.trakt_user_active(): return notification('No Trakt account connected', 3000)
	from apis import trakt_api
	pd = progress_dialog('Import Trakt to Simkl')
	pd.update('Reading Trakt library...', 0)
	added = not_found = 0
	try:
		# --- Watched movies ---
		movies_payload = []
		for i in trakt_api.get_trakt({'path': 'sync/watched/movies%s', 'with_auth': True, 'all_pages': True}) or []:
			try:
				ids = _trakt_ids_to_simkl(i['movie']['ids'])
				if not ids: continue
				movies_payload.append({'ids': ids, 'watched_at': i.get('last_watched_at') or placeholder_date})
			except: pass
		# --- Watched shows (per-episode) ---
		shows_payload = []
		for i in trakt_api.get_trakt({'path': 'sync/watched/shows%s', 'params': {'extended': 'progress'}, 'with_auth': True, 'all_pages': True}) or []:
			try:
				ids = _trakt_ids_to_simkl(i['show']['ids'])
				if not ids: continue
				seasons = []
				for s in i.get('seasons', []):
					eps = [{'number': e['number'], 'watched_at': e.get('last_watched_at') or placeholder_date} for e in s.get('episodes', [])]
					if eps: seasons.append({'number': s['number'], 'episodes': eps})
				show = {'ids': ids}
				if seasons: show['seasons'] = seasons
				shows_payload.append(show)
			except: pass
		pd.update('Writing watch history to Simkl...', 40)
		for chunk in _chunk(movies_payload, 100):
			r = call_simkl('sync/history', data={'movies': chunk})
			added, not_found = _tally(r, added, not_found)
			sleep(1100)
		for chunk in _chunk(shows_payload, 50):
			r = call_simkl('sync/history', data={'shows': chunk})
			added, not_found = _tally(r, added, not_found)
			sleep(1100)
		# --- Watchlist -> Plan to Watch ---
		pd.update('Importing watchlist...', 70)
		wl_movies, wl_shows = [], []
		for media_type, bucket in (('movies', wl_movies), ('shows', wl_shows)):
			for i in trakt_api.get_trakt({'path': 'sync/watchlist/%s', 'path_insert': media_type, 'params': {'extended': 'full'}, 'with_auth': True, 'all_pages': True}) or []:
				try:
					node = i.get('movie') if media_type == 'movies' else i.get('show')
					if not node: continue
					ids = _trakt_ids_to_simkl(node['ids'])
					if ids: bucket.append({'ids': ids, 'to': 'plantowatch'})
				except: pass
		for chunk in _chunk(wl_movies, 100):
			call_simkl('sync/add-to-list', data={'movies': chunk}); sleep(1100)
		for chunk in _chunk(wl_shows, 100):
			call_simkl('sync/add-to-list', data={'shows': chunk}); sleep(1100)
		# --- In-progress playback -> Simkl scrobble/pause (carry resume points across) ---
		pd.update('Importing in-progress playback...', 85)
		for i in trakt_api.trakt_playback_progress() or []:
			try:
				pct = float(i.get('progress') or 0)
				if pct <= 0: continue
				if i.get('type') == 'movie':
					ids = _trakt_ids_to_simkl(i['movie']['ids'])
					if ids: call_simkl('scrobble/pause', data={'movie': {'ids': ids}, 'progress': pct}, is_scrobble=True)
				elif i.get('type') == 'episode':
					ids = _trakt_ids_to_simkl(i['show']['ids'])
					ep = i.get('episode', {}) or {}
					if ids and ep.get('season') is not None and ep.get('number') is not None:
						call_simkl('scrobble/pause', data={'show': {'ids': ids}, 'episode': {'season': int(ep['season']), 'number': int(ep['number'])}, 'progress': pct}, is_scrobble=True)
				else: continue
				sleep(1100)
			except: pass
		pd.update('Finishing...', 95)
	except Exception as e: logger('SIMKL IMPORT ERROR', e)
	try: pd.close()
	except: pass
	# The one and only sync of the sign-in + import flow, once every block has been sent.
	simkl_sync_activities(force_update=True)
	kodi_utils.ok_dialog(heading='Trakt to Simkl Import', text='[B]Added:[/B] %s   [B]Not Found:[/B] %s' % (added, not_found))
	kodi_refresh()
	return True

def _chunk(seq, size):
	for i in range(0, len(seq), size): yield seq[i:i + size]

def _tally(result, added, not_found):
	try:
		if isinstance(result, dict):
			a = result.get('added', {}) or {}
			nf = result.get('not_found', {}) or {}
			added += sum(v for v in a.values() if isinstance(v, int))
			for v in nf.values():
				if isinstance(v, list): not_found += len(v)
	except: pass
	return added, not_found
