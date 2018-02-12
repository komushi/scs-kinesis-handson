

```
$ git clone https://github.com/spring-cloud-stream-app-starters/log.git
$ cd log
$ mvn clean install -PgenerateApps
$ cd apps/log-sink-rabbit
$ mvn clean package
$ java -jar target/log-sink-rabbit-2.0.0.BUILD-SNAPSHOT.jar --spring.cloud.stream.bindings.input.destination=s3line --spring.cloud.stream.defaultBinder=rabbit --cloud.aws.region.static=ap-northeast-1 --spring.cloud.stream.bindings.input.contentType=text/plain
```

```
$ git clone https://github.com/spring-cloud-stream-app-starters/http.git
$ cd http
$ mvn clean install -PgenerateApps -DskipTests
$ cd apps
```

------
### rabbit
```
$ docker pull rabbitmq:3-management
$ docker run -p 5672:5672 -p 15672:15672 --hostname my-rabbit --name rabbit rabbitmq:3-management
```



------

### aws-s3 rabbit
```
$ git clone https://github.com/spring-cloud-stream-app-starters/aws-s3.git
$ cd aws-s3
$ mvn clean install -PgenerateApps
$ cd apps/s3-source-rabbit
$ mvn clean package
$ java -jar target/s3-source-rabbit-2.0.0.BUILD-SNAPSHOT.jar --s3.remoteDir=/s3exp/short_data.csv --file.consumer.mode=lines --spring.cloud.stream.bindings.output.destination=s3_lines --spring.cloud.stream.defaultBinder=rabbit --cloud.aws.region.static=ap-northeast-1
```

### filter rabbit
```
$ git clone https://github.com/spring-cloud-stream-app-starters/filter.git
$ cd filter
$ mvn clean install -PgenerateApps
$ cd apps/filter-processor-rabbit
$ mvn clean package
$ java -jar target/filter-processor-rabbit-2.0.0.BUILD-SNAPSHOT.jar --filter.expression='payload.length>15'  --spring.cloud.stream.bindings.input.destination=s3_lines --spring.cloud.stream.bindings.input.group=filter --spring.cloud.stream.bindings.output.destination=filtered_lines --spring.cloud.stream.bindings.output.contentType=text/plain --spring.cloud.stream.defaultBinder=rabbit --cloud.aws.region.static=ap-northeast-1
```


### geocoding rabbit
```
$ java -jar target/scs-processor-geocoding-reverse-2.0.0.BUILD-SNAPSHOT.jar --spring.cloud.stream.bindings.input.destination=filtered_lines --spring.cloud.stream.bindings.input.group=geocoding --spring.cloud.stream.bindings.output.destination=geojson --spring.cloud.stream.bindings.output.contentType=application/json --properties.mongo.hostName=localhost --properties.mongo.port=27017 --properties.mongo.database=geojson --properties.mongo.collection=blocks --properties.mongo.user=root --properties.mongo.password=keepitsimple --logging.level.info.cloudnative=TRACE --spring.cloud.stream.defaultBinder=rabbit --cloud.aws.region.static=ap-northeast-1


```

### aws-s3 kinesis
```
java -jar target/s3-source-rabbit-2.0.0.BUILD-SNAPSHOT.jar --s3.remoteDir=/s3exp/short_data.csv --file.consumer.mode=lines --spring.cloud.stream.bindings.output.producer.partitionKeyExpression=1  --spring.cloud.stream.bindings.output.destination=s3_lines --spring.cloud.stream.defaultBinder=kinesis --cloud.aws.region.static=ap-northeast-1
```

### filter kinesis
```
$ git clone https://github.com/spring-cloud-stream-app-starters/filter.git
$ cd filter
$ mvn clean install -PgenerateApps
$ cd apps/filter-processor-rabbit
$ mvn clean package
$ java -jar target/filter-processor-rabbit-2.0.0.BUILD-SNAPSHOT.jar --filter.expression='payload.length>15'  --spring.cloud.stream.bindings.input.destination=s3_lines --spring.cloud.stream.bindings.input.group=filter --spring.cloud.stream.bindings.output.destination=filtered_lines --spring.cloud.stream.bindings.output.contentType=text/plain --spring.cloud.stream.bindings.output.producer.partitionKeyExpression=1 --spring.cloud.stream.defaultBinder=kinesis --cloud.aws.region.static=ap-northeast-1
```

### geocoding kinesis
```
$ java -jar target/scs-processor-geocoding-reverse-2.0.0.BUILD-SNAPSHOT.jar --spring.cloud.stream.bindings.input.destination=filtered_lines --spring.cloud.stream.bindings.input.group=geocoding --spring.cloud.stream.bindings.output.destination=geojson --spring.cloud.stream.bindings.output.contentType=application/json --spring.cloud.stream.bindings.output.producer.partitionKeyExpression=1 --properties.mongo.hostName=localhost --properties.mongo.port=27017 --properties.mongo.database=geojson --properties.mongo.collection=blocks --properties.mongo.user=root --properties.mongo.password=keepitsimple --logging.level.info.cloudnative=TRACE --spring.cloud.stream.defaultBinder=kinesis --cloud.aws.region.static=ap-northeast-1
```



------

### 1-1. Download Source

[http source](https://github.com/spring-cloud-stream-app-starters/http/archive/v1.3.1.RELEASE.zip)

### 1-2. Unzip to folder http-1.3.1.RELEASE
```
$ cd http-1.3.1.RELEASE
$ mvn clean install -PgenerateApps
$ cd apps/http-source-rabbit
```