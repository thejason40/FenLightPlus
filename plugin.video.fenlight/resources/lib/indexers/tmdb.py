import sys
import json
import random
from queue import SimpleQueue
from apis.tmdb_api import tmdb_list_items
from threading import Thread
from apis import tmdb_api
from indexers.movies import Movies
from indexers.tvshows import TVShows
from modules import kodi_utils
from modules.settings import paginate, page_limit

tmdb_get_lists = tmdb_api.get_lists
add_dir, external, sleep, get_icon = kodi_utils.add_dir, kodi_utils.external, kodi_utils.sleep, kodi_utils.get_icon
trakt_icon, fanart, add_item, set_property = get_icon('trakt'), kodi_utils.get_addon_fanart(), kodi_utils.add_item, kodi_utils.set_property
set_content, set_sort_method, set_view_mode, end_directory = kodi_utils.set_content, kodi_utils.set_sort_method, kodi_utils.set_view_mode, kodi_utils.end_directory
make_listitem, build_url, add_items = kodi_utils.make_listitem, kodi_utils.build_url, kodi_utils.add_items
nextpage_landscape, get_property, clear_property, focus_index = kodi_utils.nextpage_landscape, kodi_utils.get_property, kodi_utils.clear_property, kodi_utils.focus_index
set_category, home, folder_path = kodi_utils.set_category, kodi_utils.home, kodi_utils.folder_path

def get_tmdb_lists(params):
	def _process():
		for item in lists:
			try:
				cm = []
				cm_append = cm.append
				
				list_name, user, list_id, item_count = item['name'], item['account_object_id'], item['id'], item['number_of_items']
				list_name_upper = " ".join(w.capitalize() for w in list_name.split())
				if item['backdrop_path']:
					fanart = 'https://image.tmdb.org/t/p/w1280%s' % item.get('backdrop_path')
				else:
					fanart = kodi_utils.get_addon_fanart()
					
				url_params = {'mode': 'tmdb.list.build_tmdb_list', 'item_count': item_count, 'user': user, 'list_id': list_id, 'list_type': list_type, 'list_name': list_name}
				url = build_url(url_params)
				
				if list_type == 'liked_lists':
					# Hopefully coming in future
					display = '%s | [I]%s (x%s)[/I]' % (list_name_upper, user, str(item_count))
					cm_append(('[B]Unlike List[/B]', 'RunPlugin(%s)' % build_url({'mode': 'tmdb.tmdb_unlike_a_list', 'user': user, 'list_slug': list_id})))
				else:
					display = '%s [I](x%s)[/I]' % (list_name_upper, str(item_count))
					cm_append(('[B]Make New List[/B]', 'RunPlugin(%s)' % build_url({'mode': 'tmdb.make_new_tmdb_list'})))
					cm_append(('[B]Rename List[/B]', 'RunPlugin(%s)' % build_url({'mode': 'tmdb.rename_tmdb_list', 'list_id': list_id, 'list_name': list_name})))
					cm_append(('[B]Delete List[/B]', 'RunPlugin(%s)' % build_url({'mode': 'tmdb.delete_tmdb_list', 'list_id': list_id})))
					cm_append(('[B]Clear List Contents[/B]', 'RunPlugin(%s)' % build_url({'mode': 'tmdb.clear_tmdb_list', 'list_id': list_id, 'list_name': list_name})))
					cm_append(('[B]Import Trakt List Contents[/B]', 'RunPlugin(%s)' % build_url({'mode': 'tmdb_trakt_to_tmdb_choice', 'list_id': list_id, 'list_name': list_name})))
					cm_append(('[B]Clear List Cache[/B]', 'RunPlugin(%s)' % build_url({'mode': 'tmdb.clear_tmdb_list_cache', 'list_id': list_id, 'list_name': list_name})))
				listitem = make_listitem()
				listitem.setLabel(display)
				listitem.setArt({'icon': trakt_icon, 'poster': trakt_icon, 'thumb': trakt_icon, 'fanart': fanart, 'banner': fanart})
				info_tag = listitem.getVideoInfoTag()
				info_tag.setPlot(' ')
				listitem.addContextMenuItems(cm)
				yield (url, listitem, True)
			except Exception as e:
				kodi_utils.logger('TMDB LIST ITEM ERROR', f"{type(e).__name__}: {str(e)}")
	
	page_no = int(params.get('page_no', 1))  # a str from the URL on page 2+
	handle = int(sys.argv[1])
	list_type, shuffle = params['list_type'], params.get('shuffle', 'false') == 'true'
	returning_to_list = False
	# Define sort_method with default value
	sort_method = 'none'
	mode = params.get('mode', '')  # Make sure mode is defined
		
	try:
		data = tmdb_get_lists(list_type, page_no)
		lists = data.get('results')
		
		if shuffle:
			returning_to_list = 'tmdb.list.build_tmdb_list' in folder_path()
			
			if returning_to_list:
				try: 
					lists = json.loads(get_property('fenlight.tmdb.lists.order'))
				except Exception as e: 
					kodi_utils.logger('TMDB ERROR', f'Failed to load lists from property: {str(e)}')
			else:
				try:
					random.shuffle(lists)
					set_property('fenlight.tmdb.lists.order', json.dumps(lists))
				except Exception as e:
					kodi_utils.logger('TMDB ERROR', f'Error in shuffle: {str(e)}')
			
			sort_method = 'none'
		else:
			clear_property('fenlight.tmdb.lists.order')
		
		add_items(handle, list(_process()))
		total_pages=data.get('total_pages')
		if total_pages > page_no:
			new_page = str(page_no + 1)
			new_params = {
				'mode': 'tmdb.list.get_tmdb_lists', 
				'list_type': list_type, 
				'category_name': params.get('category_name', ''), 
				'page_no': int(new_page)
			}
			add_dir(new_params, 'Next Page (%s) >>' % new_page, handle, 'nextpage', nextpage_landscape)
		
	except Exception as e:
		kodi_utils.logger('TMDB ERROR', f'Exception in get_tmdb_list: {type(e).__name__}: {str(e)}')
		# sort_method already defined above
	
	set_content(handle, 'files')
	set_category(handle, params.get('category_name', ''))
	set_sort_method(handle, sort_method)
	end_directory(handle)
	set_view_mode('view.main')
	if shuffle and not returning_to_list: focus_index(0)

def build_tmdb_list(params):
	def _process(function, _list, _type):
		if not _list['list']: 
			kodi_utils.logger('ERROR', 'Returning')
			return
		if _type in ('movies', 'tvshows'): 
			item_list_extend(function(_list).worker())
		elif _type == 'seasons': 
			item_list_extend(function(_list['list']))
		else: 
			item_list_extend(function('episode.tmdb_list', _list['list']))
	
	handle, is_external, is_home, content, list_name = int(sys.argv[1]), external(), home(), 'movies', params.get('list_name')
	try:
		threads, item_list = [], []
		item_list_extend = item_list.extend
		list_id = params.get('list_id')
		paginate_enabled = paginate(is_home)
		page_no = int(params.get('page_no', '1'))
		if page_no == 1 and not is_external: set_property('fenlight.exit_params', folder_path())
		response = tmdb_list_items(list_id, page_no) 
		result = response.get('results', [])
		total_pages = response.get('total_pages', response.get('total_pages'))
		# Split list into different content types
		all_movies = [(idx, i) for idx, i in enumerate(result) if i['media_type'] == 'movie']
		all_tvshows = [(idx, i) for idx, i in enumerate(result) if i['media_type'] == 'tv']

		# Prepare lists for each content type
		movie_list = {'list': [(idx, {'tmdb': item['id'], 'my_tmdb_list': list_id}) for idx, item in all_movies], 'id_type': 'trakt_dict', 'custom_order': 'true'}
		tvshow_list = {'list': [(idx, {'tmdb': item['id'], 'my_tmdb_list': list_id}) for idx, item in all_tvshows], 'id_type': 'trakt_dict', 'custom_order': 'true'}
		# Determine content type (movies, tvshows, etc.)
		content = max([('movies', len(all_movies)), ('tvshows', len(all_tvshows))], key=lambda k: k[1])[0]
		# Process each content type in parallel
		for item in ((Movies, movie_list, 'movies'), (TVShows, tvshow_list, 'tvshows')):
			threaded_object = Thread(target=_process, args=item)
			threaded_object.start()
			threads.append(threaded_object)

		for i, thread in enumerate(threads):
			thread.join()

		item_list.sort(key=lambda k: k[1])

		add_items(handle, [i[0] for i in item_list])
		if not item_list:
			kodi_utils.logger('TMDB DEBUG', 'No items were added to item_list')

		if total_pages > page_no:

			new_page = str(page_no + 1)
			new_params = {'mode': 'tmdb.list.build_tmdb_list', 'list_id': list_id, 'list_type': params.get('list_type'), 'list_name': list_name, 'page_no': int(new_page), 'total_pages': total_pages}
			add_dir(new_params, 'Next Page (%s) >>' % new_page, handle, 'nextpage', nextpage_landscape)

	except Exception as e:
		kodi_utils.logger('TMDB ERROR', f'Exception in build_tmdb_list: {type(e).__name__}: {str(e)}')
		pass

	set_content(handle, content)
	set_category(handle, list_name)
	end_directory(handle, cacheToDisc=False if is_external else True)
	if not is_external:
		if params.get('refreshed') == 'true': sleep(1000)
		set_view_mode('view.%s' % content, content, is_external)
