#### Get your AWS development Account
Annotate the AWS_KEY and AWS_SECRET

#### Setup AWS cli on your Mac
```bash
$ sudo pip install awscli
```
If you get an error run this instead:
```bash
$ sudo pip install awscli --ignore-installed six
```
If you need to upgrade awscli with pip use:
```bash
$ sudo pip install --upgrade awscli
```
Test awscli by typing:
```bash
$ aws help
```
Configure awscli by running:
```bash
$ aws configure --profile ellipsis_dev
```
And follow the instructions.
For more info go to: http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html#cli-quick-configuration


You can now running the aws cli using the --profile option.
For example:

```bash
$ aws s3 ls --profile ellipsis_dev
```

#### Install brew
```bash
$ ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
$ brew update
```

#### Install Java 8
```bash
$ brew update
$ brew cask install java
```

Check java is working:
```bash
$ java -version
```

FYI On the Mac jdk is installed in:
```bash
$ ls -lFa /Library/Java/JavaVirtualMachines
```

#### Install Activator
Download link is https://www.lightbend.com/activator/download
add the Activator bin folder to your path
https://downloads.typesafe.com/typesafe-activator/1.3.10/typesafe-activator-1.3.10.zip


#### Install Node v6
(With Node 7+ you may see errors trying to build.)
```bash
$ brew install node@6
```

#### Install ngrok and run it for port 9000 (`ngrok http 9000`)
Download link is https://ngrok.com/download
Suggested installation location: /usr/local/bin
Run ngrok using:
```bash
$ ngrok http 9000
```
If you use the free version of Ngrok everytime you restart it Ngrok will assign to you a new
subdomain and you will have to update the Playapp configuration. If you upgrade your Ngrok plan
you can choose and keep a subdomain. You can then use the Ngrock config. file:

Create a file at ~/.ngrok2/ngrok.yml. It should look like this

```
authtoken: <AUTH TOKEN>
tunnels:
  dev:
    proto: http
    addr: 9000
    hostname: <CHOOSEN SUBDOMAIN>.ngrok.io
```
Then from the command line you can simply run:

```bash
$ ngrok start dev
```


#### Create a Slack API App
- Go to https://api.slack.com/apps
- Create a new API Application
- Copy the Client ID, Client Secret and the Verification Token somewhere. You will use them to
  set some environment variables.
- OAuth & Permission: Add the Ngrok url to the Redirect URL(s)
- Bot Users:
  - Click on "Add a Bot User"
  - Turn on "Always Show My Bot as Online".
- Event subscriptions:
  - Add the Request URL: <ngrok url>/slack/event
  - Subscribe to these Bot Events: `message.channels`, `message.groups`, `message.im`, `message.mpim`

#### Get the source code from Github
```bash
$ git clone git@github.com:ellipsis-ai/ellipsis.git
```

#### Install npm packages
```bash
$ cd ./ellipsis
$ npm install
```

#### Configure the Activator wrapper script
```bash
$ cp ./actw.template ./actw
```
Now edit ./actw and fill in the env. variables values
When you are done make run_app executable with:
```bash
$ chmod 755 ./actw
```

#### Install docker for Mac
https://docs.docker.com/engine/installation/mac/

#### Run docker-compose
In this step you will start a Docker container running Postgres on port 5432.
Before you do that you want to make sure you do not have any Postgres instance
running on the same port. Just do:

```bash
$ ps aux | grep postgres
```
Stop the Postgres instance if running.

Now you can run Docker Compose:

```bash
$ cd ./ellipsis
$ docker-compose up
```
You should now have a postgres instance running on port 5432.
You can verify that by running:

```bash
$ PGPASSWORD=ellipsis psql -h 127.0.0.1 -p 5432 ellipsis ellipsis
```

You also have Elasticsearch on port 9200, Kibana on port 5601, Elasticsearch-head
on 9100 and Memcached on 11211.

http://localhost:9200
http://localhost:9100
http://localhost:5601


#### Run the app
The app is run using Activator the `actw` script is just a wrapper that invokes
Activator with the necessary environment variables.

```bash
$ cp actw.template actw
```
Now edit `actw` and fill in all the value for the environment variables

```bash
$ vim actw
```
Finally you can run the app

```bash
$ ./actw help
$ ./actw run
```

#### Run the console
```bash
$ ./actw
```

#### Debug the app
```bash
$ ./actw -jvm-debug 9999 run
```
You can now use any Java debugger to attach to 9999.


#### Drop a Bad Db
```bash
$ docker exec -it ellipsis_postgres_1 bash bash
```
Now that you have a shell in the container, become the postgres user and use
the dropdb command to drop the db

```bash
$ su postgres
$ dropdb ellipsis-test
```


#### Build and run the app locally in a container
