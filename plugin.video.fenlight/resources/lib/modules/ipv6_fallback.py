# -*- coding: utf-8 -*-
import socket
import xbmcgui

ipv6_attempt_timeout = 2.0
failed_prop = 'fenlight.ipv6_failed'

def ipv6_failed():
	return xbmcgui.Window(10000).getProperty(failed_prop) == 'true'

def mark_ipv6_failed():
	xbmcgui.Window(10000).setProperty(failed_prop, 'true')

def explicit_timeout(timeout):
	return timeout is None or isinstance(timeout, (int, float))

def connect(entry, timeout, source_address, socket_options):
	family, socktype, proto, _, sockaddr = entry
	sock = socket.socket(family, socktype, proto)
	try:
		for option in socket_options or (): sock.setsockopt(*option)
		if explicit_timeout(timeout): sock.settimeout(timeout)
		if source_address: sock.bind(source_address)
		sock.connect(sockaddr)
		return sock
	except BaseException:
		sock.close()
		raise

def connect_first(entries, timeout, source_address, socket_options):
	error = OSError('getaddrinfo returns an empty list')
	for entry in entries:
		try: return connect(entry, timeout, source_address, socket_options)
		except OSError as e: error = e
	raise error

def create_connection(address, timeout=socket._GLOBAL_DEFAULT_TIMEOUT, source_address=None, socket_options=None, **kwargs):
	host, port = address[0].strip('[]'), address[1]
	entries = socket.getaddrinfo(host, port, 0, socket.SOCK_STREAM)
	ipv6 = [i for i in entries if i[0] == socket.AF_INET6]
	ipv4 = [i for i in entries if i[0] == socket.AF_INET]
	if not (ipv6 and ipv4): return connect_first(entries, timeout, source_address, socket_options)
	if not ipv6_failed():
		attempt_timeout = min(timeout, ipv6_attempt_timeout) if timeout and explicit_timeout(timeout) else ipv6_attempt_timeout
		try:
			sock = connect_first(ipv6, attempt_timeout, source_address, socket_options)
			sock.settimeout(timeout if explicit_timeout(timeout) else socket.getdefaulttimeout())
			return sock
		except OSError: pass
		sock = connect_first(ipv4, timeout, source_address, socket_options)
		mark_ipv6_failed()
		return sock
	return connect_first(ipv4, timeout, source_address, socket_options)

def install():
	if socket.create_connection is create_connection: return
	socket.create_connection = create_connection
	try:
		from urllib3.util import connection
		connection.create_connection = create_connection
	except ImportError: pass
