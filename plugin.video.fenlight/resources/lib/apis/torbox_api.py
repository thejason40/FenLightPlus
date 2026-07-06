import time
import requests
from threading import Thread
from urllib.parse import urlencode, quote
from caches.settings_cache import get_setting, set_setting
from caches.main_cache import cache_object
from modules.source_utils import supported_video_extensions, seas_ep_filter, EXTRAS
from modules.kodi_utils import make_session, kodi_dialog, ok_dialog, notification, confirm_dialog, sleep, progress_dialog
from modules.utils import copy2clip, get_datetime, jsondate_to_datetime as js2date
# from modules.kodi_utils import logger

base_url = 'https://api.torbox.app/v1/api/'
stats = 'user/me'
download = 'torrents/requestdl'
remove = 'torrents/controltorrent'
history = 'torrents/mylist'
explore = 'torrents/mylist?id=%s'
cache = 'torrents/checkcached'
cloud = 'torrents/createtorrent'
download_usenet = 'usenet/requestdl'
remove_usenet = 'usenet/controlusenetdownload'
history_usenet = 'usenet/mylist'
explore_usenet = 'usenet/mylist?id=%s'
user_agent = 'Mozilla/5.0'
timeout = 20.0
session = make_session(base_url)

class TorBoxAPI:

	def __init__(self):
		self.token = get_setting('fenlight.tb.token')

	def _get(self, url, data={}):
		if self.token in ('empty_setting', ''): return None
		headers = {'Authorization': 'Bearer %s' % self.token}
		url = base_url + url
		response = session.get(url, params=data, headers=headers, timeout=timeout)
		return response.json()

	def _post(self, url, params=None, json=None, data=None):
		if self.token in ('empty_setting', '') and not 'token' in url: return None
		headers = {'Authorization': 'Bearer %s' % self.token}
		url = base_url + url
		response = session.post(url, params=params, json=json, data=data, headers=headers, timeout=timeout)
		return response.json()

	def add_headers_to_url(self, url):
		return url + '|' + urlencode({'User-Agent': user_agent})

	def account_info(self):
		return self._get(stats)

	def user_cloud(self):
		string = 'tb_user_cloud'
		url = history
		return cache_object(self._get, string, url, False, 0.03)

	def user_cloud_usenet(self):
		string = 'tb_user_cloud_usenet'
		url = history_usenet
		return cache_object(self._get, string, url, False, 0.03)

	def user_cloud_info(self, request_id=''):
		string = 'tb_user_cloud_%s' % request_id
		url = explore % request_id
		return cache_object(self._get, string, url, False, 0.03)

	def user_cloud_info_usenet(self, request_id=''):
		string = 'tb_user_cloud_usenet_%s' % request_id
		url = explore_usenet % request_id
		return cache_object(self._get, string, url, False, 0.03)

	def user_cloud_clear(self):
		if not confirm_dialog(): return
		data = {'all': True, 'operation': 'delete'}
		self._post(remove, json=data)
		self._post(remove_usenet, json=data)
		self.clear_cache()

	def torrent_info(self, request_id=''):
		url = explore % request_id
		return self._get(url)

	def delete_torrent(self, request_id=''):
		data = {'torrent_id': request_id, 'operation': 'delete'}
		return self._post(remove, json=data)

	def delete_usenet(self, request_id=''):
		data = {'usenet_id': request_id, 'operation': 'delete'}
		return self._post(remove_usenet, json=data)

	def unrestrict_link(self, file_id):
		torrent_id, file_id = file_id.split(',')
		data = {'token': self.token, 'torrent_id': torrent_id, 'file_id': file_id}
		try: return self._get(download, data=data)['data']
		except: return None

	def unrestrict_usenet(self, file_id):
		usenet_id, file_id = file_id.split(',')
		params = {'token': self.token, 'usenet_id': usenet_id, 'file_id': file_id, 'user_ip': True}
		try: return self._get(download_usenet, params=params)['data']
		except: return None

	def add_magnet(self, magnet):
		data = {'magnet': magnet, 'seed': 3, 'allow_zip': False}
		return self._post(cloud, data=data)

	def check_cache_single(self, _hash):
		return self._get(cache, data={'hash': _hash, 'format': 'list'})

	def check_cache(self, hashlist):
		data = {'hashes': hashlist}
		return self._post(cache, params={'format': 'list'}, json=data)

	def create_transfer(self, magnet_url):
		result = self.add_magnet(magnet_url)
		if not result['success']: return ''
		return result['data'].get('torrent_id', '')

	def resolve_magnet(self, magnet_url, info_hash, store_to_cloud, title, season, episode):
		try:
			file_url, match, torrent_id = None, False, None
			extensions = supported_video_extensions()
			extras_filtering_list = tuple(i for i in EXTRAS if not i in title.lower())
			torrent = self.add_magnet(magnet_url)
			if not torrent['success']: return None
			torrent_id = torrent['data']['torrent_id']
			torrent_files = self.torrent_info(torrent_id)
			selected_files = [{'url': '%d,%d' % (torrent_id, item['id']), 'filename': item['short_name'], 'size': item['size']} \
							for item in torrent_files['data']['files'] if item['short_name'].lower().endswith(tuple(extensions))]
			if not selected_files: return None
			if season:
				selected_files = [i for i in selected_files if seas_ep_filter(season, episode, i['filename'])]
			else:
				if self._m2ts_check(selected_files): return None
				selected_files = [i for i in selected_files if not any(x in i['filename'] for x in extras_filtering_list)]
				selected_files.sort(key=lambda k: k['size'], reverse=True)
			if not selected_files: return None
			file_key = selected_files[0]['url']
			file_url = self.unrestrict_link(file_key)
			if not store_to_cloud: Thread(target=self.delete_torrent, args=(torrent_id,)).start()
			return file_url
		except:
			if torrent_id: self.delete_torrent(torrent_id)
			return None

	def display_magnet_pack(self, magnet_url, info_hash):
		from modules.source_utils import supported_video_extensions
		try:
			torrent_id = None
			extensions = supported_video_extensions()
			torrent = self.add_magnet(magnet_url)
			if not torrent['success']: return None
			torrent_id = torrent['data']['torrent_id']
			torrent_files = self.torrent_info(torrent_id)
			torrent_files = [{'link': '%d,%d' % (torrent_id, item['id']), 'filename': item['short_name'], 'size': item['size']} \
							for item in torrent_files['data']['files'] if item['short_name'].lower().endswith(tuple(extensions))]
			Thread(target=self.delete_torrent, args=(torrent_id,)).start()
			return torrent_files or None
		except Exception:
			if torrent_id: self.delete_torrent(torrent_id)
			return None

	def _m2ts_check(self, folder_items):
		for item in folder_items:
			if item['filename'].endswith('.m2ts'): return True
		return False

	def auth(self):

		try:
			r = requests.get(base_url + 'user/auth/device/start', params={'app': 'FenLight+ Companion'}, timeout=timeout).json().get('data')
		except Exception: r = None
		if not r: return ok_dialog(text='Could not start TorBox authorization')
		device_code, user_code = r['device_code'], r['code']
		verify_url = r.get('friendly_verification_url') or r.get('verification_url', 'https://torbox.app/oauth/device')
		interval = int(r.get('interval', 5))
		try:
			expires_in = int((js2date(r['expires_at'], '%Y-%m-%dT%H:%M:%SZ') - get_datetime()).total_seconds())
			if expires_in <= 0: expires_in = 300
		except Exception: expires_in = 300
		try: copy2clip(user_code)
		except Exception: pass
		qr_icon = 'https://qrcode.tec-it.com/API/QRCode?data=%s&backcolor=%%23ffffff&size=small&quietzone=1&errorcorrection=H' % quote(verify_url, safe='')
		content = 'Scan the QR code or navigate to: [B]%s[/B][CR]Enter this code: [B]%s[/B]  (copied to clipboard)' % (verify_url, user_code)
		progressDialog = progress_dialog('TorBox Authorize', qr_icon)
		progressDialog.update(content, 0)
		token, start, time_passed = None, time.time(), 0
		while not progressDialog.iscanceled() and time_passed < expires_in and not token:
			sleep(1000 * interval)
			try: response = requests.post(base_url + 'user/auth/device/token', json={'device_code': device_code}, timeout=timeout).json()
			except Exception: continue

			if response.get('success'):
				token = (response.get('data') or {}).get('access_token')
			if not token:
				time_passed = time.time() - start
				progressDialog.update(content, int(100 * time_passed / expires_in))
		try: progressDialog.close()
		except Exception: pass
		if token:
			self.token = token
			try: self.account_info()['data']['customer']  # validate the token
			except Exception: pass
			set_setting('tb.token', token)
			set_setting('tb.enabled', 'true')
			ok_dialog(text='Success')
		else:
			ok_dialog(text='Authorization cancelled or timed out')

	def revoke(self):
		if not confirm_dialog(): return
		set_setting('tb.token', 'empty_setting')
		set_setting('tb.enabled', 'false')
		notification('TorBox Authorization Reset', 3000)

	def clear_cache(self, clear_hashes=True):
		try:
			from caches.debrid_cache import debrid_cache
			from caches.base_cache import connect_database
			dbcon = connect_database('maincache_db')
			# USER CLOUD
			try:
				dbcon.execute("""DELETE FROM maincache WHERE id=?""", ('tb_user_cloud',))
				dbcon.execute("""DELETE FROM maincache WHERE id LIKE ?""", ('tb_user_cloud%',))
				user_cloud_success = True
			except: user_cloud_success = False
			# HASH CACHED STATUS
			if clear_hashes:
				try:
					debrid_cache.clear_debrid_results('tb')
					hash_cache_status_success = True
				except: hash_cache_status_success = False
			else: hash_cache_status_success = True
		except: return False
		if False in (user_cloud_success, hash_cache_status_success): return False
		return True

