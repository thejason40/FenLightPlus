# -*- coding: utf-8 -*-
from modules.kodi_utils import make_session
# from modules.kodi_utils import logger

base_url = 'https://api.theintrodb.org/v3/media?tmdb_id=%s&season=%s&episode=%s&duration_ms=%s'
timeout = 10.0
session = make_session('https://api.theintrodb.org')

def get_media(tmdb_id, season, episode, duration_ms):
	if not tmdb_id: return None
	try:
		response = session.get(base_url % (tmdb_id, season, episode, duration_ms), timeout=timeout)
		if response.status_code == 200: return response.json()
		if response.status_code == 404: return {}
		return None
	except: return None
