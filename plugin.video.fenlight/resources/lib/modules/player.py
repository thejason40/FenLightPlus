# -*- coding: utf-8 -*-
import json
from time import monotonic
from threading import Thread
from apis.trakt_api import make_trakt_slug
from caches.settings_cache import get_setting
from modules import kodi_utils as ku, settings as st, watched_status as ws
# logger = ku.logger

set_property, clear_property, get_visibility, hide_busy_dialog, xbmc_actor = ku.set_property, ku.clear_property, ku.get_visibility, ku.hide_busy_dialog, ku.xbmc_actor
xbmc_player, execute_builtin, sleep = ku.xbmc_player, ku.execute_builtin, ku.sleep
make_listitem, volume_checker, get_infolabel, xbmc_monitor = ku.make_listitem, ku.volume_checker, ku.get_infolabel, ku.xbmc_monitor
global_idle_time = ku.global_idle_time
close_all_dialog, notification, poster_empty, fanart_empty = ku.close_all_dialog, ku.notification, ku.empty_poster, ku.get_addon_fanart()
auto_resume, auto_nextep_settings, store_resolved_to_cloud = st.auto_resume, st.auto_nextep_settings, st.store_resolved_to_cloud
set_bookmark, mark_movie, mark_episode = ws.set_bookmark, ws.mark_movie, ws.mark_episode
total_time_errors = ('0.0', '', 0.0, None)
set_resume, set_watched = 5, 90
video_fullscreen_check = 'Window.IsActive(fullscreenvideo)'

class FenLightPlayer(xbmc_player):
	def __init__ (self):
		xbmc_player.__init__(self)

	def run(self, url=None, obj=None, num_episodes=None):
		hide_busy_dialog()
		self.clear_playback_properties()
		self.num_episodes = num_episodes  # Store num_episodes
		if not url: return self.run_error()
		try: return self.play_video(url, obj, num_episodes)
		except: return self.run_error()

	def play_video(self, url, obj, num_episodes=None):
		self.set_constants(url, obj)
		volume_checker()

		listing = self.make_listing()
		self.play(self.url)
		if not self.is_generic:
			self.check_playback_start()
			if self.playback_successful:
				try: self.updateInfoTag(listing)
				except Exception: pass

				try:
					self.internal_seek_at = monotonic()
					_total = self.getTotalTime()
					if self.playback_percent > 0.0 and _total > 0:
						self.seekTime(_total * self.playback_percent / 100.0)
					else:
						self.seekTime(self.getTime())
				except Exception: pass
				try:
					from modules.pack_continuity import seed_from_playback
					Thread(target=seed_from_playback, args=(self,)).start()
				except: pass
				self.start_skip_watcher()
				self.monitor()
			else:
				self.sources_object.playback_successful = self.playback_successful
				self.sources_object.cancel_all_playback = self.cancel_all_playback
				if self.cancel_all_playback: self.kill_dialog()
				self.stop()
			try: del self.kodi_monitor
			except: pass

	def check_playback_start(self):
		resolve_percent = 0
		while self.playback_successful is None:
			hide_busy_dialog()
			if not self.sources_object.progress_dialog: self.playback_successful = True
			elif self.sources_object.progress_dialog.skip_resolved(): self.playback_successful = False
			elif self.sources_object.progress_dialog.iscanceled() or self.kodi_monitor.abortRequested(): self.cancel_all_playback, self.playback_successful = True, False
			elif resolve_percent >= 100: self.playback_successful = False
			elif get_visibility('Window.IsTopMost(okdialog)'):
				execute_builtin('SendClick(okdialog, 11)')
				self.playback_successful = False
			elif self.isPlayingVideo():
				try:
					if self.getTotalTime() not in total_time_errors and get_visibility(video_fullscreen_check): self.playback_successful = True
				except: pass
			resolve_percent = round(resolve_percent + 26.0/100, 1)
			self.sources_object.progress_dialog.update_resolver(percent=resolve_percent)
			sleep(50)

	def playback_close_dialogs(self):
		self.sources_object.playback_successful = True
		self.kill_dialog()
		sleep(200)
		close_all_dialog()
		self.dialogs_cleared = True

	def monitor(self):
		try:
			ensure_dialog_dead, total_check_time = False, 0
			if self.media_type == 'episode':
				play_random_continual = self.sources_object.random_continual
				play_random = self.sources_object.random
				disable_autoplay_next_episode = self.sources_object.disable_autoplay_next_episode
				if disable_autoplay_next_episode: notification('Scrape with Custom Values - Autoplay Next Episode Cancelled', 4500)
				if any((play_random_continual, play_random, disable_autoplay_next_episode)): self.autoplay_nextep, self.autoscrape_nextep = False, False
				else: self.autoplay_nextep, self.autoscrape_nextep = self.sources_object.autoplay_nextep, self.sources_object.autoscrape_nextep
			else: play_random_continual, self.autoplay_nextep, self.autoscrape_nextep = False, False, False
			while total_check_time <= 30 and not get_visibility(video_fullscreen_check):
				sleep(100)
				total_check_time += 0.10
			hide_busy_dialog()
			sleep(1000)
			while self.isPlayingVideo():
				try:
					try: self.total_time, self.curr_time = self.getTotalTime(), self.getTime()
					except: sleep(250); continue
					if not ensure_dialog_dead:
						ensure_dialog_dead = True
						self.playback_close_dialogs()
					sleep(1000)
					if self.binge_mode: self.check_user_idle()
					self.current_point = round(float(self.curr_time/self.total_time * 100), 1)
					if self.current_point >= set_watched:
						if play_random_continual: self.run_random_continual(); break
						if not self.media_marked: self.media_watched_marker()
					if self.num_episodes and int(self.num_episodes) > 1:
						playNextNum = True
					else:
						playNextNum = False
					if playNextNum: self.autoplay_nextep = True
					if self.binge_mode: self.autoplay_nextep = True
					if self.autoplay_nextep or self.autoscrape_nextep:
						if not self.nextep_info_gathered: self.info_next_ep()
						if round(self.total_time - self.curr_time) <= self.start_prep: self.run_next_ep(); break
				except: pass
			hide_busy_dialog()
			if not self.media_marked: self.media_watched_marker()
			self.clear_playback_properties()
			self.clear_playing_item()
		except:
			hide_busy_dialog()
			self.sources_object.playback_successful = False
			self.sources_object.cancel_all_playback = True
			return self.kill_dialog()

	def make_listing(self):
		listitem = make_listitem()
		listitem.setPath(self.url)
		listitem.setContentLookup(False)
		if self.is_generic:
			info_tag = listitem.getVideoInfoTag()
			info_tag.setMediaType('video')
			info_tag.setFilenameAndPath(self.url)
		else:
			self.tmdb_id, self.imdb_id, self.tvdb_id = self.meta_get('tmdb_id', ''), self.meta_get('imdb_id', ''), self.meta_get('tvdb_id', '')
			self.media_type, self.title, self.year = self.meta_get('media_type'), self.meta_get('title'), self.meta_get('year')
			self.season, self.episode = self.meta_get('season', ''), self.meta_get('episode', '')
			self.auto_resume = auto_resume(self.media_type)
			poster = self.meta_get('poster') or poster_empty
			fanart = self.meta_get('fanart') or fanart_empty
			clearlogo = self.meta_get('clearlogo') or ''
			duration, plot, genre, trailer, mpaa = self.meta_get('duration'), self.meta_get('plot'), self.meta_get('genre', ''), self.meta_get('trailer'), self.meta_get('mpaa')
			rating, votes = self.meta_get('rating'), self.meta_get('votes')
			premiered, studio, tagline = self.meta_get('premiered'), self.meta_get('studio', ''), self.meta_get('tagline')
			director, writer, cast, country = self.meta_get('director', ''), self.meta_get('writer', ''), self.meta_get('cast', []), self.meta_get('country', '')
			listitem.setLabel(self.title)
			if self.media_type == 'movie':
				listitem.setArt({'poster': poster, 'fanart': fanart, 'icon': poster, 'clearlogo': clearlogo})
				info_tag = listitem.getVideoInfoTag()
				info_tag.setMediaType('movie'), info_tag.setTitle(self.title), info_tag.setOriginalTitle(self.meta_get('original_title')), info_tag.setPlot(plot)
				info_tag.setYear(int(self.year)), info_tag.setRating(rating), info_tag.setVotes(votes), info_tag.setMpaa(mpaa)
				info_tag.setDuration(duration), info_tag.setCountries(country), info_tag.setTrailer(trailer), info_tag.setPremiered(premiered)
				info_tag.setTagLine(tagline), info_tag.setStudios(studio), info_tag.setIMDBNumber(self.imdb_id), info_tag.setGenres(genre)
				info_tag.setWriters(writer), info_tag.setDirectors(director), info_tag.setUniqueIDs({'imdb': self.imdb_id, 'tmdb': str(self.tmdb_id)})
				info_tag.setCast([xbmc_actor(name=item['name'], role=item['role'], thumbnail=item['thumbnail']) for item in cast])
			else:
				listitem.setArt({'poster': poster, 'fanart': fanart, 'icon': poster, 'clearlogo': clearlogo, 'tvshow.poster': poster, 'tvshow.clearlogo': clearlogo})
				info_tag = listitem.getVideoInfoTag()
				info_tag.setMediaType('episode'), info_tag.setTitle(self.meta_get('ep_name')), info_tag.setOriginalTitle(self.meta_get('original_title'))
				info_tag.setTvShowTitle(self.title), info_tag.setTvShowStatus(self.meta_get('status')), info_tag.setSeason(self.season), info_tag.setEpisode(self.episode)
				info_tag.setPlot(plot), info_tag.setYear(int(self.year)), info_tag.setRating(rating), info_tag.setVotes(votes)
				info_tag.setMpaa(mpaa), info_tag.setDuration(duration), info_tag.setTrailer(trailer), info_tag.setFirstAired(premiered)
				info_tag.setStudios(studio), info_tag.setIMDBNumber(self.imdb_id), info_tag.setGenres(genre), info_tag.setWriters(writer)
				info_tag.setDirectors(director), info_tag.setUniqueIDs({'imdb': self.imdb_id, 'tmdb': str(self.tmdb_id), 'tvdb': str(self.tvdb_id)})
				info_tag.setCast([xbmc_actor(name=item['name'], role=item['role'], thumbnail=item['thumbnail']) for item in cast])
				info_tag.setFilenameAndPath(self.url)
			self.set_resume_point(listitem)
			self.set_playback_properties()
		return listitem

	def media_watched_marker(self, force_watched=False):
		self.media_marked = True
		try:
			if self.current_point >= set_watched or force_watched:
				if self.media_type == 'movie': watched_function = mark_movie
				else: watched_function = mark_episode
				watched_params = {'action': 'mark_as_watched', 'tmdb_id': self.tmdb_id, 'title': self.title, 'year': self.year, 'season': self.season, 'episode': self.episode,
									'tvdb_id': self.tvdb_id, 'from_playback': 'true'}
				Thread(target=self.run_media_progress, args=(watched_function, watched_params)).start()
			else:
				clear_property('fenlight.random_episode_history')
				if self.current_point >= set_resume:
					progress_params = {'media_type': self.media_type, 'tmdb_id': self.tmdb_id, 'curr_time': self.curr_time, 'total_time': self.total_time,
									'title': self.title, 'season': self.season, 'episode': self.episode, 'from_playback': 'true'}
					Thread(target=self.run_media_progress, args=(set_bookmark, progress_params)).start()
		except Exception as e: ku.logger('fenlight.player', 'marker failed: %s' % e)

	def run_media_progress(self, function, params):
		try: function(params)
		except Exception as e: ku.logger('fenlight.player', 'media progress failed: %s' % e)
		try:
			from modules import tracking
			if tracking.is_external(): tracking.sync_activities()
		except: pass
		try: ku.kodi_refresh()
		except: pass

	def mark_interaction(self):
		# binge counts episodes with no sign of a human, so any user input wipes the tally
		try:
			if self.binge_mode: set_property('fenlight.binge.interacted', 'true')
		except: pass

	def check_user_idle(self):
		# Kodi's idle timer only counts up — a drop means the user pressed something during this episode
		idle = global_idle_time()
		if idle is None: return
		if self.last_idle_time is not None and idle < self.last_idle_time: self.mark_interaction()
		self.last_idle_time = idle

	def internal_seek(self):
		# the resume jump and segment skips call seekTime() themselves; those aren't the user
		try: return (monotonic() - self.internal_seek_at) < 3
		except: return False

	def onPlayBackPaused(self):
		self.mark_interaction()

	def onPlayBackResumed(self):
		self.mark_interaction()

	def onPlayBackSeek(self, time, seekOffset):
		try:
			# the idle timer misses playback driven over JSON-RPC by the companion app, so catch it here too
			if not self.internal_seek(): self.mark_interaction()
			if self.is_generic or not self.total_time: return
			self.curr_time = time / 1000.0
			self.current_point = round(float(self.curr_time / self.total_time * 100), 1)
		except: pass

	def onPlayBackEnded(self):
		try:
			if self.is_generic or self.media_marked: return
			self.media_watched_marker(force_watched=True)
		except Exception as e: ku.logger('fenlight.player', 'onPlayBackEnded failed: %s' % e)

	def run_next_ep(self):
		from modules.episode_tools import EpisodeTools
		if not self.media_marked: self.media_watched_marker(force_watched=True)
		EpisodeTools(self.meta, {**self.nextep_settings, 'num_episodes': self.num_episodes}).auto_nextep()

	def run_random_continual(self):
		from modules.episode_tools import EpisodeTools
		if not self.media_marked: self.media_watched_marker(force_watched=True)
		EpisodeTools(self.meta).play_random_continual(False)

	def set_resume_point(self, listitem):
		if self.playback_percent > 0.0: listitem.setProperty('StartPercent', str(self.playback_percent))

	def info_next_ep(self):
		self.nextep_info_gathered = True
		try:
				play_type = 'autoplay_nextep' if self.autoplay_nextep else 'autoscrape_nextep'
				nextep_settings = auto_nextep_settings(play_type)
				final_chapter = self.final_chapter() if nextep_settings['use_chapters'] else None
				percentage = 100 - final_chapter if final_chapter else nextep_settings['window_percentage']
				window_time = round((percentage/100) * self.total_time)
				use_window = nextep_settings['alert_method'] == 0
				default_action = nextep_settings['default_action']
				self.start_prep = nextep_settings['scraper_time'] + window_time
				if self.num_episodes and int(self.num_episodes) > 1:
					self.nextep_settings = {'num_episodes': self.num_episodes, 'use_window': use_window, 'window_time': window_time, 'default_action': default_action, 'play_type': play_type}
				else:
					self.nextep_settings = {'use_window': use_window, 'window_time': window_time, 'default_action': default_action, 'play_type': play_type}
				if self.binge_mode:
					self.nextep_settings['binge_mode'] = True
					# snapshot the current episode's identity before next_episode_info mutates meta — needed for Stop & Unmark
					self.nextep_settings['prev_episode'] = {'tmdb_id': self.tmdb_id, 'tvdb_id': self.tvdb_id, 'season': self.season, 'episode': self.episode, 'title': self.title}
		except: pass

	def final_chapter(self):
		try:
			final_chapter = float(get_infolabel('Player.Chapters').split(',')[-1])
			if final_chapter >= 90: return final_chapter
		except: pass
		return None

	def kill_dialog(self):
		try: self.sources_object._kill_progress_dialog()
		except: close_all_dialog()

	def set_constants(self, url, obj):
		self.url = url
		self.sources_object = obj
		self.is_generic = self.sources_object == 'video'
		self.binge_mode = getattr(self.sources_object, 'binge_mode', False)
		self.last_idle_time, self.internal_seek_at = None, 0
		if not self.is_generic:
			self.meta = self.sources_object.meta
			self.meta_get, self.kodi_monitor, self.playback_percent = self.meta.get, xbmc_monitor(), self.sources_object.playback_percent or 0.0
			self.playing_filename = self.sources_object.playing_filename
			self.media_marked, self.nextep_info_gathered = False, False
			self.dialogs_cleared = False
			self.current_point, self.total_time, self.curr_time = 0, 0, 0
			self.playback_successful, self.cancel_all_playback = None, False
			self.playing_item = self.sources_object.playing_item

	def set_playback_properties(self):
		try:
			trakt_ids = {'tmdb': self.tmdb_id, 'imdb': self.imdb_id, 'slug': make_trakt_slug(self.title)}
			if self.media_type == 'episode': trakt_ids['tvdb'] = self.tvdb_id
			set_property('script.trakt.ids', json.dumps(trakt_ids))
			if self.playing_filename: set_property('subs.player_filename', self.playing_filename)
		except: pass

	def clear_playback_properties(self):
		clear_property('fenlight.window_stack')
		clear_property('script.trakt.ids')
		clear_property('subs.player_filename')

	def clear_playing_item(self):
		if self.playing_item['cache_provider'] == 'Offcloud':
			if self.playing_item.get('direct_debrid_link', False): return
			if store_resolved_to_cloud('Offcloud', 'package' in self.playing_item): return
			from apis.offcloud_api import OffcloudAPI
			OffcloudAPI().clear_played_torrent(self.playing_item)

	def run_error(self):
		try: self.sources_object.playback_successful = False
		except: pass
		self.clear_playback_properties()
		notification('Playback Failed', 3500)
		return False

	def start_skip_watcher(self):
		try:
			if self.is_generic or self.media_type != 'episode': return
			behavior = st.binge_skip_behavior() if self.binge_mode else 2
			if behavior == 1:
				# binge "Skip All Segments": every kind, silently, regardless of the skip settings
				skip_settings = {'kinds': {'intro', 'recap', 'outro'}, 'dismiss': 0, 'silent': True}
			else:
				skip_settings = st.skip_segment_settings()
				if not skip_settings: return
				skip_settings['silent'] = behavior == 0
			Thread(target=self._skip_watcher, args=(skip_settings,)).start()
		except: pass

	def _play_next_will_fire(self):
		try:
			obj = self.sources_object
			if any((obj.random_continual, obj.random, obj.disable_autoplay_next_episode)): return False
			if self.binge_mode: return True
			if obj.autoplay_nextep or obj.autoscrape_nextep: return True
			return bool(self.num_episodes and int(self.num_episodes) > 1)
		except: return False

	def _skip_watcher(self, skip_settings):
		try:
			kinds = set(skip_settings['kinds'])
			if 'outro' in kinds and self._play_next_will_fire(): kinds.discard('outro')
			if not kinds or not (self.tmdb_id or self.imdb_id): return
			from apis import skip_intro
			dismiss, silent = skip_settings['dismiss'], skip_settings.get('silent', False)
			# Wait for the duration to be known — TheIntroDB matches the release by duration_ms.
			total_time = 0
			while self.isPlayingVideo() and not total_time:
				try: total_time = self.getTotalTime()
				except: total_time = 0
				if not total_time: sleep(500)
			if not total_time: return
			windows = skip_intro.get_skip_windows(self.tmdb_id, self.imdb_id, self.season, self.episode, total_time, kinds)
			if not windows: return
			waited = 0
			while not silent and self.isPlayingVideo() and not self.dialogs_cleared and waited < 15000:
				sleep(250)
				waited += 250
			handled = set()
			while self.isPlayingVideo():
				try: curr_time = self.getTime()
				except: sleep(500); continue
				for w in windows:
					if w['kind'] in handled: continue
					if curr_time >= w['end']: handled.add(w['kind']); continue  # already past (e.g. resume)
					if curr_time >= w['start']:
						handled.add(w['kind'])
						self._do_skip(w, dismiss, silent)
						break
				if len(handled) >= len(windows): return
				sleep(500)
		except: pass

	def _do_skip(self, window, dismiss, silent=False):
		try:
			if silent:
				if self.isPlayingVideo():
					self.internal_seek_at = monotonic()
					self.seekTime(window['end'])
				return
			from windows.base_window import open_window
			choice = open_window(('windows.skip_intro', 'SkipIntro'), 'skip_intro.xml', kind=window['kind'], seconds=dismiss, meta=self.meta)
			if choice == 'skip' and self.isPlayingVideo():
				self.internal_seek_at = monotonic()
				self.seekTime(window['end'])
		except: pass
