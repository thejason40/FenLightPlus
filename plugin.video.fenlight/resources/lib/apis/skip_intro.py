# -*- coding: utf-8 -*-
from apis import theintrodb_api, introdb_api
from caches.skip_cache import skip_cache
# from modules.kodi_utils import logger

cache_key = 'v3.%s.%s.%s.%s'  # id . season . episode . duration_seconds - v3 keeps each source apart
HIT_HOURS, EMPTY_HOURS = 720, 168  # 30 days for real data, 7 days for blank data
KINDS = ('recap', 'intro', 'outro')  # ordered by where they sit in an episode
EMPTY = {'intro': None, 'recap': None, 'outro': None}

def valid_segment(kind, seg, total_time=None):
	try:
		start, end = float(seg['start_sec']), float(seg['end_sec'])
	except (KeyError, TypeError, ValueError): return False
	if start < 0 or end <= start: return False
	duration = end - start
	if kind == 'recap':
		if not 5 <= duration <= 120: return False
		if total_time and start > total_time * 0.5: return False  # recaps sit at the start
		return True
	if kind == 'outro':
		if not 5 <= duration <= 300: return False
		if total_time and start < total_time * 0.5: return False  # outros sit in the latter half
		return True
	if not 5 <= duration <= 300: return False
	if total_time and start > total_time * 0.5: return False  # intro not past the midpoint
	return True

def _first_theintrodb_seg(seg_list, kind, total_time):
	if not seg_list: return None
	for s in seg_list:
		if not isinstance(s, dict): continue
		start_ms, end_ms = s.get('start_ms'), s.get('end_ms')
		if kind == 'outro':
			if start_ms in (None, 0): continue
			start = start_ms / 1000.0
			end = float(total_time or 0) if end_ms is None else end_ms / 1000.0
		else:
			start = 0.0 if start_ms is None else start_ms / 1000.0
			if end_ms in (None, 0): continue
			end = end_ms / 1000.0
		return {'start_sec': start, 'end_sec': end}
	return None

def _from_theintrodb(data, total_time):
	return {'intro': _first_theintrodb_seg(data.get('intro'), 'intro', total_time),
			'recap': _first_theintrodb_seg(data.get('recap'), 'recap', total_time),
			'outro': _first_theintrodb_seg(data.get('credits'), 'outro', total_time)}

def _theintrodb(tmdb_id, season, episode, total_time):
	data = theintrodb_api.get_media(tmdb_id, season, episode, int((total_time or 0) * 1000))
	if data is None: return None
	return _from_theintrodb(data, total_time) if data else dict(EMPTY)

def _introdb(imdb_id, season, episode):
	data = introdb_api.get_segments(imdb_id, season, episode)
	if data is None: return None
	return {'intro': data.get('intro'), 'recap': data.get('recap'), 'outro': data.get('outro')}

def _usable(segs):
	return bool(segs) and any(segs.values())

def _needs_introdb(record, wanted, total_time):
	tdb = record.get('theintrodb')
	if not _usable(tdb): return True
	return 'outro' in wanted and not valid_segment('outro', tdb.get('outro'), total_time)

def _merge(record, total_time):
	tdb, idb = record.get('theintrodb'), record.get('introdb')
	if not _usable(tdb): return dict(idb or EMPTY)
	merged = dict(tdb)
	if idb and not valid_segment('outro', merged.get('outro'), total_time): merged['outro'] = idb.get('outro')
	return merged

def get_segments(tmdb_id, imdb_id, season, episode, total_time, wanted=KINDS, cache_only=False):
	try: season, episode = int(season), int(episode)
	except (TypeError, ValueError): return None
	key_id = tmdb_id or imdb_id
	if not key_id: return None
	key = cache_key % (key_id, season, episode, int(total_time or 0))
	record = skip_cache.get(key) or {}
	if cache_only: return _merge(record, total_time) if record else None
	fresh = False
	if 'theintrodb' not in record and tmdb_id:
		result = _theintrodb(tmdb_id, season, episode, total_time)
		if result is not None: record['theintrodb'], fresh = result, True
	if 'introdb' not in record and imdb_id and _needs_introdb(record, wanted, total_time):
		result = _introdb(imdb_id, season, episode)
		if result is not None: record['introdb'], fresh = result, True
	segments = _merge(record, total_time)
	if fresh: skip_cache.set(key, record, expiration=HIT_HOURS if any(segments.values()) else EMPTY_HOURS)
	return segments

def get_skip_windows(tmdb_id, imdb_id, season, episode, total_time, enabled_kinds, cache_only=False):
	segments = get_segments(tmdb_id, imdb_id, season, episode, total_time, enabled_kinds, cache_only=cache_only)
	if not segments: return []
	windows = []
	for kind in KINDS:
		if kind not in enabled_kinds: continue
		seg = segments.get(kind)
		if seg and valid_segment(kind, seg, total_time):
			windows.append({'kind': kind, 'start': float(seg['start_sec']), 'end': float(seg['end_sec'])})
	return windows
