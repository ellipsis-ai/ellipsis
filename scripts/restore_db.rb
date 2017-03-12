# The script assumes you created an ssh tunnel to the production rds

# Source DB
src_db_name = 'ellipsis'
src_db_host = 'localhost'
src_db_user = 'ellipsis'
src_db_port = '5555'
src_db_pwd  = ENV['SRC_DB_PWD']

unless src_db_pwd
  puts ""
  puts "Usage: SRC_DB_PWD=production_db_pwd> ruby restore_db.rb"
  puts ""
  abort
end

# Target DB
target_db_name = 'ellipsis'
target_db_host = 'localhost'
target_db_user = 'ellipsis'
target_db_port = '5432'
target_db_pwd  = 'ellipsis'

# DO not buffer when writting to STDOUT
STDOUT.sync = true

t = Time.now.strftime('%Y%m%d%H%M')
file_path =   "/tmp/#{t}_backup.postgres"

dump_cmd = "PGPASSWORD=#{src_db_pwd} pg_dump --host #{src_db_host} --port #{src_db_port} --username #{src_db_user} --no-password  --format custom --blobs --file #{file_path} #{src_db_name}"
drop_cmd = "PGPASSWORD=#{target_db_pwd} dropdb --host #{target_db_host} --port #{target_db_port} -U #{target_db_user} --no-password #{target_db_name}"
create_cmd = "PGPASSWORD=#{target_db_pwd} createdb --host #{target_db_host} --port #{target_db_port} -U #{target_db_user} --no-password --owner=#{target_db_user} #{target_db_name}"
restore_cmd = "PGPASSWORD=#{target_db_pwd} pg_restore --host #{target_db_host} --port #{target_db_port} -U #{target_db_user} --no-password --dbname=\"#{target_db_name}\" --no-owner --verbose #{file_path}"

puts Time.now.to_s + " | Backing up to #{file_path}"
system(dump_cmd)
puts Time.now.to_s + " | Finished #{file_path} (" + File.stat(file_path).size.to_s + " bytes)"
puts Time.now.to_s + " | Backup file at: #{file_path}"

puts "Drop DB command:"
puts "   #{drop_cmd}"
puts "Create DB command:"
puts "   #{create_cmd}"
puts "Restore DB command:"
puts "   #{restore_cmd}"
puts ""

# Do not ask any questions, this is quite useful when running this task using Capistrano
STDOUT.puts "Are you sure? (yes/no)"
input = STDIN.gets.strip
abort("\n\nBye!\n\n\n") if input != 'yes'

system("PGPASSWORD=#{target_db_pwd} psql --host #{target_db_host} --port #{target_db_port} -U #{target_db_user} -d postgres -c 'SELECT datname,usename,client_addr,waiting,query_start FROM pg_stat_activity ORDER BY query_start;'")
system(drop_cmd)
system(create_cmd)
system(restore_cmd) do |status, result|
  # intentionally left blank to ignore any restore errors
end

puts " Cleaning up local tmp directory..."
puts " Found #{Dir.glob('/tmp/*_backup.postgres').count} backup files that can be deleted"
puts " Deleting..."

Dir.glob('/tmp/*_backup.postgres').map { |f| File.delete(f) }
puts Time.now.to_s + " | Done."
