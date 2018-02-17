package info.cloudnative.scs.processor.geocoding.reverse.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPolygon;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Created by lei_xu on 7/29/16.
 */
//@Document(collection = "mixblocks")
@Document
public class Block {

    @Id
    public String id;

//    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
//    public GeoJsonPolygon geometry;

    @Field("properties")
    public Properties properties;

    public class Properties {

        @Field("district")
        private String district;

        @Field("block")
        private String block;

        @Field("district_code")
        private String districCode;

        @Field("block_code")
        private String blockCode;

        public void setDistricCode(String districCode){
            this.districCode = districCode;
        }

        public String getDistricCode(){
            return this.districCode;
        }

        public void setBlockCode(String blockCode){
            this.blockCode = blockCode;
        }

        public String getBlockCode(){
            return this.blockCode;
        }

        public void setDistrict(String district){
            this.district = district;
        }

        public String getDistrict(){
            return this.district;
        }

        public void setBlock(String block){
            this.block = block;
        }

        public String getBlock(){
            return this.block;
        }
    }
}
