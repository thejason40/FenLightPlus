# -*- coding: utf-8 -*-
import json
from datetime import datetime, timedelta
from xbmc import getRegion
from windows.base_window import BaseDialog, select_dialog, ok_dialog
from modules.source_utils import source_filters
from modules.settings import provider_sort_ranks, easynews_active, easynews_no_seek_default, scraper_window_settings
from modules import kodi_utils
# logger = kodi_utils.logger

kodi_dialog = kodi_utils.kodi_dialog
hide_busy_dialog, addon_fanart, empty_poster = kodi_utils.hide_busy_dialog, kodi_utils.addon_fanart(), kodi_utils.empty_poster
get_icon, img_url = kodi_utils.get_icon, kodi_utils.img_url

resume_dict = {10: 'resume', 11: 'start_over', 12: 'cancel'}
info_icons_dict = {'easynews': get_icon('easynews'), 'alldebrid': get_icon('alldebrid'), 'real-debrid': get_icon('realdebrid'), 'premiumize': get_icon('premiumize'),
'offcloud': get_icon('offcloud'), 'easydebrid': get_icon('easydebrid'), 'torbox': get_icon('torbox'), 'ad_cloud': get_icon('alldebrid'), 'rd_cloud': get_icon('realdebrid'),
'pm_cloud': get_icon('premiumize'), 'oc_cloud': get_icon('offcloud'), 'tb_cloud': get_icon('torbox')}
info_quality_dict = {'4k': get_icon('flag_4k'), '1080p': get_icon('flag_1080p'), '720p': get_icon('flag_720p'), 'sd': get_icon('flag_sd')}
quality_choices = ('4K', '1080P', '720P', 'SD', 'CAM/SCR/TELE')
prerelease_values, prerelease_key = ('CAM', 'SCR', 'TELE'), 'CAM/SCR/TELE'
poster_lists, pack_check = ('list', 'medialist'), ('true', 'show', 'season')
xml_choices = [('List', img_url % 'layout_list'), ('Rows', img_url % 'layout_rows'), ('WideList', img_url % 'layout_widelist')]
run_plugin_str = 'RunPlugin(%s)'
string = str
upper, lower = string.upper, string.lower
resume_timeout = 10000
plus_separator, plus_progress_id = '  |  ', 5000
plus_chips_id, plus_tally_id, plus_chips_pos, plus_tally_pos = 2200, 2210, (90, 980), (90, 828)
plus_chips_per_row, plus_chips_max_rows, plus_chips_row_height = 7, 4, 56
plus_provider_names = {'rd_cloud': 'RD Cloud', 'pm_cloud': 'PM Cloud', 'ad_cloud': 'AD Cloud', 'oc_cloud': 'OC Cloud', 'tb_cloud': 'TB Cloud', 'easynews': 'Easynews'}
episode_type_labels = {'series_premiere': 'Series Premiere', 'season_premiere': 'Season Premiere', 'mid_season_premiere': 'Mid-Season Premiere',
'mid_season_finale': 'Mid-Season Finale', 'season_finale': 'Season Finale', 'series_finale': 'Series Finale'}
tv_plot_order = {'0': ('episode', 'season', 'show'), '1': ('season', 'show'), '2': ('show',), '3': ()}

class SourcesResults(BaseDialog):
	def __init__(self, *args, **kwargs):
		BaseDialog.__init__(self, *args)
		self.window_format = kwargs.get('window_format', 'list')
		self.window_id = kwargs.get('window_id', 2000)
		self.filter_window_id = 2100
		self.results = kwargs.get('results')
		self.uncached_results = kwargs.get('uncached_results', [])
		self.info_highlights_dict = kwargs.get('scraper_settings')
		self.episode_group_label = kwargs.get('episode_group_label', '')
		self.prescrape = kwargs.get('prescrape')
		self.meta = kwargs.get('meta')
		self.filters_ignored = kwargs.get('filters_ignored', False)
		self.meta_get = self.meta.get
		self.make_poster = self.window_format in poster_lists
		self.poster = self.meta_get('poster') or empty_poster
		self.make_items()
		self.make_filter_items()
		self.set_properties()

	def onInit(self):
		self.filter_applied = False
		if self.make_poster: self.set_poster()
		self.add_items(self.window_id, self.item_list)
		self.add_items(self.filter_window_id, self.filter_list)
		self.setFocusId(self.window_id)

	def run(self):
		self.doModal()
		self.clearProperties()
		hide_busy_dialog()
		return self.selected

	def get_provider_and_path(self, provider):
		try: return provider, info_icons_dict[provider]
		except: return 'folders', get_icon('provider_folder')

	def get_quality_and_path(self, quality):
		try: return quality, info_quality_dict[quality]
		except: return 'sd', get_icon('flag_sd')

	def filter_action(self, action):
		if action == self.right_action or action in self.closing_actions:
			self.select_item(self.filter_window_id, 0)
			self.setFocusId(self.window_id)
		if action in self.selection_actions:
			chosen_listitem = self.get_listitem(self.filter_window_id)
			filter_type, filter_value = chosen_listitem.getProperty('filter_type'), chosen_listitem.getProperty('filter_value')
			if filter_type in ('quality', 'provider'):
				if filter_value == prerelease_key: filtered_list = [i for i in self.item_list if i.getProperty(filter_type) in filter_value.split('/')]
				else: filtered_list = [i for i in self.item_list if i.getProperty(filter_type) == filter_value]
			elif filter_type == 'special':
				if filter_value == 'title':
					keywords = kodi_dialog().input('Enter Keyword (Comma Separated for Multiple)')
					if not keywords: return
					keywords = keywords.replace(' ', '').split(',')
					choice = [upper(i) for i in keywords]
					filtered_list = [i for i in self.item_list if all(x in i.getProperty('name') for x in choice)]
				elif filter_value == 'extraInfo':
					list_items = [{'line1': item[0], 'icon': self.poster} for item in source_filters]
					kwargs = {'items': json.dumps(list_items), 'heading': 'Filter Results', 'multi_choice': 'true'}
					choice = select_dialog(source_filters, **kwargs)
					if choice == None: return
					choice = [i[1] for i in choice]
					filtered_list = [i for i in self.item_list if all(x in i.getProperty('extraInfo') for x in choice)]
				elif filter_value == 'showuncached': filtered_list = self.make_items(self.uncached_results)
			if not filtered_list: return ok_dialog(text='No Results')
			self.set_filter(filtered_list)

	def onAction(self, action):
		if self.get_visibility('Control.HasFocus(%s)' % self.filter_window_id): return self.filter_action(action)
		chosen_listitem = self.get_listitem(self.window_id)
		if action in self.closing_actions:
			if self.filter_applied: return self.clear_filter()
			self.selected = (None, '')
			return self.close()
		if action == self.info_action:
			self.open_window(('windows.sources', 'SourcesInfo'), 'sources_info.xml', item=chosen_listitem)
		elif action in self.selection_actions:
			if self.prescrape and chosen_listitem.getProperty('perform_full_search') == 'true':
				self.selected = ('perform_full_search', '')
				return self.close()
			chosen_source = json.loads(chosen_listitem.getProperty('source'))
			if 'Uncached' in chosen_source.get('cache_provider', ''):
				from modules.debrid import manual_add_magnet_to_cloud
				return manual_add_magnet_to_cloud({'mode': 'manual_add_magnet_to_cloud', 'provider': chosen_source['debrid'], 'magnet_url': chosen_source['url']})
			self.selected = ('play', chosen_source)
			return self.close()
		elif action in self.context_actions:
			source = json.loads(chosen_listitem.getProperty('source'))
			choice = self.context_menu(source)
			if choice:
				if choice in ('play_noseek', 'play_seek'):
					self.selected = (choice, source)
					return self.close()
				if isinstance(choice, dict): return self.execute_code(run_plugin_str % self.build_url(choice))
				if choice == 'results_info': return self.open_window(('windows.sources', 'SourcesInfo'), 'sources_info.xml', item=chosen_listitem)

	def make_items(self, filtered_list=None):
		def builder(results):
			for count, item in enumerate(results, 1):
				try:
					get = item.get
					listitem = self.make_listitem()
					set_properties = listitem.setProperties
					scrape_provider, source, quality, name = get('scrape_provider'), get('source'), get('quality', 'SD'), get('display_name')
					basic_quality, quality_icon = self.get_quality_and_path(lower(quality))
					pack = get('package', 'false') in pack_check
					extraInfo = get('extraInfo', '')
					extraInfo = extraInfo.rstrip('| ')
					if pack: extraInfo = '[B]%s PACK[/B] | %s' % (get('package'), extraInfo)
					if self.episode_group_label: extraInfo = '%s | %s' % (self.episode_group_label, extraInfo)
					if not extraInfo: extraInfo = 'N/A'
					if scrape_provider == 'external':
						source_site = upper(get('provider'))
						provider = upper(get('debrid', source_site).replace('.me', ''))
						provider_lower = lower(provider)
						provider_icon = self.get_provider_and_path(provider_lower)[1]
						if 'Uncached' in item['cache_provider']:
							if 'seeders' in item: set_properties({'source_type': 'UNCACHED (%d SEEDERS)' % get('seeders', 0)})
							else: set_properties({'source_type': 'UNCACHED'})
							set_properties({'highlight': 'FF7C7C7C'})
						else:
							cache_flag = 'UNCHECKED' if provider in ('REAL-DEBRID', 'ALLDEBRID') else '[B]CACHED[/B]'
							if highlight_type == 0: key = provider_lower
							else: key = basic_quality
							set_properties({'highlight': self.info_highlights_dict[key]})
							if pack: set_properties({'source_type': '%s [B]PACK[/B]' % cache_flag})
							else: set_properties({'source_type': '%s' % cache_flag})
						set_properties({'provider': provider})
					else:
						source_site = upper(source)
						provider, provider_icon = self.get_provider_and_path(lower(source))
						if highlight_type == 0: key = provider
						else: key = basic_quality
						set_properties({'highlight': self.info_highlights_dict[key], 'source_type': 'DIRECT', 'provider': upper(provider)})
					set_properties({'name': upper(name), 'source_site': source_site, 'provider_icon': provider_icon, 'quality_icon': quality_icon, 'count': '%02d.' % count,
								'size_label': get('size_label', 'N/A'), 'extraInfo': extraInfo, 'quality': upper(quality), 'hash': get('hash', 'N/A'), 'source': json.dumps(item)})	
					yield listitem
				except: pass
		try:
			highlight_type = self.info_highlights_dict['highlight_type']
			if filtered_list: return list(builder(filtered_list))
			self.item_list = list(builder(self.results))
			if self.prescrape:
				prescrape_listitem = self.make_listitem()
				prescrape_listitem.setProperty('perform_full_search', 'true')
			self.total_results = string(len(self.item_list))
			if self.prescrape: self.item_list.append(prescrape_listitem)
		except: pass

	def make_filter_items(self):
		def builder(data):
			for item in data:
				listitem = self.make_listitem()
				listitem.setProperties({'label': item[0], 'filter_type': item[1], 'filter_value': item[2]})
				yield listitem
		duplicates = set()
		qualities = [i.getProperty('quality') for i in self.item_list \
							if not (i.getProperty('quality') in duplicates or duplicates.add(i.getProperty('quality'))) \
							and not i.getProperty('quality') == '']
		if any(i in prerelease_values for i in qualities): qualities = [i for i in qualities if not i in prerelease_values] + [prerelease_key]
		qualities.sort(key=quality_choices.index)
		duplicates = set()
		providers = [i.getProperty('provider') for i in self.item_list \
							if not (i.getProperty('provider') in duplicates or duplicates.add(i.getProperty('provider'))) \
							and not i.getProperty('provider') == '']
		sort_ranks = provider_sort_ranks()
		sort_ranks['premiumize'] = sort_ranks.pop('premiumize.me')
		provider_choices = sorted(sort_ranks.keys(), key=sort_ranks.get)
		provider_choices = [upper(i) for i in provider_choices]
		providers.sort(key=provider_choices.index)
		qualities = [('Show [B]%s[/B] Only' % i, 'quality', i) for i in qualities]
		providers = [('Show [B]%s[/B] Only' % i, 'provider', i) for i in providers]
		data = qualities + providers
		if self.uncached_results: data.append(('Show [B]Uncached[/B] Only', 'special', 'showuncached'))
		data.extend([('Filter by [B]Title[/B]...', 'special', 'title'), ('Filter by [B]Info[/B]...', 'special', 'extraInfo')])
		self.filter_list = list(builder(data))

	def set_properties(self):
		self.setProperty('window_format', self.window_format)
		self.setProperty('fanart', self.meta_get('fanart') or addon_fanart)
		self.setProperty('clearlogo', self.meta_get('clearlogo') or '')
		self.setProperty('title', self.meta_get('title'))
		self.setProperty('total_results', self.total_results)
		self.setProperty('filters_ignored', '| Filters Ignored' if self.filters_ignored else '')

	def set_poster(self):
		if self.window_id == 2000: self.set_image(200, self.poster)

	def context_menu(self, item):
		pack_or_file, download_id, delete_params, down_file_params, down_pack_params, browse_pack_params, add_magnet_to_cloud_params = None, None, None, None, None, None, None
		item_get = item.get
		item_id, name, magnet_url, info_hash = item_get('id', None), item_get('name'), item_get('url', 'None'), item_get('hash', 'None')
		provider_source, scrape_provider, cache_provider = item_get('source'), item_get('scrape_provider'), item_get('cache_provider', 'None')
		uncached = 'Uncached' in cache_provider
		source, meta_json = json.dumps(item), json.dumps(self.meta)
		choices = []
		choices_append = choices.append
		delete_id = item_get('delete_id', None)  # Retrieve pack info from the item dictionary
		download_id = item_get('dl_id', None)
		if provider_source == 'rd_cloud':
			if delete_id:
				delete_params = {'mode': 'real_debrid.delete', 'id': delete_id, 'cache_type': 'torrent'}
				pack_or_file = 'Pack'
			if download_id:
				delete_params = {'mode': 'real_debrid.delete', 'id': download_id, 'cache_type': 'download'}
				pack_or_file = 'File'
		if delete_params: choices_append((f"Delete {pack_or_file}",delete_params))
		if not uncached and scrape_provider != 'folders':
			down_file_params = {'mode': 'downloader.runner', 'action': 'meta.single', 'name': name, 'source': source,
								'url': None, 'provider': scrape_provider, 'meta': meta_json}
		if 'package' in item and not uncached and cache_provider != 'EasyDebrid':
			down_pack_params = {'mode': 'downloader.runner', 'action': 'meta.pack', 'name': self.meta.get('rootname', ''), 'source': source, 'url': None,
								'provider': cache_provider, 'meta': meta_json, 'magnet_url': magnet_url, 'info_hash': info_hash}
		if provider_source == 'torrent' and not uncached:
			browse_pack_params = {'mode': 'debrid.browse_packs', 'provider': cache_provider, 'name': name,
								'magnet_url': magnet_url, 'info_hash': info_hash}
			if cache_provider != 'EasyDebrid': add_magnet_to_cloud_params = {'mode': 'manual_add_magnet_to_cloud', 'provider': cache_provider, 'magnet_url': magnet_url}
		choices_append(('Info', 'results_info'))
		if add_magnet_to_cloud_params: choices_append(('Add to Cloud', add_magnet_to_cloud_params))
		if browse_pack_params: choices_append(('Browse', browse_pack_params))
		if down_pack_params: choices_append(('Download Pack', down_pack_params))
		if down_file_params: choices_append(('Download File', down_file_params))
		if scrape_provider == 'easynews' and easynews_active():
			if easynews_no_seek_default(): choices.insert(0, ('Play With Seeking', 'play_seek'))
			else: choices.insert(0, ('Play With No Seek', 'play_noseek'))
		list_items = [{'line1': i[0], 'icon': self.poster} for i in choices]
		kwargs = {'items': json.dumps(list_items)}
		choice = select_dialog([i[1] for i in choices], **kwargs)
		return choice

	def set_filter(self, filtered_list):
		self.filter_applied = True
		self.reset_window(self.window_id)
		self.add_items(self.window_id, filtered_list)
		self.setFocusId(self.window_id)
		self.setProperty('total_results', string(len(filtered_list)))
		self.setProperty('filter_applied', 'true')
		self.setProperty('filter_info', '| Press [B]BACK[/B] to Cancel')

	def clear_filter(self):
		self.filter_applied = False
		self.reset_window(self.window_id)
		self.add_items(self.window_id, self.item_list)
		self.setFocusId(self.window_id)
		self.select_item(self.filter_window_id, 0)
		self.setProperty('total_results', self.total_results)
		self.setProperty('filter_applied', 'false')
		self.setProperty('filter_info', '')

class SourcesPlayback(BaseDialog):
	def __init__(self, *args, **kwargs):
		BaseDialog.__init__(self, *args)
		self.meta = kwargs.get('meta')
		self.is_canceled, self.skip_resolve, self.resume_choice = False, False, None
		self.meta_get = self.meta.get
		self.plus = args[0] == 'sources_playback_plus.xml'
		if self.plus:
			self.plus_settings = scraper_window_settings()
			self.plus_chip_names, self.plus_chip_items, self.plus_chip_state = None, [], []
		self.enable_scraper()

	def run(self):
		self.doModal()
		self.clearProperties()
		self.clear_modals()

	def onClick(self, controlID):
		if controlID in resume_dict: self.resume_choice = resume_dict[controlID]

	def onAction(self, action):
		if action in self.closing_actions: self.is_canceled = True
		elif action == self.right_action and self.window_mode == 'resolver': self.skip_resolve = True

	def iscanceled(self):
		return self.is_canceled

	def skip_resolved(self):
		status = self.skip_resolve
		self.skip_resolve = False
		return status

	def reset_is_cancelled(self):
		self.is_canceled = False

	def enable_scraper(self):
		self.window_mode = 'scraper'
		self.set_scraper_properties()

	def enable_resolver(self):
		self.window_mode = 'resolver'
		self.set_resolver_properties()

	def enable_resume(self, percent, no_seek=False):
		self.window_mode = 'resume'
		self.set_resume_properties(percent, no_seek)

	def busy_spinner(self, toggle='true'):
		self.setProperty('enable_busy_spinner', toggle)

	def set_scraper_properties(self):
		title, year, genre = self.meta_get('title'), string(self.meta_get('year')), self.meta_get('genre', '')
		poster = self.meta_get('poster') or empty_poster
		fanart = self.meta_get('fanart') or addon_fanart
		clearlogo = self.meta_get('clearlogo') or ''
		self.setProperty('window_mode', self.window_mode)
		self.setProperty('title', title)
		self.setProperty('fanart', fanart)
		self.setProperty('clearlogo', clearlogo)
		self.setProperty('year', year)
		self.setProperty('poster', poster)
		self.setProperty('genre', ', '.join(genre))
		if self.plus: self.set_plus_properties()

	def set_plus_properties(self):
		settings, is_movie = self.plus_settings, self.meta_get('media_type') == 'movie'
		if is_movie:
			duration, rating, premiere_label = self.meta_get('duration'), self.meta_get('rating'), ''
			line1 = [string(self.meta_get('year') or ''), self.plus_runtime(duration)] if settings['details'] else []
			plot = self.meta_get('plot', '') if settings['movie_plot'] else ''
		else:
			duration, rating = self.meta_get('ep_duration') or self.meta_get('duration'), self.meta_get('ep_rating') or self.meta_get('rating')
			premiere_label = episode_type_labels.get(self.meta_get('episode_type'), '')
			try: episode_code = '[B]S%02dE%02d[/B]' % (self.meta_get('season'), self.meta_get('episode'))
			except: episode_code = ''
			line1 = [episode_code, self.meta_get('ep_name') or '']
			if settings['details']: line1.append(self.plus_runtime(duration))
			plot = self.plus_tv_plot(settings['tv_plot'])
		if settings['ends_at']: line1.append(self.plus_ends_at(duration))
		line2 = [premiere_label, self.meta_get('mpaa') or '', ', '.join((self.meta_get('genre') or [])[:3]), self.plus_rating(rating)] if settings['details'] else []
		self.setProperty('line1', plus_separator.join(i for i in line1 if i))
		self.setProperty('line2', plus_separator.join(i for i in line2 if i))
		self.setProperty('plot_text', plot or '')
		self.setProperty('credits_line', self.plus_credits(is_movie))
		for quality in ('4k', '1080p', '720p', 'sd', 'total'): self.setProperty('results_%s' % quality, '0')

	def plus_credits(self, is_movie):
		items = []
		if self.plus_settings['crew']:
			if is_movie:
				director = self.meta_get('director') or []
				if director: items.append('%s: %s' % ('Directors' if len(director) > 1 else 'Director', ', '.join(director[:2])))
			else:
				created_by = (self.meta_get('extra_info') or {}).get('created_by')
				if created_by and created_by != 'N/A': items.append('Created by %s' % created_by)
		if self.plus_settings['cast']:
			names = [i['name'] for i in (self.meta_get('cast') or [])[:3] if i.get('name')]
			if names: items.append('Starring: %s' % ', '.join(names))
		return plus_separator.join(items)

	def plus_tv_plot(self, choice):
		for item in tv_plot_order.get(choice, ()):
			if item == 'episode': plot = self.meta_get('plot')
			elif item == 'show': plot = self.meta_get('tvshow_plot')
			else:
				try: plot = next(i['overview'] for i in self.meta_get('season_data') if int(i['season_number']) == int(self.meta_get('season')))
				except: plot = ''
			if plot: return plot
		return ''

	def plus_runtime(self, duration):
		try: hours, minutes = divmod(int(duration) // 60, 60)
		except: return ''
		if hours: return '%dh %dm' % (hours, minutes)
		return '%dm' % minutes if minutes else ''

	def plus_rating(self, rating):
		try: rating = float(rating)
		except: return ''
		return '%.1f' % rating if rating > 0 else ''

	def plus_ends_at(self, duration):
		try: duration = int(duration)
		except: return ''
		if not duration: return ''
		time_format = getRegion('time').replace(':%S', '')
		return 'Ends at %s' % (datetime.now() + timedelta(seconds=duration)).strftime(time_format)

	def plus_provider_name(self, name):
		return plus_provider_names.get(name) or (name.title() if name == name.lower() else name)

	def update_plus_time(self, phase, elapsed, timeout, late):
		if elapsed is None or not timeout: return
		elapsed = min(max(elapsed, 0), timeout)
		self.setProperty('time_label', '[COLOR FFB40025]%.1fs left[/COLOR]' % (timeout - elapsed) if late else '%.1fs' % elapsed)
		self.setProperty('time_sub', 'of %ds %s' % (timeout, 'cache check' if phase == 'cache' else 'timeout'))

	def update_plus_chips(self, providers):
		names = [i[0] for i in providers]
		if names != self.plus_chip_names: return self.build_plus_chips(providers)
		for item, last, (name, state, count) in zip(self.plus_chip_items, self.plus_chip_state, providers):
			if last != (state, count): self.set_plus_chip(item, state, count)
		self.plus_chip_state = [(i[1], i[2]) for i in providers]

	def set_plus_chip(self, item, state, count):
		item.setProperty('state', state)
		item.setProperty('count', string(count) if state == 'done' else 'failed' if state == 'failed' else '')

	def build_plus_chips(self, providers):
		items = []
		for name, state, count in providers:
			item = self.make_listitem()
			item.setProperty('name', self.plus_provider_name(name))
			self.set_plus_chip(item, state, count)
			items.append(item)
		self.reset_window(plus_chips_id)
		self.add_items(plus_chips_id, items)
		self.plus_chip_names, self.plus_chip_items = [i[0] for i in providers], items
		self.plus_chip_state = [(i[1], i[2]) for i in providers]
		rows = min(max(-(-len(providers) // plus_chips_per_row), 1), plus_chips_max_rows)
		lift = (rows - 1) * plus_chips_row_height
		try:
			chips = self.get_control(plus_chips_id)
			chips.setPosition(plus_chips_pos[0], plus_chips_pos[1] - lift)
			chips.setHeight(rows * plus_chips_row_height)
			self.get_control(plus_tally_id).setPosition(plus_tally_pos[0], plus_tally_pos[1] - lift)
		except: pass

	def update_plus_progress(self, percent):
		try:
			percent = float(percent)
			self.setProperty('progress_late', 'true' if percent >= 75 else 'false')
			self.set_percent(plus_progress_id, percent)
			self.set_percent(plus_progress_id + 1, percent)
		except: pass

	def set_resolver_properties(self):
		if self.meta_get('media_type') == 'movie': self.text = self.meta_get('plot')
		else: self.text = '[B]%02dx%02d - %s[/B][CR][CR]%s' % (self.meta_get('season'), self.meta_get('episode'), self.meta_get('ep_name', 'N/A').upper(), self.meta_get('plot', '') 
															or self.meta_get('tvshow_plot', ''))
		self.setProperty('window_mode', self.window_mode)
		self.setProperty('text', self.text)

	def set_resume_properties(self, percent, no_seek=False):
		self.setProperty('window_mode', self.window_mode)
		self.setProperty('resume_percent', percent)
		self.setProperty('resume_no_seek', 'true' if no_seek else 'false')
		self.setProperty('resume_label', "Resume (Cancel 'no seek')" if no_seek else 'Resume %s%%' % percent)
		self.setFocusId(10)
		self.update_resumer()

	def update_scraper(self, results_sd, results_720p, results_1080p, results_4k, results_total, content='', percent=0, phase='scrape', elapsed=None, timeout=None, providers=None):
		if self.plus: return self.update_scraper_plus(results_sd, results_720p, results_1080p, results_4k, results_total, percent, phase, elapsed, timeout, providers)
		self.setProperty('results_4k', string(results_4k))
		self.setProperty('results_1080p', string(results_1080p))
		self.setProperty('results_720p', string(results_720p))
		self.setProperty('results_sd', string(results_sd))
		self.setProperty('results_total', string(results_total))
		self.setProperty('percent', string(percent))
		self.set_text(2001, content)

	def update_scraper_plus(self, results_sd, results_720p, results_1080p, results_4k, results_total, percent, phase, elapsed, timeout, providers):
		self.setProperty('results_4k', string(results_4k))
		self.setProperty('results_1080p', string(results_1080p))
		self.setProperty('results_720p', string(results_720p))
		self.setProperty('results_sd', string(results_sd))
		self.setProperty('results_total', string(results_total))
		self.update_plus_progress(percent)
		try:
			self.update_plus_time(phase, elapsed, timeout, float(percent) >= 75)
			if providers is not None: self.update_plus_chips(providers)
		except: pass

	def update_resolver(self, text='', percent=0):
		try: self.setProperty('percent', string(percent))
		except: pass
		if self.plus: self.update_plus_progress(percent)
		if text: self.set_text(2002, text)

	def update_resumer(self):
		count = 0
		while self.resume_choice is None:
			percent = int((float(count)/resume_timeout)*100)
			if percent >= 100: self.resume_choice = 'resume'
			self.setProperty('percent', string(percent))
			if self.plus: self.update_plus_progress(percent)
			count += 100
			self.sleep(100)

class SourcesInfo(BaseDialog):
	def __init__(self, *args, **kwargs):
		BaseDialog.__init__(self, *args)
		self.item = kwargs['item']
		self.item_get_property = self.item.getProperty
		self.set_properties()

	def run(self):
		self.doModal()

	def onAction(self, action):
		self.close()

	def get_provider_and_path(self):
		try:
			provider = lower(self.item_get_property('provider'))
			icon_path = info_icons_dict[provider]
		except: provider, icon_path = 'folders', get_icon('provider_folder')
		return provider, icon_path

	def get_quality_and_path(self):
		quality = lower(self.item_get_property('quality'))
		icon_path = info_quality_dict[quality]
		return quality, icon_path

	def set_properties(self):
		provider, provider_path = self.get_provider_and_path()
		quality, quality_path = self.get_quality_and_path()
		self.setProperty('name', self.item_get_property('name'))
		self.setProperty('source_type', self.item_get_property('source_type'))
		self.setProperty('source_site', self.item_get_property('source_site'))
		self.setProperty('size_label', self.item_get_property('size_label'))
		self.setProperty('extraInfo', self.item_get_property('extraInfo'))
		self.setProperty('highlight', self.item_get_property('highlight'))
		self.setProperty('hash', self.item_get_property('hash'))
		self.setProperty('provider', provider)
		self.setProperty('quality', quality)
		self.setProperty('provider_icon', provider_path)
		self.setProperty('quality_icon', quality_path)

class SourcesChoice(BaseDialog):
	def __init__(self, *args, **kwargs):
		BaseDialog.__init__(self, *args)
		self.window_id = 5001
		self.item_list = []
		self.make_items()

	def onInit(self):
		self.add_items(self.window_id, self.item_list)
		self.setFocusId(self.window_id)

	def run(self):
		self.doModal()
		return self.choice

	def onAction(self, action):
		if action in self.closing_actions:
			self.choice = None
			self.close()
		if action in self.selection_actions:
			chosen_listitem = self.get_listitem(self.window_id)
			self.choice = chosen_listitem.getProperty('name')
			self.close()

	def make_items(self):
		append = self.item_list.append
		for item in xml_choices:
			listitem = self.make_listitem()
			listitem.setProperties({'name': item[0], 'image': item[1]})
			append(listitem)
