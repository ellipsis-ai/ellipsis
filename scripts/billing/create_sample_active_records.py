#!/usr/bin/python

import psycopg2
import datetime
import string, random


def insert_team(cur, id, name):
  sql = """INSERT INTO public.teams(id, name, time_zone, created_at, organization_id) VALUES (%s, %s, %s, %s, %s);"""
  cur.execute(sql, (id, name, None, datetime.datetime.now(), None))

def insert_user(cur, id, team_id):
  sql = """INSERT INTO public.users(id, team_id, email) VALUES (%s, %s, %s);"""
  email = "pippo" + ''.join(random.sample(string.ascii_lowercase, 5)) + "@m.com"
  cur.execute(sql, (id, team_id, email))

def insert_active_user_record(cur, id, user_id):
  sql = """INSERT INTO public.active_user_records(id, user_id, created_at) VALUES (%s, %s, %s);"""
  cur.execute(sql, (id, user_id, datetime.datetime.now()))


data = [
  { 't_name': "pippo", 'member_count': 10, 'active_last_30_days': 5 },
  { 't_name': "pippo2", 'member_count': 20, 'active_last_30_days': 15 }
  ]

conn = psycopg2.connect("host=localhost dbname=ellipsis user=ellipsis password=ellipsis")

new_team_ids = []
new_user_ids = []

for r in data:
  team_id = ''.join(random.sample(string.ascii_lowercase, 12))
  insert_team(conn.cursor(), team_id, r['t_name'])
  conn.commit()
  new_team_ids.append(team_id)
  print(new_team_ids)

  for i in range(r['member_count']):
    user_id = ''.join(random.sample(string.ascii_lowercase, 12))
    insert_user(conn.cursor(), user_id, team_id)
    new_user_ids.append(user_id)
  conn.commit()
  print(new_user_ids)

  active_users = new_user_ids[:(r['active_last_30_days'])]
  print(active_users)
  for user_id in active_users:
    aur_id = ''.join(random.sample(string.ascii_lowercase, 12))
    insert_active_user_record(conn.cursor(), aur_id, user_id)
  conn.commit()

conn.close()





