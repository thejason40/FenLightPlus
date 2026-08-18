# -*- coding: utf-8 -*-
import re
from caches.pack_cache import pack_cache
from modules import settings
from modules.kodi_utils import logger
from modules.source_utils import seas_ep_filter, pack_enable_check, get_file_info, release_info_format, normalize, EXTRAS
from modules.utils import clean_file_name

# Debrid provider name -> the scrape_provider string resolve_internal() understands.
resolver_map = {'Real-Debrid': 'rd_cloud', 'TorBox': 'tb_cloud', 'AllDebrid': 'ad_cloud', 'Offcloud': 'oc_cloud',
				'rd_cloud': 'rd_cloud', 'tb_cloud': 'tb_cloud', 'ad_cloud': 'ad_cloud', 'oc_cloud': 'oc_cloud'}

def seed_from_playback(player):
	try:
		if not settings.pack_continuity(): return logger('PACK seed', 'skipped - setting off')
		if player.is_generic or player.media_type != 'episode': return
		info = (player.playing_item or {}).get('pack_info')
		if not info: return logger('PACK seed', 'skipped - no pack_info on played item')
		files = info.get('files') or []
		if len(files) < 2: return logger('PACK seed', 'skipped - only %d file(s) in pack' % len(files))
		search_info = player.sources_object.search_info
		tmdb_id, season, episode = search_info['tmdb_id'], search_info['season'], search_info['episode']
		if not tmdb_id or season in (None, ''): return logger('PACK seed', 'skipped - no tmdb_id/season')
		if not pack_enable_check(player.meta, season, episode)[0]:
			return logger('PACK seed', 'skipped - %s S%s has unaired episodes' % (tmdb_id, season))
		pack_cache.set(tmdb_id, season, info.get('provider', ''), info.get('source_type', ''), player.playing_item.get('name', ''),
						info.get('magnet', ''), info.get('hash', ''), 1 if info.get('direct') else 0, files)
		logger('PACK seed', 'stored %s S%s via %s (%s): %d files, %d with links' % (tmdb_id, season, info.get('provider'),
				info.get('source_type'), len(files), len([i for i in files if i.get('link')])))
	except Exception as e:
		logger('PACK seed error', str(e))

def find_episode_file(entry, season, episode, title):
	try:
		compare_title = re.sub(r'[^A-Za-z0-9]+', '.', title.replace('\'', '').replace('&', 'and').replace('%', '.percent')).lower()
		for item in entry['files']:
			filename = item.get('filename') or ''
			try:
				if not seas_ep_filter(season, episode, filename): continue
				tail = re.sub(compare_title, '', seas_ep_filter(season, episode, filename, split=True))
				if any(x in tail for x in EXTRAS): continue
				return item
			except: continue
	except Exception as e:
		logger('PACK match error', str(e))
	return None

def build_items(entry, chosen):
	# Tier 1 replays the stored per-file handle through resolve_internal.
	# Tier 2 replays the magnet through the normal debrid path, so a dead/deleted handle still plays.
	items = []
	try:
		name = chosen.get('filename') or ''
		try: size = round(float(chosen.get('size') or 0) / 1073741824, 2)
		except: size = 0
		quality, extraInfo = get_file_info(name_info=release_info_format(name))
		base = {'name': name, 'display_name': clean_file_name(normalize(name.replace('html', ' ').replace('+', ' ').replace('-', ' '))),
				'quality': quality, 'extraInfo': extraInfo, 'size': size, 'size_label': '%.2f GB' % size, 'from_pack_cache': True}
		provider, link = entry.get('provider'), chosen.get('link')
		internal_provider = resolver_map.get(provider)
		if link and internal_provider:
			items.append(dict(base, **{'scrape_provider': internal_provider, 'source': internal_provider, 'provider': internal_provider, 'id': link,
										'url_dl': link, 'direct_debrid_link': bool(entry.get('direct')), 'pack_tier': 1}))
		if entry.get('magnet'):
			items.append(dict(base, **{'scrape_provider': 'external', 'source': 'torrent', 'provider': 'pack cache', 'debrid': provider,
										'cache_provider': provider, 'url': entry['magnet'], 'hash': entry.get('hash', ''),
										'package': 'season', 'pack_tier': 2}))
	except Exception as e:
		logger('PACK build error', str(e))
	return items
