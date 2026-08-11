# -*- coding: utf-8 -*-
import os
from caches.settings_cache import set_setting
from modules.kodi_utils import get_infolabel, get_visibility, jsonrpc_get_system_setting, confirm_dialog, ok_dialog, notification
# from modules.kodi_utils import logger

AMLOGIC_PATH = '/sys/class/amhdmitx/amhdmitx0'

def _read(path):
	try:
		with open(path) as f: return f.read()
	except: return ''

def detect():
	# returns (caps_list, dv_supported, hdr_supported), or None when the platform can't report display capabilities
	label = get_infolabel('System.SupportedHDRTypes')
	if label:
		caps = [i.strip() for i in label.split(',') if i.strip()]
		dv = 'Dolby Vision' in caps
		if dv and get_visibility('System.Platform.Android'):
			# Kodi 21 Android lets the user strip DV passthrough (0=DV) even when the display supports it
			allowed = jsonrpc_get_system_setting('videoplayer.allowedhdrformats', None)
			if isinstance(allowed, list) and 0 not in allowed: dv = False
		return caps, dv, True
	if os.path.exists(AMLOGIC_PATH):
		# CoreELEC/AMLogic reports the sink's capabilities via sysfs, not the infolabel
		dv_cap, hdr_cap = _read(AMLOGIC_PATH + '/dv_cap'), _read(AMLOGIC_PATH + '/hdr_cap')
		caps = []
		if 'SMPTE ST 2084' in hdr_cap: caps.append('HDR10')
		if 'HDR10Plus Supported: 1' in hdr_cap: caps.append('HDR10+')
		if 'support list' in dv_cap.lower(): caps.append('Dolby Vision')
		if caps: return caps, 'Dolby Vision' in caps, True
		return ['SDR only'], False, False
	if get_visibility('System.Platform.Windows') or get_visibility('System.Platform.Android'):
		# these windowing backends fill the infolabel when the display does HDR, so empty means SDR
		return ['SDR only'], False, False
	return None  # desktop Linux/macOS: Kodi has no display capability reporting

def detect_capabilities_choice(params=None):
	detected = detect()
	if detected is None:
		return ok_dialog(heading='Detect Display Capabilities',
			text='Kodi cannot report display capabilities on this platform.[CR]Please set the filters manually.')
	caps, dv, hdr = detected
	text = 'Display supports: [B]%s[/B][CR][CR]Apply recommended filters?[CR]Dolby Vision Files Filter: [B]%s[/B][CR]HDR Files Filter: [B]%s[/B]' \
		% (', '.join(caps), 'INCLUDE' if dv else 'EXCLUDE', 'INCLUDE' if hdr else 'EXCLUDE')
	if not dv and hdr: text += '[CR]Dolby Vision files with an HDR10 fallback layer will still be included.'
	if not confirm_dialog(heading='Detect Display Capabilities', text=text, ok_label='Apply', cancel_label='Cancel'): return
	set_setting('filter.dv', '0' if dv else '1')
	set_setting('filter.hdr', '0' if hdr else '1')
	notification('Filters Updated', 3000)
