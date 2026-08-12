# -*- coding: utf-8 -*-
from windows.base_window import BaseDialog
# from modules.kodi_utils import logger

countdown_seconds = 60
button_actions = {11: 'continue', 12: 'stop'}

class StillWatching(BaseDialog):
	def __init__(self, *args, **kwargs):
		BaseDialog.__init__(self, *args)
		self.closed = False
		self.meta = kwargs.get('meta')
		self.selected = 'timeout'
		self.set_properties()

	def onInit(self):
		self.setFocusId(11)
		self.monitor()

	def run(self):
		self.doModal()
		self.clearProperties()
		self.clear_modals()
		return self.selected

	def onAction(self, action):
		if action in self.closing_actions:
			self.selected = 'stop'
			self.closed = True
			self.close()

	def onClick(self, controlID):
		if controlID in button_actions:
			self.selected = button_actions[controlID]
			self.closed = True
			self.close()

	def set_properties(self):
		self.setProperty('thumb', self.meta.get('ep_thumb', None) or self.meta.get('fanart', ''))
		self.setProperty('clearlogo', self.meta.get('clearlogo', ''))
		try: self.setProperty('episode_label', '%s[B] | [/B]%02dx%02d[B] | [/B]%s' % (self.meta['title'], self.meta['season'], self.meta['episode'], self.meta['ep_name']))
		except: self.setProperty('episode_label', self.meta.get('title', ''))

	def monitor(self):
		# 'timeout' stands if the countdown expires or playback ends before the user answers
		remaining = countdown_seconds
		while remaining > 0 and self.player.isPlaying():
			if self.closed: break
			self.setProperty('countdown', str(remaining))
			self.sleep(1000)
			remaining -= 1
		self.close()
