
## 1. Prerequisite
## 準備

### 1-1. Java 1.8

### 1-2. Maven 3.5+

### 1-3. git CLI

### 1-4. Docker for macOS or Windows

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

### 1-7. Get flar-file-reader
```
docker pull komushi/flat-file-reader
```

------
## 2. Build and run with RabbitMQ
## RabbitMQでビルドし、動作確認する

### 2-1. Build http source rabbit and run
#### Replace 'ap-northeast-1' with your expected region
```
$ git clone https://github.com/spring-cloud-stream-app-starters/http.git
$ cd http
$ mvn clean install -PgenerateApps
$ cd apps/http-source-rabbit
$ mvn clean package
$ java -jar target/http-source-rabbit-2.0.0.BUILD-SNAPSHOT.jar --server.port=8000 --spring.cloud.stream.default.contentType=text/plain --spring.cloud.stream.bindings.output.destination=http_lines --spring.cloud.stream.defaultBinder=rabbit --cloud.aws.region.static=ap-northeast-1
```

### 2-2. Build filter processor rabbit and run
#### Only accept lines more than 10-digit
#### Replace 'ap-northeast-1' with your expected region
```
$ git clone https://github.com/spring-cloud-stream-app-starters/filter.git
$ cd filter
$ mvn clean install -PgenerateApps
$ cd apps/filter-processor-rabbit
$ mvn clean package
$ java -jar target/filter-processor-rabbit-2.0.0.BUILD-SNAPSHOT.jar --filter.expression='payload.length>10'  --spring.cloud.stream.bindings.input.destination=http_lines --spring.cloud.stream.bindings.input.group=filter --spring.cloud.stream.bindings.output.destination=filtered_lines --spring.cloud.stream.bindings.output.contentType=text/plain --server.port=8010 --spring.cloud.stream.defaultBinder=rabbit --cloud.aws.region.static=ap-northeast-1
```

### 2-3. Build geocoding processor rabbit and run
#### Convert lines to json data
#### Replace 'ap-northeast-1' with your expected region
```
$ cd scs-process-geocoding-reverse
$ mcn clean package
$ java -jar target/scs-processor-geocoding-reverse-2.0.0.BUILD-SNAPSHOT.jar --spring.cloud.stream.bindings.input.contentType=text/plain --spring.cloud.stream.bindings.input.destination=filtered_lines --spring.cloud.stream.bindings.input.group=geocoding --spring.cloud.stream.bindings.output.destination=geojson --spring.cloud.stream.bindings.output.contentType=application/json --properties.mongo.hostName=localhost --properties.mongo.port=27017 --properties.mongo.database=geojson --properties.mongo.collection=blocks --properties.mongo.user=root --properties.mongo.password=keepitsimple --logging.level.info.cloudnative=TRACE --server.port=8020 --spring.cloud.stream.defaultBinder=rabbit --cloud.aws.region.static=ap-northeast-1
```

### 2-4. Run a second filter processor rabbit
#### Filter json with specified dropoff longitude range
#### Replace 'ap-northeast-1' with your expected region
```
$ java -jar target/filter-processor-rabbit-2.0.0.BUILD-SNAPSHOT.jar --filter.expression="#jsonPath(new String(payload),'$.dropoffLongitude') < 139.76 && #jsonPath(new String(payload),'$.dropoffLongitude') > 139.74" --spring.cloud.stream.bindings.input.destination=geojson --spring.cloud.stream.bindings.input.group=filter --spring.cloud.stream.bindings.input.contentType=application/json --spring.cloud.stream.bindings.output.destination=filtered_geojson --spring.cloud.stream.bindings.output.contentType=application/json --server.port=8030 --spring.cloud.stream.defaultBinder=rabbit --cloud.aws.region.static=ap-northeast-1
```

### 2-5. Build log sink rabbit and run
#### Export filtered data to log
#### Replace 'ap-northeast-1' with your expected region
```
$ git clone https://github.com/spring-cloud-stream-app-starters/log.git
$ cd log
$ mvn clean install -PgenerateApps
$ cd apps/log-sink-rabbit
$ mvn clean package
$ java -jar target/log-sink-rabbit-2.0.0.BUILD-SNAPSHOT.jar --log.expression="#jsonPath(new String(payload),'$')" --spring.cloud.stream.bindings.input.destination=filtered_geojson --spring.cloud.stream.bindings.input.group=log --spring.cloud.stream.bindings.input.contentType=application/json --server.port=8040 --spring.cloud.stream.defaultBinder=rabbit --cloud.aws.region.static=ap-northeast-1 
```

### 2-6. Build aws-s3 sink rabbit and run
#### Replace 'glue-output-ap-northeast-1' with your own bucket to export filtered data to S3 bucket
#### Replace 'ap-northeast-1' with your expected region
```
$ git clone https://github.com/spring-cloud-stream-app-starters/aws-s3.git
$ cd aws-s3
$ mvn clean install -PgenerateApps
$ cd apps/s3-sink-rabbit
$ mvn clean package
$ java -jar target/s3-sink-rabbit-2.0.0.BUILD-SNAPSHOT.jar --s3.key-expression="#jsonPath(new String(payload),'$.uuid') + '.json'"  --s3.bucket=/glue-output-ap-northeast-1/geojson --spring.cloud.stream.bindings.input.destination=filtered_geojson --spring.cloud.stream.bindings.input.group=s3 --spring.cloud.stream.bindings.input.contentType=application/json --server.port=8050 --spring.cloud.stream.defaultBinder=rabbit --cloud.aws.region.static=ap-northeast-1 
```

### 2-7. Start the stream
```
curl -H "Content-Type: text/plain" -X POST -d'468244D1361B8A3EB8D206CC394BC9E9,BB899DFEA9CC964B50C540A1D685CCFB,2013-01-01 00:00:00,2013-01-01 00:04:00,240,1.71,139.752268,35.677043,139.771699,35.697283,CSH,6.50,0.50,0.50,0.00,0.00,7.50' 'http://localhost:8000'
```

```
docker run komushi/flat-file-reader --url=http://docker.for.mac.localhost:8000
```

### 2-8. Athena query count and data
* Use [AWS Glue](https://console.aws.amazon.com/glue/home) to create a table geojson with crawler.

* Use [Amazon Athena](https://console.aws.amazon.com/athena/home) to query exported data.

```
select count(*) from geojson

select * from geojson
```

------
## 3. Scale-out test
## スケールアウト

### 3-1. Start the stream with a data file in a much bigger size.
#### Upload the 'data/long_data.csv' to the source s3 bucket to trigger the events
#### Check the CPU load and th PID of the Java proccesses

### 3-2. Scale out geocoding processor

#### Scale out the 2nd geocoding processor
#### Replace 'ap-northeast-1' with your expected region
```
$ cd scs-process-geocoding-reverse
$ java -jar target/scs-processor-geocoding-reverse-2.0.0.BUILD-SNAPSHOT.jar --spring.cloud.stream.bindings.input.contentType=text/plain --spring.cloud.stream.bindings.input.destination=filtered_lines --spring.cloud.stream.bindings.input.group=geocoding --spring.cloud.stream.bindings.output.destination=geojson --spring.cloud.stream.bindings.output.contentType=application/json --properties.mongo.hostName=localhost --properties.mongo.port=27017 --properties.mongo.database=geojson --properties.mongo.collection=blocks --properties.mongo.user=root --properties.mongo.password=keepitsimple --logging.level.info.cloudnative=TRACE --server.port=8021 --spring.cloud.stream.defaultBinder=rabbit --cloud.aws.region.static=ap-northeast-1
```

#### Scale out the 3rd geocoding processor
#### Replace 'ap-northeast-1' with your expected region
```
$ cd scs-process-geocoding-reverse
$ java -jar target/scs-processor-geocoding-reverse-2.0.0.BUILD-SNAPSHOT.jar --spring.cloud.stream.bindings.input.contentType=text/plain --spring.cloud.stream.bindings.input.destination=filtered_lines --spring.cloud.stream.bindings.input.group=geocoding --spring.cloud.stream.bindings.output.destination=geojson --spring.cloud.stream.bindings.output.contentType=application/json --properties.mongo.hostName=localhost --properties.mongo.port=27017 --properties.mongo.database=geojson --properties.mongo.collection=blocks --properties.mongo.user=root --properties.mongo.password=keepitsimple --logging.level.info.cloudnative=TRACE --server.port=8022 --spring.cloud.stream.defaultBinder=rabbit --cloud.aws.region.static=ap-northeast-1
```

#### Check the CPU load and th PID of the Java proccesses - scs-processor-geocoding-reverse-2.0.0.BUILD-SNAPSHOT.jar
------

## 4. Build and run with Kinesis
## Kinesisでビルドし、動作確認する

### 4-1. Build http source rabbit and run
#### Add Kinesis Binder Dependency in 'http/apps/http-source-rabbit/pom.xml'
```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-stream-kinesis</artifactId>
    <version>1.0.0.M1</version>
</dependency>
```

#### Replace 'ap-northeast-1' with your expected region
```
$ cd http/apps/http-source-rabbit
$ mvn clean package
$ java -jar target/http-source-rabbit-2.0.0.BUILD-SNAPSHOT.jar --server.port=8000 --spring.cloud.stream.default.contentType=text/plain --spring.cloud.stream.bindings.output.destination=http_lines --spring.cloud.stream.bindings.output.producer.partitionKeyExpression=1 --spring.cloud.stream.defaultBinder=kinesis --cloud.aws.region.static=ap-northeast-1
```

### 4-2. Build filter processor kinesis and run
#### Add Kinesis Binder Dependency in 'filter/apps/filter-processor-rabbit/pom.xml'
```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-stream-kinesis</artifactId>
    <version>1.0.0.M1</version>
</dependency>
```

#### Replace 'ap-northeast-1' with your expected region
```
$ cd filter/apps/filter-processor-rabbit
$ mvn clean package
$ java -jar target/filter-processor-rabbit-2.0.0.BUILD-SNAPSHOT.jar --filter.expression='payload.length>10'  --spring.cloud.stream.bindings.input.destination=http_lines --spring.cloud.stream.bindings.input.group=filter --spring.cloud.stream.bindings.output.destination=filtered_lines --spring.cloud.stream.bindings.output.contentType=text/plain --spring.cloud.stream.bindings.output.producer.partitionKeyExpression=1 --server.port=8010 --spring.cloud.stream.defaultBinder=kinesis --cloud.aws.region.static=ap-northeast-1
```

### 4-3. Run geocoding processor with kinesis
#### Replace 'ap-northeast-1' with your expected region
```
$ java -jar target/scs-processor-geocoding-reverse-2.0.0.BUILD-SNAPSHOT.jar --spring.cloud.stream.bindings.input.contentType=text/plain --spring.cloud.stream.bindings.input.destination=filtered_lines --spring.cloud.stream.bindings.input.group=geocoding --spring.cloud.stream.bindings.output.destination=geojson --spring.cloud.stream.bindings.output.contentType=application/json --spring.cloud.stream.bindings.output.producer.partitionKeyExpression=1 --properties.mongo.hostName=localhost --properties.mongo.port=27017 --properties.mongo.database=geojson --properties.mongo.collection=blocks --properties.mongo.user=root --properties.mongo.password=keepitsimple --logging.level.info.cloudnative=TRACE --server.port=8020 --spring.cloud.stream.defaultBinder=kinesis --cloud.aws.region.static=ap-northeast-1
```

### 4-4. Run a second filter processor kinesis
#### Filter json with specified dropoff longitude range
#### Replace 'ap-northeast-1' with your expected region
```
$ java -jar target/filter-processor-rabbit-2.0.0.BUILD-SNAPSHOT.jar --filter.expression="#jsonPath(new String(payload),'$.dropoffLongitude') < 139.76 && #jsonPath(new String(payload),'$.dropoffLongitude') > 139.74" --spring.cloud.stream.bindings.input.destination=geojson --spring.cloud.stream.bindings.input.group=filter --spring.cloud.stream.bindings.input.contentType=application/json --spring.cloud.stream.bindings.output.destination=filtered_geojson --spring.cloud.stream.bindings.output.contentType=application/json --spring.cloud.stream.bindings.output.producer.partitionKeyExpression=1 --server.port=8030 --spring.cloud.stream.defaultBinder=kinesis --cloud.aws.region.static=ap-northeast-1
```

### 4-5. Build log sink kinesis and run
#### Add Kinesis Binder Dependency in 'log/apps/log-sink-rabbit/pom.xml'
```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-stream-kinesis</artifactId>
    <version>1.0.0.M1</version>
</dependency>
```

#### Replace 'ap-northeast-1' with your expected region
```
$ cd log/apps/log-sink-rabbit
$ mvn clean package
$ java -jar target/log-sink-rabbit-2.0.0.BUILD-SNAPSHOT.jar --log.expression="#jsonPath(new String(payload),'$')" --spring.cloud.stream.bindings.input.destination=filtered_geojson --spring.cloud.stream.bindings.input.group=log --spring.cloud.stream.bindings.input.contentType=application/json --server.port=8040 --spring.cloud.stream.defaultBinder=kinesis --cloud.aws.region.static=ap-northeast-1 
```

### 4-6. Build aws-s3 sink rabbit and run
#### Add Kinesis Binder Dependency in 'aws-s3/apps/s3-sink-rabbit/pom.xml'
```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-stream-kinesis</artifactId>
    <version>1.0.0.M1</version>
</dependency>
```

#### Replace 'glue-output-ap-northeast-1' with your own bucket to export filtered data to S3 bucket
#### Replace 'ap-northeast-1' with your expected region
```
$ cd aws-s3/apps/s3-sink-rabbit
$ mvn clean package
$ java -jar target/s3-sink-rabbit-2.0.0.BUILD-SNAPSHOT.jar --s3.key-expression="#jsonPath(new String(payload),'$.uuid') + '.json'"  --s3.bucket=/glue-output-ap-northeast-1/geojson --spring.cloud.stream.bindings.input.destination=filtered_geojson --spring.cloud.stream.bindings.input.group=s3 --spring.cloud.stream.bindings.input.contentType=application/json --server.port=8050 --spring.cloud.stream.defaultBinder=kinesis --cloud.aws.region.static=ap-northeast-1 
```

### 4-8. Start the stream
```
curl -H "Content-Type: text/plain" -X POST -d'468244D1361B8A3EB8D206CC394BC9E9,BB899DFEA9CC964B50C540A1D685CCFB,2013-01-01 00:00:00,2013-01-01 00:04:00,240,1.71,139.752268,35.677043,139.771699,35.697283,CSH,6.50,0.50,0.50,0.00,0.00,7.50' 'http://localhost:8000'
```

```
docker run komushi/flat-file-reader --url=http://docker.for.mac.localhost:8000
```

### 4-7. Athena query count and data
* Use [AWS Glue](https://console.aws.amazon.com/glue/home) to create a table geojson with crawler.

* Use [Amazon Athena](https://console.aws.amazon.com/athena/home) to query exported data.

```
select count(*) from geojson

select * from geojson
```

------
## 5. Dockerize the Apps
## Spring Cloud Stream アプリケーションをDocker化する

### 5-1. Dockerize http source kinesis
#### Modify 'plugin.configuration.images.image.name' of 'docker-maven-plugin' in 'http/apps/http-source-rabbit/pom.xml' to '<your_docker_hub_id>/http-source-kinesis'
```
  <plugin>
    <groupId>io.fabric8</groupId>
    <artifactId>docker-maven-plugin</artifactId>
    <version>0.14.2</version>
    <configuration>
      <images>
        <image>
          <name>komushi/http-source-kinesis</name>
          <build>
            <from>anapsix/alpine-java:8</from>
            <volumes>
              <volume>/tmp</volume>
            </volumes>
            <entryPoint>
              <exec>
                <arg>java</arg>
                <arg>-jar</arg>
                <arg>/maven/http-source-rabbit.jar</arg>
              </exec>
            </entryPoint>
            <assembly>
              <descriptor>assembly.xml</descriptor>
            </assembly>
          </build>
        </image>
      </images>
    </configuration>
  </plugin>
```

#### Build and push Docker image
#### Replace 'komushi' with your your_docker_hub_id
```
$ mvn clean package docker:build
$ docker push komushi/http-source-kinesis
```

### 5-2. Dockerize filter processor kinesis
#### Modify 'plugin.configuration.images.image.name' of 'docker-maven-plugin' in 'filter/apps/filter-processor-rabbit/pom.xml' to '<your_docker_hub_id>/filter-processor-kinesis'

```
  <plugin>
    <groupId>io.fabric8</groupId>
    <artifactId>docker-maven-plugin</artifactId>
    <version>0.14.2</version>
    <configuration>
      <images>
        <image>
          <name>komushi/filter-processor-kinesis</name>
          <build>
            <from>anapsix/alpine-java:8</from>
            <volumes>
              <volume>/tmp</volume>
            </volumes>
            <entryPoint>
              <exec>
                <arg>java</arg>
                <arg>-jar</arg>
                <arg>/maven/filter-processor-rabbit.jar</arg>
              </exec>
            </entryPoint>
            <assembly>
              <descriptor>assembly.xml</descriptor>
            </assembly>
          </build>
        </image>
      </images>
    </configuration>
  </plugin>
```

#### Build and push Docker image
#### Replace 'komushi' with your your_docker_hub_id
```
$ mvn clean package docker:build
$ docker push komushi/filter-processor-kinesis
```

### 5-3. Dockerize geocoding processor kinesis
#### Modify 'plugin.configuration.images.image.name' of 'docker-maven-plugin' in 'scs-process-geocoding-reverse/pom.xml' to '<your_docker_hub_id>/geocoding-processor-kinesis'

```
  <plugin>
    <groupId>io.fabric8</groupId>
    <artifactId>docker-maven-plugin</artifactId>
    <version>0.14.2</version>
    <configuration>
      <images>
        <image>
          <name>komushi/geocoding-processor-kinesis</name>
          <build>
            <from>anapsix/alpine-java:8</from>
            <volumes>
              <volume>/tmp</volume>
            </volumes>
            <entryPoint>
              <exec>
                <arg>java</arg>
                <arg>-jar</arg>
                <arg>/maven/scs-processor-geocoding-reverse.jar</arg>
              </exec>
            </entryPoint>
            <assembly>
              <descriptor>assembly.xml</descriptor>
            </assembly>
          </build>
        </image>
      </images>
    </configuration>
  </plugin>
```

#### Build and push Docker image
#### Replace 'komushi' with your your_docker_hub_id
```
$ mvn clean package docker:build
$ docker push komushi/geocoding-processor-kinesis
```

### 5-4. Dockerize log sink kinesis
#### Modify 'plugin.configuration.images.image.name' of 'docker-maven-plugin' in 'log/apps/log-sink-rabbit/pom.xml' to '<your_docker_hub_id>/log-sink-kinesis'

```
  <plugin>
    <groupId>io.fabric8</groupId>
    <artifactId>docker-maven-plugin</artifactId>
    <version>0.14.2</version>
    <configuration>
      <images>
        <image>
          <name>komushi/log-sink-kinesis</name>
          <build>
            <from>anapsix/alpine-java:8</from>
            <volumes>
              <volume>/tmp</volume>
            </volumes>
            <entryPoint>
              <exec>
                <arg>java</arg>
                <arg>-jar</arg>
                <arg>/maven/log-sink-rabbit.jar</arg>
              </exec>
            </entryPoint>
            <assembly>
              <descriptor>assembly.xml</descriptor>
            </assembly>
          </build>
        </image>
      </images>
    </configuration>
  </plugin>
```

#### Build and push Docker image
#### Replace 'komushi' with your your_docker_hub_id
```
$ mvn clean package docker:build
$ docker push komushi/log-sink-kinesis
```

### 5-5. Dockerize s3 sink kinesis
#### Modify 'plugin.configuration.images.image.name' of 'docker-maven-plugin' in 'aws-s3/apps/s3-sink-rabbit/pom.xml' to '<your_docker_hub_id>/s3-sink-kinesis'

```
  <plugin>
    <groupId>io.fabric8</groupId>
    <artifactId>docker-maven-plugin</artifactId>
    <version>0.14.2</version>
    <configuration>
      <images>
        <image>
          <name>komushi/s3-sink-kinesis</name>
          <build>
            <from>anapsix/alpine-java:8</from>
            <volumes>
              <volume>/tmp</volume>
            </volumes>
            <entryPoint>
              <exec>
                <arg>java</arg>
                <arg>-jar</arg>
                <arg>/maven/s3-sink-rabbit.jar</arg>
              </exec>
            </entryPoint>
            <assembly>
              <descriptor>assembly.xml</descriptor>
            </assembly>
          </build>
        </image>
      </images>
    </configuration>
  </plugin>
```

#### Build and push Docker image
#### Replace 'komushi' with your your_docker_hub_id
```
$ mvn clean package docker:build
$ docker push komushi/s3-sink-kinesis
```

------
## 6. Deploy to AWS ECS
## AWS ECS Fargateに展開する

### 6-1. Create a fargate cluster with a new VPC with Console Management
[Creating a Cluster](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/create_cluster.html)

### 6-2. Set the default security group inbound rules as below

| Type            | Protocol | Port Range  | Source                      |
| --------------- | -------- | ----------- | --------------------------- |
| Custom TCP Rule | TCP      | 8000        | 0.0.0.0/0                   |
| Custom TCP Rule | TCP      | 27017       | 0.0.0.0/0                   |

### 6-3. Edit 'ContainerDefinitions.Image' in 'cfn/task-def-apps.yaml'
#### Replace 'komushi' with your your_docker_hub_id

### 6-4. Deploy resources with CloudFormation Template
![Master Stack](cfn/master.yaml)

| Parameters          | Value                                             |
| ------------------- | ------------------------------------------------- |
| Stack name          | The preferred name without spaces                 |
| FargateClusterName  | The Fargate Cluster Name created at #6-1          |
| VpcId               | The VPC created at #6-1                           |
| PublicSubnets       | The Public Subnets of the VPC created at #6-1     |
| SecurityGroup       | The ECS Security Group of the VPC created at #6-1 |
| DestinationPath     | The Destination Bucket Path to store the result   |

### 6-5. Start a new ECS task using def-flat-file-reader
#### Check the log of the apps
#### Scale out the busy apps
* def-geocoding-processor
* def-http-source

### 6-6. Athena query count and data
* Use [AWS Glue](https://console.aws.amazon.com/glue/home) to create a table geojson with crawler.
* Use [Amazon Athena](https://console.aws.amazon.com/athena/home) to query exported data.

```
select count(*) from geojson

select * from geojson
```
