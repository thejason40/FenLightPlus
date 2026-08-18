# -*- coding: utf-8 -*-
from caches.base_cache import connect_database
from modules.kodi_utils import logger

GET = 'SELECT provider, source_type, pack_name, magnet, hash, direct, files FROM pack_data WHERE tmdb_id = ? AND season = ?'
SET = 'INSERT OR REPLACE INTO pack_data VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)'
DELETE = 'DELETE FROM pack_data WHERE tmdb_id = ? AND season = ?'
CLEAR = 'DELETE FROM pack_data'

# Packs are remembered until they stop working - no TTL. Validity is proven by playback, not guessed by a clock.
class PackCache:
	def get(self, tmdb_id, season):
		try:
			dbcon = connect_database('debridcache_db')
			row = dbcon.execute(GET, (str(tmdb_id), int(season))).fetchone()
			dbcon.close()
			if not row: return None
			return {'provider': row[0], 'source_type': row[1], 'pack_name': row[2], 'magnet': row[3],
					'hash': row[4], 'direct': row[5], 'files': eval(row[6])}
		except Exception as e:
			logger('PACK cache.get error', '%s S%s: %s' % (tmdb_id, season, e))
			return None

	def set(self, tmdb_id, season, provider, source_type, pack_name, magnet, _hash, direct, files):
		try:
			dbcon = connect_database('debridcache_db')
			dbcon.execute(SET, (str(tmdb_id), int(season), provider, source_type, pack_name, magnet, _hash, int(direct), repr(files)))
			dbcon.close()
			return True
		except Exception as e:
			logger('PACK cache.set error', '%s S%s: %s' % (tmdb_id, season, e))
			return False

	def delete(self, tmdb_id, season):
		try:
			dbcon = connect_database('debridcache_db')
			dbcon.execute(DELETE, (str(tmdb_id), int(season)))
			dbcon.close()
			return True
		except Exception as e:
			logger('PACK cache.delete error', '%s S%s: %s' % (tmdb_id, season, e))
			return False

	def clear_cache(self):
		try:
			dbcon = connect_database('debridcache_db')
			dbcon.execute(CLEAR)
			dbcon.execute('VACUUM')
			dbcon.close()
			return True
		except: return False

pack_cache = PackCache()

_pack_stash = {}
_stash_limit = 4

def stash_pack(info_hash, files, direct=False):
	try:
		if not info_hash or not files: return
		if len(_pack_stash) >= _stash_limit: _pack_stash.clear()
		_pack_stash[info_hash] = {'files': files, 'direct': direct}
		logger('PACK stash', '%s: %d files, %d with links, direct=%s' % (info_hash, len(files), len([i for i in files if i.get('link')]), direct))
	except Exception as e:
		logger('PACK stash error', str(e))

def take_pack(info_hash):
	try: return _pack_stash.pop(info_hash, None)
	except: return None
