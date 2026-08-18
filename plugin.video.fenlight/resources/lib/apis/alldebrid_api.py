# -*- coding: utf-8 -*-
import re
import time
import requests
from threading import Thread
from caches.main_cache import cache_object
from caches.settings_cache import get_setting, set_setting
from modules.utils import copy2clip
from modules.source_utils import supported_video_extensions, seas_ep_filter, EXTRAS
from modules import kodi_utils
# logger = kodi_utils.logger

path_exists, get_icon = kodi_utils.path_exists, kodi_utils.get_icon
show_busy_dialog, confirm_dialog = kodi_utils.show_busy_dialog, kodi_utils.confirm_dialog
sleep, ok_dialog = kodi_utils.sleep, kodi_utils.ok_dialog
progress_dialog, notification, hide_busy_dialog, xbmc_monitor = kodi_utils.progress_dialog, kodi_utils.notification, kodi_utils.hide_busy_dialog, kodi_utils.xbmc_monitor
logger = kodi_utils.logger
base_url = 'https://api.alldebrid.com/'
user_agent = 'Fen Light for Kodi'
timeout = 20.0
files_chunk_size = 50
icon = get_icon('alldebrid')

def _flatten_files(tree):
	results = []
	append = results.append
	def _walk(nodes):
		for node in nodes:
			if not isinstance(node, dict): continue
			if node.get('l'): append({'filename': node.get('n', ''), 'size': node.get('s', 0), 'link': node['l']})
			children = node.get('e') or node.get('files')
			if children: _walk(children)
	try: _walk(tree or [])
	except Exception as e: logger('alldebrid', 'flatten files error: %s' % e)
	return results

class AllDebridAPI:
	def __init__(self):
		self.token = get_setting('fenlight.ad.token', 'empty_setting')
		self.break_auth_loop = False

	def auth(self):
		self.token = ''
		response = requests.get(base_url + 'v4/pin/get', timeout=timeout).json()
		response = response['data']
		expires_in = int(response['expires_in'])
		user_code = response['pin']
		poll_url = response.get('check_url')
		if not poll_url: poll_url = base_url + 'v4/pin/check?check=%s&pin=%s' % (response.get('check'), user_code)
		try: copy2clip(user_code)
		except: pass
		sleep_interval = 5
		content = 'Scan the QR Code or navigate to: [B]%s[/B][CR]Enter the following code: [B]%s[/B]' % (response.get('base_url'), user_code)
		tiny_url, qr_icon = kodi_utils.make_qr(response.get('user_url') or response.get('base_url'))
		progressDialog = progress_dialog('All Debrid Authorize', qr_icon)
		progressDialog.update(content, 0)
		start, time_passed = time.time(), 0
		sleep(2000)
		while not progressDialog.iscanceled() and time_passed < expires_in and not self.token:
			sleep(1000 * sleep_interval)
			try: response = requests.get(poll_url, timeout=timeout).json()
			except: continue
			response = response.get('data', {})
			activated = response.get('activated')
			if not activated:
				time_passed = time.time() - start
				progress = int(100 * time_passed/float(expires_in))
				progressDialog.update(content, progress)
				continue
			try:
				progressDialog.close()
				self.token = str(response['apikey'])
				set_setting('ad.token', self.token)
			except:
				ok_dialog(text='Error')
				break
		try: progressDialog.close()
		except: pass
		if self.token:
			sleep(2000)
			account_info = self.account_info()
			if not account_info: return ok_dialog(text='Error')
			set_setting('ad.account_id', str(account_info['user']['username']))
			set_setting('ad.enabled', 'true')
			ok_dialog(text='Success')

	def revoke(self):
		set_setting('ad.token', 'empty_setting')
		set_setting('ad.account_id', 'empty_setting')
		set_setting('ad.enabled', 'false')
		notification('All Debrid Authorization Reset', 3000)

	def account_info(self):
		return self._request('v4/user')

	def user_cloud(self):
		return cache_object(self._user_cloud, 'ad_user_cloud', [], False, 0.03)

	def _user_cloud(self):
		result = self._request('v4.1/magnet/status', {'status': 'ready'})
		magnets = (result or {}).get('magnets') or []
		if isinstance(magnets, dict): magnets = [magnets]
		return {'magnets': magnets}

	def unrestrict_link(self, link):
		result = self._request('v4/link/unlock', {'link': link})
		try: return result['link']
		except: return None

	def create_transfer(self, magnet):
		result = self._request('v4/magnet/upload', {'magnets[]': [magnet]})
		try: result = result['magnets'][0]
		except: return None, False
		if result.get('error'):
			logger('alldebrid', 'magnet/upload: %s' % result['error'])
			return None, False
		return result.get('id', ''), result.get('ready', False) is True

	def list_transfer(self, transfer_id):
		result = self._request('v4.1/magnet/status', {'id': transfer_id})
		magnets = (result or {}).get('magnets')
		if isinstance(magnets, list): magnets = magnets[0] if magnets else None
		return magnets

	def magnet_files(self, transfer_ids):
		transfer_ids = [str(i) for i in transfer_ids]
		results = []
		for index in range(0, len(transfer_ids), files_chunk_size):
			result = self._request('v4/magnet/files', {'id[]': transfer_ids[index:index+files_chunk_size]})
			if not result: continue
			results.extend(result.get('magnets') or [])
		return results

	def delete_transfer(self, transfer_id):
		result = self._request('v4/magnet/delete', {'id': transfer_id})
		if not result: return False
		return result.get('message', '') == 'Magnet was successfully deleted'

	def resolve_magnet(self, magnet_url, info_hash, store_to_cloud, title, season, episode):
		try:
			file_url, media_id, transfer_id = None, None, None
			extensions = supported_video_extensions()
			correct_files = []
			transfer_id, transfer_finished = self.create_transfer(magnet_url)
			if not transfer_id: return None
			elapsed_time = 0
			if not transfer_finished:
				sleep(1000)
				while elapsed_time <= 15 and not transfer_finished:
					transfer_info = self.list_transfer(transfer_id)
					if not transfer_info: break
					status_code = transfer_info['statusCode']
					if status_code > 4: break
					elapsed_time += 1
					if status_code == 4: transfer_finished = True
					elif status_code < 4: sleep(1000)
			if not transfer_finished:
				self.delete_transfer(transfer_id)
				return None
			transfer_files = _flatten_files(self.magnet_files([transfer_id]))
			valid_results = [i for i in transfer_files if any(i.get('filename').lower().endswith(x) for x in extensions) and not i.get('link', '') == '']
			if valid_results:
				# hand the whole pack up before the season filter narrows it
				from caches.pack_cache import stash_pack
				stash_pack(info_hash, [{'filename': i['filename'], 'link': i['link'] if store_to_cloud else '', 'size': i.get('size', 0)} for i in valid_results])
				if season:
					correct_files = [i for i in valid_results if seas_ep_filter(season, episode, i['filename'])]
					if correct_files:
						extras = [i for i in EXTRAS if not i == title.lower()]
						episode_title = re.sub(r'[^A-Za-z0-9-]+', '.', title.replace('\'', '').replace('&', 'and').replace('%', '.percent')).lower()
						try: media_id = [i['link'] for i in correct_files if not any(x in re.sub(episode_title, '', seas_ep_filter(season, episode, i['filename'], split=True)) \
											for x in extras)][0]
						except: media_id = None
				else: media_id = max(valid_results, key=lambda x: x.get('size')).get('link', None)
			if not store_to_cloud: Thread(target=self.delete_transfer, args=(transfer_id,)).start()
			if media_id:
				file_url = self.unrestrict_link(media_id)
				if not any(file_url.lower().endswith(x) for x in extensions): file_url = None
			return file_url
		except:
			try:
				if transfer_id: self.delete_transfer(transfer_id)
			except: pass
			return None

	def display_magnet_pack(self, magnet_url, info_hash):
		from modules.source_utils import supported_video_extensions
		try:
			transfer_id = None
			extensions = supported_video_extensions()
			transfer_id, transfer_finished = self.create_transfer(magnet_url)
			if not transfer_id: return None
			elapsed_time = 0
			if not transfer_finished:
				sleep(1000)
				while elapsed_time <= 15 and not transfer_finished:
					transfer_info = self.list_transfer(transfer_id)
					if not transfer_info: break
					status_code = transfer_info['statusCode']
					if status_code > 4: break
					elapsed_time += 1
					if status_code == 4: transfer_finished = True
					elif status_code < 4: sleep(1000)
			if not transfer_finished:
				self.delete_transfer(transfer_id)
				return None
			end_results = []
			append = end_results.append
			for item in _flatten_files(self.magnet_files([transfer_id])):
				if any(item.get('filename').lower().endswith(x) for x in extensions) and not item.get('link', '') == '':
					append({'link': item['link'], 'filename': item['filename'], 'size': item['size']})
			self.delete_transfer(transfer_id)
			return end_results
		except:
			try:
				if transfer_id: self.delete_transfer(transfer_id)
			except: pass
			return None

	def _request(self, path, data=None):
		try:
			if self.token in ('empty_setting', ''): return None
			headers = {'Authorization': 'Bearer %s' % self.token, 'User-Agent': user_agent}
			result = requests.post(base_url + path, headers=headers, data=data or {}, timeout=timeout).json()
			if result.get('status') == 'success': return result.get('data')
			error = result.get('error') or {}
			logger('alldebrid', '%s: %s | %s' % (path, error.get('code', 'unknown_error'), error.get('message', result)))
			return None
		except Exception as e:
			logger('alldebrid', '%s: %s' % (path, e))
			return None

	def clear_cache(self, clear_hashes=True):
		try:
			from caches.debrid_cache import debrid_cache
			from caches.base_cache import connect_database
			dbcon = connect_database('maincache_db')
			# USER CLOUD
			try:
				dbcon.execute("""DELETE FROM maincache WHERE id=?""", ('ad_user_cloud',))
				user_cloud_success = True
			except: user_cloud_success = False
			# HASH CACHED STATUS
			if clear_hashes:
				try:
					debrid_cache.clear_debrid_results('ad')
					hash_cache_status_success = True
				except: hash_cache_status_success = False
			else: hash_cache_status_success = True
		except: return False
		if False in (user_cloud_success, hash_cache_status_success): return False
		return True
