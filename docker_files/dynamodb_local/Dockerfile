FROM java:8

MAINTAINER Matteo Melani <m@ellipsis.ai>

RUN mkdir /var/dynamodb
WORKDIR /var/dynamodb

# Default port for DynamoDB Local
EXPOSE 8000

# Get the package from Amazon
RUN wget -O /var/dynamodb/dynamodb_local_latest http://dynamodb-local.s3-website-us-west-2.amazonaws.com/dynamodb_local_latest
RUN tar xfz /var/dynamodb/dynamodb_local_latest

# Default command for image
ENTRYPOINT ["/usr/bin/java", "-Djava.library.path=./DynamoDBLocal_lib", "-jar", "DynamoDBLocal.jar", "-dbPath", "/var/dynamodb_local"]
CMD ["-port", "8000"]

# Add VOLUMEs to allow backup of config, logs and databases
VOLUME ["/var/dynamodb_local", "/var/dynamodb"]
