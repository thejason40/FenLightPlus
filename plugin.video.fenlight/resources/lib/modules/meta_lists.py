# -*- coding: utf-8 -*-
from datetime import datetime

def get_years(start_year: int):
    current_year = datetime.now().year
    
    # Generate years in descending order from current_year down to start_year (inclusive)
    years = [
        {'name': str(year), 'id': year} for year in range(current_year, start_year - 1, -1)
    ]
    
    return years

years_movies = get_years(1900)
years_tvshows = get_years(1944)
years_anime = get_years(1961)

def get_decades(start_decade: int):
    current_year = datetime.now().year
    # Determine the current decade. For example, if current_year is 2031, then:
    # 2031 // 10 gives 203, multiplied by 10 gives 2030.
    current_decade = (current_year // 10) * 10

    # Generate a list of decades in descending order from current_decade down to start_decade.
    decades = [
        {'name': f"{decade}s", 'id': decade}
        for decade in range(current_decade, start_decade - 1, -10)
    ]
    return decades


decades_movies  = get_decades(1900)
decades_tvshows = get_decades(1940)
decades_anime   = get_decades(1960)

oscar_winners = (
{'results': [{'id': 872585}, {'id': 545611}, {'id': 776503}, {'id': 581734}, {'id': 496243}, {'id': 490132}, {'id': 399055}, {'id': 376867}, {'id': 314365},
{'id': 194662}, {'id': 76203}, {'id': 68734}, {'id': 74643}, {'id': 45269}, {'id': 12162}, {'id': 12405}, {'id': 6977}, {'id': 1422}, {'id': 1640}, {'id': 70}],
'total_pages': 5, 'page': 1},
{'results': [{'id': 122}, {'id': 1574}, {'id': 453}, {'id': 98}, {'id': 14}, {'id': 1934}, {'id': 597}, {'id': 409}, {'id': 197}, {'id': 13}, {'id': 424}, {'id': 33},
{'id': 274}, {'id': 581}, {'id': 403}, {'id': 380}, {'id': 746}, {'id': 792}, {'id': 606}, {'id': 279}], 'total_pages': 5, 'page': 2},
{'results': [{'id': 11050}, {'id': 783}, {'id': 9443}, {'id': 16619}, {'id': 12102}, {'id': 11778}, {'id': 703}, {'id': 1366}, {'id': 510}, {'id': 240}, {'id': 9277},
{'id': 238}, {'id': 1051}, {'id': 11202}, {'id': 3116}, {'id': 17917}, {'id': 10633}, {'id': 874}, {'id': 15121}, {'id': 11113}], 'total_pages': 5, 'page': 3},
{'results': [{'id': 5769}, {'id': 947}, {'id': 1725}, {'id': 284}, {'id': 665}, {'id': 17281}, {'id': 826}, {'id': 2897}, {'id': 15919}, {'id': 654}, {'id': 11426},
{'id': 27191}, {'id': 2769}, {'id': 705}, {'id': 25430}, {'id': 23383}, {'id': 33667}, {'id': 887}, {'id': 28580}, {'id': 17661}], 'total_pages': 5, 'page': 4},
{'results': [{'id': 27367}, {'id': 289}, {'id': 43266}, {'id': 223}, {'id': 770}, {'id': 34106}, {'id': 43278}, {'id': 43277}, {'id': 12311}, {'id': 3078}, {'id': 56164},
{'id': 33680}, {'id': 42861}, {'id': 143}, {'id': 65203}, {'id': 28966}, {'id': 631}], 'total_pages': 5, 'page': 5}
	)

movie_certifications = [
{'name': 'G', 'id': 'G'}, {'name': 'PG', 'id': 'PG'}, {'name': 'PG-13', 'id': 'PG-13'},
{'name': 'R', 'id': 'R'}, {'name': 'NC-17', 'id': 'NC-17'}, {'name': 'NR', 'id': 'NR'}
	]

tvshow_certifications = [
{'name': 'TV-Y', 'id': 'tv-y'}, {'name': 'TV-Y7', 'id': 'tv-y7'}, {'name': 'TV-G', 'id': 'tv-g'},
{'name': 'TV-PG', 'id': 'tv-pg'}, {'name': 'TV-14', 'id': 'tv-14'}, {'name': 'TV-MA', 'id': 'tv-ma'}
	]

languages = [
{'name': 'Arabic', 'id': 'ar'}, {'name': 'Bosnian', 'id': 'bs'}, {'name': 'Bulgarian', 'id': 'bg'}, {'name': 'Chinese', 'id': 'zh'}, {'name': 'Croatian', 'id': 'hr'},
{'name': 'Dutch', 'id': 'nl'}, {'name': 'English', 'id': 'en'}, {'name': 'Finnish', 'id': 'fi'}, {'name': 'French', 'id': 'fr'}, {'name': 'German', 'id': 'de'},
{'name': 'Greek', 'id': 'el'}, {'name': 'Hebrew', 'id': 'he'}, {'name': 'Hindi', 'id': 'hi'}, {'name': 'Hungarian', 'id': 'hu'}, {'name': 'Icelandic', 'id': 'is'},
{'name': 'Italian', 'id': 'it'}, {'name': 'Japanese', 'id': 'ja'}, {'name': 'Korean', 'id': 'ko'}, {'name': 'Macedonian', 'id': 'mk'}, {'name': 'Norwegian', 'id': 'no'},
{'name': 'Persian', 'id': 'fa'}, {'name': 'Polish', 'id': 'pl'}, {'name': 'Portuguese', 'id': 'pt'}, {'name': 'Punjabi', 'id': 'pa'}, {'name': 'Romanian', 'id': 'ro'},
{'name': 'Russian', 'id': 'ru'}, {'name': 'Serbian', 'id': 'sr'}, {'name': 'Slovenian', 'id': 'sl'}, {'name': 'Spanish', 'id': 'es'}, {'name': 'Swedish', 'id': 'sv'},
{'name': 'Turkish', 'id': 'tr'}, {'name': 'Ukrainian', 'id': 'uk'}]

language_choices =  {
'None': 'None',              'Afrikaans': 'afr',            'Albanian': 'alb',             'Arabic': 'ara',
'Armenian': 'arm',           'Basque': 'baq',               'Bengali': 'ben',              'Bosnian': 'bos',
'Breton': 'bre',             'Bulgarian': 'bul',            'Burmese': 'bur',              'Catalan': 'cat',
'Chinese': 'chi',            'Croatian': 'hrv',             'Czech': 'cze',                'Danish': 'dan',
'Dutch': 'dut',              'English': 'eng',              'Esperanto': 'epo',            'Estonian': 'est',
'Finnish': 'fin',            'French': 'fre',               'Galician': 'glg',             'Georgian': 'geo',
'German': 'ger',             'Greek': 'ell',                'Hebrew': 'heb',               'Hindi': 'hin',
'Hungarian': 'hun',          'Icelandic': 'ice',            'Indonesian': 'ind',           'Italian': 'ita',
'Japanese': 'jpn',           'Kazakh': 'kaz',               'Khmer': 'khm',                'Korean': 'kor',
'Latvian': 'lav',            'Lithuanian': 'lit',           'Luxembourgish': 'ltz',        'Macedonian': 'mac',
'Malay': 'may',              'Malayalam': 'mal',            'Manipuri': 'mni',             'Mongolian': 'mon',
'Montenegrin': 'mne',        'Norwegian': 'nor',            'Occitan': 'oci',              'Persian': 'per',
'Polish': 'pol',             'Portuguese': 'por',           'Portuguese(Brazil)': 'pob',   'Romanian': 'rum',
'Russian': 'rus',            'Serbian': 'scc',              'Sinhalese': 'sin',            'Slovak': 'slo',
'Slovenian': 'slv',          'Spanish': 'spa',              'Swahili': 'swa',              'Swedish': 'swe',
'Syriac': 'syr',             'Tagalog': 'tgl',              'Tamil': 'tam',                'Telugu': 'tel',
'Thai': 'tha',               'Turkish': 'tur',              'Ukrainian': 'ukr',            'Urdu': 'urd',
'Vietnamese': 'vie'
	}

regions = [
{'id': 'AF', 'name': 'Afghanistan'},        {'id': 'AL', 'name': 'Albania'},          {'id': 'DZ', 'name': 'Algeria'},
{'id': 'AQ', 'name': 'Antarctica'},         {'id': 'AR', 'name': 'Argentina'},        {'id': 'AM', 'name': 'Armenia'},
{'id': 'AU', 'name': 'Australia'},          {'id': 'AT', 'name': 'Austria'},          {'id': 'BD', 'name': 'Bangladesh'},
{'id': 'BY', 'name': 'Belarus'},            {'id': 'BE', 'name': 'Belgium'},          {'id': 'BR', 'name': 'Brazil'},
{'id': 'BG', 'name': 'Bulgaria'},           {'id': 'KH', 'name': 'Cambodia'},         {'id': 'CA', 'name': 'Canada'},
{'id': 'CL', 'name': 'Chile'},              {'id': 'CN', 'name': 'China'},            {'id': 'HR', 'name': 'Croatia'},
{'id': 'CZ', 'name': 'Czech Republic'},     {'id': 'DK', 'name': 'Denmark'},          {'id': 'DE', 'name': 'Egypt'},
{'id': 'FR', 'name': 'Finland'},            {'id': 'FI', 'name': 'France'},           {'id': 'EG', 'name': 'Germany'},
{'id': 'GR', 'name': 'Greece'},             {'id': 'HK', 'name': 'Hong Kong'},        {'id': 'HU', 'name': 'Hungary'},
{'id': 'IS', 'name': 'Iceland'},            {'id': 'IN', 'name': 'India'},            {'id': 'ID', 'name': 'Indonesia'},
{'id': 'IR', 'name': 'Iran'},               {'id': 'IQ', 'name': 'Iraq'},             {'id': 'IE', 'name': 'Ireland'},
{'id': 'IL', 'name': 'Israel'},             {'id': 'IT', 'name': 'Italy'},            {'id': 'JP', 'name': 'Japan'},
{'id': 'MY', 'name': 'Malaysia'},           {'id': 'NP', 'name': 'Nepal'},            {'id': 'NL', 'name': 'Netherlands'},
{'id': 'NZ', 'name': 'New Zealand'},        {'id': 'NO', 'name': 'Norway'},           {'id': 'PK', 'name': 'Pakistan'},
{'id': 'PY', 'name': 'Paraguay'},           {'id': 'PE', 'name': 'Peru'},             {'id': 'PH', 'name': 'Philippines'},
{'id': 'PL', 'name': 'Poland'},             {'id': 'PT', 'name': 'Portugal'},         {'id': 'PR', 'name': 'Puerto Rico'},
{'id': 'RO', 'name': 'Romania'},            {'id': 'RU', 'name': 'Russia'},           {'id': 'SA', 'name': 'Saudi Arabia'},
{'id': 'RS', 'name': 'Serbia'},             {'id': 'SG', 'name': 'Singapore'},        {'id': 'SK', 'name': 'Slovakia'},
{'id': 'SI', 'name': 'Slovenia'},           {'id': 'ZA', 'name': 'South Africa'},     {'id': 'ES', 'name': 'Spain'},
{'id': 'LK', 'name': 'Sri Lanka'},          {'id': 'SE', 'name': 'Sweden'},           {'id': 'CH', 'name': 'Switzerland'},
{'id': 'TH', 'name': 'Thailand'},           {'id': 'TR', 'name': 'Turkey'},           {'id': 'UA', 'name': 'Ukraine'},
{'id': 'AE', 'name': 'UAE'},                {'id': 'GB', 'name': 'UK'},               {'id': 'US', 'name': 'USA'},
{'id': 'UY', 'name': 'Uruguay'},            {'id': 'VE', 'name': 'Venezuela'},        {'id': 'VN', 'name': 'Viet Nam'},
{'id': 'YE', 'name': 'Yemen'},              {'id': 'ZW', 'name': 'Zimbabwe'}
	]

movie_genres = [
{'name': 'Action', 'id': '28', 'icon': 'genre_action'}, {'name': 'Adventure', 'id': '12', 'icon': 'genre_adventure'}, {'name': 'Animation', 'id': '16', 'icon': 'genre_animation'},
{'name': 'Comedy', 'id': '35', 'icon': 'genre_comedy'}, {'name': 'Crime', 'id': '80', 'icon': 'genre_crime'}, {'name': 'Documentary', 'id': '99', 'icon': 'genre_documentary'},
{'name': 'Drama', 'id': '18', 'icon': 'genre_drama'}, {'name': 'Family', 'id': '10751', 'icon': 'genre_family'}, {'name': 'Fantasy', 'id': '14', 'icon': 'genre_fantasy'},
{'name': 'History', 'id': '36', 'icon': 'genre_history'}, {'name': 'Horror', 'id': '27', 'icon': 'genre_horror'}, {'name': 'Music', 'id': '10402', 'icon': 'genre_music'},
{'name': 'Mystery', 'id': '9648', 'icon': 'genre_mystery'}, {'name': 'Romance', 'id': '10749', 'icon': 'genre_romance'},
{'name': 'Science Fiction', 'id': '878', 'icon': 'genre_scifi'}, {'name': 'TV Movie', 'id': '10770', 'icon': 'genre_soap'}, {'name': 'Thriller', 'id': '53', 'icon': 'genre_thriller'},
{'name': 'War', 'id': '10752', 'icon': 'genre_war'}, {'name': 'Western', 'id': '37', 'icon': 'genre_western'}
	]

tvshow_genres = [
{'name': 'Action & Adventure', 'id': '10759', 'icon': 'genre_action'}, {'name': 'Animation', 'id': '16', 'icon': 'genre_animation'},
{'name': 'Comedy', 'id': '35', 'icon': 'genre_comedy'}, {'name': 'Crime', 'id': '80', 'icon': 'genre_crime'}, {'name': 'Documentary', 'id': '99', 'icon': 'genre_documentary'},
{'name': 'Drama', 'id': '18', 'icon': 'genre_drama'}, {'name': 'Family', 'id': '10751', 'icon': 'genre_family'}, {'name': 'Kids', 'id': '10762', 'icon': 'genre_kids'},
{'name': 'Mystery', 'id': '9648', 'icon': 'genre_mystery'}, {'name': 'News', 'id': '10763', 'icon': 'genre_news'}, {'name': 'Reality', 'id': '10764', 'icon': 'genre_reality'},
{'name': 'Sci-Fi & Fantasy', 'id': '10765', 'icon': 'genre_scifi'}, {'name': 'Soap', 'id': '10766', 'icon': 'genre_soap'}, {'name': 'Talk', 'id': '10767', 'icon': 'genre_talk'},
{'name': 'War & Politics', 'id': '10768', 'icon': 'genre_war'}, {'name': 'Western', 'id': '37', 'icon': 'genre_western'}
	]

anime_genres = [
{'name': 'Action & Adventure', 'id': '10759', 'icon': 'genre_action'}, {'name': 'Comedy', 'id': '35', 'icon': 'genre_comedy'}, {'name': 'Crime', 'id': '80', 'icon': 'genre_crime'},
{'name': 'Drama', 'id': '18', 'icon': 'genre_drama'}, {'name': 'Family', 'id': '10751', 'icon': 'genre_family'}, {'name': 'Kids', 'id': '10762', 'icon': 'genre_kids'},
{'name': 'Mystery', 'id': '9648', 'icon': 'genre_mystery'}, {'name': 'Sci-Fi & Fantasy', 'id': '10765', 'icon': 'genre_scifi'},
{'name': 'War & Politics', 'id': '10768', 'icon': 'genre_war'}, {'name': 'Western', 'id': '37', 'icon': 'genre_western'}
	]

networks = [
{'id': 54, 'name': 'Disney Channel', 'icon': 'network_disney'},                                  {'id': 44, 'name': 'Disney XD', 'icon': 'network_disneyxd'},
{'id': 2, 'name': 'ABC', 'icon': 'network_abc'},                                                 {'id': 493, 'name': 'BBC America', 'icon': 'network_bbcamerica'},
{'id': 6, 'name': 'NBC', 'icon': 'network_nbc'},                                                 {'id': 13, 'name': 'Nickelodeon', 'icon': 'network_nickelodeon'},
{'id': 14, 'name': 'PBS', 'icon': 'network_pbs'},                                                {'id': 16, 'name': 'CBS', 'icon': 'network_cbs'},
{'id': 19, 'name': 'FOX', 'icon': 'network_fox'},                                                {'id': 21, 'name': 'The WB', 'icon': 'network_thewb'},
{'id': 24, 'name': 'BET', 'icon': 'network_bet'},                                                {'id': 30, 'name': 'USA Network', 'icon': 'network_usanetwork'},
{'id': 23, 'name': 'CBC', 'icon': 'network_cbc'},                                                {'id': 88, 'name': 'FX', 'icon': 'network_fx'},
{'id': 33, 'name': 'MTV', 'icon': 'network_mtv'},                                                {'id': 34, 'name': 'Lifetime', 'icon': 'network_lifetime'},
{'id': 35, 'name': 'Nick Junior', 'icon': 'network_nickjr'},                                     {'id': 41, 'name': 'TNT', 'icon': 'network_tnt'},
{'id': 43, 'name': 'National Geographic', 'icon': 'network_natgeo'},                             {'id': 47, 'name': 'Comedy Central', 'icon': 'network_comedycentral'},
{'id': 49, 'name': 'HBO', 'icon': 'network_hbo'},                                                {'id': 55, 'name': 'Spike', 'icon': 'network_spike'},
{'id': 67, 'name': 'Showtime', 'icon': 'network_showtime'},                                      {'id': 56, 'name': 'Cartoon Network', 'icon': 'network_cartoonnetwork'},
{'id': 65, 'name': 'History Channel', 'icon': 'network_history'},                                {'id': 84, 'name': 'TLC', 'icon': 'network_tlc'},
{'id': 68, 'name': 'TBS', 'icon': 'network_tbs'},                                                {'id': 71, 'name': 'The CW', 'icon': 'network_thecw'},
{'id': 74, 'name': 'Bravo', 'icon': 'network_bravo'},                                            {'id': 76, 'name': 'E!', 'icon': 'network_e'},
{'id': 77, 'name': 'Syfy', 'icon': 'network_syfy'},                                              {'id': 80, 'name': 'Adult Swim', 'icon': 'network_adultswim'},
{'id': 91, 'name': 'Animal Planet', 'icon': 'network_animalplanet'},                             {'id': 110, 'name': 'CTV', 'icon': 'network_ctv'},
{'id': 129, 'name': 'A&E', 'icon': 'network_ane'},                                               {'id': 158, 'name': 'VH1', 'icon': 'network_vh1'},
{'id': 174, 'name': 'AMC', 'icon': 'network_amc'},                                               {'id': 928, 'name': 'Crackle', 'icon': 'network_crackle'},
{'id': 202, 'name': 'WGN America', 'icon': 'network_wgnamerica'},                                {'id': 209, 'name': 'Travel Channel', 'icon': 'network_travel'},
{'id': 213, 'name': 'Netflix', 'icon': 'network_netflix'},                                       {'id': 251, 'name': 'Audience', 'icon': 'network_audience'},
{'id': 270, 'name': 'SundanceTV', 'icon': 'network_sundancetv'},                                 {'id': 318, 'name': 'Starz', 'icon': 'network_starz'},
{'id': 359, 'name': 'Cinemax', 'icon': 'network_cinemax'},                                       {'id': 364, 'name': 'truTV', 'icon': 'network_trutv'},
{'id': 384, 'name': 'Hallmark Channel', 'icon': 'network_hallmark'},                             {'id': 397, 'name': 'TV Land', 'icon': 'network_tvland'},
{'id': 1024, 'name': 'Amazon', 'icon': 'network_amazon'},                                        {'id': 1267, 'name': 'Freeform', 'icon': 'network_freeform'},
{'id': 4, 'name': 'BBC 1', 'icon': 'network_bbc1'},                                              {'id': 332, 'name': 'BBC 2', 'icon': 'network_bbc2'},
{'id': 3, 'name': 'BBC 3', 'icon': 'network_bbc3'},                                              {'id': 100, 'name': 'BBC 4', 'icon': 'network_bbc4'},
{'id': 214, 'name': 'Sky 1', 'icon': 'network_sky1'},                                            {'id': 9, 'name': 'ITV', 'icon': 'network_itv'},
{'id': 26, 'name': 'Channel 4', 'icon': 'network_channel4'},                                     {'id': 99, 'name': 'Channel 5', 'icon': 'network_channel5'},
{'id': 136, 'name': 'E4', 'icon': 'network_e4'},                                                 {'id': 210, 'name': 'HGTV', 'icon': 'network_hgtv'},
{'id': 453, 'name': 'Hulu', 'icon': 'network_hulu'},                                             {'id': 1436, 'name': 'YouTube Red', 'icon': 'network_youtubered'},
{'id': 64, 'name': 'Discovery Channel', 'icon': 'network_discovery'},                            {'id': 2739, 'name': 'Disney+', 'icon': 'network_disneyplus'},
{'id': 2552, 'name': 'Apple TV +', 'icon': 'network_appletvplus'},                               {'id': 2697, 'name': 'Acorn TV', 'icon': 'network_acorntv'},
{'id': 1709, 'name': 'CBS All Access', 'icon': 'network_cbsallaccess'},                          {'id': 3186, 'name': 'HBO Max', 'icon': 'network_hbomax'},
{'id': 2243, 'name': 'DC Universe', 'icon': 'network_dcuniverse'},                               {'id': 2076, 'name': 'Paramount Network', 'icon': 'network_paramount'},
{'id': 4330, 'name': 'Paramount+', 'icon': 'network_paramountplus'},                             {'id': 3353, 'name': 'Peacock', 'icon': 'network_peacock'},
{'id': 4353, 'name': 'Discovery+', 'icon': 'network_discoveryplus'},                             {'id': 132, 'name': 'Oxygen', 'icon': 'network_oxygen'},
{'id': 244, 'name': 'Discovery ID', 'icon': 'network_discoveryid'}
	]

watch_providers_movies = [
{'name': 'Netflix', 'id': 8, 'icon': 't2yyOv40HZeVlLjYsCsPHnWLk4W.jpg'},                     {'name': 'Amazon Prime Video', 'id': 9, 'icon': 'emthp39XA2YScoYL1p0sdbAH2WA.jpg'},
{'name': 'Disney Plus', 'id': 337, 'icon': '7rwgEs15tFwyR9NPQ5vpzxTj19Q.jpg'},               {'name': 'Google Play Movies', 'id': 3, 'icon': 'tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg'},
{'name': 'Sun Nxt', 'id': 309, 'icon': 'uW4dPCcbXaaFTyfL5HwhuDt5akK.jpg'},                   {'name': 'Apple TV', 'id': 2, 'icon': 'peURlLlr8jggOwK53fJ5wdQl05y.jpg'},
{'name': 'MUBI', 'id': 11, 'icon': 'bVR4Z1LCHY7gidXAJF5pMa4QrDS.jpg'},                       {'name': 'Apple TV Plus', 'id': 350, 'icon': '6uhKBfmtzFqOcLousHwZuzcrScK.jpg'},
{'name': 'fuboTV', 'id': 257, 'icon': 'jPXksae158ukMLFhhlNvzsvaEyt.jpg'},                    {'name': 'Classix', 'id': 445, 'icon': 'iaMw6nOyxUzXSacrLQ0Au6CfZkc.jpg'},
{'name': 'Hulu', 'id': 15, 'icon': 'zxrVdFjIjLqkfnwyghnfywTn3Lh.jpg'},                       {'name': 'Curiosity Stream', 'id': 190, 'icon': '67Ee4E6qOkQGHeUTArdJ1qRxzR2.jpg'},
{'name': 'Paramount Plus', 'id': 531, 'icon': 'xbhHHa1YgtpwhC8lb1NQ3ACVcLd.jpg'},            {'name': 'GuideDoc', 'id': 100, 'icon': 'iX0pvJ2GFATbVIH5IHMwG0ffIdV.jpg'},
{'name': 'Public Domain Movies', 'id': 638, 'icon': 'liEIj6CkvojVDiMWeexGvflSPZT.jpg'},      {'name': 'HBO Max', 'id': 384, 'icon': 'Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg'},
{'name': 'Netflix Kids', 'id': 175, 'icon': 'j2OLGxyy0gKbPVI0DYFI2hJxP6y.jpg'},              {'name': 'Eventive', 'id': 677, 'icon': 'fadQYOyKL0tqfyj012nYJxm3N2I.jpg'},
{'name': 'Spamflix', 'id': 521, 'icon': 'xN97FFkFAdY1JvHhS4zyPD4URgD.jpg'},                  {'name': 'AMC+', 'id': 526, 'icon': 'xlonQMSmhtA2HHwK3JKF9ghx7M8.jpg'},
{'name': 'Cultpix', 'id': 692, 'icon': '59azlQKUgFdYq6QI5QEAxIeecyL.jpg'},                   {'name': 'DOCSVILLE', 'id': 475, 'icon': 'bvcdVO7SDHKEa6D40g1jntXKNj.jpg'},
{'name': 'Peacock', 'id': 386, 'icon': '8VCV78prwd9QzZnEm0ReO6bERDa.jpg'},                   {'name': 'VIX ', 'id': 457, 'icon': '58aUMVWJRolhWpi4aJCkGHwfKdg.jpg'},
{'name': 'FilmBox+', 'id': 701, 'icon': '4FqTBYsUSZgS9z9UGKgxSDBbtc8.jpg'},                  {'name': 'Peacock Premium', 'id': 387, 'icon': 'xTHltMrZPAJFLQ6qyCBjAnXSmZt.jpg'},
{'name': 'aha', 'id': 532, 'icon': 'm3NWxxR23l1w1e156fyTuw931gx.jpg'},                       {'name': 'Amazon Video', 'id': 10, 'icon': '5NyLm42TmCqCMOZFvH4fcoSNKEW.jpg'},
{'name': 'Kocowa', 'id': 464, 'icon': 'xfAAOAERZCnPB5jW5lhboAcXk8L.jpg'},                    {'name': 'WOW Presents Plus', 'id': 546, 'icon': 'mgD0T960hnYU4gBxbPPBrcDfgWg.jpg'},
{'name': 'Takflix', 'id': 1771, 'icon': 'cnIHBy3uLWhHRR7VeWQhK3ZsYP0.jpg'},                  {'name': 'Crunchyroll', 'id': 283, 'icon': '8Gt1iClBlzTeQs8WQm8UrCoIxnQ.jpg'},
{'name': 'YouTube', 'id': 192, 'icon': 'oIkQkEkwfmcG7IGpRR1NB8frZZM.jpg'},                   {'name': 'Magellan TV', 'id': 551, 'icon': 'gekkP93StjYdiMAInViVmrnldNY.jpg'},
{'name': 'BroadwayHD', 'id': 554, 'icon': 'xLu1rkZNOKuNnRNr70wySosfTBf.jpg'},                {'name': 'KoreaOnDemand', 'id': 575, 'icon': 'uHv6Y4YSsr4cj7q4cBbAg7WXKEI.jpg'},
{'name': 'Dekkoo', 'id': 444, 'icon': 'u2H29LCxRzjZVUoZUQAHKm5P8Zc.jpg'},                    {'name': 'Starz Apple TV', 'id': 1855, 'icon': 'hB24bAA8Y2ei6pbEGuCNdKUOjxI.jpg'},
{'name': 'Filmzie', 'id': 559, 'icon': 'olmH7t5tEng8Yuq33KmvpvaaVIg.jpg'},                   {'name': 'Showtime Apple TV', 'id': 675, 'icon': 'xVN3LKkOtCrlFT9mavhkx8SzMwV.jpg'},
{'name': 'True Story', 'id': 567, 'icon': 'osREemsc9uUB2J8VTkQeAVk2fu9.jpg'},                {'name': 'AMC Plus Apple TV ', 'id': 1854, 'icon': 'yFgm7vxwKZ4jfXIlPizlgoba2yi.jpg'},
{'name': 'DocAlliance Films', 'id': 569, 'icon': 'aQ1ritN00jXc7RAFfUoQKGAAfp7.jpg'},         {'name': 'Britbox Apple TV ', 'id': 1852, 'icon': 'cN85Wjk0FIFr3z6rbiimz10uWVo.jpg'},
{'name': 'Hoichoi', 'id': 315, 'icon': 'd4vHcXY9rwnr763wQns2XJThclt.jpg'},                   {'name': 'BritBox', 'id': 151, 'icon': 'aGIS8maihUm60A3moKYD9gfYHYT.jpg'},
{'name': 'Pluto TV', 'id': 300, 'icon': 't6N57S17sdXRXmZDAkaGP0NHNG0.jpg'},                  {'name': 'Starz', 'id': 43, 'icon': 'eWp5LdR4p4uKL0wACBBXapDV2lB.jpg'},
{'name': 'Rakuten Viki', 'id': 344, 'icon': 'qjtOUIUnk4kRpcZmaddjqDHM0dR.jpg'},              {'name': 'Discovery Plus Amazon', 'id': 584, 'icon': 'a2OcajC4bM5ItniQdjyOV7tgthW.jpg'},
{'name': 'iQIYI', 'id': 581, 'icon': '8MXYXzZGoPAEQU13GWk1GVvKNUS.jpg'},                     {'name': 'Showtime Amazon', 'id': 203, 'icon': 'zoL69abPHiVC1Qzd4kM6hwLSo0j.jpg'},
{'name': 'AMC+ Amazon', 'id': 528, 'icon': '9edKQczyuMmQM1yS520hgmJbcaC.jpg'},               {'name': 'Funimation Now', 'id': 269, 'icon': 'fWq61Fy4onav0wZJTA3c2fs0G66.jpg'},
{'name': 'The Roku Channel', 'id': 207, 'icon': 'z0h7mBHwm5KfMB2MKeoQDD2ngEZ.jpg'},          {'name': 'Showtime Roku Premium', 'id': 632, 'icon': 'qMf2zirM2w0sO0mdAIIoP5XnQn8.jpg'},
{'name': 'Runtime', 'id': 1875, 'icon': 'nvCfpn94VKJN4ZpkDgoupJWlXqq.jpg'},                  {'name': 'AMC+ Roku Premium', 'id': 635, 'icon': 'ni2NgPmIqqJRXeiA8Zdj4UhBZnU.jpg'},
{'name': 'YouTube Premium', 'id': 188, 'icon': '6IPjvnYl6WWkIwN158qBFXCr2Ne.jpg'},           {'name': 'YouTube Free', 'id': 235, 'icon': '4SCmZgf7AeJLKKRPcbf5VFkGpBj.jpg'},
{'name': 'Hoopla', 'id': 212, 'icon': 'aJ0b9BLU1Cvv5hIz9fEhKKc1x1D.jpg'},                    {'name': 'The CW', 'id': 83, 'icon': '6Y6w3F5mYoRHCcNAG0ZD2AndLJ2.jpg'},
{'name': 'Vudu', 'id': 7, 'icon': '21dEscfO8n1tL35k4DANixhffsR.jpg'},                        {'name': 'Starz Roku Premium', 'id': 634, 'icon': '5OAb2w7D9C2VHa0k5PaoAYeFYFE.jpg'},
{'name': 'VUDU Free', 'id': 332, 'icon': 'xzfVRl1CgJPYa9dOoyVI3TDSQo2.jpg'},                 {'name': 'Criterion Channel', 'id': 258, 'icon': '4TJTNWd2TT1kYj6ocUEsQc8WRgr.jpg'},
{'name': 'Showtime', 'id': 37, 'icon': '4kL33LoKd99YFIaSOoOPMQOSw1A.jpg'},                   {'name': 'PBS', 'id': 209, 'icon': 'bbxgdl6B5T75wJE713BiTCIBXyS.jpg'},
{'name': 'FXNow', 'id': 123, 'icon': 'twV9iQPYeaoBzwsfRFGMGoMIUg8.jpg'},                     {'name': 'Pantaflix', 'id': 177, 'icon': '2tAjxjo1n3H7fsXqMsxWFMeFUWp.jpg'},
{'name': 'Tubi TV', 'id': 73, 'icon': 'w2TDH9TRI7pltf5LjN3vXzs7QbN.jpg'},                    {'name': 'Kanopy', 'id': 191, 'icon': 'wbCleYwRFpUtWcNi7BLP3E1f6VI.jpg'},
{'name': 'Comedy Central', 'id': 243, 'icon': 'gmU9aPV3XUFusVs4kK1rcICUKqL.jpg'},            {'name': 'Microsoft Store', 'id': 68, 'icon': 'shq88b09gTBYC4hA7K7MUL8Q4zP.jpg'},
{'name': 'Redbox', 'id': 279, 'icon': 'gbyLHzl4eYP0oP9oJZ2oKbpkhND.jpg'},                    {'name': 'ABC', 'id': 148, 'icon': 'l9BRdAgQ3MkooOalsuu3yFQv2XP.jpg'},
{'name': 'Crackle', 'id': 12, 'icon': '7P2JHkfv4AmU2MgSPGaJ0z6nNLG.jpg'},                    {'name': 'DIRECTV', 'id': 358, 'icon': 'xL9SUR63qrEjFZAhtsipskeAMR7.jpg'},
{'name': 'Fandor', 'id': 25, 'icon': 'eAhAUvV2ouai3cGti5y70YOtrBN.jpg'},                     {'name': 'MGM Plus', 'id': 34, 'icon': '6A1gRIJqLfFHOoTvbTxDAbuU2nQ.jpg'},
{'name': 'Freeform', 'id': 211, 'icon': 'rgpmwMkXqFYch9cway9qWMw0uXu.jpg'},                  {'name': 'Syfy', 'id': 215, 'icon': 'f7iqKjWYdVoYVIvKP3nboULcrM2.jpg'},
{'name': 'Lifetime', 'id': 157, 'icon': '3wJNOOCbvqi7fJAdgf1QpL7Wwe2.jpg'},                  {'name': 'realeyz', 'id': 14, 'icon': '10BQc1kYmgjXFrFKb3xsRcDDn14.jpg'},
{'name': 'Shudder', 'id': 99, 'icon': 'pheENW1BxlexXX1CKJ4GyWudyMA.jpg'},                    {'name': 'Screambox', 'id': 185, 'icon': 'c2Ey5Q3uUjZgfWWQQIdVIjVfxE4.jpg'},
{'name': 'Acorn TV', 'id': 87, 'icon': '5P99DkK1jVs95KcE8bYG9MBtGQ.jpg'},                    {'name': 'Sundance Now', 'id': 143, 'icon': 'pZ9TSk3wlRYwiwwRxTsQJ7t2but.jpg'},
{'name': 'Popcornflix', 'id': 241, 'icon': 'olvOut34aWUFf1YoOqiqtjidiTK.jpg'},               {'name': 'Pantaya', 'id': 247, 'icon': '94IdHexespnJs96kmGiJlflfiwU.jpg'},
{'name': 'Boomerang', 'id': 248, 'icon': 'oRXiHzPl2HJMXXFR4eebsb8F5Oc.jpg'},                 {'name': 'Urban Movie Channel', 'id': 251, 'icon': '5uTsmZnDQmIOjZPEv8TNTy7GRJB.jpg'},
{'name': 'Dove Channel', 'id': 254, 'icon': 'cBCzPOX6ir5L8hCoJlfIWycxauh.jpg'},              {'name': 'History Vault', 'id': 268, 'icon': '3bm7P1O8WRqK6CYqfffJv4fba2p.jpg'},
{'name': 'Nickhits', 'id': 261, 'icon': 'oMwjMgYiT2jcR7ELqCH3TPzpgTX.jpg'},                  {'name': 'Eros Now', 'id': 218, 'icon': '4XYI2rzRm34skcvamytegQx7Dmu.jpg'},
{'name': 'Yupp TV', 'id': 255, 'icon': '8qNJcPBHZ4qewHrDJ7C7s2DBQ3V.jpg'},                   {'name': 'Magnolia Selects', 'id': 259, 'icon': 'foT1TtL67MgEOWR6Cib8dKyCvJI.jpg'},
{'name': 'WWE Network', 'id': 260, 'icon': 'rDYZ9v3Y09fuFyan51tHKE1mFId.jpg'},               {'name': 'Noggin', 'id': 262, 'icon': 'yxBUPUBFzHE72uFXvFr1l0fnMJA.jpg'},
{'name': 'Smithsonian Channel', 'id': 276, 'icon': 'UAZ2lJBWszijybQD4frqw2jxRO.jpg'},        {'name': 'Laugh Out Loud', 'id': 275, 'icon': 'w4GTJ1EDrgJku49XKSnRag9kKCT.jpg'},
{'name': 'Hallmark Movies', 'id': 281, 'icon': 'llEJ6av9kAniTQUR9hF9mhVbzlB.jpg'},           {'name': 'Pure Flix', 'id': 278, 'icon': 'orsVBNvPWxJNOVSEHMOk2h8R1wA.jpg'},
{'name': 'Lifetime Movie Club', 'id': 284, 'icon': 'p1v0UKH13xQsMjumRgCGmCdlgKm.jpg'},       {'name': 'Cinemax', 'id': 289, 'icon': 'kEnyHRflZPNWEOIXroZPhfdGi46.jpg'},
{'name': 'OVID', 'id': 433, 'icon': 'nXi2nRDPMNivJyFOifEa2t15Xuu.jpg'},                      {'name': 'Cohen Media Amazon', 'id': 1811, 'icon': 'jV7sSPzUYYHHmoATkD9PhFoEZXb.jpg'},
{'name': 'Viewster Amazon', 'id': 295, 'icon': 'mlH42JbZMrapSF6zc8iTYURcZlH.jpg'},           {'name': 'USA Network', 'id': 322, 'icon': 'ldU2RCgdvkcSEBWWbttCpVO450z.jpg'},
{'name': 'Sling TV Orange and Blue', 'id': 299, 'icon': 'tZ4xzOtCRHjAw7tYJphivEfDr1L.jpg'},  {'name': 'HiDive', 'id': 430, 'icon': '9baY98ZKyDaNArp1H9fAWqiR3Zi.jpg'},
{'name': 'Topic', 'id': 454, 'icon': 'ubWucXFn34TrVlJBaJFgPaC4tOP.jpg'},                     {'name': 'Night Flight Plus', 'id': 455, 'icon': 'ba8l0e5CkpVnrdFgzBySP7ckZnZ.jpg'},
{'name': 'Retrocrush', 'id': 446, 'icon': '9ONs8SMAXtkiyaEIKATTpbwckx8.jpg'},                {'name': 'Shout! Factory TV', 'id': 439, 'icon': 'ju3T8MFGNIoPiYpwHFpNlrYNyG7.jpg'},
{'name': 'Chai Flicks', 'id': 438, 'icon': '3tCqvc5hPm5nl8Hm8o2koDRZlPo.jpg'},               {'name': 'PBS Masterpiece Amazon', 'id': 294, 'icon': 'mMALQK52OFGoYUKOSCZILZkfGWs.jpg'},
{'name': 'The Film Detective', 'id': 470, 'icon': 'rOwEnT8oDSTZ5rDKmyaa3O4gUnc.jpg'},        {'name': 'MUBI Amazon', 'id': 201, 'icon': 'aJUiN18NZFbpSkHZQV1C1cTpz8H.jpg'},       
{'name': 'AcornTV Amazon', 'id': 196, 'icon': '8WWD7t5Irwq9kAH4rufQ4Pe1Dog.jpg'},            {'name': 'Screambox Amazon', 'id': 202, 'icon': 'naqM14qSfg2q0S2zDylM5zQQ3jn.jpg'},
{'name': 'Bet+ Amazon', 'id': 343, 'icon': 'obBJU4ak4XvAOUM5iVmSUxDvqC3.jpg'},               {'name': 'FlixFling', 'id': 331, 'icon': '4U02VrbgLfUKJAUCHKzxWFtnPx4.jpg'},
{'name': 'Darkmatter TV', 'id': 355, 'icon': 'x4AFz5koB2R8BRn8WNh6EqXUGHc.jpg'},             {'name': 'AMC on Demand', 'id': 352, 'icon': 'kJlVJLgbNPvKDYC0YMp3yA2OKq2.jpg'},
{'name': 'TCM', 'id': 361, 'icon': '8TbsXATKVD4Humjzi6a8SVaSY7o.jpg'},                       {'name': 'TNT', 'id': 363, 'icon': 'gJnQ40Z6T7HyY6fbmmI6qKE0zmK.jpg'},
{'name': 'BBC America', 'id': 397, 'icon': 'ukSXbR5qFjO2qCHpc6ZhcGPSjTJ.jpg'},               {'name': 'IndieFlix', 'id': 368, 'icon': '2NRn6OApVKfDTKLuHDRN8UadLRw.jpg'},
{'name': 'Here TV', 'id': 417, 'icon': 'sa10pK4Jwr5aA7rvafFP2zyLFjh.jpg'},                   {'name': 'Flix Premiere', 'id': 432, 'icon': '6fX0J6x7zXsUCvPFczgOW4oD34D.jpg'},
{'name': 'TBS', 'id': 506, 'icon': 'rcebVnRvZvPXauK4353Jgiu4DWI.jpg'},                       {'name': 'AsianCrush', 'id': 514, 'icon': '3VxDqUk25KU5860XxHKwV9cy3L8.jpg'},
{'name': 'FILMRISE', 'id': 471, 'icon': 'mEiBVz62M9j3TCebmOspMfqkIn.jpg'},                   {'name': 'Revry', 'id': 473, 'icon': 'r1UgUKmt83FSDOIHBdRWKooZPNx.jpg'},
{'name': 'Spectrum On Demand', 'id': 486, 'icon': '79mRAYq40lcYiXkQm6N7YErSSHd.jpg'},        {'name': 'VRV', 'id': 504, 'icon': 'rtTqPKRrVVXxvPV0T9OmSXhwXnY.jpg'},
{'name': 'Hi-YAH', 'id': 503, 'icon': 'mB2eDIncwSAlyl8WAtfV24qEIkk.jpg'},                    {'name': 'tru TV', 'id': 507, 'icon': 'pg4bIFyUsSIhFChqOz5Up1BxuIU.jpg'},
{'name': 'Discovery Plus', 'id': 520, 'icon': 'wYRiUqIgWcfUvO6OPcXuUNd4tc2.jpg'},            {'name': 'ARROW', 'id': 529, 'icon': '4UfmxLzph9Aso9pr9bXohp0V3sr.jpg'},
{'name': 'Plex', 'id': 538, 'icon': 'wDWvnupneMbY6RhBTHQC9zU0SCX.jpg'},                      {'name': 'Alamo on Demand', 'id': 547, 'icon': '1UP7ysjKolfD0rmp2fLmvyRHkdn.jpg'},
{'name': 'Dogwoof On Demand', 'id': 536, 'icon': '9sk88OAxDZSdMOzg8VuqtGpgWQ3.jpg'},         {'name': 'MovieSaints', 'id': 562, 'icon': 'fdWE8jpmQqkZrwg2ZMuCLz6ms5P.jpg'},
{'name': 'Film Movement Plus', 'id': 579, 'icon': 'tKJdVrC0fjEtQtYYjlVwX9rmqrj.jpg'},        {'name': 'Metrograph', 'id': 585, 'icon': '8PmpsrVDLJ3m8I37W6UNFEymhm7.jpg'},
{'name': 'Freevee', 'id': 613, 'icon': 'uBE4RMH15mrkuz6vXzuJc7ZLXp1.jpg'},                   {'name': 'Kino Now', 'id': 640, 'icon': 'ttxbDVmHMuNTKcSLOyIHFs7TdRh.jpg'},
{'name': 'ShortsTV Amazon', 'id': 688, 'icon': 'm0mvKlSjn38S9w7WVNV7a7XyPIe.jpg'},           {'name': 'Bet+', 'id': 1759, 'icon': 'eZVDDqlBHpuk8GELhQchRIkA6th.jpg'},
{'name': 'ESPN Plus', 'id': 1768, 'icon': 'iJBj5b4HYbjEPiwKJWQfcRr3nP2.jpg'},                {'name': 'Paramount+ Showtime', 'id': 1770, 'icon': 'vfUoancVnPRAxj8iBqhllanF0Eq.jpg'},
{'name': 'Klassiki', 'id': 1793, 'icon': 'fXGdolQR7QlHgdx2hPCxoVQG8eP.jpg'},                 {'name': 'Starz Amazon', 'id': 1794, 'icon': 'x36C6aseF5l4uX99Kpse9dbPwBo.jpg'},
{'name': 'Viaplay', 'id': 76, 'icon': 'cvl65OJnz14LUlC3yGK1KHj8UYs.jpg'},                    {'name': 'Popflick', 'id': 1832, 'icon': 'wbKHI2d5417yAAY7QestC3qnXyo.jpg'}
	]

watch_providers_tvshows = [
{'name': 'Netflix', 'id': 8, 'icon': 't2yyOv40HZeVlLjYsCsPHnWLk4W.jpg'},                        {'name': 'Amazon Prime Video', 'id': 9, 'icon': 'emthp39XA2YScoYL1p0sdbAH2WA.jpg'},
{'name': 'Disney +', 'id': 337, 'icon': '7rwgEs15tFwyR9NPQ5vpzxTj19Q.jpg'},                     {'name': 'Apple TV', 'id': 2, 'icon': 'peURlLlr8jggOwK53fJ5wdQl05y.jpg'},
{'name': 'Google Play Movies', 'id': 3, 'icon': 'tbEdFQDwx5LEVr8WpSeXQSIirVq.jpg'},             {'name': 'Hulu', 'id': 15, 'icon': 'zxrVdFjIjLqkfnwyghnfywTn3Lh.jpg'},
{'name': 'MUBI', 'id': 11, 'icon': 'bVR4Z1LCHY7gidXAJF5pMa4QrDS.jpg'},                          {'name': 'Rooster Teeth', 'id': 485, 'icon': '3MflXNopMv3EFKbVgJGoEkJEnnF.jpg'},
{'name': 'fuboTV', 'id': 257, 'icon': 'jPXksae158ukMLFhhlNvzsvaEyt.jpg'},                       {'name': 'Paramount +', 'id': 531, 'icon': 'xbhHHa1YgtpwhC8lb1NQ3ACVcLd.jpg'},
{'name': 'HBO Max', 'id': 384, 'icon': 'Ajqyt5aNxNGjmF9uOfxArGrdf3X.jpg'},                      {'name': 'Max', 'id': 1899, 'icon': '6Q3ZYUNA9Hsgj6iWnVsw2gR5V6z.jpg'},
{'name': 'Netflix Kids', 'id': 175, 'icon': 'j2OLGxyy0gKbPVI0DYFI2hJxP6y.jpg'},                 {'name': 'Apple TV +', 'id': 350, 'icon': '6uhKBfmtzFqOcLousHwZuzcrScK.jpg'},
{'name': 'Max Amazon', 'id': 1825, 'icon': '7TVfqxyWGqaJZM715IPHTwtgcXo.jpg'},                  {'name': 'MGM + Amazon', 'id': 583, 'icon': 'hoqk74y8HTJTMWcVes1ZVwohCue.jpg'},
{'name': 'Sun Nxt', 'id': 309, 'icon': 'uW4dPCcbXaaFTyfL5HwhuDt5akK.jpg'},                      {'name': 'Crunchyroll Amazon', 'id': 1968, 'icon': 'xQfBwtG1pLWhpejDoMFUxDS14eP.jpg'},
{'name': 'Curiosity Stream', 'id': 190, 'icon': '67Ee4E6qOkQGHeUTArdJ1qRxzR2.jpg'},             {'name': 'VIX ', 'id': 457, 'icon': 'ijHUSuVY0PLfTBMfRwH2PrzTD7G.jpg'},
{'name': 'aha', 'id': 532, 'icon': 'm3NWxxR23l1w1e156fyTuw931gx.jpg'},                          {'name': 'Peacock', 'id': 386, 'icon': '8VCV78prwd9QzZnEm0ReO6bERDa.jpg'},
{'name': 'Peacock Premium', 'id': 387, 'icon': 'xTHltMrZPAJFLQ6qyCBjAnXSmZt.jpg'},              {'name': 'Kocowa', 'id': 464, 'icon': 'xfAAOAERZCnPB5jW5lhboAcXk8L.jpg'},
{'name': 'DOCSVILLE', 'id': 475, 'icon': 'bvcdVO7SDHKEa6D40g1jntXKNj.jpg'},                     {'name': 'WOW Presents +', 'id': 546, 'icon': 'mgD0T960hnYU4gBxbPPBrcDfgWg.jpg'},
{'name': 'Amazon Video', 'id': 10, 'icon': '5NyLm42TmCqCMOZFvH4fcoSNKEW.jpg'},                  {'name': 'Magellan TV', 'id': 551, 'icon': 'gekkP93StjYdiMAInViVmrnldNY.jpg'},
{'name': 'BroadwayHD', 'id': 554, 'icon': 'xLu1rkZNOKuNnRNr70wySosfTBf.jpg'},                   {'name': 'YouTube', 'id': 192, 'icon': 'oIkQkEkwfmcG7IGpRR1NB8frZZM.jpg'},
{'name': 'Showtime Apple TV', 'id': 675, 'icon': 'xVN3LKkOtCrlFT9mavhkx8SzMwV.jpg'},            {'name': 'Dekkoo', 'id': 444, 'icon': 'u2H29LCxRzjZVUoZUQAHKm5P8Zc.jpg'},
{'name': 'Paramount+ with Showtime', 'id': 1770, 'icon': 'cqizxemZmCDDOfaGCqALiCbrAoR.jpg'},    {'name': 'Starz Apple TV', 'id': 1855, 'icon': 'hB24bAA8Y2ei6pbEGuCNdKUOjxI.jpg'},
{'name': 'Hoichoi', 'id': 315, 'icon': 'd4vHcXY9rwnr763wQns2XJThclt.jpg'},                      {'name': 'AMC + Apple TV ', 'id': 1854, 'icon': 'yFgm7vxwKZ4jfXIlPizlgoba2yi.jpg'},
{'name': 'BritBox', 'id': 151, 'icon': 'aGIS8maihUm60A3moKYD9gfYHYT.jpg'},                      {'name': 'KoreaOnDemand', 'id': 575, 'icon': 'uHv6Y4YSsr4cj7q4cBbAg7WXKEI.jpg'},
{'name': 'Britbox Apple TV ', 'id': 1852, 'icon': 'cN85Wjk0FIFr3z6rbiimz10uWVo.jpg'},           {'name': 'Rakuten Viki', 'id': 344, 'icon': 'qjtOUIUnk4kRpcZmaddjqDHM0dR.jpg'},
{'name': 'Paramount+ Amazon', 'id': 582, 'icon': '3E0RkIEQrrGYazs63NMsn3XONT6.jpg'},            {'name': 'Pluto TV', 'id': 300, 'icon': 't6N57S17sdXRXmZDAkaGP0NHNG0.jpg'},
{'name': 'iQIYI', 'id': 581, 'icon': '8MXYXzZGoPAEQU13GWk1GVvKNUS.jpg'},                        {'name': 'Cultpix', 'id': 692, 'icon': '59azlQKUgFdYq6QI5QEAxIeecyL.jpg'},
{'name': 'Starz Play Amazon', 'id': 194, 'icon': 'x36C6aseF5l4uX99Kpse9dbPwBo.jpg'},            {'name': 'FilmBox+', 'id': 701, 'icon': '4FqTBYsUSZgS9z9UGKgxSDBbtc8.jpg'},
{'name': 'Discovery+ Amazon', 'id': 584, 'icon': 'a2OcajC4bM5ItniQdjyOV7tgthW.jpg'},            {'name': 'AMC+ Amazon', 'id': 528, 'icon': '9edKQczyuMmQM1yS520hgmJbcaC.jpg'},
{'name': 'Shahid VIP', 'id': 1715, 'icon': 'uurfHKuprPDeKfIs7FYd5lQzw0L.jpg'},                  {'name': 'Funimation Now', 'id': 269, 'icon': 'fWq61Fy4onav0wZJTA3c2fs0G66.jpg'},
{'name': 'The Roku Channel', 'id': 207, 'icon': 'z0h7mBHwm5KfMB2MKeoQDD2ngEZ.jpg'},             {'name': 'Classix', 'id': 445, 'icon': 'iaMw6nOyxUzXSacrLQ0Au6CfZkc.jpg'},
{'name': 'Crunchyroll', 'id': 283, 'icon': '8Gt1iClBlzTeQs8WQm8UrCoIxnQ.jpg'},                  {'name': 'Showtime Roku', 'id': 632, 'icon': 'qMf2zirM2w0sO0mdAIIoP5XnQn8.jpg'},
{'name': 'Paramount+ Roku Premium', 'id': 633, 'icon': 'qlVSrZgfXlFw0Jj6hsYq2zi70JD.jpg'},      {'name': 'Univer Video', 'id': 1860, 'icon': 'esfuWSTkFr39ETpD9xvb0sduZt7.jpg'},
{'name': 'Starz Roku Premium', 'id': 634, 'icon': '5OAb2w7D9C2VHa0k5PaoAYeFYFE.jpg'},           {'name': 'Runtime', 'id': 1875, 'icon': 'nvCfpn94VKJN4ZpkDgoupJWlXqq.jpg'},
{'name': 'AMC+ Roku Premium', 'id': 635, 'icon': 'ni2NgPmIqqJRXeiA8Zdj4UhBZnU.jpg'},            {'name': 'Paramount+ AppleTV ', 'id': 1853, 'icon': '9pdeflA0P1b8qlkeDA1oLfyvR06.jpg'},
{'name': 'Showtime Amazon', 'id': 203, 'icon': 'zoL69abPHiVC1Qzd4kM6hwLSo0j.jpg'},              {'name': 'AMC+', 'id': 526, 'icon': 'xlonQMSmhtA2HHwK3JKF9ghx7M8.jpg'},
{'name': 'MGM + Roku Premium', 'id': 636, 'icon': '3sE2JNYZJRD9Le1P8B6oVEqarad.jpg'},           {'name': 'YouTube Premium', 'id': 188, 'icon': '6IPjvnYl6WWkIwN158qBFXCr2Ne.jpg'},
{'name': 'Hoopla', 'id': 212, 'icon': 'aJ0b9BLU1Cvv5hIz9fEhKKc1x1D.jpg'},                       {'name': 'The CW', 'id': 83, 'icon': '6Y6w3F5mYoRHCcNAG0ZD2AndLJ2.jpg'},
{'name': 'CW Seed', 'id': 206, 'icon': '7UpZTaQFcdISOzDOBMx6RavcaR.jpg'},                       {'name': 'Vudu', 'id': 7, 'icon': '21dEscfO8n1tL35k4DANixhffsR.jpg'},
{'name': 'Starz', 'id': 43, 'icon': 'eWp5LdR4p4uKL0wACBBXapDV2lB.jpg'},                         {'name': 'VUDU Free', 'id': 332, 'icon': 'xzfVRl1CgJPYa9dOoyVI3TDSQo2.jpg'},
{'name': 'Showtime', 'id': 37, 'icon': '4kL33LoKd99YFIaSOoOPMQOSw1A.jpg'},                      {'name': 'PBS', 'id': 209, 'icon': 'bbxgdl6B5T75wJE713BiTCIBXyS.jpg'},
{'name': 'FXNow', 'id': 123, 'icon': 'twV9iQPYeaoBzwsfRFGMGoMIUg8.jpg'},                        {'name': 'Pantaflix', 'id': 177, 'icon': '2tAjxjo1n3H7fsXqMsxWFMeFUWp.jpg'},
{'name': 'Tubi TV', 'id': 73, 'icon': 'w2TDH9TRI7pltf5LjN3vXzs7QbN.jpg'},                       {'name': 'Kanopy', 'id': 191, 'icon': 'wbCleYwRFpUtWcNi7BLP3E1f6VI.jpg'},
{'name': 'Comedy Central', 'id': 243, 'icon': 'gmU9aPV3XUFusVs4kK1rcICUKqL.jpg'},               {'name': 'Microsoft Store', 'id': 68, 'icon': 'shq88b09gTBYC4hA7K7MUL8Q4zP.jpg'},
{'name': 'Redbox', 'id': 279, 'icon': 'gbyLHzl4eYP0oP9oJZ2oKbpkhND.jpg'},                       {'name': 'ABC', 'id': 148, 'icon': 'l9BRdAgQ3MkooOalsuu3yFQv2XP.jpg'},
{'name': 'Crackle', 'id': 12, 'icon': '7P2JHkfv4AmU2MgSPGaJ0z6nNLG.jpg'},                       {'name': 'DIRECTV', 'id': 358, 'icon': 'xL9SUR63qrEjFZAhtsipskeAMR7.jpg'},
{'name': 'AMC', 'id': 80, 'icon': 'dTKs9JkJl06hnbnqUXHAxUwZrUS.jpg'},                           {'name': 'NBC', 'id': 79, 'icon': 'wSAxtofaArEuTOsqBmghVuJx7eP.jpg'},
{'name': 'MGM +', 'id': 34, 'icon': '6A1gRIJqLfFHOoTvbTxDAbuU2nQ.jpg'},                         {'name': 'Freeform', 'id': 211, 'icon': 'rgpmwMkXqFYch9cway9qWMw0uXu.jpg'},
{'name': 'History', 'id': 155, 'icon': 'm6pLJ0l6MQJiKg1yxEs1holRSiq.jpg'},                      {'name': 'Syfy', 'id': 215, 'icon': 'f7iqKjWYdVoYVIvKP3nboULcrM2.jpg'},
{'name': 'A&E', 'id': 156, 'icon': 'ujE7L9z0Ceu1T74RcahVn1FMbbK.jpg'},                          {'name': 'Lifetime', 'id': 157, 'icon': '3wJNOOCbvqi7fJAdgf1QpL7Wwe2.jpg'},
{'name': 'Shudder', 'id': 99, 'icon': 'pheENW1BxlexXX1CKJ4GyWudyMA.jpg'},                       {'name': 'Screambox', 'id': 185, 'icon': 'c2Ey5Q3uUjZgfWWQQIdVIjVfxE4.jpg'},
{'name': 'Acorn TV', 'id': 87, 'icon': '5P99DkK1jVs95KcE8bYG9MBtGQ.jpg'},                       {'name': 'Sundance Now', 'id': 143, 'icon': 'pZ9TSk3wlRYwiwwRxTsQJ7t2but.jpg'},
{'name': 'Popcornflix', 'id': 241, 'icon': 'olvOut34aWUFf1YoOqiqtjidiTK.jpg'},                  {'name': 'Pantaya', 'id': 247, 'icon': '94IdHexespnJs96kmGiJlflfiwU.jpg'},
{'name': 'Boomerang', 'id': 248, 'icon': 'oRXiHzPl2HJMXXFR4eebsb8F5Oc.jpg'},                    {'name': 'Urban Movie Channel', 'id': 251, 'icon': '5uTsmZnDQmIOjZPEv8TNTy7GRJB.jpg'},
{'name': 'Dove Channel', 'id': 254, 'icon': 'cBCzPOX6ir5L8hCoJlfIWycxauh.jpg'},                 {'name': 'Nickhits Amazon', 'id': 261, 'icon': 'oMwjMgYiT2jcR7ELqCH3TPzpgTX.jpg'},
{'name': 'Eros Now', 'id': 218, 'icon': '4XYI2rzRm34skcvamytegQx7Dmu.jpg'},                     {'name': 'Yupp TV', 'id': 255, 'icon': '8qNJcPBHZ4qewHrDJ7C7s2DBQ3V.jpg'},
{'name': 'MyOutdoorTV', 'id': 264, 'icon': 'tTLB4xkjrKXxdtiWTeeS6qQB1v9.jpg'},                  {'name': 'Magnolia Selects', 'id': 259, 'icon': 'foT1TtL67MgEOWR6Cib8dKyCvJI.jpg'},
{'name': 'WWE Network', 'id': 260, 'icon': 'rDYZ9v3Y09fuFyan51tHKE1mFId.jpg'},                  {'name': 'Noggin Amazon', 'id': 262, 'icon': 'yxBUPUBFzHE72uFXvFr1l0fnMJA.jpg'},
{'name': 'Hopster TV', 'id': 267, 'icon': 'gYC72bT1nz4NvOFe7pPuCsNdKch.jpg'},                   {'name': 'Smithsonian Channel', 'id': 276, 'icon': 'UAZ2lJBWszijybQD4frqw2jxRO.jpg'},
{'name': 'Laugh Out Loud', 'id': 275, 'icon': 'w4GTJ1EDrgJku49XKSnRag9kKCT.jpg'},               {'name': 'Pure Flix', 'id': 278, 'icon': 'orsVBNvPWxJNOVSEHMOk2h8R1wA.jpg'},
{'name': 'Hallmark Movies', 'id': 281, 'icon': 'llEJ6av9kAniTQUR9hF9mhVbzlB.jpg'},              {'name': 'PBS Kids Amazon', 'id': 293, 'icon': 'tU4tamrqRjbg3Lbmkryp3EiLPQJ.jpg'},
{'name': 'Boomerang Amazon', 'id': 288, 'icon': '1zfRJQc14uEzZThdwNvxtxeWJw6.jpg'},             {'name': 'Cinemax Amazon', 'id': 289, 'icon': 'kEnyHRflZPNWEOIXroZPhfdGi46.jpg'},
{'name': 'Pantaya Amazon', 'id': 292, 'icon': 'fvSJ17mOt3MxKfnSgQVrtXTuepq.jpg'},               {'name': 'Hallmark Amazon', 'id': 290, 'icon': '6L2wLiZz3IG2X4MRbdRlGLgftMK.jpg'},
{'name': 'PBS Masterpiece Amazon', 'id': 294, 'icon': 'mMALQK52OFGoYUKOSCZILZkfGWs.jpg'},       {'name': 'MZ Choice Amazon', 'id': 291, 'icon': '72tiOIjZQPqm7MGhqoqyjyTJzSv.jpg'},
{'name': 'Viewster Amazon', 'id': 295, 'icon': 'mlH42JbZMrapSF6zc8iTYURcZlH.jpg'},              {'name': 'HiDive', 'id': 430, 'icon': '9baY98ZKyDaNArp1H9fAWqiR3Zi.jpg'},
{'name': 'Topic', 'id': 454, 'icon': 'ubWucXFn34TrVlJBaJFgPaC4tOP.jpg'},                        {'name': 'MTV', 'id': 453, 'icon': 'ttCYMg3dbKYeGCgCxzsNvT3L4qF.jpg'},
{'name': 'Retrocrush', 'id': 446, 'icon': '9ONs8SMAXtkiyaEIKATTpbwckx8.jpg'},                   {'name': 'Shout! Factory TV', 'id': 439, 'icon': 'ju3T8MFGNIoPiYpwHFpNlrYNyG7.jpg'},
{'name': 'Chai Flicks', 'id': 438, 'icon': '3tCqvc5hPm5nl8Hm8o2koDRZlPo.jpg'},                  {'name': 'Mhz Choice', 'id': 427, 'icon': 'vuS4VlY50SJVHbCU3vGxQehcsAg.jpg'},
{'name': 'Vice TV ', 'id': 458, 'icon': 'oYpUb0xkRfEE5iccELlumPGubt4.jpg'},                     {'name': 'Shudder Amazon', 'id': 204, 'icon': 'sc5pTTCFbx7GQyOst5SG4U7nkPH.jpg'},
{'name': 'MUBI Amazon', 'id': 201, 'icon': 'aJUiN18NZFbpSkHZQV1C1cTpz8H.jpg'},                  {'name': 'AcornTV Amazon', 'id': 196, 'icon': '8WWD7t5Irwq9kAH4rufQ4Pe1Dog.jpg'},
{'name': 'BritBox Amazon', 'id': 197, 'icon': 'xTfyFZqWv8c8sxlFooUzemi6WRM.jpg'},               {'name': 'Fandor Amazon', 'id': 199, 'icon': '8vBJZkwkrUDYMSfmw5R0ZENd7yw.jpg'},
{'name': 'Screambox Amazon', 'id': 202, 'icon': 'naqM14qSfg2q0S2zDylM5zQQ3jn.jpg'},             {'name': 'Sundance Now Amazon', 'id': 205, 'icon': 'xImSZRKRYzIMPr4COgJNsEHdd2T.jpg'},
{'name': 'Cartoon Network', 'id': 317, 'icon': 'A5vrIl7YqlmNrOHZikrtO41V0sY.jpg'},              {'name': 'Adult Swim', 'id': 318, 'icon': 'sPlIWhBAcoyw2IWuQ2PDdToNXld.jpg'},
{'name': 'USA Network', 'id': 322, 'icon': 'ldU2RCgdvkcSEBWWbttCpVO450z.jpg'},                  {'name': 'Fox', 'id': 328, 'icon': 'rbCRT408gY44bZH0KdtmKzoituI.jpg'},
{'name': 'Bet+ Amazon', 'id': 343, 'icon': 'obBJU4ak4XvAOUM5iVmSUxDvqC3.jpg'},                  {'name': 'FlixFling', 'id': 331, 'icon': '4U02VrbgLfUKJAUCHKzxWFtnPx4.jpg'},
{'name': 'Darkmatter TV', 'id': 355, 'icon': 'x4AFz5koB2R8BRn8WNh6EqXUGHc.jpg'},                {'name': 'Bravo TV', 'id': 365, 'icon': 'cezAIHmsUVvgAahfCR7J0z30y1N.jpg'},
{'name': 'TNT', 'id': 363, 'icon': 'gJnQ40Z6T7HyY6fbmmI6qKE0zmK.jpg'},                          {'name': 'Food Network', 'id': 366, 'icon': 'auXCWejtQmZL7DplgokLXYq73Ed.jpg'},
{'name': 'BBC America', 'id': 397, 'icon': 'ukSXbR5qFjO2qCHpc6ZhcGPSjTJ.jpg'},                  {'name': 'IndieFlix', 'id': 368, 'icon': '2NRn6OApVKfDTKLuHDRN8UadLRw.jpg'},
{'name': 'TLC', 'id': 412, 'icon': 'eZK2W0v3yA2Dq7cFzifK0v9FN1b.jpg'},                          {'name': 'AHCTV', 'id': 398, 'icon': 'gxCvG3STez0PrDqi05LSYyWjLPk.jpg'},
{'name': 'HGTV', 'id': 406, 'icon': 'bwTpY8DTKUjoi6YfuiMenahGrTj.jpg'},                         {'name': 'DIY Network', 'id': 405, 'icon': 'odh8CexN7yXa7IX4aIYtsUc0vHY.jpg'},
{'name': 'Investigation Discovery', 'id': 408, 'icon': 'gMV6YwrWO9YpLiUQ5dAxnxJiWWj.jpg'},      {'name': 'Science Channel', 'id': 411, 'icon': '3bRK8VOvIfWIhOLGGwNA67kphXC.jpg'},
{'name': 'Destination America', 'id': 402, 'icon': 'xZMxO6tGdeMmKxIvT4QjPz59ujm.jpg'},          {'name': 'Discovery Life', 'id': 404, 'icon': '3LGhdwqMB0iuEwidFusc0I38Omm.jpg'},
{'name': 'Animal Planet', 'id': 399, 'icon': 'fXcLPLz67yG0JzLWXIsNJrdwRzr.jpg'},                {'name': 'Discovery', 'id': 403, 'icon': 'dfz7hQm0icTUdXJrScZXPMeO963.jpg'},
{'name': 'Motor Trend', 'id': 410, 'icon': 'st6VcNMu18MKbiTFhaWnxU9rBat.jpg'},                  {'name': 'Travel Channel', 'id': 413, 'icon': '7pkbHGkSYh6MKMTojJ80bT0KtPY.jpg'},
{'name': 'Cooking Channel', 'id': 400, 'icon': 'aTiukuAuttjE2OdGv1eUhk3xsi0.jpg'},              {'name': 'Paramount Network', 'id': 418, 'icon': 'hG3NOo8CJJTq7CQMj44kLFHoWOi.jpg'},
{'name': 'Here TV', 'id': 417, 'icon': 'sa10pK4Jwr5aA7rvafFP2zyLFjh.jpg'},                      {'name': 'TV Land', 'id': 419, 'icon': 'zU4b7cGYV6kRDOI6s8dgZqUvwFI.jpg'},
{'name': 'VH1', 'id': 422, 'icon': 'jJUUb3clz84u347JWx7RUFMdjwP.jpg'},                          {'name': 'Logo TV', 'id': 420, 'icon': 'eWm07gxivsHwDx8CZRzVQIfVO4h.jpg'},
{'name': 'DreamWorksTV Amazon', 'id': 263, 'icon': '1Vzd0eRyJJ7djh0GuZczx4ap8PK.jpg'},          {'name': 'TBS', 'id': 506, 'icon': 'rcebVnRvZvPXauK4353Jgiu4DWI.jpg'},
{'name': 'AsianCrush', 'id': 514, 'icon': '3VxDqUk25KU5860XxHKwV9cy3L8.jpg'},                   {'name': 'FILMRISE', 'id': 471, 'icon': 'mEiBVz62M9j3TCebmOspMfqkIn.jpg'},
{'name': 'Revry', 'id': 473, 'icon': 'r1UgUKmt83FSDOIHBdRWKooZPNx.jpg'},                        {'name': 'OXYGEN', 'id': 487, 'icon': 'lrZQdxtEHMbDZDnDo92KBkEHxSl.jpg'},
{'name': 'Spectrum On Demand', 'id': 486, 'icon': '1tLCqSH5xiViDxMiTVWl6DmE8hd.jpg'},           {'name': 'tru TV', 'id': 507, 'icon': 'pg4bIFyUsSIhFChqOz5Up1BxuIU.jpg'},
{'name': 'DisneyNOW', 'id': 508, 'icon': 'pu5I5Fis0r7ReAOswcJzOKmdLrK.jpg'},                    {'name': 'WeTV', 'id': 509, 'icon': 'qiwHTuSh91SgVMtY9lP7y5tH6kN.jpg'},
{'name': 'Plex', 'id': 538, 'icon': 'swMyOSh6p3ZOTr76yPV6EyQFTik.jpg'},                         {'name': 'Plex Player', 'id': 1945, 'icon': '2qHTjqMoBxpa2gJPbbEDvqpLZJS.jpg'},
{'name': 'Oprah Winfrey Network', 'id': 555, 'icon': 'jbcfM4YaulkzcPRIpiPZWIfcA67.jpg'},        {'name': 'Freevee', 'id': 613, 'icon': 'uBE4RMH15mrkuz6vXzuJc7ZLXp1.jpg'},
{'name': 'Bet+', 'id': 1759, 'icon': 'eZVDDqlBHpuk8GELhQchRIkA6th.jpg'},                        {'name': 'Starz Amazon', 'id': 1794, 'icon': 'x36C6aseF5l4uX99Kpse9dbPwBo.jpg'},
{'name': 'Netflix basic with Ads', 'id': 1796, 'icon': 'mShqQVDhHoK7VUbfYG3Un6xE8Mv.jpg'},      {'name': 'Cohen Media Amazon', 'id': 1811, 'icon': 'jV7sSPzUYYHHmoATkD9PhFoEZXb.jpg'},
{'name': 'Popflick', 'id': 1832, 'icon': 'wbKHI2d5417yAAY7QestC3qnXyo.jpg'},                    {'name': 'Viaplay', 'id': 76, 'icon': 'cvl65OJnz14LUlC3yGK1KHj8UYs.jpg'},
{'name': 'Discovery+', 'id': 520, 'icon': 'wYRiUqIgWcfUvO6OPcXuUNd4tc2.jpg'},                   {'name': 'Reveel', 'id': 1948, 'icon': '9X86vPeIfv7mNHW9DOf88n4g03x.jpg'},
{'name': 'Ovation TV', 'id': 1953, 'icon': 'ua5RmU2I9tkmpPR6IenXUPxnkzC.jpg'},                  {'name': 'Angel Studios', 'id': 1956, 'icon': 'c1H5A9BqqceTzZvLXmfh0fehNTY.jpg'},
{'name': 'Cineverse', 'id': 1957, 'icon': 'pPLjA4eW9186YXMh48r7pmLoNYH.jpg'},                   {'name': 'AD tv', 'id': 1958, 'icon': 'x6N4Sp4cwP9lYutveMruJcAKNQg.jpg'},
{'name': 'Midnight Pulp', 'id': 1960, 'icon': 'pHwuJc9HFgK229r4C3f1AajjGA2.jpg'},               {'name': 'FYI Network', 'id': 1962, 'icon': 'nUXrIDIQzQuA3ZptGznUfHcAVHe.jpg'},
{'name': 'Xumo Play', 'id': 1963, 'icon': '6Mr2gYhkC1AxcRlcPFV1rnmMAjC.jpg'},                   {'name': 'National Geographic', 'id': 1964, 'icon': 'd81hzdOMy560vJZMsbpYgnLbK4s.jpg'},
{'name': 'DistroTV', 'id': 1971, 'icon': '77HXTBLPXGYRx1xm4w0Qyj1S5DN.jpg'},                    {'name': 'myfilmfriend', 'id': 1972, 'icon': 'u6RDPCBgkTpM5OIJ1LXO2KeekiX.jpg'},
{'name': 'Hallmark Movies & Mysteries', 'id': 1966, 'icon': '80iXgRgamAX3JOMVg1EDHMX9s1f.jpg'}, {'name': 'Outside Watch', 'id': 1976, 'icon': '7N6w9Hz541qNhlHEHjHgEGCjKDh.jpg'},
{'name': 'Citytv', 'id': 1985, 'icon': 'piaX7JA1HEJ3qA4CKQ5LZyxxshm.jpg'}]

movie_sorts = [
{'name': 'Popularity (asc)', 'id': '&sort_by=popularity.asc'}, {'name': 'Popularity (desc)', 'id': '&sort_by=popularity.desc'},
{'name': 'Release Date (asc)', 'id': '&sort_by=primary_release_date.asc'}, {'name': 'Release Date (desc)', 'id': '&sort_by=primary_release_date.desc'},
{'name': 'Total Revenue (asc)', 'id': '&sort_by=revenue.asc'}, {'name': 'Total Revenue (desc)', 'id': '&sort_by=revenue.desc'},
{'name': 'Title (asc)', 'id': '&sort_by=original_title.asc'}, {'name': 'Title (desc)', 'id': '&sort_by=original_title.desc'},
{'name': 'Rating (asc)', 'id': '&sort_by=vote_average.asc'}, {'name': 'Rating (desc)', 'id': '&sort_by=vote_average.desc'},
{'name': 'Random', 'id': '[random]'}
		]

tvshow_sorts = [
{'name': 'Popularity (asc)', 'id': '&sort_by=popularity.asc'}, {'name': 'Popularity (desc)', 'id': '&sort_by=popularity.desc'},
{'name': 'First Aired (asc)', 'id': '&sort_by=first_air_date.asc'}, {'name': 'First Aired (desc)', 'id': '&sort_by=first_air_date.desc'},
{'name': 'Rating (asc)', 'id': '&sort_by=vote_average.asc'}, {'name': 'Rating (desc)', 'id': '&sort_by=vote_average.desc'},
{'name': 'Random', 'id': '[random]'}
		]

discover_items = {
'with_year_start': {'label': 'Year Start', 'key': 'with_year_start', 'display_key': 'with_year_start_display', 'action': 'years',
'url_insert_movie': '&primary_release_date.gte=%s-01-01', 'url_insert_tvshow': '&first_air_date.gte=%s-01-01', 'name_value': ' | %s onwards', 'icon': 'calender'},
'with_year_end': {'label': 'Year End', 'key': 'with_year_end', 'display_key': 'with_year_end_display', 'action': 'years',
'url_insert_movie': '&primary_release_date.lte=%s-12-31', 'url_insert_tvshow': '&first_air_date.lte=%s-12-31', 'name_value': ' | up to %s', 'icon': 'calender'},
'with_genres': {'label': 'With Genres', 'key': 'with_genres', 'display_key': 'with_genres_display', 'action': 'genres',
'url_insert': '&with_genres=%s', 'name_value': ' | %s', 'icon': 'genres'},
'without_genres': {'label': 'Without Genres', 'key': 'without_genres', 'display_key': 'without_genres_display', 'action': 'genres',
'url_insert': '&without_genres=%s', 'name_value': ' | exclude %s', 'icon': 'genres'},
'with_network': {'label': 'Network', 'key': 'with_network', 'display_key': 'with_network_display', 'action': 'network',
'url_insert': '&with_networks=%s', 'name_value': ' | %s', 'limited': 'tvshow', 'icon': 'networks'},
'with_provider': {'label': 'Provider', 'key': 'with_provider', 'display_key': 'with_provider_display', 'action': 'provider',
'url_insert': '&with_watch_providers=%s', 'name_value': ' | %s', 'icon': 'providers'},
'with_certification': {'label': 'Certification', 'key': 'with_certification', 'display_key': 'with_certification_display', 'action': 'certifications',
'url_insert': '&certification_country=US&certification=%s', 'name_value': ' | %s', 'limited': 'movie', 'icon': 'certifications'},
'with_certification_and_lower': {'label': 'Certification (& lower)', 'key': 'with_certification_and_lower', 'display_key': 'with_certification_and_lower_display',
'action': 'certification_and_lowers', 'url_insert': '&certification_country=US&certification.lte=%s', 'name_value': ' | %s', 'limited': 'movie', 'icon': 'certifications'},		
'with_keywords': {'label': 'With Keywords', 'key': 'with_keywords', 'display_key': 'with_keywords_display', 'action': 'keywords',
'url_insert': '&with_keywords=%s', 'name_value': ' | Keywords: %s', 'icon': 'genre_fantasy'},
'with_rating': {'label': 'Minimum Rating', 'key': 'with_rating', 'display_key': 'with_rating_display', 'action': 'ratings',
'url_insert': '&vote_average.gte=%s', 'name_value': ' | %s+', 'icon': 'most_watched'},
'with_rating_votes': {'label': 'Minimum Number of Votes', 'key': 'with_rating_votes', 'display_key': 'with_rating_votes_display', 'action': 'votes',
'url_insert': '&vote_count.gte=%s', 'name_value': ' | %s votes', 'icon': 'most_voted'},
'with_cast': {'label': 'Include Cast', 'key': 'with_cast', 'display_key': 'with_cast_display', 'action': 'casts',
'url_insert': '&with_cast=%s', 'name_value': ' | with %s', 'limited': 'movie', 'icon': 'people'},
'with_sort': {'label': 'Sort By', 'key': 'with_sort', 'display_key': 'with_sort_display', 'action': 'sort',
'url_insert': '%s', 'name_value': ' | %s', 'icon': 'lists'},
'with_released': {'label': 'Released Only', 'key': 'with_released', 'display_key': 'with_released_display', 'action': 'released',
'url_insert_movie': '&primary_release_date.lte=%s', 'url_insert_tvshow': '&include_null_first_air_dates=false&first_air_date.lte=%s', 'name_value': ' | Released Only', 'icon': 'dvd'},
'with_adult': {'label': 'Include Adult', 'key': 'with_adult', 'display_key': 'with_adult_display', 'action': 'adult',
'url_insert': '&include_adult=%s', 'name_value': ' | Include Adult', 'limited': 'movie', 'icon': 'genre_romance'}
		}

color_palette = [
'FFFFFFE3', 'FFFFFAE6', 'FFFEF5E6', 'FFFEF0E5', 'FFFEEBE5', 'FFFFEFEF', 'FFFFE6EA', 'FFFFE6F1', 'FFFEE6F4', 'FFFFE6FB', 'FFFEE6FE', 'FFFAE6FF', 'FFF4E6FF', 'FFF0E6FF', 'FFEAE7FC',
'FFE6E7FC', 'FFE6EBFF', 'FFE7F0FF', 'FFE7F5FF', 'FFE7FAFF', 'FFE6FFFF', 'FFE6FFFB', 'FFE7FEF4', 'FFE7FFF1', 'FFE6FFEA', 'FFE7FFE7', 'FFEBFFF3', 'FFF1FFE6', 'FFF5FFE6', 'FFFBFFE6',
'FFFFFFFF', 'FFFFFFCB', 'FFFEFACA', 'FFFFEACB', 'FFFFE0CC', 'FFFED6CC', 'FFFFCACD', 'FFFFCCD5', 'FFFFCDE0', 'FFFFCCEB', 'FFFFCBF5', 'FFFECCFD', 'FFF6CBFF', 'FFECCCFE', 'FFE0CCFF',
'FFD6CCFE', 'FFCCCCFE', 'FFCDD6FF', 'FFCAE1FF', 'FFCCEBFF', 'FFCEF4FD', 'FFCAFFFF', 'FFCCFFF6', 'FFCBFEEB', 'FFCCFFE0', 'FFCCFFD6', 'FFCDFFCC', 'FFD7FFCB', 'FFE1FFCD', 'FFEBFFCC',
'FFF5FFCB', 'FFEFEFEF', 'FFFEFFB3', 'FFFFF1B2', 'FFFFE0B2', 'FFFDD2B2', 'FFFFC2B3', 'FFFFB3B3', 'FFFFB2C2', 'FFFFB3D1', 'FFFFB3E1', 'FFFFB2F4', 'FFFFB3FE', 'FFF0B3FF', 'FFE1B2FF',
'FFD2B3FF', 'FFC1B3FE', 'FFB4B3FF', 'FFB3C1FE', 'FFB2D1FF', 'FFB3E0FF', 'FFB2F0FF', 'FFB3FFFF', 'FFB3FFF0', 'FFB4FFE0', 'FFB3FFD1', 'FFB4FEC3', 'FFB3FFB4', 'FFC2FFB2', 'FFD1FFB4',
'FFE0FFB3', 'FFF1FFB4', 'FFE0E0E0', 'FFFEFF99', 'FFFFEB9A', 'FFFED699', 'FFFFC299', 'FFFFAD98', 'FFFF9899', 'FFFF99AE', 'FFFF99C1', 'FFFE99D5', 'FFFF99EC', 'FFFF99FF', 'FFEB99FF',
'FFD699FF', 'FFC299FF', 'FFAE99FF', 'FF9A99FF', 'FF98ADFE', 'FF9AC2FF', 'FF98D6FF', 'FF99EBFF', 'FF99FFFF', 'FF99FFEA', 'FF99FFD7', 'FF9AFFC3', 'FF99FFAC', 'FF99FF99', 'FFADFF99',
'FFC2FF98', 'FFD6FF99', 'FFEAFF98', 'FFD0D0D0', 'FFFFFF80', 'FFFFE681', 'FFFFCC80', 'FFFFB381', 'FFFF9980', 'FFFE8081', 'FFFF8199', 'FFFF80B3', 'FFFF80CD', 'FFFF80E7', 'FFFC81FE',
'FFE680FF', 'FFCC7FFF', 'FFB380FF', 'FF9980FF', 'FF807FFE', 'FF8099FE', 'FF7FB3FF', 'FF80CCFE', 'FF80E6FF', 'FF7FFFFE', 'FF7FFEE0', 'FF80FFCC', 'FF80FFB2', 'FF80FF98', 'FF81FF81',
'FF99FF80', 'FFB3FF80', 'FFCCFF80', 'FFE6FF80', 'FFC0C0C0', 'FFFFFF6B', 'FFFEE066', 'FFFFC267', 'FFFFA366', 'FFFF8566', 'FFFF6766', 'FFFF6685', 'FFFF66A4', 'FFFF66C1', 'FFFF66E0',
'FFFF66FF', 'FFE166FF', 'FFC366FF', 'FFA366FF', 'FF8566FF', 'FF6665FE', 'FF6785FF', 'FF66A3FE', 'FF65C2FF', 'FF65E0FF', 'FF65FFFF', 'FF66FFE0', 'FF65FFC1', 'FF66FFA4', 'FF65FF85',
'FF66FF66', 'FF84FF66', 'FFA2FF66', 'FFC2FF66', 'FFE0FF66', 'FFAFAFAF', 'FFFFFF4D', 'FFFFDB4E', 'FFFFB84E', 'FFFF944C', 'FFFF714D', 'FFFF4D4D', 'FFFF4D6F', 'FFFE4D93', 'FFFE4DB7',
'FFFE4DDB', 'FFFF4DFF', 'FFDC4DFF', 'FFB84DFF', 'FF944EFF', 'FF704DFF', 'FF4D4CFF', 'FF4D70FE', 'FF4D94FE', 'FF4DB8FF', 'FF4DDBFF', 'FF4DFFFF', 'FF4DFFDB', 'FF4EFFB9', 'FF4EFF95',
'FF4DFE70', 'FF4CFF4C', 'FF70FF4D', 'FF94FF4D', 'FFB8FE4D', 'FFDAFF4D', 'FF8C8C8C', 'FFFFFF33', 'FFFFD634', 'FFFFAD33', 'FFFF8532', 'FFFF5C33', 'FFFF3334', 'FFFF335C', 'FFFF3287',
'FFFF33AE', 'FFFF33D6', 'FFFE33FF', 'FFD633FE', 'FFAD34FF', 'FF8534FF', 'FF5D33FF', 'FF3233FF', 'FF325CFE', 'FF3285FF', 'FF33ADFF', 'FF33D6FF', 'FF33FFFE', 'FF32FFD6', 'FF34FFAD',
'FF33FF84', 'FF32FF5C', 'FF34FF33', 'FF5CFF34', 'FF85FE33', 'FFADFE33', 'FFD5FF33', 'FF7C7C7C', 'FFFFFF19', 'FFFFD119', 'FFFFA418', 'FFFF751A', 'FFFF4719', 'FFFF1919', 'FFFF1947',
'FFFF1874', 'FFFF19A3', 'FFFF19D1', 'FFFF19FF', 'FFD019FF', 'FFA219FF', 'FF751AFE', 'FF4719FF', 'FF1819FF', 'FF1947FF', 'FF1974FF', 'FF19A3FE', 'FF18D1FF', 'FF19FFFF', 'FF19FFD1',
'FF19FFA4', 'FF18FF75', 'FF19FF47', 'FF19FF19', 'FF48FF19', 'FF76FF19', 'FFA3FE1A', 'FFD1FF19', 'FF6B6B6B', 'FFFFFF00', 'FFFFCC00', 'FFFE9900', 'FFFF6600', 'FFFF3300', 'FFFE0000',
'FFFE0032', 'FFFF0066', 'FFFF0198', 'FFFF00CC', 'FFFF00FE', 'FFCC00FF', 'FF9A00FF', 'FF6601FF', 'FF3300FF', 'FF0000FE', 'FF0033FF', 'FF0166FF', 'FF0097FE', 'FF00CCFF', 'FF00FFFF',
'FF01FFCD', 'FF00FF99', 'FF00FE67', 'FF00FF33', 'FF00FF01', 'FF33FF00', 'FF65FF00', 'FF99FE00', 'FFCCFF00', 'FF5D5D5D', 'FFE8E500', 'FFE6B800', 'FFE68B00', 'FFE65C01', 'FFE72E00',
'FFE60000', 'FFE6002E', 'FFE6005B', 'FFE80183', 'FFE600B8', 'FFE600E6', 'FFB700E6', 'FF8900E6', 'FF5C01E5', 'FF2E00E6', 'FF0000E6', 'FF012EE1', 'FF005BE7', 'FF008AE5', 'FF00B8E6',
'FF00E6E6', 'FF00E6B7', 'FF00E78B', 'FF00E65F', 'FF00E532', 'FF00E600', 'FF2FE600', 'FF5DE600', 'FF8AE501', 'FFB8E600', 'FF4F4F4F', 'FFCDCC00', 'FFCDA301', 'FFCA7B02', 'FFCC5200',
'FFCC2900', 'FFCC0001', 'FFCD0029', 'FFCE0052', 'FFCC007B', 'FFCD00A3', 'FFCB00CC', 'FFA300CB', 'FF7A01CC', 'FF5201CC', 'FF2A00D0', 'FF0000CC', 'FF0029CB', 'FF0052CC', 'FF007ACD',
'FF00A3CC', 'FF00CCCB', 'FF00CCA3', 'FF01CC7A', 'FF03CB51', 'FF00CC29', 'FF01CC00', 'FF29CC01', 'FF52CB00', 'FF7ACB00', 'FFA2CC00', 'FF434343', 'FFB4B300', 'FFB38E00', 'FFB36B00',
'FFB34700', 'FFB32501', 'FFB30101', 'FFB40025', 'FFB40047', 'FFB4006B', 'FFB5008B', 'FFB300B3', 'FF8F00B2', 'FF6B00B2', 'FF4700B4', 'FF2300B2', 'FF0000B2', 'FF0025B4', 'FF0047B3',
'FF006BB3', 'FF008EB2', 'FF00B3B2', 'FF00B38E', 'FF00B36C', 'FF00B346', 'FF00B324', 'FF00B300', 'FF24B301', 'FF47B200', 'FF6CB201', 'FF90B301', 'FF373737', 'FF999A00', 'FF987A00',
'FF995C01', 'FF9A3D00', 'FF9A1F00', 'FF990100', 'FF99001F', 'FF9A003E', 'FF99005B', 'FF9A007A', 'FF990099', 'FF7B0099', 'FF5D0099', 'FF3D0099', 'FF1F0099', 'FF000098', 'FF011F99',
'FF003D98', 'FF005C99', 'FF007A99', 'FF009999', 'FF00997A', 'FF00995B', 'FF00993E', 'FF00991F', 'FF009900', 'FF1E9900', 'FF3C9900', 'FF5C9900', 'FF7A9900', 'FF2E2E2E', 'FF7F8000',
'FF7F6601', 'FF804C00', 'FF803201', 'FF801A01', 'FF800000', 'FF800019', 'FF800033', 'FF80004B', 'FF810065', 'FF81007F', 'FF660080', 'FF4C007F', 'FF33007F', 'FF1A0080', 'FF010080',
'FF011A7F', 'FF003480', 'FF004C80', 'FF00667F', 'FF008081', 'FF008067', 'FF037F4B', 'FF008033', 'FF00801B', 'FF008001', 'FF1A8000', 'FF338000', 'FF4C8001', 'FF668100', 'FF242424',
'FF656600', 'FF675201', 'FF653D00', 'FF672900', 'FF661400', 'FF660000', 'FF660015', 'FF660028', 'FF65003C', 'FF660053', 'FF660066', 'FF550069', 'FF3D0067', 'FF290066', 'FF150067',
'FF010066', 'FF001465', 'FF012966', 'FF003D66', 'FF005267', 'FF006766', 'FF006651', 'FF00663E', 'FF01662A', 'FF006613', 'FF006600', 'FF146600', 'FF296600', 'FF3D6600', 'FF516600',
'FF181818', 'FF4B4C00', 'FF4C3E01', 'FF4D2E00', 'FF4C1F00', 'FF4D0F00', 'FF4C0000', 'FF4C000F', 'FF4B001F', 'FF4C002E', 'FF4C003E', 'FF4C004B', 'FF3D004D', 'FF2E004B', 'FF1F004C',
'FF0E004B', 'FF01004C', 'FF000E4B', 'FF001F4D', 'FF012E4D', 'FF003D4C', 'FF004C4C', 'FF004D3D', 'FF004C2E', 'FF004C1E', 'FF004C0E', 'FF004C01', 'FF0F4C00', 'FF204C01', 'FF2D4C00',
'FF3E4C01', 'FF000000'
	]