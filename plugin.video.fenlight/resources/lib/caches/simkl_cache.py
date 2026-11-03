# -*- coding: utf-8 -*-
from threading import Thread
from caches.base_cache import connect_database, replace_rows
from modules.kodi_utils import sleep, confirm_dialog, close_all_dialog
# from modules.kodi_utils import logger

DELETE = 'DELETE FROM simkl_data WHERE id=?'
DELETE_LIKE = 'DELETE FROM simkl_data WHERE id LIKE ?'
WATCHED_INSERT = 'INSERT OR IGNORE INTO watched VALUES (?, ?, ?, ?, ?, ?)'
WATCHED_DELETE = 'DELETE FROM watched WHERE db_type = ?'
SHOW_DELETE = 'DELETE FROM watched WHERE db_type = ? AND media_id = ?'
SEASONS_SELECT = 'SELECT DISTINCT season FROM watched WHERE db_type = ? AND media_id = ?'
PROGRESS_INSERT = 'INSERT OR IGNORE INTO progress VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)'
PROGRESS_DELETE = 'DELETE FROM progress WHERE db_type = ?'
BASE_DELETE = 'DELETE FROM %s'
SC_BASE_GET = 'SELECT data FROM simkl_data WHERE id = ?'
SC_BASE_SET = 'INSERT OR REPLACE INTO simkl_data (id, data) VALUES (?, ?)'
SC_BASE_DELETE = 'DELETE FROM simkl_data WHERE id = ?'

class SimklCache:
	def get(self, string):
		result = None
		try:
			dbcon = connect_database('simkl_db')
			cache_data = dbcon.execute(SC_BASE_GET, (string,)).fetchone()
			if cache_data: result = eval(cache_data[0])
		except: pass
		return result

	def set(self, string, data):
		try:
			dbcon = connect_database('simkl_db')
			dbcon.execute(SC_BASE_SET, (string, repr(data)))
		except: return None

	def delete(self, string):
		try:
			dbcon = connect_database('simkl_db')
			dbcon.execute(SC_BASE_DELETE, (string,))
		except: pass

simkl_cache = SimklCache()

class SimklWatched():
	def set_bulk_movie_watched(self, insert_list):
		replace_rows('simkl_db', WATCHED_DELETE, ('movie',), WATCHED_INSERT, insert_list)

	def set_bulk_tvshow_watched(self, insert_list):
		replace_rows('simkl_db', WATCHED_DELETE, ('episode',), WATCHED_INSERT, insert_list)

	def set_bulk_movie_progress(self, insert_list):
		replace_rows('simkl_db', PROGRESS_DELETE, ('movie',), PROGRESS_INSERT, insert_list)

	def set_bulk_tvshow_progress(self, insert_list):
		replace_rows('simkl_db', PROGRESS_DELETE, ('episode',), PROGRESS_INSERT, insert_list)

	# --- Phase-2 delta merge: touch only the items in the delta, leave everything else intact ---
	def replace_show(self, media_id, insert_list):
		dbcon = connect_database('simkl_db')
		dbcon.execute(SHOW_DELETE, ('episode', str(media_id)))
		if insert_list: dbcon.executemany(WATCHED_INSERT, insert_list)

	def remove_show(self, media_id):
		dbcon = connect_database('simkl_db')
		dbcon.execute(SHOW_DELETE, ('episode', str(media_id)))

	def upsert_movie(self, row):
		dbcon = connect_database('simkl_db')
		dbcon.execute('INSERT OR REPLACE INTO watched VALUES (?, ?, ?, ?, ?, ?)', row)

	def remove_movie(self, media_id):
		dbcon = connect_database('simkl_db')
		dbcon.execute(SHOW_DELETE, ('movie', str(media_id)))

	# --- Post-write local echo: single-row writes fed from a write response, not a re-fetch ---
	def upsert_progress(self, row):
		# REPLACE, not IGNORE — pausing an item we already hold a resume point for must overwrite it.
		dbcon = connect_database('simkl_db')
		dbcon.execute('INSERT OR REPLACE INTO progress VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)', row)


	def get_watched_seasons(self, media_id):
		try:
			dbcon = connect_database('simkl_db')
			rows = dbcon.execute(SEASONS_SELECT, ('episode', str(media_id))).fetchall()
			return sorted(int(r[0]) for r in rows if r[0] is not None)
		except: return []

simkl_watched_cache = SimklWatched()

def cache_simkl_object(function, string, url):
	cache = simkl_cache.get(string)
	if cache: return cache
	result = function(url)
	simkl_cache.set(string, result)
	return result

def clear_simkl_status_data(status=None):
	string = 'simkl_status_%s' % status if status else 'simkl_status_%'
	try:
		dbcon = connect_database('simkl_db')
		if status: dbcon.execute(DELETE, (string,))
		else: dbcon.execute(DELETE_LIKE, (string,))
	except: pass

def clear_simkl_calendar():
	try:
		dbcon = connect_database('simkl_db')
		dbcon.execute(DELETE_LIKE, ('simkl_get_my_calendar_%',))
	except: return

def clear_all_simkl_cache_data(silent=False, refresh=True):
	try:
		start = silent or confirm_dialog()
		if not start: return False
		dbcon = connect_database('simkl_db')
		for table in ('simkl_data', 'progress', 'watched', 'watched_status'): dbcon.execute(BASE_DELETE % table)
		dbcon.execute('VACUUM')
		if refresh:
			from apis.simkl_api import simkl_sync_activities
			Thread(target=simkl_sync_activities).start()
		return True
	except: return False

def default_activities():
	base = '2020-01-01T00:00:00Z'
	return {
			'all': base,
			'tv_shows':
				{
				'all': base,
				'removed_from_list': base,
				'rated_at': base,
				'plantowatch': base,
				'watching': base,
				'completed': base,
				'hold': base,
				'dropped': base
				},
			'anime':
				{
				'all': base,
				'removed_from_list': base,
				'rated_at': base,
				'plantowatch': base,
				'watching': base,
				'completed': base,
				'hold': base,
				'dropped': base
				},
			'movies':
				{
				'all': base,
				'removed_from_list': base,
				'rated_at': base,
				'plantowatch': base,
				'completed': base,
				'dropped': base
				}
			}
