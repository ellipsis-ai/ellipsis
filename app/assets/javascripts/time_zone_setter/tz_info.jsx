define(function() {
  /*
   Time zone data pulled from CLDR <http://cldr.unicode.org/index/downloads>

   COPYRIGHT AND PERMISSION NOTICE

   Copyright © 1991-2017 Unicode, Inc. All rights reserved.
   Distributed under the Terms of Use in http://www.unicode.org/copyright.html.

   Permission is hereby granted, free of charge, to any person obtaining
   a copy of the Unicode data files and any associated documentation
   (the "Data Files") or Unicode software and any associated documentation
   (the "Software") to deal in the Data Files or Software
   without restriction, including without limitation the rights to use,
   copy, modify, merge, publish, distribute, and/or sell copies of
   the Data Files or Software, and to permit persons to whom the Data Files
   or Software are furnished to do so, provided that either
   (a) this copyright and permission notice appear with all copies
   of the Data Files or Software, or
   (b) this copyright and permission notice appear in associated
   Documentation.

   THE DATA FILES AND SOFTWARE ARE PROVIDED "AS IS", WITHOUT WARRANTY OF
   ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
   WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
   NONINFRINGEMENT OF THIRD PARTY RIGHTS.
   IN NO EVENT SHALL THE COPYRIGHT HOLDER OR HOLDERS INCLUDED IN THIS
   NOTICE BE LIABLE FOR ANY CLAIM, OR ANY SPECIAL INDIRECT OR CONSEQUENTIAL
   DAMAGES, OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE,
   DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER
   TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
   PERFORMANCE OF THE DATA FILES OR SOFTWARE.

   Except as contained in this notice, the name of a copyright holder
   shall not be used in advertising or otherwise to promote the sale,
   use or other dealings in these Data Files or Software without prior
   written authorization of the copyright holder.
   */
  const tzNames = [{
    name: "Andorra",
    timeZones: ["Europe/Andorra"]
  }, {
    name: "Dubai, United Arab Emirates",
    timeZones: ["Asia/Dubai"]
  }, {
    name: "Kabul, Afghanistan",
    timeZones: ["Asia/Kabul"]
  }, {
    name: "Antigua",
    timeZones: ["America/Antigua"]
  }, {
    name: "Anguilla",
    timeZones: ["America/Anguilla"]
  }, {
    name: "Tirane, Albania",
    timeZones: ["Europe/Tirane"]
  }, {
    name: "Yerevan, Armenia",
    timeZones: ["Asia/Yerevan"]
  }, {
    name: "Curaçao",
    timeZones: ["America/Curacao"]
  }, {
    name: "Luanda, Angola",
    timeZones: ["Africa/Luanda"]
  }, {
    name: "Casey Station, Bailey Peninsula",
    timeZones: ["Antarctica/Casey"]
  }, {
    name: "Davis Station, Vestfold Hills",
    timeZones: ["Antarctica/Davis"]
  }, {
    name: "Dumont d'Urville Station, Terre Adélie",
    timeZones: ["Antarctica/DumontDUrville"]
  }, {
    name: "Mawson Station, Holme Bay",
    timeZones: ["Antarctica/Mawson"]
  }, {
    name: "McMurdo Station, Ross Island",
    timeZones: ["Antarctica/McMurdo"]
  }, {
    name: "Palmer Station, Anvers Island",
    timeZones: ["Antarctica/Palmer"]
  }, {
    name: "Rothera Station, Adelaide Island",
    timeZones: ["Antarctica/Rothera"]
  }, {
    name: "Syowa Station, East Ongul Island",
    timeZones: ["Antarctica/Syowa"]
  }, {
    name: "Troll Station, Queen Maud Land",
    timeZones: ["Antarctica/Troll"]
  }, {
    name: "Vostok Station, Lake Vostok",
    timeZones: ["Antarctica/Vostok"]
  }, {
    name: "Buenos Aires, Argentina",
    timeZones: ["America/Buenos_Aires", "America/Argentina/Buenos_Aires"]
  }, {
    name: "Córdoba, Argentina",
    timeZones: ["America/Cordoba", "America/Argentina/Cordoba", "America/Rosario"]
  }, {
    name: "Catamarca, Argentina",
    timeZones: ["America/Catamarca", "America/Argentina/Catamarca", "America/Argentina/ComodRivadavia"]
  }, {
    name: "La Rioja, Argentina",
    timeZones: ["America/Argentina/La_Rioja"]
  }, {
    name: "Jujuy, Argentina",
    timeZones: ["America/Jujuy", "America/Argentina/Jujuy"]
  }, {
    name: "San Luis, Argentina",
    timeZones: ["America/Argentina/San_Luis"]
  }, {
    name: "Mendoza, Argentina",
    timeZones: ["America/Mendoza", "America/Argentina/Mendoza"]
  }, {
    name: "Río Gallegos, Argentina",
    timeZones: ["America/Argentina/Rio_Gallegos"]
  }, {
    name: "Salta, Argentina",
    timeZones: ["America/Argentina/Salta"]
  }, {
    name: "Tucumán, Argentina",
    timeZones: ["America/Argentina/Tucuman"]
  }, {
    name: "San Juan, Argentina",
    timeZones: ["America/Argentina/San_Juan"]
  }, {
    name: "Ushuaia, Argentina",
    timeZones: ["America/Argentina/Ushuaia"]
  }, {
    name: "Pago Pago, American Samoa",
    timeZones: ["Pacific/Pago_Pago", "Pacific/Samoa", "US/Samoa"]
  }, {
    name: "Vienna, Austria",
    timeZones: ["Europe/Vienna"]
  }, {
    name: "Adelaide, Australia",
    timeZones: ["Australia/Adelaide", "Australia/South"]
  }, {
    name: "Broken Hill, Australia",
    timeZones: ["Australia/Broken_Hill", "Australia/Yancowinna"]
  }, {
    name: "Brisbane, Australia",
    timeZones: ["Australia/Brisbane", "Australia/Queensland"]
  }, {
    name: "Darwin, Australia",
    timeZones: ["Australia/Darwin", "Australia/North"]
  }, {
    name: "Eucla, Australia",
    timeZones: ["Australia/Eucla"]
  }, {
    name: "Hobart, Australia",
    timeZones: ["Australia/Hobart", "Australia/Tasmania"]
  }, {
    name: "Currie, Australia",
    timeZones: ["Australia/Currie"]
  }, {
    name: "Lindeman Island, Australia",
    timeZones: ["Australia/Lindeman"]
  }, {
    name: "Lord Howe Island, Australia",
    timeZones: ["Australia/Lord_Howe", "Australia/LHI"]
  }, {
    name: "Melbourne, Australia",
    timeZones: ["Australia/Melbourne", "Australia/Victoria"]
  }, {
    name: "Macquarie Island Station, Macquarie Island",
    timeZones: ["Antarctica/Macquarie"]
  }, {
    name: "Perth, Australia",
    timeZones: ["Australia/Perth", "Australia/West"]
  }, {
    name: "Sydney, Australia",
    timeZones: ["Australia/Sydney", "Australia/ACT", "Australia/Canberra", "Australia/NSW"]
  }, {
    name: "Aruba",
    timeZones: ["America/Aruba"]
  }, {
    name: "Baku, Azerbaijan",
    timeZones: ["Asia/Baku"]
  }, {
    name: "Sarajevo, Bosnia and Herzegovina",
    timeZones: ["Europe/Sarajevo"]
  }, {
    name: "Barbados",
    timeZones: ["America/Barbados"]
  }, {
    name: "Dhaka, Bangladesh",
    timeZones: ["Asia/Dhaka", "Asia/Dacca"]
  }, {
    name: "Brussels, Belgium",
    timeZones: ["Europe/Brussels"]
  }, {
    name: "Ouagadougou, Burkina Faso",
    timeZones: ["Africa/Ouagadougou"]
  }, {
    name: "Sofia, Bulgaria",
    timeZones: ["Europe/Sofia"]
  }, {
    name: "Bahrain",
    timeZones: ["Asia/Bahrain"]
  }, {
    name: "Bujumbura, Burundi",
    timeZones: ["Africa/Bujumbura"]
  }, {
    name: "Porto-Novo, Benin",
    timeZones: ["Africa/Porto-Novo"]
  }, {
    name: "Bermuda",
    timeZones: ["Atlantic/Bermuda"]
  }, {
    name: "Brunei",
    timeZones: ["Asia/Brunei"]
  }, {
    name: "La Paz, Bolivia",
    timeZones: ["America/La_Paz"]
  }, {
    name: "Bonaire, Sint Estatius and Saba",
    timeZones: ["America/Kralendijk"]
  }, {
    name: "Araguaína, Brazil",
    timeZones: ["America/Araguaina"]
  }, {
    name: "Belém, Brazil",
    timeZones: ["America/Belem"]
  }, {
    name: "Boa Vista, Brazil",
    timeZones: ["America/Boa_Vista"]
  }, {
    name: "Cuiabá, Brazil",
    timeZones: ["America/Cuiaba"]
  }, {
    name: "Campo Grande, Brazil",
    timeZones: ["America/Campo_Grande"]
  }, {
    name: "Eirunepé, Brazil",
    timeZones: ["America/Eirunepe"]
  }, {
    name: "Fernando de Noronha, Brazil",
    timeZones: ["America/Noronha", "Brazil/DeNoronha"]
  }, {
    name: "Fortaleza, Brazil",
    timeZones: ["America/Fortaleza"]
  }, {
    name: "Manaus, Brazil",
    timeZones: ["America/Manaus", "Brazil/West"]
  }, {
    name: "Maceió, Brazil",
    timeZones: ["America/Maceio"]
  }, {
    name: "Porto Velho, Brazil",
    timeZones: ["America/Porto_Velho"]
  }, {
    name: "Rio Branco, Brazil",
    timeZones: ["America/Rio_Branco", "America/Porto_Acre", "Brazil/Acre"]
  }, {
    name: "Recife, Brazil",
    timeZones: ["America/Recife"]
  }, {
    name: "São Paulo, Brazil",
    timeZones: ["America/Sao_Paulo", "Brazil/East"]
  }, {
    name: "Bahia, Brazil",
    timeZones: ["America/Bahia"]
  }, {
    name: "Santarém, Brazil",
    timeZones: ["America/Santarem"]
  }, {
    name: "Nassau, Bahamas",
    timeZones: ["America/Nassau"]
  }, {
    name: "Thimphu, Bhutan",
    timeZones: ["Asia/Thimphu", "Asia/Thimbu"]
  }, {
    name: "Gaborone, Botswana",
    timeZones: ["Africa/Gaborone"]
  }, {
    name: "Minsk, Belarus",
    timeZones: ["Europe/Minsk"]
  }, {
    name: "Belize",
    timeZones: ["America/Belize"]
  }, {
    name: "Creston, Canada",
    timeZones: ["America/Creston"]
  }, {
    name: "Edmonton, Canada",
    timeZones: ["America/Edmonton", "Canada/Mountain"]
  }, {
    name: "Rainy River, Canada",
    timeZones: ["America/Rainy_River"]
  }, {
    name: "Fort Nelson, Canada",
    timeZones: ["America/Fort_Nelson"]
  }, {
    name: "Glace Bay, Canada",
    timeZones: ["America/Glace_Bay"]
  }, {
    name: "Goose Bay, Canada",
    timeZones: ["America/Goose_Bay"]
  }, {
    name: "Halifax, Canada",
    timeZones: ["America/Halifax", "Canada/Atlantic"]
  }, {
    name: "Iqaluit, Canada",
    timeZones: ["America/Iqaluit"]
  }, {
    name: "Moncton, Canada",
    timeZones: ["America/Moncton"]
  }, {
    name: "Pangnirtung, Canada",
    timeZones: ["America/Pangnirtung"]
  }, {
    name: "Resolute, Canada",
    timeZones: ["America/Resolute"]
  }, {
    name: "Regina, Canada",
    timeZones: ["America/Regina", "Canada/East-Saskatchewan", "Canada/Saskatchewan"]
  }, {
    name: "St. John's, Canada",
    timeZones: ["America/St_Johns", "Canada/Newfoundland"]
  }, {
    name: "Nipigon, Canada",
    timeZones: ["America/Nipigon"]
  }, {
    name: "Thunder Bay, Canada",
    timeZones: ["America/Thunder_Bay"]
  }, {
    name: "Toronto, Canada",
    timeZones: ["America/Toronto", "Canada/Eastern"]
  }, {
    name: "Vancouver, Canada",
    timeZones: ["America/Vancouver", "Canada/Pacific"]
  }, {
    name: "Winnipeg, Canada",
    timeZones: ["America/Winnipeg", "Canada/Central"]
  }, {
    name: "Blanc-Sablon, Canada",
    timeZones: ["America/Blanc-Sablon"]
  }, {
    name: "Cambridge Bay, Canada",
    timeZones: ["America/Cambridge_Bay"]
  }, {
    name: "Dawson, Canada",
    timeZones: ["America/Dawson"]
  }, {
    name: "Dawson Creek, Canada",
    timeZones: ["America/Dawson_Creek"]
  }, {
    name: "Rankin Inlet, Canada",
    timeZones: ["America/Rankin_Inlet"]
  }, {
    name: "Inuvik, Canada",
    timeZones: ["America/Inuvik"]
  }, {
    name: "Whitehorse, Canada",
    timeZones: ["America/Whitehorse", "Canada/Yukon"]
  }, {
    name: "Swift Current, Canada",
    timeZones: ["America/Swift_Current"]
  }, {
    name: "Yellowknife, Canada",
    timeZones: ["America/Yellowknife"]
  }, {
    name: "Atikokan, Canada",
    timeZones: ["America/Coral_Harbour", "America/Atikokan"]
  }, {
    name: "Cocos (Keeling) Islands",
    timeZones: ["Indian/Cocos"]
  }, {
    name: "Lubumbashi, Democratic Republic of the Congo",
    timeZones: ["Africa/Lubumbashi"]
  }, {
    name: "Kinshasa, Democratic Republic of the Congo",
    timeZones: ["Africa/Kinshasa"]
  }, {
    name: "Bangui, Central African Republic",
    timeZones: ["Africa/Bangui"]
  }, {
    name: "Brazzaville, Republic of the Congo",
    timeZones: ["Africa/Brazzaville"]
  }, {
    name: "Zurich, Switzerland",
    timeZones: ["Europe/Zurich"]
  }, {
    name: "Abidjan, Côte d'Ivoire",
    timeZones: ["Africa/Abidjan"]
  }, {
    name: "Rarotonga, Cook Islands",
    timeZones: ["Pacific/Rarotonga"]
  }, {
    name: "Easter Island, Chile",
    timeZones: ["Pacific/Easter", "Chile/EasterIsland"]
  }, {
    name: "Punta Arenas, Chile",
    timeZones: ["America/Punta_Arenas"]
  }, {
    name: "Santiago, Chile",
    timeZones: ["America/Santiago", "Chile/Continental"]
  }, {
    name: "Douala, Cameroon",
    timeZones: ["Africa/Douala"]
  }, {
    name: "Shanghai, China",
    timeZones: ["Asia/Shanghai", "Asia/Chongqing", "Asia/Chungking", "Asia/Harbin", "PRC"]
  }, {
    name: "Ürümqi, China",
    timeZones: ["Asia/Urumqi", "Asia/Kashgar"]
  }, {
    name: "Bogotá, Colombia",
    timeZones: ["America/Bogota"]
  }, {
    name: "Costa Rica",
    timeZones: ["America/Costa_Rica"]
  // }, {
  //   name: "POSIX style time zone for US Central Time",
  //   timeZones: ["CST6CDT"]
  }, {
    name: "Havana, Cuba",
    timeZones: ["America/Havana", "Cuba"]
  }, {
    name: "Cape Verde",
    timeZones: ["Atlantic/Cape_Verde"]
  }, {
    name: "Christmas Island",
    timeZones: ["Indian/Christmas"]
  }, {
    name: "Famagusta, Cyprus",
    timeZones: ["Asia/Famagusta"]
  }, {
    name: "Nicosia, Cyprus",
    timeZones: ["Asia/Nicosia", "Europe/Nicosia"]
  }, {
    name: "Prague, Czech Republic",
    timeZones: ["Europe/Prague"]
  }, {
    name: "Berlin, Germany",
    timeZones: ["Europe/Berlin"]
  }, {
    name: "Busingen, Germany",
    timeZones: ["Europe/Busingen"]
  }, {
    name: "Djibouti",
    timeZones: ["Africa/Djibouti"]
  }, {
    name: "Copenhagen, Denmark",
    timeZones: ["Europe/Copenhagen"]
  }, {
    name: "Dominica",
    timeZones: ["America/Dominica"]
  }, {
    name: "Santo Domingo, Dominican Republic",
    timeZones: ["America/Santo_Domingo"]
  }, {
    name: "Algiers, Algeria",
    timeZones: ["Africa/Algiers"]
  }, {
    name: "Galápagos Islands, Ecuador",
    timeZones: ["Pacific/Galapagos"]
  }, {
    name: "Guayaquil, Ecuador",
    timeZones: ["America/Guayaquil"]
  }, {
    name: "Tallinn, Estonia",
    timeZones: ["Europe/Tallinn"]
  }, {
    name: "Cairo, Egypt",
    timeZones: ["Africa/Cairo", "Egypt"]
  }, {
    name: "El Aaiún, Western Sahara",
    timeZones: ["Africa/El_Aaiun"]
  }, {
    name: "Asmara, Eritrea",
    timeZones: ["Africa/Asmera", "Africa/Asmara"]
  }, {
    name: "Ceuta, Spain",
    timeZones: ["Africa/Ceuta"]
  }, {
    name: "Canary Islands, Spain",
    timeZones: ["Atlantic/Canary"]
  }, {
    name: "Madrid, Spain",
    timeZones: ["Europe/Madrid"]
  // }, {
  //   name: "POSIX style time zone for US Eastern Time",
  //   timeZones: ["EST5EDT"]
  }, {
    name: "Addis Ababa, Ethiopia",
    timeZones: ["Africa/Addis_Ababa"]
  }, {
    name: "Helsinki, Finland",
    timeZones: ["Europe/Helsinki"]
  }, {
    name: "Mariehamn, Åland, Finland",
    timeZones: ["Europe/Mariehamn"]
  }, {
    name: "Fiji",
    timeZones: ["Pacific/Fiji"]
  }, {
    name: "Stanley, Falkland Islands",
    timeZones: ["Atlantic/Stanley"]
  }, {
    name: "Kosrae, Micronesia",
    timeZones: ["Pacific/Kosrae"]
  }, {
    name: "Pohnpei, Micronesia",
    timeZones: ["Pacific/Ponape", "Pacific/Pohnpei"]
  }, {
    name: "Chuuk, Micronesia",
    timeZones: ["Pacific/Truk", "Pacific/Chuuk", "Pacific/Yap"]
  }, {
    name: "Faroe Islands",
    timeZones: ["Atlantic/Faeroe", "Atlantic/Faroe"]
  }, {
    name: "Paris, France",
    timeZones: ["Europe/Paris"]
  }, {
    name: "Libreville, Gabon",
    timeZones: ["Africa/Libreville"]
  }, {
    name: "Gaza Strip, Palestinian Territories",
    timeZones: ["Asia/Gaza"]
  }, {
    name: "London, United Kingdom",
    timeZones: ["Europe/London", "Europe/Belfast", "GB", "GB-Eire"]
  }, {
    name: "Grenada",
    timeZones: ["America/Grenada"]
  }, {
    name: "Tbilisi, Georgia",
    timeZones: ["Asia/Tbilisi"]
  }, {
    name: "Cayenne, French Guiana",
    timeZones: ["America/Cayenne"]
  }, {
    name: "Guernsey",
    timeZones: ["Europe/Guernsey"]
  }, {
    name: "Accra, Ghana",
    timeZones: ["Africa/Accra"]
  }, {
    name: "Gibraltar",
    timeZones: ["Europe/Gibraltar"]
  }, {
    name: "Danmarkshavn, Greenland",
    timeZones: ["America/Danmarkshavn"]
  }, {
    name: "Nuuk (Godthåb), Greenland",
    timeZones: ["America/Godthab"]
  }, {
    name: "Ittoqqortoormiit (Scoresbysund), Greenland",
    timeZones: ["America/Scoresbysund"]
  }, {
    name: "Qaanaaq (Thule), Greenland",
    timeZones: ["America/Thule"]
  }, {
    name: "Banjul, Gambia",
    timeZones: ["Africa/Banjul"]
  }, {
    name: "Greenwich Mean Time",
    timeZones: ["Etc/GMT", "Etc/GMT+0", "Etc/GMT-0", "Etc/GMT0", "Etc/Greenwich", "GMT", "GMT+0", "GMT-0", "GMT0", "Greenwich"]
  }, {
    name: "Conakry, Guinea",
    timeZones: ["Africa/Conakry"]
  }, {
    name: "Guadeloupe",
    timeZones: ["America/Guadeloupe"]
  }, {
    name: "Marigot, Saint Martin",
    timeZones: ["America/Marigot"]
  }, {
    name: "Saint Barthélemy",
    timeZones: ["America/St_Barthelemy"]
  }, {
    name: "Malabo, Equatorial Guinea",
    timeZones: ["Africa/Malabo"]
  }, {
    name: "Athens, Greece",
    timeZones: ["Europe/Athens"]
  }, {
    name: "South Georgia and the South Sandwich Islands",
    timeZones: ["Atlantic/South_Georgia"]
  }, {
    name: "Guatemala",
    timeZones: ["America/Guatemala"]
  }, {
    name: "Guam",
    timeZones: ["Pacific/Guam"]
  }, {
    name: "Bissau, Guinea-Bissau",
    timeZones: ["Africa/Bissau"]
  }, {
    name: "Guyana",
    timeZones: ["America/Guyana"]
  }, {
    name: "West Bank, Palestinian Territories",
    timeZones: ["Asia/Hebron"]
  }, {
    name: "Hong Kong SAR China",
    timeZones: ["Asia/Hong_Kong", "Hongkong"]
  }, {
    name: "Tegucigalpa, Honduras",
    timeZones: ["America/Tegucigalpa"]
  }, {
    name: "Zagreb, Croatia",
    timeZones: ["Europe/Zagreb"]
  }, {
    name: "Port-au-Prince, Haiti",
    timeZones: ["America/Port-au-Prince"]
  }, {
    name: "Budapest, Hungary",
    timeZones: ["Europe/Budapest"]
  }, {
    name: "Jayapura, Indonesia",
    timeZones: ["Asia/Jayapura"]
  }, {
    name: "Jakarta, Indonesia",
    timeZones: ["Asia/Jakarta"]
  }, {
    name: "Makassar, Indonesia",
    timeZones: ["Asia/Makassar", "Asia/Ujung_Pandang"]
  }, {
    name: "Pontianak, Indonesia",
    timeZones: ["Asia/Pontianak"]
  }, {
    name: "Dublin, Ireland",
    timeZones: ["Europe/Dublin", "Eire"]
  }, {
    name: "Isle of Man",
    timeZones: ["Europe/Isle_of_Man"]
  }, {
    name: "Kolkata, India",
    timeZones: ["Asia/Calcutta", "Asia/Kolkata"]
  }, {
    name: "Chagos Archipelago",
    timeZones: ["Indian/Chagos"]
  }, {
    name: "Baghdad, Iraq",
    timeZones: ["Asia/Baghdad"]
  }, {
    name: "Tehran, Iran",
    timeZones: ["Asia/Tehran", "Iran"]
  }, {
    name: "Reykjavik, Iceland",
    timeZones: ["Atlantic/Reykjavik", "Iceland"]
  }, {
    name: "Rome, Italy",
    timeZones: ["Europe/Rome"]
  }, {
    name: "Jerusalem",
    timeZones: ["Asia/Jerusalem", "Asia/Tel_Aviv", "Israel"]
  }, {
    name: "Jersey",
    timeZones: ["Europe/Jersey"]
  }, {
    name: "Jamaica",
    timeZones: ["America/Jamaica", "Jamaica"]
  }, {
    name: "Amman, Jordan",
    timeZones: ["Asia/Amman"]
  }, {
    name: "Tokyo, Japan",
    timeZones: ["Asia/Tokyo", "Japan"]
  }, {
    name: "Nairobi, Kenya",
    timeZones: ["Africa/Nairobi"]
  }, {
    name: "Bishkek, Kyrgyzstan",
    timeZones: ["Asia/Bishkek"]
  }, {
    name: "Phnom Penh, Cambodia",
    timeZones: ["Asia/Phnom_Penh"]
  }, {
    name: "Kiritimati, Kiribati",
    timeZones: ["Pacific/Kiritimati"]
  }, {
    name: "Enderbury Island, Kiribati",
    timeZones: ["Pacific/Enderbury"]
  }, {
    name: "Tarawa, Kiribati",
    timeZones: ["Pacific/Tarawa"]
  }, {
    name: "Comoros",
    timeZones: ["Indian/Comoro"]
  }, {
    name: "Saint Kitts",
    timeZones: ["America/St_Kitts"]
  }, {
    name: "Pyongyang, North Korea",
    timeZones: ["Asia/Pyongyang"]
  }, {
    name: "Seoul, South Korea",
    timeZones: ["Asia/Seoul", "ROK"]
  }, {
    name: "Kuwait",
    timeZones: ["Asia/Kuwait"]
  }, {
    name: "Cayman Islands",
    timeZones: ["America/Cayman"]
  }, {
    name: "Aqtau, Kazakhstan",
    timeZones: ["Asia/Aqtau"]
  }, {
    name: "Aqtobe, Kazakhstan",
    timeZones: ["Asia/Aqtobe"]
  }, {
    name: "Almaty, Kazakhstan",
    timeZones: ["Asia/Almaty"]
  }, {
    name: "Atyrau (Guryev), Kazakhstan",
    timeZones: ["Asia/Atyrau"]
  }, {
    name: "Kyzylorda, Kazakhstan",
    timeZones: ["Asia/Qyzylorda"]
  }, {
    name: "Oral, Kazakhstan",
    timeZones: ["Asia/Oral"]
  }, {
    name: "Vientiane, Laos",
    timeZones: ["Asia/Vientiane"]
  }, {
    name: "Beirut, Lebanon",
    timeZones: ["Asia/Beirut"]
  }, {
    name: "Saint Lucia",
    timeZones: ["America/St_Lucia"]
  }, {
    name: "Vaduz, Liechtenstein",
    timeZones: ["Europe/Vaduz"]
  }, {
    name: "Colombo, Sri Lanka",
    timeZones: ["Asia/Colombo"]
  }, {
    name: "Monrovia, Liberia",
    timeZones: ["Africa/Monrovia"]
  }, {
    name: "Maseru, Lesotho",
    timeZones: ["Africa/Maseru"]
  }, {
    name: "Vilnius, Lithuania",
    timeZones: ["Europe/Vilnius"]
  }, {
    name: "Luxembourg",
    timeZones: ["Europe/Luxembourg"]
  }, {
    name: "Riga, Latvia",
    timeZones: ["Europe/Riga"]
  }, {
    name: "Tripoli, Libya",
    timeZones: ["Africa/Tripoli", "Libya"]
  }, {
    name: "Casablanca, Morocco",
    timeZones: ["Africa/Casablanca"]
  }, {
    name: "Monaco",
    timeZones: ["Europe/Monaco"]
  }, {
    name: "Chişinău, Moldova",
    timeZones: ["Europe/Chisinau", "Europe/Tiraspol"]
  }, {
    name: "Podgorica, Montenegro",
    timeZones: ["Europe/Podgorica"]
  }, {
    name: "Antananarivo, Madagascar",
    timeZones: ["Indian/Antananarivo"]
  }, {
    name: "Kwajalein, Marshall Islands",
    timeZones: ["Pacific/Kwajalein", "Kwajalein"]
  }, {
    name: "Majuro, Marshall Islands",
    timeZones: ["Pacific/Majuro"]
  }, {
    name: "Skopje, Macedonia",
    timeZones: ["Europe/Skopje"]
  }, {
    name: "Bamako, Mali",
    timeZones: ["Africa/Bamako", "Africa/Timbuktu"]
  }, {
    name: "Yangon (Rangoon), Burma",
    timeZones: ["Asia/Rangoon", "Asia/Yangon"]
  }, {
    name: "Choibalsan, Mongolia",
    timeZones: ["Asia/Choibalsan"]
  }, {
    name: "Khovd (Hovd), Mongolia",
    timeZones: ["Asia/Hovd"]
  }, {
    name: "Ulaanbaatar (Ulan Bator), Mongolia",
    timeZones: ["Asia/Ulaanbaatar", "Asia/Ulan_Bator"]
  }, {
    name: "Macau SAR China",
    timeZones: ["Asia/Macau", "Asia/Macao"]
  }, {
    name: "Saipan, Northern Mariana Islands",
    timeZones: ["Pacific/Saipan"]
  }, {
    name: "Martinique",
    timeZones: ["America/Martinique"]
  }, {
    name: "Nouakchott, Mauritania",
    timeZones: ["Africa/Nouakchott"]
  }, {
    name: "Montserrat",
    timeZones: ["America/Montserrat"]
  // }, {
  //   name: "POSIX style time zone for US Mountain Time",
  //   timeZones: ["MST7MDT"]
  }, {
    name: "Malta",
    timeZones: ["Europe/Malta"]
  }, {
    name: "Mauritius",
    timeZones: ["Indian/Mauritius"]
  }, {
    name: "Maldives",
    timeZones: ["Indian/Maldives"]
  }, {
    name: "Blantyre, Malawi",
    timeZones: ["Africa/Blantyre"]
  }, {
    name: "Chihuahua, Mexico",
    timeZones: ["America/Chihuahua"]
  }, {
    name: "Cancún, Mexico",
    timeZones: ["America/Cancun"]
  }, {
    name: "Hermosillo, Mexico",
    timeZones: ["America/Hermosillo"]
  }, {
    name: "Matamoros, Mexico",
    timeZones: ["America/Matamoros"]
  }, {
    name: "Mexico City, Mexico",
    timeZones: ["America/Mexico_City", "Mexico/General"]
  }, {
    name: "Mérida, Mexico",
    timeZones: ["America/Merida"]
  }, {
    name: "Monterrey, Mexico",
    timeZones: ["America/Monterrey"]
  }, {
    name: "Mazatlán, Mexico",
    timeZones: ["America/Mazatlan", "Mexico/BajaSur"]
  }, {
    name: "Ojinaga, Mexico",
    timeZones: ["America/Ojinaga"]
  }, {
    name: "Bahía de Banderas, Mexico",
    timeZones: ["America/Bahia_Banderas"]
  }, {
    name: "Santa Isabel (Baja California), Mexico",
    timeZones: ["America/Santa_Isabel"]
  }, {
    name: "Tijuana, Mexico",
    timeZones: ["America/Tijuana", "America/Ensenada", "Mexico/BajaNorte"]
  }, {
    name: "Kuching, Malaysia",
    timeZones: ["Asia/Kuching"]
  }, {
    name: "Kuala Lumpur, Malaysia",
    timeZones: ["Asia/Kuala_Lumpur"]
  }, {
    name: "Maputo, Mozambique",
    timeZones: ["Africa/Maputo"]
  }, {
    name: "Windhoek, Namibia",
    timeZones: ["Africa/Windhoek"]
  }, {
    name: "Noumea, New Caledonia",
    timeZones: ["Pacific/Noumea"]
  }, {
    name: "Niamey, Niger",
    timeZones: ["Africa/Niamey"]
  }, {
    name: "Norfolk Island",
    timeZones: ["Pacific/Norfolk"]
  }, {
    name: "Lagos, Nigeria",
    timeZones: ["Africa/Lagos"]
  }, {
    name: "Managua, Nicaragua",
    timeZones: ["America/Managua"]
  }, {
    name: "Amsterdam, Netherlands",
    timeZones: ["Europe/Amsterdam"]
  }, {
    name: "Oslo, Norway",
    timeZones: ["Europe/Oslo"]
  }, {
    name: "Kathmandu, Nepal",
    timeZones: ["Asia/Katmandu", "Asia/Kathmandu"]
  }, {
    name: "Nauru",
    timeZones: ["Pacific/Nauru"]
  }, {
    name: "Niue",
    timeZones: ["Pacific/Niue"]
  }, {
    name: "Auckland, New Zealand",
    timeZones: ["Pacific/Auckland", "Antarctica/South_Pole", "NZ"]
  }, {
    name: "Chatham Islands, New Zealand",
    timeZones: ["Pacific/Chatham", "NZ-CHAT"]
  }, {
    name: "Muscat, Oman",
    timeZones: ["Asia/Muscat"]
  }, {
    name: "Panama",
    timeZones: ["America/Panama"]
  }, {
    name: "Lima, Peru",
    timeZones: ["America/Lima"]
  }, {
    name: "Gambiera Islands, French Polynesia",
    timeZones: ["Pacific/Gambier"]
  }, {
    name: "Marquesas Islands, French Polynesia",
    timeZones: ["Pacific/Marquesas"]
  }, {
    name: "Tahiti, French Polynesia",
    timeZones: ["Pacific/Tahiti"]
  }, {
    name: "Port Moresby, Papua New Guinea",
    timeZones: ["Pacific/Port_Moresby"]
  }, {
    name: "Bougainville, Papua New Guinea",
    timeZones: ["Pacific/Bougainville"]
  }, {
    name: "Manila, Philippines",
    timeZones: ["Asia/Manila"]
  }, {
    name: "Karachi, Pakistan",
    timeZones: ["Asia/Karachi"]
  }, {
    name: "Warsaw, Poland",
    timeZones: ["Europe/Warsaw", "Poland"]
  }, {
    name: "Saint Pierre and Miquelon",
    timeZones: ["America/Miquelon"]
  }, {
    name: "Pitcairn Islands",
    timeZones: ["Pacific/Pitcairn"]
  }, {
    name: "Puerto Rico",
    timeZones: ["America/Puerto_Rico"]
  // }, {
  //   name: "POSIX style time zone for US Pacific Time",
  //   timeZones: ["PST8PDT"]
  }, {
    name: "Madeira, Portugal",
    timeZones: ["Atlantic/Madeira"]
  }, {
    name: "Lisbon, Portugal",
    timeZones: ["Europe/Lisbon", "Portugal"]
  }, {
    name: "Azores, Portugal",
    timeZones: ["Atlantic/Azores"]
  }, {
    name: "Palau",
    timeZones: ["Pacific/Palau"]
  }, {
    name: "Asunción, Paraguay",
    timeZones: ["America/Asuncion"]
  }, {
    name: "Qatar",
    timeZones: ["Asia/Qatar"]
  }, {
    name: "Réunion",
    timeZones: ["Indian/Reunion"]
  }, {
    name: "Bucharest, Romania",
    timeZones: ["Europe/Bucharest"]
  }, {
    name: "Belgrade, Serbia",
    timeZones: ["Europe/Belgrade"]
  }, {
    name: "Astrakhan, Russia",
    timeZones: ["Europe/Astrakhan"]
  }, {
    name: "Barnaul, Russia",
    timeZones: ["Asia/Barnaul"]
  }, {
    name: "Chita Zabaykalsky, Russia",
    timeZones: ["Asia/Chita"]
  }, {
    name: "Anadyr, Russia",
    timeZones: ["Asia/Anadyr"]
  }, {
    name: "Magadan, Russia",
    timeZones: ["Asia/Magadan"]
  }, {
    name: "Irkutsk, Russia",
    timeZones: ["Asia/Irkutsk"]
  }, {
    name: "Kaliningrad, Russia",
    timeZones: ["Europe/Kaliningrad"]
  }, {
    name: "Khandyga Tomponsky, Russia",
    timeZones: ["Asia/Khandyga"]
  }, {
    name: "Krasnoyarsk, Russia",
    timeZones: ["Asia/Krasnoyarsk"]
  }, {
    name: "Samara, Russia",
    timeZones: ["Europe/Samara"]
  }, {
    name: "Kirov, Russia",
    timeZones: ["Europe/Kirov"]
  }, {
    name: "Moscow, Russia",
    timeZones: ["Europe/Moscow", "W-SU"]
  }, {
    name: "Novokuznetsk, Russia",
    timeZones: ["Asia/Novokuznetsk"]
  }, {
    name: "Omsk, Russia",
    timeZones: ["Asia/Omsk"]
  }, {
    name: "Novosibirsk, Russia",
    timeZones: ["Asia/Novosibirsk"]
  }, {
    name: "Kamchatka Peninsula, Russia",
    timeZones: ["Asia/Kamchatka"]
  }, {
    name: "Saratov, Russia",
    timeZones: ["Europe/Saratov"]
  }, {
    name: "Srednekolymsk, Russia",
    timeZones: ["Asia/Srednekolymsk"]
  }, {
    name: "Tomsk, Russia",
    timeZones: ["Asia/Tomsk"]
  }, {
    name: "Ulyanovsk, Russia",
    timeZones: ["Europe/Ulyanovsk"]
  }, {
    name: "Ust-Nera Oymyakonsky, Russia",
    timeZones: ["Asia/Ust-Nera"]
  }, {
    name: "Sakhalin, Russia",
    timeZones: ["Asia/Sakhalin"]
  }, {
    name: "Volgograd, Russia",
    timeZones: ["Europe/Volgograd"]
  }, {
    name: "Vladivostok, Russia",
    timeZones: ["Asia/Vladivostok"]
  }, {
    name: "Yekaterinburg, Russia",
    timeZones: ["Asia/Yekaterinburg"]
  }, {
    name: "Yakutsk, Russia",
    timeZones: ["Asia/Yakutsk"]
  }, {
    name: "Kigali, Rwanda",
    timeZones: ["Africa/Kigali"]
  }, {
    name: "Riyadh, Saudi Arabia",
    timeZones: ["Asia/Riyadh"]
  }, {
    name: "Guadalcanal, Solomon Islands",
    timeZones: ["Pacific/Guadalcanal"]
  }, {
    name: "Mahé, Seychelles",
    timeZones: ["Indian/Mahe"]
  }, {
    name: "Khartoum, Sudan",
    timeZones: ["Africa/Khartoum"]
  }, {
    name: "Stockholm, Sweden",
    timeZones: ["Europe/Stockholm"]
  }, {
    name: "Singapore",
    timeZones: ["Asia/Singapore", "Singapore"]
  }, {
    name: "Saint Helena",
    timeZones: ["Atlantic/St_Helena"]
  }, {
    name: "Ljubljana, Slovenia",
    timeZones: ["Europe/Ljubljana"]
  }, {
    name: "Longyearbyen, Svalbard",
    timeZones: ["Arctic/Longyearbyen", "Atlantic/Jan_Mayen"]
  }, {
    name: "Bratislava, Slovakia",
    timeZones: ["Europe/Bratislava"]
  }, {
    name: "Freetown, Sierra Leone",
    timeZones: ["Africa/Freetown"]
  }, {
    name: "San Marino",
    timeZones: ["Europe/San_Marino"]
  }, {
    name: "Dakar, Senegal",
    timeZones: ["Africa/Dakar"]
  }, {
    name: "Mogadishu, Somalia",
    timeZones: ["Africa/Mogadishu"]
  }, {
    name: "Paramaribo, Suriname",
    timeZones: ["America/Paramaribo"]
  }, {
    name: "Juba, South Sudan",
    timeZones: ["Africa/Juba"]
  }, {
    name: "São Tomé, São Tomé and Príncipe",
    timeZones: ["Africa/Sao_Tome"]
  }, {
    name: "El Salvador",
    timeZones: ["America/El_Salvador"]
  }, {
    name: "Sint Maarten",
    timeZones: ["America/Lower_Princes"]
  }, {
    name: "Damascus, Syria",
    timeZones: ["Asia/Damascus"]
  }, {
    name: "Mbabane, Swaziland",
    timeZones: ["Africa/Mbabane"]
  }, {
    name: "Grand Turk, Turks and Caicos Islands",
    timeZones: ["America/Grand_Turk"]
  }, {
    name: "N'Djamena, Chad",
    timeZones: ["Africa/Ndjamena"]
  }, {
    name: "Kerguelen Islands, French Southern Territories",
    timeZones: ["Indian/Kerguelen"]
  }, {
    name: "Lomé, Togo",
    timeZones: ["Africa/Lome"]
  }, {
    name: "Bangkok, Thailand",
    timeZones: ["Asia/Bangkok"]
  }, {
    name: "Dushanbe, Tajikistan",
    timeZones: ["Asia/Dushanbe"]
  }, {
    name: "Fakaofo, Tokelau",
    timeZones: ["Pacific/Fakaofo"]
  }, {
    name: "Dili, East Timor",
    timeZones: ["Asia/Dili"]
  }, {
    name: "Ashgabat, Turkmenistan",
    timeZones: ["Asia/Ashgabat", "Asia/Ashkhabad"]
  }, {
    name: "Tunis, Tunisia",
    timeZones: ["Africa/Tunis"]
  }, {
    name: "Tongatapu, Tonga",
    timeZones: ["Pacific/Tongatapu"]
  }, {
    name: "Istanbul, Turkey",
    timeZones: ["Europe/Istanbul", "Asia/Istanbul", "Turkey"]
  }, {
    name: "Port of Spain, Trinidad and Tobago",
    timeZones: ["America/Port_of_Spain"]
  }, {
    name: "Funafuti, Tuvalu",
    timeZones: ["Pacific/Funafuti"]
  }, {
    name: "Taipei, Taiwan",
    timeZones: ["Asia/Taipei", "ROC"]
  }, {
    name: "Dar es Salaam, Tanzania",
    timeZones: ["Africa/Dar_es_Salaam"]
  }, {
    name: "Kiev, Ukraine",
    timeZones: ["Europe/Kiev"]
  }, {
    name: "Zaporizhia (Zaporozhye), Ukraine",
    timeZones: ["Europe/Zaporozhye"]
  }, {
    name: "Simferopol, Ukraine",
    timeZones: ["Europe/Simferopol"]
  }, {
    name: "Uzhhorod (Uzhgorod), Ukraine",
    timeZones: ["Europe/Uzhgorod"]
  }, {
    name: "Kampala, Uganda",
    timeZones: ["Africa/Kampala"]
  }, {
    name: "Wake Island, U.S. Minor Outlying Islands",
    timeZones: ["Pacific/Wake"]
  }, {
    name: "Johnston Atoll, U.S. Minor Outlying Islands",
    timeZones: ["Pacific/Johnston"]
  }, {
    name: "Midway Islands, U.S. Minor Outlying Islands",
    timeZones: ["Pacific/Midway"]
  }, {
    name: "Unknown time zone",
    timeZones: ["Etc/Unknown"]
  }, {
    name: "Adak (Alaska), United States",
    timeZones: ["America/Adak", "America/Atka", "US/Aleutian"]
  }, {
    name: "Marengo (Indiana), United States",
    timeZones: ["America/Indiana/Marengo"]
  }, {
    name: "Anchorage, United States",
    timeZones: ["America/Anchorage", "US/Alaska"]
  }, {
    name: "Boise (Idaho), United States",
    timeZones: ["America/Boise"]
  }, {
    name: "Chicago, United States",
    timeZones: ["America/Chicago", "US/Central"]
  }, {
    name: "Denver, United States",
    timeZones: ["America/Denver", "America/Shiprock", "Navajo", "US/Mountain"]
  }, {
    name: "Detroit, United States",
    timeZones: ["America/Detroit", "US/Michigan"]
  }, {
    name: "Honolulu, United States",
    timeZones: ["Pacific/Honolulu", "US/Hawaii"]
  }, {
    name: "Indianapolis, United States",
    timeZones: ["America/Indianapolis", "America/Fort_Wayne", "America/Indiana/Indianapolis", "US/East-Indiana"]
  }, {
    name: "Vevay (Indiana), United States",
    timeZones: ["America/Indiana/Vevay"]
  }, {
    name: "Juneau (Alaska), United States",
    timeZones: ["America/Juneau"]
  }, {
    name: "Knox (Indiana), United States",
    timeZones: ["America/Indiana/Knox", "America/Knox_IN", "US/Indiana-Starke"]
  }, {
    name: "Los Angeles, United States",
    timeZones: ["America/Los_Angeles", "US/Pacific", "US/Pacific-New"]
  }, {
    name: "Louisville (Kentucky), United States",
    timeZones: ["America/Louisville", "America/Kentucky/Louisville"]
  }, {
    name: "Menominee (Michigan), United States",
    timeZones: ["America/Menominee"]
  }, {
    name: "Metlakatla (Alaska), United States",
    timeZones: ["America/Metlakatla"]
  }, {
    name: "Monticello (Kentucky), United States",
    timeZones: ["America/Kentucky/Monticello"]
  }, {
    name: "Center (North Dakota), United States",
    timeZones: ["America/North_Dakota/Center"]
  }, {
    name: "New Salem (North Dakota), United States",
    timeZones: ["America/North_Dakota/New_Salem"]
  }, {
    name: "New York, United States",
    timeZones: ["America/New_York", "US/Eastern"]
  }, {
    name: "Vincennes (Indiana), United States",
    timeZones: ["America/Indiana/Vincennes"]
  }, {
    name: "Nome (Alaska), United States",
    timeZones: ["America/Nome"]
  }, {
    name: "Phoenix, United States",
    timeZones: ["America/Phoenix", "US/Arizona"]
  }, {
    name: "Sitka (Alaska), United States",
    timeZones: ["America/Sitka"]
  }, {
    name: "Tell City (Indiana), United States",
    timeZones: ["America/Indiana/Tell_City"]
  }, {
    name: "Winamac (Indiana), United States",
    timeZones: ["America/Indiana/Winamac"]
  }, {
    name: "Petersburg (Indiana), United States",
    timeZones: ["America/Indiana/Petersburg"]
  }, {
    name: "Beulah (North Dakota), United States",
    timeZones: ["America/North_Dakota/Beulah"]
  }, {
    name: "Yakutat (Alaska), United States",
    timeZones: ["America/Yakutat"]
  }, {
    name: "UTC (Coordinated Universal Time)",
    timeZones: ["Etc/UTC", "Etc/UCT", "Etc/Universal", "Etc/Zulu", "UCT", "UTC", "Universal", "Zulu"]
  }, {
    name: "1 hour ahead of UTC",
    timeZones: ["Etc/GMT-1"]
  }, {
    name: "2 hours ahead of UTC",
    timeZones: ["Etc/GMT-2"]
  }, {
    name: "3 hours ahead of UTC",
    timeZones: ["Etc/GMT-3"]
  }, {
    name: "4 hours ahead of UTC",
    timeZones: ["Etc/GMT-4"]
  }, {
    name: "5 hours ahead of UTC",
    timeZones: ["Etc/GMT-5"]
  }, {
    name: "6 hours ahead of UTC",
    timeZones: ["Etc/GMT-6"]
  }, {
    name: "7 hours ahead of UTC",
    timeZones: ["Etc/GMT-7"]
  }, {
    name: "8 hours ahead of UTC",
    timeZones: ["Etc/GMT-8"]
  }, {
    name: "9 hours ahead of UTC",
    timeZones: ["Etc/GMT-9"]
  }, {
    name: "10 hours ahead of UTC",
    timeZones: ["Etc/GMT-10"]
  }, {
    name: "11 hours ahead of UTC",
    timeZones: ["Etc/GMT-11"]
  }, {
    name: "12 hours ahead of UTC",
    timeZones: ["Etc/GMT-12"]
  }, {
    name: "13 hours ahead of UTC",
    timeZones: ["Etc/GMT-13"]
  }, {
    name: "14 hours ahead of UTC",
    timeZones: ["Etc/GMT-14"]
  }, {
    name: "1 hour behind UTC",
    timeZones: ["Etc/GMT+1"]
  }, {
    name: "2 hours behind UTC",
    timeZones: ["Etc/GMT+2"]
  }, {
    name: "3 hours behind UTC",
    timeZones: ["Etc/GMT+3"]
  }, {
    name: "4 hours behind UTC",
    timeZones: ["Etc/GMT+4"]
  }, {
    name: "5 hours behind UTC",
    timeZones: ["Etc/GMT+5", "EST"]
  }, {
    name: "6 hours behind UTC",
    timeZones: ["Etc/GMT+6"]
  }, {
    name: "7 hours behind UTC",
    timeZones: ["Etc/GMT+7", "MST"]
  }, {
    name: "8 hours behind UTC",
    timeZones: ["Etc/GMT+8"]
  }, {
    name: "9 hours behind UTC",
    timeZones: ["Etc/GMT+9"]
  }, {
    name: "10 hours behind UTC",
    timeZones: ["Etc/GMT+10", "HST"]
  }, {
    name: "11 hours behind UTC",
    timeZones: ["Etc/GMT+11"]
  }, {
    name: "12 hours behind UTC",
    timeZones: ["Etc/GMT+12"]
  }, {
    name: "Montevideo, Uruguay",
    timeZones: ["America/Montevideo"]
  }, {
    name: "Samarkand, Uzbekistan",
    timeZones: ["Asia/Samarkand"]
  }, {
    name: "Tashkent, Uzbekistan",
    timeZones: ["Asia/Tashkent"]
  }, {
    name: "Vatican City",
    timeZones: ["Europe/Vatican"]
  }, {
    name: "Saint Vincent, Saint Vincent and the Grenadines",
    timeZones: ["America/St_Vincent"]
  }, {
    name: "Caracas, Venezuela",
    timeZones: ["America/Caracas"]
  }, {
    name: "Tortola, British Virgin Islands",
    timeZones: ["America/Tortola"]
  }, {
    name: "Saint Thomas, U.S. Virgin Islands",
    timeZones: ["America/St_Thomas", "America/Virgin"]
  }, {
    name: "Ho Chi Minh City, Vietnam",
    timeZones: ["Asia/Saigon", "Asia/Ho_Chi_Minh"]
  }, {
    name: "Efate, Vanuatu",
    timeZones: ["Pacific/Efate"]
  }, {
    name: "Wallis Islands, Wallis and Futuna",
    timeZones: ["Pacific/Wallis"]
  }, {
    name: "Apia, Samoa",
    timeZones: ["Pacific/Apia"]
  }, {
    name: "Aden, Yemen",
    timeZones: ["Asia/Aden"]
  }, {
    name: "Mayotte",
    timeZones: ["Indian/Mayotte"]
  }, {
    name: "Johannesburg, South Africa",
    timeZones: ["Africa/Johannesburg"]
  }, {
    name: "Lusaka, Zambia",
    timeZones: ["Africa/Lusaka"]
  }, {
    name: "Harare, Zimbabwe",
    timeZones: ["Africa/Harare"]
  }];

  return tzNames;
});
