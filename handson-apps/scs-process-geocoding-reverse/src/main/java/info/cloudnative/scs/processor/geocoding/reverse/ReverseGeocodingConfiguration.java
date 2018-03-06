package info.cloudnative.scs.processor.geocoding.reverse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.deser.Deserializers;
import com.mongodb.Mongo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Processor;;

import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.messaging.Message;

import org.springframework.tuple.Tuple;
import org.springframework.tuple.TupleBuilder;

import java.util.Calendar;
import java.util.UUID;
import java.util.List;

import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.MongoOperations;

import info.cloudnative.scs.processor.geocoding.reverse.model.Block;

/**
 * Created by lei_xu on 7/28/16.
 */
@EnableBinding(Processor.class)
@EnableConfigurationProperties(MongoProperties.class)
public class ReverseGeocodingConfiguration {

    private static final Log logger = LogFactory.getLog(ReverseGeocodingConfiguration.class);

    private static final String delims = "[,]";

    @Autowired
    private MongoProperties properties;

    @Autowired
    private MongoOperations mongoOperations;

    @Transformer(inputChannel = Processor.INPUT, outputChannel = Processor.OUTPUT)
    public Tuple transform(Message<?> message) {
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("Handling message: %s", message));
        }

        Object payloadObj = message.getPayload();
        String payload = null;

        if (payloadObj instanceof String) {
            logger.trace(String.format("payloadObj is String"));
            payload = payloadObj.toString();
        } else {
            logger.trace(String.format("payloadObj is byte[]"));
            byte[] bytes = (byte[])payloadObj;
            
            try {
                payload = new String(bytes, "UTF-8");    
            }
            catch (Exception ex) {
                throw new MessageTransformationException("payloadObj transform failed");
            }
            
        }

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("payload: %s", payload));
        }

        if (payload == null) {
            throw new MessageTransformationException(message, "payload empty");
        }


        String[] tokens = payload.split(delims);

        double pickupLatitude = java.lang.Double.parseDouble(tokens[9]);
        double pickupLongitude = java.lang.Double.parseDouble(tokens[8]);
        double dropoffLatitude = java.lang.Double.parseDouble(tokens[7]);
        double dropoffLongitude = java.lang.Double.parseDouble(tokens[6]);
        String dropoffDatetime = tokens[3];
        String pickupDatetime = tokens[2];


        // get block name
        Query pickupQuery = Query.query(Criteria.where("geometry").intersects(new GeoJsonPoint(pickupLongitude, pickupLatitude)));
        pickupQuery.fields().include("_id");
        pickupQuery.fields().include("properties");
        Query dropoffQuery = Query.query(Criteria.where("geometry").intersects(new GeoJsonPoint(dropoffLongitude, dropoffLatitude)));
        dropoffQuery.fields().include("_id");
        dropoffQuery.fields().include("properties");

        List<Block> pickupBlocks =  mongoOperations.find(pickupQuery, Block.class, properties.getCollection());
        List<Block> dropoffBlocks =  mongoOperations.find(dropoffQuery, Block.class, properties.getCollection());

        if (pickupBlocks.size() == 0 || dropoffBlocks.size() == 0) {
            if (pickupBlocks.size() == 0) {
                throw new MessageTransformationException(message, "pickup coordinates out of scope:" + new GeoJsonPoint(pickupLongitude, pickupLatitude));
            } else if (dropoffBlocks.size() == 0) {
                throw new MessageTransformationException(message, "dropoff coordinates out of scope:" + new GeoJsonPoint(dropoffLongitude, dropoffLatitude));
            }
        }

        String pickupBlockCode = pickupBlocks.get(0).properties.getBlockCode();
        String pickupBlock = pickupBlocks.get(0).properties.getBlock();
        String pickupDistrictCode = pickupBlocks.get(0).properties.getDistricCode();
        String pickupDistrict = pickupBlocks.get(0).properties.getDistrict();

        String dropoffBlockCode = dropoffBlocks.get(0).properties.getBlockCode();
        String dropoffBlock = dropoffBlocks.get(0).properties.getBlock();
        String dropoffDistrictCode = dropoffBlocks.get(0).properties.getDistricCode();
        String dropoffDistrict = dropoffBlocks.get(0).properties.getDistrict();

        String route = pickupBlockCode + "_" + dropoffBlockCode;
        String pickupAddress = pickupDistrict + " " + pickupBlock;
        pickupAddress = pickupAddress.trim();
        String dropoffAddress = dropoffDistrict + " " + dropoffBlock;
        dropoffAddress = dropoffAddress.trim();

        Tuple tuple = null;

        tuple = TupleBuilder.tuple()
            .put("uuid", UUID.randomUUID())
            .put("route", route)
            .put("timestamp", Calendar.getInstance().getTimeInMillis())
            .put("pickupAddress", pickupAddress)
            .put("dropoffAddress", dropoffAddress)
            .put("pickupLatitude", pickupLatitude)
            .put("pickupLongitude", pickupLongitude)
            .put("dropoffLatitude", dropoffLatitude)
            .put("dropoffLongitude", dropoffLongitude)
            .put("pickupDatetime", pickupDatetime)
            .put("dropoffDatetime", dropoffDatetime)
            .put("pickupBlockCode", pickupBlockCode)
            .put("pickupBlock", pickupBlock)
            .put("pickupDistrictCode", pickupDistrictCode)
            .put("pickupDistrict", pickupDistrict)
            .put("dropoffBlockCode", dropoffBlockCode)
            .put("dropoffBlock", dropoffBlock)
            .put("dropoffDistrictCode", dropoffDistrictCode)
            .put("dropoffDistrict", dropoffDistrict)
            .build();

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("tuple: %s", tuple.toString()));
        }

        return tuple;

    }

}
