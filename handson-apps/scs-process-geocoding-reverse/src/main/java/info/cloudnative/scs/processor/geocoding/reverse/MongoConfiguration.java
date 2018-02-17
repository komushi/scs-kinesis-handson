package info.cloudnative.scs.processor.geocoding.reverse;

import java.util.Arrays;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.mongodb.config.AbstractMongoConfiguration;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.MongoClientOptions;



/**
 * Created by lei_xu on 7/29/16.
 */
@Configuration
@EnableConfigurationProperties(MongoProperties.class)
public class MongoConfiguration extends AbstractMongoConfiguration {

    @Autowired
    private MongoProperties properties;

    @Override
    protected String getDatabaseName() {
        return properties.getDatabase();
    }

    @Override
    public MongoClient mongoClient() {
        MongoCredential credential = MongoCredential.createCredential(properties.getUser(), properties.getDatabase(), properties.getPassword().toCharArray());
        ServerAddress serverAddress = new ServerAddress(properties.getHostName(), properties.getPort());

        MongoClientOptions.Builder optionsBuilder = new MongoClientOptions.Builder();
        optionsBuilder.maxConnectionIdleTime(60000);
        MongoClientOptions opts = optionsBuilder.build();

        // Mongo Client
        return new MongoClient(serverAddress, Arrays.asList(credential), opts); 
    }

/*
    @Override
    public Mongo mongo() throws Exception {
        MongoCredential credential = MongoCredential.createCredential(properties.getUser(), properties.getDatabase(), properties.getPassword().toCharArray());
        ServerAddress serverAddress = new ServerAddress(properties.getHostName(), properties.getPort());

        MongoClientOptions.Builder optionsBuilder = new MongoClientOptions.Builder();
        optionsBuilder.maxConnectionIdleTime(60000);
        MongoClientOptions opts = optionsBuilder.build();

        // Mongo Client
        return new MongoClient(serverAddress, Arrays.asList(credential), opts); 
    }
*/
}