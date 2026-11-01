# -*- coding: utf-8 -*-
import sys
from modules.ipv6_fallback import install as ipv6_fallback
ipv6_fallback()
from modules.router import routing, sys_exit_check
# from modules.kodi_utils import logger

routing(sys)
if sys_exit_check(): sys.exit(1)

