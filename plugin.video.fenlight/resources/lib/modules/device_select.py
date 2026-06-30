# Device source selection: lets a companion app (e.g. FenLight+ Companion) list scraped
# sources via JSON-RPC Files.GetDirectory, choose one on the phone, then play it on Kodi.
import os
import sys
import time
import json
from modules import kodi_utils

PROTO = 1  # bump when the list_sources / play_source contract changes
KEEP_SECONDS = 900  # prune persisted result lists older than 15 minutes

def _store_dir():
    base = os.path.join(kodi_utils.addon_profile(), 'device_select')
    if not os.path.exists(base):
        try: os.makedirs(base)
        except Exception: pass
    return base

def _store_path(request_id):
    return os.path.join(_store_dir(), '%s.json' % request_id)

def _cleanup(base):
    now = time.time()
    try:
        for name in os.listdir(base):
            path = os.path.join(base, name)
            try:
                if now - os.path.getmtime(path) > KEEP_SECONDS: os.remove(path)
            except Exception: pass
    except Exception: pass

def _serialise_source(item):
    get = item.get
    scrape_provider = get('scrape_provider', '')
    if scrape_provider == 'external':
        provider = get('debrid', get('source', '')).replace('.me', '')
    elif scrape_provider == 'folders':
        provider = get('source', '')
    else:
        provider = scrape_provider or get('source', '')
    cache_provider = get('cache_provider', '')
    info = (get('extraInfo', '') or '').rstrip('| ') or 'N/A'
    meta = {
        'provider': provider.upper(),
        'quality': (get('quality', 'SD') or 'SD').upper(),
        'size': get('size_label', 'N/A'),
        'info': info,
        'name': get('display_name', '') or get('name', ''),
        'debrid': cache_provider,
        'cached': 'Uncached' not in cache_provider,
    }
    if 'seeders' in item: meta['seeders'] = get('seeders', 0)
    if 'package' in item: meta['pack'] = get('package', '')
    return meta

def output_sources_directory(src, results):
    # Called from Sources.get_sources when device_list is set. src is the Sources instance.
    handle = int(sys.argv[1])
    base = _store_dir()
    _cleanup(base)
    request_id = src.params.get('request_id') or str(int(time.time() * 1000))
    # Show every source. Real Debrid no longer reports cached vs uncached, so we can't
    # reliably pre-filter; an uncached pick is added to the cloud at play time (see below).
    try:
        with open(_store_path(request_id), 'w') as f:
            json.dump(results, f)
    except Exception:
        pass

    _drop = ('mode', 'device_list', 'background', 'request_id', 'selected_index', 'meta', 'play_selected')
    carry = {k: v for k, v in src.params.items() if k not in _drop}
    items = []
    for index, item in enumerate(results):
        meta = _serialise_source(item)
        label = '%s | %s | %s' % (meta['provider'], meta['quality'], meta['size'])
        url = kodi_utils.build_url({
            **carry,
            'mode': 'app_play_source',
            'request_id': request_id,
            'selected_index': index,
            'meta': json.dumps(meta),
        })
        listitem = kodi_utils.make_listitem()
        listitem.setLabel(label)
        items.append((url, listitem, False))
    kodi_utils.add_items(handle, items)
    kodi_utils.set_content(handle, 'files')
    kodi_utils.end_directory(handle, cacheToDisc=False)

def play_selected_source(src):
    # Called from Sources.get_sources when play_selected is set. Loads the persisted ordered
    # list and plays the chosen source; play_file fails over through the rest on resolve failure.
    request_id = src.play_selected
    try: index = int(src.params.get('selected_index', 0))
    except Exception: index = 0
    results = []
    try:
        with open(_store_path(request_id)) as f:
            results = json.load(f)
    except Exception:
        pass
    if not results: return
    if index < 0 or index >= len(results): index = 0
    source = results[index]
    # Mirror FenLight's picker: an uncached pick is added to the debrid cloud, not played.
    if 'Uncached' in source.get('cache_provider', ''):
        from modules.debrid import manual_add_magnet_to_cloud
        return manual_add_magnet_to_cloud({'mode': 'manual_add_magnet_to_cloud',
                                           'provider': source.get('debrid'), 'magnet_url': source.get('url')})
    return src.play_file(results, source)

def capabilities():
    handle = int(sys.argv[1])
    listitem = kodi_utils.make_listitem()
    listitem.setLabel('FenLight+ device select')
    url = kodi_utils.build_url({'caps': '1', 'device_select': '1', 'proto': PROTO})
    kodi_utils.add_items(handle, [(url, listitem, False)])
    kodi_utils.set_content(handle, 'files')
    kodi_utils.end_directory(handle, cacheToDisc=False)
