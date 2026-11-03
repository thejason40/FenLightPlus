# -*- coding: utf-8 -*-
from modules.kodi_utils import make_session
# from modules.kodi_utils import logger

base_url = 'https://api.introdb.app/segments?imdb_id=%s&season=%s&episode=%s'
timeout = 10.0
session = make_session('https://api.introdb.app')

def get_segments(imdb_id, season, episode):
	if not imdb_id: return None
	try:
		response = session.get(base_url % (imdb_id, season, episode), timeout=timeout)
		if response.status_code == 200: return response.json()
		# 404 is "no segments" - not a failure
		if response.status_code == 404: return {}
		return None
	except: return None
