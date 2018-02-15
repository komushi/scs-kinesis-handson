package info.cloudnative.scs.processor.geocoding.reverse;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
/**
 * Created by lei_xu on 7/28/16.
 */
@ConfigurationProperties(prefix="properties.mongo")
public class MongoProperties {

    /**
     * Host name or the IP address of the MongoDB Server
     */
    private String hostName = "localhost";

    /**
     * Port of the MongoDB Server
     */
    private Integer port = 27017;

    /**
     * Database Name
     */
    private String database = "test";

    /**
     * Collection Name
     */
    private String collection;

    /**
     * User
     */
    private String user = "";

    /**
     * Password
     */
    private String password = "";

    public String getHostName() {
        return this.hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public Integer getPort() {
        return this.port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getDatabase() {
        return this.database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    @NotBlank(message = "A valid collection is required")
    public String getCollection() {
        return this.collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public String getUser() {
        return this.user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCredential() {
        if (this.user.isEmpty()) {
            return "";
        }
        else {
            if (this.password.isEmpty()) {
                return this.user + "@";
            }
            else {
                return this.user + ":" + this.password + "@";
            }
        }

    }
}
