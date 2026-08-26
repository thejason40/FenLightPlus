# -*- coding: utf-8 -*-
import re
import json
import requests
from caches.base_cache import connect_database
from caches.main_cache import cache_object, main_cache
from modules.dom_parser import parseDOM
from modules.kodi_utils import logger
from modules.utils import remove_accents

gql_url = 'https://graphql.imdb.com/'
gql_headers = {
	'Content-Type': 'application/json',
	'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
	'Accept': 'application/json', 'Origin': 'https://www.imdb.com', 'Referer': 'https://www.imdb.com/',
	'x-imdb-client-name': 'imdb-web-next-localized', 'x-imdb-user-language': 'en-US', 'x-imdb-user-country': 'US'}
people_search_url = 'https://sg.media-imdb.com/suggests/%s/%s.json'
people_search_url_backup = 'https://www.imdb.com/search/name/?name=%s'
timeout = 20.0

# GraphQL queries. $id is a title id (tt...) except name_trivia, which takes a name id (nm...).
more_like_this_gql = 'query($id:ID!){title(id:$id){moreLikeThisTitles(first:24){edges{node{id}}}}}'
reviews_gql = ('query($id:ID!){title(id:$id){reviews(first:25,sort:{by:TOTAL_VOTES,order:DESC}){edges{node{'
				'authorRating submissionDate spoiler summary{originalText} text{originalText{plainText}}}}}}}')
trivia_gql = 'query($id:ID!){title(id:$id){trivia(first:250){edges{node{text{plainText}}}}}}'
goofs_gql = 'query($id:ID!){title(id:$id){goofs(first:250){edges{node{text{plainText}}}}}}'
parents_gql = ('query($id:ID!){title(id:$id){parentsGuide{categories{category{text} severity{text} '
				'guideItems(first:250){edges{node{text{plainText}}}}}}}}')
name_trivia_gql = 'query($id:ID!){name(id:$id){trivia(first:250){edges{node{text{plainText}}}}}}'

def _gql(query, imdb_id):
	# POST a GraphQL query for one title/name id; return the parsed 'data' dict (or {} on any failure).
	try:
		payload = json.dumps({'query': query, 'variables': {'id': imdb_id}}).encode('utf-8')
		body = requests.post(gql_url, data=payload, headers=gql_headers, timeout=timeout).json()
		if body.get('errors'): logger('FenLight IMDB', 'GQL errors for %s: %s' % (imdb_id, body['errors'][:1]))
		return body.get('data') or {}
	except Exception as e:
		logger('FenLight IMDB', 'GQL request failed for %s: %s' % (imdb_id, e))
		return {}

def _nodes(data, kind, field):
	# walk data[kind][field]['edges'] -> [node, ...]; kind is 'title' or 'name'
	try: return [edge['node'] for edge in data[kind][field]['edges']]
	except Exception: return []

def _cached(fetch_string, params, expiration):
	# cache the result; drop empties so a transient IMDB failure isn't stuck for the whole TTL
	result = cache_object(get_imdb, fetch_string, params, False, expiration)[0]
	if not result: main_cache.delete(fetch_string)
	return result

def imdb_more_like_this(imdb_id):
	return _cached('imdb_more_like_this_%s' % imdb_id, {'action': 'imdb_more_like_this', 'id': imdb_id}, 168)

def imdb_reviews(imdb_id):
	return _cached('imdb_reviews_%s' % imdb_id, {'action': 'imdb_reviews', 'id': imdb_id}, 168)

def imdb_trivia(imdb_id):
	return _cached('imdb_trivia_%s' % imdb_id, {'action': 'imdb_trivia', 'id': imdb_id}, 168)

def imdb_blunders(imdb_id):
	return _cached('imdb_blunders_%s' % imdb_id, {'action': 'imdb_blunders', 'id': imdb_id}, 168)

def imdb_parentsguide(imdb_id):
	return _cached('imdb_parentsguide_%s' % imdb_id, {'action': 'imdb_parentsguide', 'id': imdb_id}, 168)

def imdb_people_trivia(imdb_id):
	return _cached('imdb_people_trivia_%s' % imdb_id, {'action': 'imdb_people_trivia', 'id': imdb_id}, 168)

def imdb_people_id(actor_name):
	name = actor_name.lower()
	string = 'imdb_people_id_%s' % name
	url, url_backup = people_search_url % (name[0], name.replace(' ', '%20')), people_search_url_backup % name
	params = {'url': url, 'action': 'imdb_people_id', 'name': name, 'url_backup': url_backup}
	return cache_object(get_imdb, string, params, False, 8736)[0]

def get_imdb(params):
	imdb_list = []
	next_page = None
	action = params.get('action')
	if action == 'imdb_people_id':
		try:
			name = params['name']
			result = requests.get(params['url'], timeout=timeout)
			results = json.loads(re.sub(r'imdb\$(.+?)\(', '', result.text)[:-1])['d']
			imdb_list = [i['id'] for i in results if i['id'].startswith('nm') and i['l'].lower() == name][0]
		except: imdb_list = []
		if not imdb_list:
			try:
				result = requests.get(params['url_backup'], timeout=timeout, headers=gql_headers)
				result = remove_accents(result.text).replace('\n', ' ')
				result = parseDOM(result, 'div', attrs={'class': 'lister-item-image'})[0]
				imdb_list = re.search(r'href="/name/(.+?)"', result, re.DOTALL).group(1)
			except: pass
		return (imdb_list, next_page)

	imdb_id = params.get('id')
	if action == 'imdb_more_like_this':
		seen = set()
		for node in _nodes(_gql(more_like_this_gql, imdb_id), 'title', 'moreLikeThisTitles'):
			tid = node.get('id')
			if tid and tid not in seen:
				seen.add(tid)
				imdb_list.append(tid)
	elif action == 'imdb_reviews':
		count = 1
		for node in _nodes(_gql(reviews_gql, imdb_id), 'title', 'reviews'):
			try:
				content = ((node.get('text') or {}).get('originalText') or {}).get('plainText')
				if not content: continue
				rating = node.get('authorRating') or '-'
				date = node.get('submissionDate') or '-----'
				title = (node.get('summary') or {}).get('originalText') or '-----'
				review = '[B]%02d. [I]%s/10 - %s - %s[/I][/B][CR][CR]%s' % (count, rating, date, title, content)
				if node.get('spoiler'): review = '[B][COLOR red][%s][/COLOR][CR][/B]' % 'CONTAINS SPOILERS' + review
				count += 1
				imdb_list.append(review)
			except: pass
	elif action in ('imdb_trivia', 'imdb_blunders', 'imdb_people_trivia'):
		if action == 'imdb_trivia': query, kind, field, label = trivia_gql, 'title', 'trivia', 'TRIVIA'
		elif action == 'imdb_blunders': query, kind, field, label = goofs_gql, 'title', 'goofs', 'BLUNDERS'
		else: query, kind, field, label = name_trivia_gql, 'name', 'trivia', 'TRIVIA'
		count = 1
		for node in _nodes(_gql(query, imdb_id), kind, field):
			try:
				content = (node.get('text') or {}).get('plainText')
				if not content: continue
				imdb_list.append('[B]%s %02d.[/B][CR][CR]%s' % (label, count, content))
				count += 1
			except: pass
	elif action == 'imdb_parentsguide':
		try: categories = _gql(parents_gql, imdb_id)['title']['parentsGuide']['categories']
		except Exception: categories = []
		for cat in categories:
			try:
				title = (cat.get('category') or {}).get('text')
				if not title: continue
				ranking = (cat.get('severity') or {}).get('text') or 'none'
				listings = [n['node']['text']['plainText'] for n in ((cat.get('guideItems') or {}).get('edges') or [])
							if (n.get('node') or {}).get('text', {}).get('plainText')]
				if not listings and ranking.lower() == 'none': continue
				item_dict = {'title': title, 'ranking': ranking, 'total_count': len(listings),
							'content': '\n\n'.join('%02d. %s' % (n, t) for n, t in enumerate(listings, 1)) if listings else ''}
				imdb_list.append(item_dict)
			except: pass
	try: _count = len(imdb_list)
	except: _count = imdb_list
	logger('FenLight IMDB', 'get_imdb action=%s count=%s' % (action, _count))
	return (imdb_list, next_page)

def clear_imdb_cache(silent=False):
	from modules.kodi_utils import clear_property
	try:
		dbcon = connect_database('maincache_db')
		imdb_results = [str(i[0]) for i in dbcon.execute("SELECT id FROM maincache WHERE id LIKE ?", ('imdb_%',)).fetchall()]
		if not imdb_results: return True
		dbcon.execute("DELETE FROM maincache WHERE id LIKE ?", ('imdb_%',))
		for i in imdb_results: clear_property(i)
		return True
	except: return False

def refresh_imdb_meta_data(imdb_id):
	from modules.kodi_utils import clear_property
	try:
		imdb_results = []
		insert1, insert2 = '%%_%s' % imdb_id, '%%_%s_%%' % imdb_id
		dbcon = connect_database('maincache_db')
		for item in (insert1, insert2):
			imdb_results += [str(i[0]) for i in dbcon.execute("SELECT id FROM maincache WHERE id LIKE ?", (item,)).fetchall()]
		if not imdb_results: return True
		dbcon.execute("DELETE FROM maincache WHERE id LIKE ?", (insert1,))
		dbcon.execute("DELETE FROM maincache WHERE id LIKE ?", (insert2,))
		for i in imdb_results: clear_property(i)
	except: pass
