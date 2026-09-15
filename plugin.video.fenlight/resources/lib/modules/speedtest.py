# -*- coding: utf-8 -*-
import time
from caches.settings_cache import get_setting, set_setting
from modules.kodi_utils import make_session, progress_dialog, confirm_dialog, ok_dialog, notification, xbmc_monitor
from modules.source_utils import max_size_for_line_speed
# from modules.kodi_utils import logger

ENDPOINTS = ('https://speed.cloudflare.com/__down?bytes=%s',)
HEADERS = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36'}
TEST_BYTES = 99000000
MAX_SECONDS = 8.0
RAMP_SECONDS = 1.0
CHUNK = 65536
UPDATE_INTERVAL = 0.25
MIN_DISPLAY_WINDOW = 0.5
MOVIE_DURATION, EPISODE_DURATION = 5400, 2400

def _stream_chunks(session, endpoint, deadline):
	while time.time() < deadline:
		response = session.get(endpoint % TEST_BYTES, headers=HEADERS, stream=True, timeout=(5, 10))
		try:
			response.raise_for_status()
			for chunk in response.iter_content(CHUNK):
				yield chunk
				if time.time() >= deadline: return
		finally:
			try: response.close()
			except Exception: pass

def measure(progress=None):
	monitor = xbmc_monitor()
	for endpoint in ENDPOINTS:
		try:
			start, last_update = time.time(), 0
			measuring_from, measured_bytes = None, 0
			for chunk in _stream_chunks(make_session(), endpoint, start + MAX_SECONDS):
				elapsed = time.time() - start
				if measuring_from is None:
					# still ramping - throw these bytes away and start the clock afresh
					if elapsed < RAMP_SECONDS: continue
					measuring_from = time.time()
					continue
				measured_bytes += len(chunk)
				if progress and elapsed - last_update > UPDATE_INTERVAL:
					last_update = elapsed
					window = time.time() - measuring_from
					# bytes buffered during the ramp land in a near-empty window, so don't show a figure until it fills
					if window >= MIN_DISPLAY_WINDOW: progress.update('%.1f Mbit/s' % _mbps(measured_bytes, window), int(min(elapsed / MAX_SECONDS, 1) * 100))
					else: progress.update('Testing...', int(min(elapsed / MAX_SECONDS, 1) * 100))
					if progress.iscanceled(): return None
				if monitor.abortRequested(): break
			if measuring_from:
				window = time.time() - measuring_from
				if window > 0 and measured_bytes: return _mbps(measured_bytes, window)
		except Exception:
			pass
	return None

def _mbps(measured_bytes, seconds):
	return (measured_bytes * 8) / seconds / 1000000

def run_speedtest_choice(params=None):
	canceled = False
	progress = progress_dialog('Testing Internet Speed')
	try:
		mbps = measure(progress)
		canceled = progress.iscanceled()
	finally:
		try: progress.close()
		except Exception: pass
	if canceled: return
	if mbps is None:
		return ok_dialog(heading='Test Internet Speed',
			text='The speed test could not reach the test server.[CR]Please check this device\'s internet connection and try again.')
	new_value = max(1, int(mbps))
	text = 'Measured download speed: [B]%.1f Mbit/s[/B][CR][CR]Set Internet Speed to [B]%s[/B] (currently [B]%s[/B])?[CR][CR]' \
		'Maximum file size becomes [B]%.1f GB[/B] for a 90 minute movie and [B]%.1f GB[/B] for a 40 minute episode.' \
		% (mbps, new_value, get_setting('fenlight.results.line_speed', '25'),
			max_size_for_line_speed(new_value, MOVIE_DURATION), max_size_for_line_speed(new_value, EPISODE_DURATION))
	if not confirm_dialog(heading='Test Internet Speed', text=text, ok_label='Apply', cancel_label='Cancel'): return
	set_setting('results.line_speed', str(new_value))
	notification('Internet Speed Updated', 3000)
