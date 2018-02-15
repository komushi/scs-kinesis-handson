## 1. Prerequisite

### 1-1. Java 1.8

### 1-2. Maven 3.5+

### 1-3 git CLI

### 1-4. Docker for mac or windows

### 1-5. RabbitMQ
```
$ docker pull rabbitmq:3-management
$ docker run -p 5672:5672 -p 15672:15672 --hostname my-rabbit --name rabbit rabbitmq:3-management
```

### 1-6. Mongodb with geojson database
```
$ docker pull komushi/mongo-geojson
$ docker run -p 27017:27017 komushi/mongo-geojson
```

------
## 2. Build and run with RabbitMQ

### 2-1. Build aws-s3 source rabbit and run
### Replace 's3exp' with your own bucket to upload json file 'short_data.csv'
```
$ git clone https://github.com/spring-cloud-stream-app-starters/aws-s3.git
$ cd aws-s3
$ mvn clean install -PgenerateApps
$ cd apps/s3-source-rabbit
$ mvn clean package
$ java -jar target/s3-source-rabbit-2.0.0.BUILD-SNAPSHOT.jar --s3.remoteDir=/s3exp/short_data.csv --file.consumer.mode=lines --spring.cloud.stream.bindings.output.destination=s3_lines --spring.cloud.stream.bindings.output.contentType=text/plain --server.port=8000 --spring.cloud.stream.defaultBinder=rabbit --cloud.aws.region.static=ap-northeast-1
```

### 2-2. Build filter processor rabbit and run
### Only accept lines more than 10-digit
```
$ git clone https://github.com/spring-cloud-stream-app-starters/filter.git
$ cd filter
$ mvn clean install -PgenerateApps
$ cd apps/filter-processor-rabbit
$ mvn clean package
$ java -jar target/filter-processor-rabbit-2.0.0.BUILD-SNAPSHOT.jar --filter.expression='payload.length>10'  --spring.cloud.stream.bindings.input.destination=s3_lines --spring.cloud.stream.bindings.input.group=filter --spring.cloud.stream.bindings.output.destination=filtered_lines --spring.cloud.stream.bindings.output.contentType=text/plain --server.port=8010 --spring.cloud.stream.defaultBinder=rabbit --cloud.aws.region.static=ap-northeast-1
```

### 2-3. Build geocoding processor rabbit and run
### Convert lines to json data
```
$ java -jar target/scs-processor-geocoding-reverse-2.0.0.BUILD-SNAPSHOT.jar --spring.cloud.stream.bindings.input.contentType=text/plain --spring.cloud.stream.bindings.input.destination=filtered_lines --spring.cloud.stream.bindings.input.group=geocoding --spring.cloud.stream.bindings.output.destination=geojson --spring.cloud.stream.bindings.output.contentType=application/json --properties.mongo.hostName=localhost --properties.mongo.port=27017 --properties.mongo.database=geojson --properties.mongo.collection=blocks --properties.mongo.user=root --properties.mongo.password=keepitsimple --logging.level.info.cloudnative=TRACE --server.port=8020 --spring.cloud.stream.defaultBinder=rabbit --cloud.aws.region.static=ap-northeast-1
```

### 2-4. Run a second filter processor rabbit
### Filter json with specified dropoff longitude range
```
$ java -jar target/filter-processor-rabbit-2.0.0.BUILD-SNAPSHOT.jar --filter.expression="#jsonPath(new String(payload),'$.dropoffLongitude') < 139.76 && #jsonPath(new String(payload),'$.dropoffLongitude') > 139.73" --spring.cloud.stream.bindings.input.destination=geojson --spring.cloud.stream.bindings.input.group=filter --spring.cloud.stream.bindings.input.contentType=application/json --spring.cloud.stream.bindings.output.destination=filtered_geojson --spring.cloud.stream.bindings.output.contentType=application/json --server.port=8030 --spring.cloud.stream.defaultBinder=rabbit --cloud.aws.region.static=ap-northeast-1
```

### 2-5. Build log sink rabbit and run
### Export filtered data to log
```
$ git clone https://github.com/spring-cloud-stream-app-starters/log.git
$ cd log
$ mvn clean install -PgenerateApps
$ cd apps/log-sink-rabbit
$ mvn clean package
$ java -jar target/log-sink-rabbit-2.0.0.BUILD-SNAPSHOT.jar --log.expression="#jsonPath(new String(payload),'$')" --spring.cloud.stream.bindings.input.destination=filtered_geojson --spring.cloud.stream.bindings.input.group=log --spring.cloud.stream.bindings.input.contentType=application/json --server.port=8040 --spring.cloud.stream.defaultBinder=rabbit --cloud.aws.region.static=ap-northeast-1 
```

### 2-6. Build aws-s3 sink rabbit and run
### Replace 'glue-output-ap-northeast-1' with your own bucket to export filtered data to S3 bucket
```
$ git clone https://github.com/spring-cloud-stream-app-starters/aws-s3.git
$ cd aws-s3
$ mvn clean install -PgenerateApps
$ cd apps/s3-sink-rabbit
$ mvn clean package
$ java -jar target/s3-sink-rabbit-2.0.0.BUILD-SNAPSHOT.jar --s3.key-expression="#jsonPath(new String(payload),'$.uuid') + '.json'"  --s3.bucket=/glue-output-ap-northeast-1/geojson --spring.cloud.stream.bindings.input.destination=filtered_geojson --spring.cloud.stream.bindings.input.group=s3 --spring.cloud.stream.bindings.input.contentType=application/json --server.port=8050 --spring.cloud.stream.defaultBinder=rabbit --cloud.aws.region.static=ap-northeast-1 
```

### 2-7. Athena query count and data
* Use [AWS Glue](https://console.aws.amazon.com/glue/home) to create a table geojson with crawler.

* Use [Amazon Athena](https://console.aws.amazon.com/athena/home) to query exported data.

```
select count(*) from geojson

select * from geojson
```

------
## 3. Build and run with Kinesis
### 3-1. Build aws-s3 source rabbit and run
### Add Kinesis Binder Dependency
```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-stream-kinesis</artifactId>
    <version>1.0.0.BUILD-SNAPSHOT</version>
</dependency>
```

### Replace 's3exp' with your own bucket to upload json file 'short_data.csv'
```
$ cd aws-s3/apps/s3-source-rabbit
$ mvn clean package
$ java -jar target/s3-source-rabbit-2.0.0.BUILD-SNAPSHOT.jar --s3.remoteDir=/s3exp/short_data.csv --file.consumer.mode=lines --spring.cloud.stream.bindings.output.producer.partitionKeyExpression=1  --spring.cloud.stream.bindings.output.destination=s3_lines --spring.cloud.stream.bindings.output.contentType=text/plain --server.port=8000 --spring.cloud.stream.defaultBinder=kinesis --cloud.aws.region.static=ap-northeast-1
```

### 3-2. Build filter processor kinesis and run
### Add Kinesis Binder Dependency
```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-stream-kinesis</artifactId>
    <version>1.0.0.BUILD-SNAPSHOT</version>
</dependency>
```

```
$ cd filter/apps/filter-processor-rabbit
$ mvn clean package
$ java -jar target/filter-processor-rabbit-2.0.0.BUILD-SNAPSHOT.jar --filter.expression='payload.length>10'  --spring.cloud.stream.bindings.input.destination=s3_lines --spring.cloud.stream.bindings.input.group=filter --spring.cloud.stream.bindings.output.destination=filtered_lines --spring.cloud.stream.bindings.output.contentType=text/plain --spring.cloud.stream.bindings.output.producer.partitionKeyExpression=1 --server.port=8010 --spring.cloud.stream.defaultBinder=kinesis --cloud.aws.region.static=ap-northeast-1
```

### 3-3. Run geocoding processor with kinesis
```
$ java -jar target/scs-processor-geocoding-reverse-2.0.0.BUILD-SNAPSHOT.jar --spring.cloud.stream.bindings.input.destination=filtered_lines --spring.cloud.stream.bindings.input.group=geocoding --spring.cloud.stream.bindings.output.destination=geojson --spring.cloud.stream.bindings.output.contentType=application/json --spring.cloud.stream.bindings.output.producer.partitionKeyExpression=1 --properties.mongo.hostName=localhost --properties.mongo.port=27017 --properties.mongo.database=geojson --properties.mongo.collection=blocks --properties.mongo.user=root --properties.mongo.password=keepitsimple --logging.level.info.cloudnative=TRACE --server.port=8020 --spring.cloud.stream.defaultBinder=kinesis --cloud.aws.region.static=ap-northeast-1
```

### 3-4. Run a second filter processor kinesis
### Filter json with specified dropoff longitude range
```
$ java -jar target/filter-processor-rabbit-2.0.0.BUILD-SNAPSHOT.jar --filter.expression="#jsonPath(new String(payload),'$.dropoffLongitude') < 139.76 && #jsonPath(new String(payload),'$.dropoffLongitude') > 139.73" --spring.cloud.stream.bindings.input.destination=geojson --spring.cloud.stream.bindings.input.group=filter --spring.cloud.stream.bindings.input.contentType=application/json --spring.cloud.stream.bindings.output.destination=filtered_geojson --spring.cloud.stream.bindings.output.contentType=application/json --spring.cloud.stream.bindings.output.producer.partitionKeyExpression=1 --server.port=8030 --spring.cloud.stream.defaultBinder=kinesis --cloud.aws.region.static=ap-northeast-1
```

### 3-5. Build log sink kinesis and run
### Add Kinesis Binder Dependency
```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-stream-kinesis</artifactId>
    <version>1.0.0.BUILD-SNAPSHOT</version>
</dependency>
```

```
$ cd log/apps/log-sink-rabbit
$ mvn clean package
$ java -jar target/log-sink-rabbit-2.0.0.BUILD-SNAPSHOT.jar --log.expression="#jsonPath(new String(payload),'$')" --spring.cloud.stream.bindings.input.destination=filtered_geojson --spring.cloud.stream.bindings.input.group=log --spring.cloud.stream.bindings.input.contentType=application/json --server.port=8040 --spring.cloud.stream.defaultBinder=kinesis --cloud.aws.region.static=ap-northeast-1 
```

### 3-6. Build aws-s3 sink rabbit and run
### Add Kinesis Binder Dependency
```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-stream-kinesis</artifactId>
    <version>1.0.0.BUILD-SNAPSHOT</version>
</dependency>
```

### Replace 'glue-output-ap-northeast-1' with your own bucket to export filtered data to S3 bucket
```
$ cd aws-s3/apps/s3-sink-rabbit
$ mvn clean package
$ java -jar target/s3-sink-rabbit-2.0.0.BUILD-SNAPSHOT.jar --s3.key-expression="#jsonPath(new String(payload),'$.uuid') + '.json'"  --s3.bucket=/glue-output-ap-northeast-1/geojson --spring.cloud.stream.bindings.input.destination=filtered_geojson --spring.cloud.stream.bindings.input.group=s3 --spring.cloud.stream.bindings.input.contentType=application/json --server.port=8050 --spring.cloud.stream.defaultBinder=kinesis --cloud.aws.region.static=ap-northeast-1 
```


### 3-7. Athena query count and data
* Use [AWS Glue](https://console.aws.amazon.com/glue/home) to create a table geojson with crawler.

* Use [Amazon Athena](https://console.aws.amazon.com/athena/home) to query exported data.

```
select count(*) from geojson

select * from geojson
```

------
## 4. Dockerize and deploy to AWS ECS 