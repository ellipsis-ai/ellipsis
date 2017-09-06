#!/usr/bin/python
import sys
import psycopg2
import pyhocon
from psycopg2.extensions import ISOLATION_LEVEL_AUTOCOMMIT

if (len(sys.argv) > 1):
    env = sys.argv[1]
else:
    env = "development"
if (env=="dev"):
    env = "development"

# from pyhocon import ConfigFactory
conf = pyhocon.ConfigFactory.parse_file("../conf/{0}.conf".format(env))

host = conf['slick.dbs.default.host']
port = conf['slick.dbs.default.port']
dbname = conf['slick.dbs.default.dbname']
username = conf['slick.dbs.default.username']
password = conf['slick.dbs.default.password']

conn_string = ("dbname='%s' user='%s' host='%s' password='%s'" % (dbname, username, host, password))

conn = None
try:
    conn = psycopg2.connect(conn_string)
    conn.set_isolation_level(ISOLATION_LEVEL_AUTOCOMMIT)
    cur = conn.cursor()
    cur.execute(("CREATE DATABASE %s;" % dbname))
    cur.execute("GRANT ALL PRIVILEGES ON DATABASE %s TO %s;" % (dbname,username))

except (Exception, psycopg2.DatabaseError) as error:
    print(error)
finally:
    if conn is not None:
        conn.close()
