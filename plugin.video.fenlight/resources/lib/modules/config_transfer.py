# -*- coding: utf-8 -*-
import sys
import json
import time
import gzip
import base64
import hashlib
from modules import kodi_utils
from caches.base_cache import connect_database
from caches import settings_cache as sc
logger = kodi_utils.logger

kodi_dialog, ok_dialog, confirm_dialog, select_dialog = kodi_utils.kodi_dialog, kodi_utils.ok_dialog, kodi_utils.confirm_dialog, kodi_utils.select_dialog
notification, progress_dialog, get_infolabel = kodi_utils.notification, kodi_utils.progress_dialog, kodi_utils.get_infolabel
open_file, path_exists, addon_version = kodi_utils.open_file, kodi_utils.path_exists, kodi_utils.addon_version

SCHEMA = 1
CHUNK_SIZE = 1000
MAGIC = 'FENLIGHTCFG'
DEFAULT_PORT = 8080
PROBE_TIMEOUT = 0.6
PROBE_WORKERS = 40
RPC_TIMEOUT = 15.0

# Rotating OAuth material
NEVER_TRANSFER = frozenset({
	'trakt.token', 'trakt.refresh', 'trakt.expires', 'trakt.user',
	'rd.token', 'rd.refresh', 'rd.client_id', 'rd.secret', 'rd.enabled', 'rd.account_id',
	'trakt.next_daily_clear', 'skip_segments.disclaimer_shown'})

# Static keys with no rotation
ACCOUNT_KEYS = frozenset({
	'trakt.client', 'trakt.secret', 'simkl.user', 'simkl.token', 'simkl.client',
	'tmdb_api', 'tmdb_read_access_token', 'tmdb.access_token', 'tmdb.account_id', 'omdb_api',
	'pm.token', 'pm.enabled', 'pm.account_id', 'ad.token', 'ad.enabled', 'ad.account_id',
	'oc.token', 'oc.enabled', 'ed.token', 'ed.enabled', 'tb.token', 'tb.enabled',
	'easynews_user', 'easynews_password', 'provider.easynews',
	'provider.external', 'external_scraper.name', 'external_scraper.module'})

# Skin view IDs. these only travel between boxes running the same skin
SKIN_BOUND = frozenset({
	'use_viewtypes', 'manual_viewtypes', 'view.main', 'view.movies', 'view.tvshows',
	'view.seasons', 'view.episodes', 'view.episodes_single', 'view.premium'})

# Hardware and line-dependent
DEVICE_TUNING = frozenset({
	'limit_concurrent_threads', 'max_threads', 'results.line_speed', 'results.filter_size_method'})

PATHS = frozenset(
	{'movie_download_directory', 'tvshow_download_directory', 'premium_download_directory',
	 'image_download_directory', 'default_addon_fanart', 'provider.folders', 'check.folders'} |
	{'folder%s.%s' % (n, k) for n in range(1, 6) for k in ('display_name', 'movies_directory', 'tv_shows_directory')})

CATEGORIES = ('general', 'accounts', 'views', 'tuning', 'paths')
CATEGORY_LABELS = {
	'general': 'Preferences (filters, sorting, playback, layout)',
	'accounts': 'Account keys (debrid, Easynews, Simkl, TMDb)',
	'views': 'View types (same skin only)',
	'tuning': 'Device tuning (threads, line speed)',
	'paths': 'Download and folder paths',
	'menus': 'Menu customisations and shortcut folders',
	'favorites': 'Favorites'}

DEFAULT_ON = ('general', 'views', 'menus', 'favorites')

SELECT_MENUS = 'SELECT list_name, list_type, list_contents FROM navigator WHERE list_type IN (?, ?)'
INSERT_MENU = 'INSERT OR REPLACE INTO navigator VALUES (?, ?, ?)'
SELECT_FAVS = 'SELECT db_type, tmdb_id, title FROM favourites'
INSERT_FAV = 'INSERT OR IGNORE INTO favourites VALUES (?, ?, ?)'

def category_for(setting_id):
	if setting_id in NEVER_TRANSFER: return None
	if setting_id in ACCOUNT_KEYS: return 'accounts'
	if setting_id in SKIN_BOUND: return 'views'
	if setting_id in DEVICE_TUNING: return 'tuning'
	if setting_id in PATHS: return 'paths'
	return 'general'

#=================================== SERIALISING =====================================#

def collect(categories):
	categories = set(categories)
	settings = {}
	for setting_id, row in sc.settings_cache.get_all_rows().items():
		category = category_for(setting_id)
		if category and category in categories: settings[setting_id] = row[1]
	payload = {
		'schema': SCHEMA, 'addon_version': addon_version(), 'exported_utc': int(time.time()),
		'source': {'skin': kodi_utils.getSkinDir(), 'name': get_infolabel('System.FriendlyName'),
					'authorised': _authorised_services()},
		'categories': sorted(categories), 'settings': settings}
	if 'menus' in categories: payload['menus'] = _collect_menus()
	if 'favorites' in categories: payload['favorites'] = _collect_favorites()
	return payload

def _authorised_services():
	# Which of the uncopyable accounts were actually live on the source.
	live = []
	if sc.get_setting('fenlight.trakt.user', 'empty_setting') not in ('empty_setting', ''): live.append('trakt')
	if sc.get_setting('fenlight.rd.token', 'empty_setting') not in ('empty_setting', ''): live.append('real_debrid')
	return live

def _collect_menus():
	try:
		dbcon = connect_database('navigator_db')
		return [{'list_name': i[0], 'list_type': i[1], 'list_contents': i[2]}
				for i in dbcon.execute(SELECT_MENUS, ('edited', 'shortcut_folder')).fetchall()]
	except Exception: return []

def _collect_favorites():
	try:
		dbcon = connect_database('favorites_db')
		return [{'db_type': i[0], 'tmdb_id': i[1], 'title': i[2]} for i in dbcon.execute(SELECT_FAVS).fetchall()]
	except Exception: return []

def encode_labels(payload):
	raw = json.dumps(payload).encode('utf-8')
	blob = base64.b64encode(gzip.compress(raw)).decode('ascii')
	pieces = [blob[i:i + CHUNK_SIZE] for i in range(0, len(blob), CHUNK_SIZE)]
	header = '%s|%s|%s|%s' % (MAGIC, SCHEMA, len(pieces), hashlib.sha256(raw).hexdigest()[:16])
	return [header] + ['%s|%s' % (i, piece) for i, piece in enumerate(pieces)]

def decode_labels(labels):
	header = next((l for l in labels if l.startswith('%s|' % MAGIC)), None)
	if not header: raise ValueError('not a FenLight+ config payload')
	parts = header.split('|')
	if len(parts) != 4: raise ValueError('not a FenLight+ config payload')
	schema, count, digest = int(parts[1]), int(parts[2]), parts[3]
	if schema > SCHEMA: raise ValueError('config was exported by a newer FenLight+ (schema %s)' % schema)
	chunks = []
	for label in labels:
		if label is header or not label or '|' not in label: continue
		index, _, data = label.partition('|')
		if not index.isdigit(): continue
		chunks.append((int(index), data))
	if len(chunks) != count: raise ValueError('payload incomplete (%s of %s parts)' % (len(chunks), count))
	raw = gzip.decompress(base64.b64decode(''.join(data for _, data in sorted(chunks))))
	if hashlib.sha256(raw).hexdigest()[:16] != digest: raise ValueError('payload failed its checksum')
	return json.loads(raw.decode('utf-8'))

#==================================== APPLYING =======================================#

def apply_payload(payload, categories):
	categories = set(categories)
	settings = payload.get('settings') or {}
	insert_list, skipped_unknown, skipped_invalid = [], 0, 0
	# watched_indicators is held back until the re-auth chain has run
	desired_tracker = settings.get('watched_indicators')
	for setting_id, value in settings.items():
		if setting_id == 'watched_indicators': continue
		category = category_for(setting_id)
		if not category or category not in categories: continue
		info = sc.default_setting_values(setting_id)
		if not info:
			# settings_cache.set() writes a companion "<id>_name" row for action settings
			if not (setting_id.endswith('_name') and sc.default_setting_values(setting_id[:-5])): skipped_unknown += 1
			continue
		if not isinstance(value, str): value = str(value)
		if not _valid_value(info, value):
			skipped_invalid += 1
			continue
		insert_list.append((setting_id, info['setting_type'], info['setting_default'], value))
		if info['setting_type'] == 'action' and 'settings_options' in info:
			insert_list.append(('%s_name' % setting_id, 'name', '', info['settings_options'][value]))
	if insert_list: sc.settings_cache.set_many(insert_list)

	menu_count = _apply_menus(payload) if 'menus' in categories else 0
	fav_count = _apply_favorites(payload) if 'favorites' in categories else 0

	if 'accounts' in categories: _verify_external_scraper()
	pending = _reauth_chain(payload)
	tracker_note = _reconcile_tracker(desired_tracker, categories)

	lines = ['[B]%s[/B] settings applied.' % len(insert_list)]
	if menu_count: lines.append('%s menu customisations restored.' % menu_count)
	if fav_count: lines.append('%s favorites merged.' % fav_count)
	if skipped_unknown: lines.append('%s unrecognised settings skipped (older or newer addon).' % skipped_unknown)
	if skipped_invalid: lines.append('%s settings skipped - this version has no such value for them.' % skipped_invalid)
	if tracker_note: lines.append(tracker_note)
	if pending: lines.append('Still to authorise on this device: %s.' % ', '.join(pending))
	lines.append('[CR]Restart Kodi for every change to take effect.')
	ok_dialog(heading='Config Imported', text='[CR]'.join(lines))

def _valid_value(info, value):
	if info['setting_type'] == 'boolean': return value in ('true', 'false')
	if 'settings_options' in info: return value in info['settings_options']
	return True

def _apply_menus(payload):
	rows = payload.get('menus') or []
	if not rows: return 0
	try:
		dbcon = connect_database('navigator_db')
		for row in rows:
			dbcon.execute(INSERT_MENU, (row['list_name'], row['list_type'], row['list_contents']))
		for row in rows: kodi_utils.clear_property('fenlight_%s_%s' % (row['list_name'], row['list_type']))
		return len(rows)
	except Exception as e:
		logger('config_transfer', 'menu import failed: %s' % e)
		return 0

def _apply_favorites(payload):
	rows = payload.get('favorites') or []
	if not rows: return 0
	try:
		dbcon = connect_database('favorites_db')
		for row in rows: dbcon.execute(INSERT_FAV, (row['db_type'], str(row['tmdb_id']), row['title']))
		return len(rows)
	except Exception as e:
		logger('config_transfer', 'favorites import failed: %s' % e)
		return 0

def _verify_external_scraper():
	module = sc.get_setting('fenlight.external_scraper.module', 'empty_setting')
	if module in ('empty_setting', ''): return
	try:
		import xbmcaddon
		xbmcaddon.Addon(module)
	except Exception:
		sc.set_setting('provider.external', 'false')
		notification('External scraper %s not installed - disabled' % module, 4000)

def _reauth_chain(payload):
	from modules import settings
	authorised = (payload.get('source') or {}).get('authorised') or []
	prompt = 'Trakt authorisation cannot be copied between devices.[CR]Authorise Trakt on this device now?'
	pending = []
	if 'trakt' in authorised and not settings.trakt_user_active():
		if confirm_dialog(heading='Trakt', text=prompt, ok_label='Authorise', cancel_label='Later', default_control=11):
			from apis.trakt_api import trakt_authenticate
			if not trakt_authenticate(): pending.append('Trakt')
		else: pending.append('Trakt')
	if 'real_debrid' in authorised and sc.get_setting('fenlight.rd.token', 'empty_setting') in ('empty_setting', ''):
		if confirm_dialog(heading='Real Debrid', text=prompt.replace('Trakt', 'Real Debrid'),
							ok_label='Authorise', cancel_label='Later', default_control=11):
			from apis.real_debrid_api import RealDebridAPI
			RealDebridAPI().auth()
			if sc.get_setting('fenlight.rd.token', 'empty_setting') in ('empty_setting', ''): pending.append('Real Debrid')
		else: pending.append('Real Debrid')
	return pending

def _reconcile_tracker(desired, categories):
	if desired is None or 'general' not in categories: return ''
	from modules import settings
	if desired == '1' and not settings.trakt_user_active():
		sc.set_setting('watched_indicators', '0')
		return 'Watched indicators set to Fen Light - Trakt is not authorised here.'
	if desired == '2' and not settings.simkl_user_active():
		sc.set_setting('watched_indicators', '0')
		return 'Watched indicators set to Fen Light - Simkl is not authorised here.'
	sc.set_setting('watched_indicators', desired)
	return ''

#================================ CATEGORY PICKING ===================================#

def choose_categories(available, heading, preselect_keys=DEFAULT_ON, skin_warning=''):
	options = [k for k in ('general', 'accounts', 'views', 'tuning', 'paths', 'menus', 'favorites') if k in available]
	if not options: return None
	list_items = []
	for key in options:
		label = CATEGORY_LABELS[key]
		if key == 'views' and skin_warning: label = '%s  [COLOR red](%s)[/COLOR]' % (label, skin_warning)
		list_items.append({'line1': label})
	preselect = [i for i, key in enumerate(options) if key in preselect_keys and not (key == 'views' and skin_warning)]
	kwargs = {'items': json.dumps(list_items), 'heading': heading, 'multi_choice': 'true', 'preselect': preselect}
	chosen = select_dialog(options, **kwargs)
	if chosen in (None, []): return None
	return chosen

def _skin_warning(payload):
	source_skin = (payload.get('source') or {}).get('skin')
	if source_skin and source_skin != kodi_utils.getSkinDir(): return 'exported from %s' % source_skin
	return ''

def _describe(payload):
	present = {category_for(k) for k in (payload.get('settings') or {})} - {None}
	declared = set(payload.get('categories') or [])
	available = (declared & (present | {'menus', 'favorites'})) if declared else set(present)
	if payload.get('menus'): available.add('menus')
	else: available.discard('menus')
	if payload.get('favorites'): available.add('favorites')
	else: available.discard('favorites')
	return available

#================================== FILE TRANSPORT ===================================#

def export_file(params={}):
	categories = choose_categories(set(CATEGORIES) | {'menus', 'favorites'}, 'Choose What to Export')
	if not categories: return
	if 'accounts' in categories and not confirm_dialog(heading='Account Keys',
			text='The file will contain your debrid API keys and Easynews password in plain text.[CR]Continue?',
			ok_label='Continue', cancel_label='Cancel', default_control=11): return
	folder = kodi_dialog().browse(3, 'Choose Where to Save', '')
	if not folder: return
	if not folder.endswith(('/', '\\')): folder += '/'
	path = '%sfenlight_config_%s.json' % (folder, time.strftime('%Y%m%d-%H%M'))
	try:
		payload = collect(categories)
		f = open_file(path, 'w')
		f.write(json.dumps(payload, indent=1))
		f.close()
	except Exception as e:
		logger('config_transfer', 'export failed: %s' % e)
		return ok_dialog(heading='Export Failed', text='Could not write the config file.[CR]%s' % e)
	ok_dialog(heading='Config Exported', text='Saved to:[CR]%s' % path)

def import_file(params={}):
	path = kodi_dialog().browse(1, 'Choose a Config File', '', '.json')
	if not path or not path_exists(path): return
	try:
		f = open_file(path)
		payload = json.loads(f.read())
		f.close()
	except Exception as e:
		return ok_dialog(heading='Import Failed', text='Could not read that file.[CR]%s' % e)
	_run_import(payload)

#================================== LAN TRANSPORT ====================================#

def _rpc(host, port, method, rpc_params, user=None, password=None, timeout=RPC_TIMEOUT):
	import requests
	auth = (user, password) if user else None
	response = requests.post('http://%s:%s/jsonrpc' % (host, port), auth=auth, timeout=timeout,
								json={'jsonrpc': '2.0', 'id': 1, 'method': method, 'params': rpc_params})
	if response.status_code == 401: raise ValueError('the username or password was rejected')
	response.raise_for_status()
	return response.json().get('result')

def _probe(host, port):
	import requests
	try:
		response = requests.post('http://%s:%s/jsonrpc' % (host, port), timeout=PROBE_TIMEOUT,
									json={'jsonrpc': '2.0', 'id': 1, 'method': 'JSONRPC.Ping'})
	except Exception: return None
	if response.status_code == 401: return (host, 'needs login')
	if response.status_code == 200 and 'pong' in response.text: return (host, '')
	return None

def _usable_ip(ip):
	if not ip or ip.count('.') != 3: return False
	return not ip.startswith('127.') and not ip.startswith('169.254.')

def _local_ip():
	import socket
	for target in (('8.8.8.8', 80), ('10.255.255.255', 1)):
		try:
			s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
			try:
				s.connect(target)
				ip = s.getsockname()[0]
			finally: s.close()
			if _usable_ip(ip): return ip
		except Exception: pass
	ip = get_infolabel('Network.IPAddress')
	if _usable_ip(ip): return ip
	try:
		ip = socket.gethostbyname(socket.gethostname())
		if _usable_ip(ip): return ip
	except Exception: pass
	return ''

def _discover_peers(port, local_ip):
	from concurrent.futures import ThreadPoolExecutor
	subnet = local_ip.rsplit('.', 1)[0]
	hosts = ['%s.%s' % (subnet, i) for i in range(1, 255)]
	hosts = [h for h in hosts if h != local_ip]
	found, done = [], 0
	pd = progress_dialog('Scanning %s.0/24' % subnet)
	try:
		with ThreadPoolExecutor(max_workers=PROBE_WORKERS) as pool:
			for result in pool.map(lambda h: _probe(h, port), hosts):
				done += 1
				if pd.iscanceled(): break
				if result: found.append(result)
				pd.update('%s found' % len(found), int(done * 100 / len(hosts)))
	finally:
		try: pd.close()
		except Exception: pass
	return found

def pull_device(params={}):
	port = DEFAULT_PORT
	local_ip = _local_ip()
	peers = _discover_peers(port, local_ip) if local_ip else []
	options = list(peers) + [('__manual__', '')]
	list_items = [{'line1': '%s%s' % (h, '  [COLOR grey](%s)[/COLOR]' % n if n else '')} for h, n in peers]
	if not local_ip: note = 'no network detected to scan'
	elif not peers: note = 'nothing found on %s.0/24' % local_ip.rsplit('.', 1)[0]
	else: note = ''
	list_items.append({'line1': 'Enter an IP address manually...%s' % ('  [COLOR grey](%s)[/COLOR]' % note if note else '')})
	chosen = select_dialog(options, **{'items': json.dumps(list_items), 'heading': 'Choose a Device', 'narrow_window': 'true'})
	if not chosen: return
	host = chosen[0]
	if host == '__manual__':
		host = kodi_dialog().input('Device IP Address')
		if not host: return
		entered_port = kodi_dialog().input('Port', defaultt=str(DEFAULT_PORT), type=kodi_utils.numeric_input)
		if entered_port: port = int(entered_port)
	user = kodi_dialog().input('Kodi Username (blank if none)', defaultt='kodi')
	password = kodi_dialog().input('Kodi Password') if user else ''

	pd = progress_dialog('Fetching config from %s' % host)
	try:
		pd.update('Checking the device...', 10)
		addons = _rpc(host, port, 'Addons.GetAddons', {'type': 'xbmc.python.pluginsource'}, user, password) or {}
		if not any(a.get('addonid') == 'plugin.video.fenlight' for a in addons.get('addons') or []):
			return ok_dialog(heading='Not Found', text='%s does not have FenLight+ installed.' % host)
		pd.update('Reading configuration...', 40)
		directory = 'plugin://plugin.video.fenlight/?mode=app_export_config&categories=%s' % ','.join(CATEGORIES + ('menus', 'favorites'))
		result = _rpc(host, port, 'Files.GetDirectory', {'directory': directory, 'media': 'files'}, user, password) or {}
		labels = [i.get('label', '') for i in result.get('files') or []]
		if not labels: raise ValueError('the device returned nothing - is it running an older FenLight+?')
		pd.update('Decoding...', 80)
		payload = decode_labels(labels)
	except Exception as e:
		logger('config_transfer', 'pull from %s failed: %s' % (host, e))
		return ok_dialog(heading='Pull Failed', text='Could not fetch the config from %s.[CR]%s' % (host, e))
	finally:
		try: pd.close()
		except Exception: pass
	_run_import(payload)

def _run_import(payload):
	if not isinstance(payload, dict) or 'settings' not in payload:
		return ok_dialog(heading='Import Failed', text='That is not a FenLight+ config.')
	schema = payload.get('schema', 0)
	if schema > SCHEMA:
		return ok_dialog(heading='Import Failed', text='That config was exported by a newer FenLight+ (schema %s).[CR]Update this device first.' % schema)
	available = _describe(payload)
	if not available: return ok_dialog(heading='Nothing to Import', text='That config is empty.')
	source = payload.get('source') or {}
	heading = 'Import from %s' % (source.get('name') or 'file')
	categories = choose_categories(available, heading, preselect_keys=available & set(DEFAULT_ON), skin_warning=_skin_warning(payload))
	if not categories: return
	apply_payload(payload, categories)

#=================================== RPC ENDPOINT ====================================#

def app_export_config(params={}):
	handle = int(sys.argv[1])
	requested = [c for c in (params.get('categories', '') or '').split(',') if c]
	categories = set(requested) or (set(CATEGORIES) | {'menus', 'favorites'})
	items = []
	try:
		for index, label in enumerate(encode_labels(collect(categories))):
			listitem = kodi_utils.make_listitem()
			listitem.setLabel(label)
			items.append((kodi_utils.build_url({'config_part': index}), listitem, False))
	except Exception as e:
		logger('config_transfer', 'export endpoint failed: %s' % e)
	kodi_utils.add_items(handle, items)
	kodi_utils.set_content(handle, 'files')
	kodi_utils.end_directory(handle, cacheToDisc=False)
