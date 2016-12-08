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


#### Install Node
```bash
$ brew install node
```

#### Install ngrok and run it for port 9000 (`ngrok http 9000`)
Download link is https://ngrok.com/download
Suggested installation location: /usr/local/bin
Run ngrok using:
```bash
$ ngrok http 9000
```

#### Create a Slack App
Copy the App id and secret somewhere.
Configure the redirect url using the ngrok url.

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

You should also have Elasticsearch running on port 9200 and Kibana on port 5601

http://localhost:9200
http://localhost:9200/_plugin/head/
http://localhost:5601
http://localhost:5601/app/sense

You should have a DynamoDb Local running on port 8000, try out the shell at:

http://localhost:8000/shell

#### Run the app
The app is run using Activator the run_app script is just a wrapper that invokes
Activator with the necessary env. variables.

```bash
$ ./actw help
$ ./actw run
```

#### Run the console
```bash
$ ./actw console
```

#### Run a query in the console
```scala
import play.api._
val env = Environment(new java.io.File("."), this.getClass.getClassLoader, Mode.Dev)
val context = ApplicationLoader.createContext(env)
val loader = ApplicationLoader(context)
val app = loader.load(context)
Play.start(app)

import services.{AWSDynamoDBService, DataService}

val dataService = app.injector.instanceOf(classOf[DataService])
val dynService = app.injector.instanceOf(classOf[AWSDynamoDBService])

import scala.concurrent.Await
import scala.concurrent.duration._

Await.result(dataService.teams.find("foo"), 10.seconds)
```

#### Run tests
```bash
./actw test
```

```bash
./actw "testOnly controllers.api.dev.v1.SmallStorageControllerSpec"
```

#### Debug the app
```bash
$ ./actw -jvm-debug 9999 run
```
You can now use any Java debugger to attach to 9999.


### Play around with Elasticsearch
Make sure you have Elasticsearch up and running by going at http://localhost/_plugin/head
Then fire up a console with:
```bash
$ ./actw console
```
And type the following scala: 

```scala
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.ElasticDsl._

val client = ElasticClient.transport(ElasticsearchClientUri("localhost", 9300))
client.execute { index into "bands" / "artists" fields "name"->"coldplay" }.await
val resp = client.execute { search in "bands" / "artists" query "coldplay" }.await
println(resp)

```
