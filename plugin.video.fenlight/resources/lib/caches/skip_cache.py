# -*- coding: utf-8 -*-
from caches.base_cache import BaseCache, get_timestamp
# from modules.kodi_utils import logger

CLEAN = 'DELETE from skip_segments WHERE CAST(expires AS INT) <= ?'
CLEAR = 'DELETE FROM skip_segments'

class SkipCache(BaseCache):
	def __init__(self):
		BaseCache.__init__(self, 'maincache_db', 'skip_segments')

	def clear_cache(self):
		try:
			dbcon = self.manual_connect('maincache_db')
			dbcon.execute(CLEAR)
			dbcon.execute('VACUUM')
			return True
		except: return False

	def clean_database(self):
		try:
			dbcon = self.manual_connect('maincache_db')
			dbcon.execute(CLEAN, (get_timestamp(),))
			return True
		except: return False

skip_cache = SkipCache()
