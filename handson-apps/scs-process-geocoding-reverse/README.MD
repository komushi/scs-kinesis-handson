# scs-processor-geocoding-reverse
### Message-driven data microservices to transform input flat coordinates signal to local address in json format(using MongoDB for Reverse-Geocoding Query with Tokyo GeoJson data).

# I. Quick Start Steps 
## 1. Prerequisite

* [Homebrew](http://brew.sh/) if you use Mac OS X and prefer homebrew otherwise you need to install/start all the middleware components manually.
* [Homebrew Services](https://github.com/Homebrew/homebrew-services)
* [Maven](http://brewformulas.org/Maven)
* [RabbitMQ](http://brewformulas.org/Rabbitmq)
* [MongoDB](http://brewformulas.org/Mongodb)
* [Kafka](http://brewformulas.org/Kafka) is an alternative for messaging.
* [scs-source-http](https://github.com/komushi/scs-source-http)
* [scs-sink-gemfire](https://github.com/komushi/scs-sink-gemfire)

### 1-1. Start RabbitMQ
```
brew services start rabbitmq
```

### 1-2. MongoDB for Reverse-Geocoding

Start MongoDB
```
brew services start mongodb
```
The default dbpath is explained [here](https://gist.github.com/subfuzion/9630872).

Import GeoJson for Reverse-Geocoding.
```
mongoimport --db geojson --collection blocks --type json --file "data/chiyoda_sample.json"
```

Create a user to enable geojson access
```
use geojson

db.createUser(
    {
      user: "root",
      pwd: "keepitsimple",
      roles: ["readWrite"]
    }
) 
```

Test in mongo shell
```
db.blocks.createIndex({ geometry: "2dsphere" })

db.blocks.find(
   {
     geometry: {
       $geoIntersects: {
          $geometry: {
             type: "Point" ,
             coordinates: [ 139.743317, 35.682955 ]
          }
       }
     }
   }
)
```

Result:
```
{
  "_id" : ObjectId("579c2aaa35a06dc3a623daf7"),
  "properties": {
    "district": "千代田区",
    "block": "隼町",
    "code": "1010070"
  },
  "geometry": {
    "type": "Polygon",
    "coordinates": [
      [
        [
          139.74167300322014,
          35.683406014170174
        ],
        ...
        ...
        [
          139.74167300322014,
          35.683406014170174
        ]
      ]
    ]
  }
}
```

## 2. Download and Build

```
git clone https://github.com/komushi/scs-processor-geocoding-reverse.git
cd scs-processor-geocoding-reverse
mvn clean package docker:build
docker push komushi/scs-processor-geocoding-reverse
```

## 3. Run in Java CLI
### 3-1. Command to start jar
```
java -jar target/scs-processor-geocoding-reverse-1.0.0.RELEASE.jar --spring.cloud.stream.defaultBinder=<rabbit/kinesis/kafka> --properties.mongo.hostName=<mongodb-address> --logging.level.info.cloudnative=TRACE
```

### 3-2. Test with scs-source-http and scs-sink-gemfire
```
java -jar target/scs-source-http-1.0.0-RELEASE.jar

java -jar target/scs-processor-geocoding-reverse-1.0.0-RELEASE.jar --logging.level.info.cloudnative=TRACE

java -jar target/scs-sink-gemfire-1.0.0.RELEASE.jar

curl -H "Content-Type: text/plain" -X POST -d'468244D1361B8A3EB8D206CC394BC9E9,BB899DFEA9CC964B50C540A1D685CCFB,2013-01-01 00:00:00,2013-01-01 00:04:00,240,1.71,139.752268,35.677043,139.771699,35.697283,CSH,6.50,0.50,0.50,0.00,0.00,7.50' 'http://localhost:9000'
```

# 4. Another deployment option - spring boot in docker

### 4-1. Local Execution on RabbitMQ
```
docker run -p 9000:9000 komushi/scs-source-http --spring.rabbitmq.host=docker.for.mac.localhost

docker run komushi/scs-processor-geocoding-reverse --spring.rabbitmq.host=docker.for.mac.localhost --properties.mongo.hostName=docker.for.mac.localhost --logging.level.info.cloudnative=TRACE
```

### 4-2. Deploy on AWS
```
docker run komushi/scs-processor-geocoding-reverse --spring.cloud.stream.defaultBinder=kinesis --cloud.aws.region.static=us-east-1  --properties.mongo.hostName=<mongodb-address>
```

# II. Appendix
### Default Parameters - application.properties
```
server.port=9100
spring.cloud.stream.defaultBinder=kinesis
spring.cloud.stream.bindings.input.destination=http_raw
spring.cloud.stream.bindings.input.group=geocoding
spring.cloud.stream.bindings.input.contentType=text/plain
spring.cloud.stream.bindings.output.destination=transform_geotuple
spring.cloud.stream.bindings.output.contentType=application/json
spring.cloud.stream.bindings.output.producer.partitionKeyExpression=1
cloud.aws.region.static=us-east-1
properties.mongo.hostName=docker.for.mac.localhost
properties.mongo.port=27017
properties.mongo.database=geojson
properties.mongo.collection=blocks
properties.mongo.user=root
properties.mongo.password=keepitsimple
```