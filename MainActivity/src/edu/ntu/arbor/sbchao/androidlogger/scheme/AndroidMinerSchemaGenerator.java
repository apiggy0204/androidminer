package edu.ntu.arbor.sbchao.androidlogger.scheme;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Property;
import de.greenrobot.daogenerator.Schema;
import de.greenrobot.daogenerator.ToMany;
import edu.ntu.arbor.sbchao.androidlogger.logmanager.DataManager;

public class AndroidMinerSchemaGenerator {

	public static void main(String[] args) throws Exception {
        Schema schema = new Schema(8, "edu.ntu.arbor.sbchao.androidlogger.scheme");

        addMobileLog(schema);
        addNetworkLog(schema);
        addActivityLog(schema);
        
        new DaoGenerator().generateAll(schema, ".");
        
        //On database schema update, remember to change createAllTables(db, false) to createAllTables(db, true) in line 45 of DaoMaster.java.
    }

    private static void addMobileLog(Schema schema){
    	Entity mobileLog = schema.addEntity("MobileLog");
    	
    	mobileLog.addIdProperty();
    	mobileLog.addStringProperty(DataManager.DEVICE_ID).notNull();
    	mobileLog.addDateProperty(DataManager.TIME).notNull();
    	mobileLog.addIntProperty(DataManager.DAY_OF_WEEK).notNull();
    	mobileLog.addIntProperty(DataManager.HOUR_OF_DAY).notNull(); 
    	mobileLog.addIntProperty(DataManager.RECORD_FREQUENCY).notNull();
    	mobileLog.addIntProperty(DataManager.BAT_STATUS).notNull();
    	mobileLog.addDoubleProperty(DataManager.BAT_PERCENTAGE).notNull();
    	mobileLog.addIntProperty(DataManager.GPS_PROVIDER_STATUS).notNull();
    	mobileLog.addIntProperty(DataManager.NETWORK_PROVIDER_STATUS).notNull();    	
    	mobileLog.addStringProperty(DataManager.WIFI_STATE).notNull();
    	mobileLog.addStringProperty(DataManager.MOBILE_STATE).notNull();
    	mobileLog.addStringProperty(DataManager.PROCESS_CURRENT_PACKAGE).notNull();
    	mobileLog.addBooleanProperty(DataManager.IS_LOW_MEMORY).notNull();
    	mobileLog.addBooleanProperty(DataManager.IS_USING).notNull();
    	
    	mobileLog.addDoubleProperty(DataManager.LOC_ACCURACY);
    	mobileLog.addDoubleProperty(DataManager.LOC_LATITUDE);
    	mobileLog.addDoubleProperty(DataManager.LOC_LONGITUDE);
    	mobileLog.addStringProperty(DataManager.LOC_PROVIDER);
    	mobileLog.addDoubleProperty(DataManager.LOC_SPEED);
    }
    
    private static void addNetworkLog(Schema schema){
    	Entity mobileLog = schema.addEntity("NetworkLog");
    	
    	mobileLog.addIdProperty();
    	mobileLog.addStringProperty(DataManager.DEVICE_ID).notNull();
    	mobileLog.addDateProperty(DataManager.TIME).notNull();
    	mobileLog.addIntProperty(DataManager.DAY_OF_WEEK).notNull();
    	mobileLog.addIntProperty(DataManager.HOUR_OF_DAY).notNull(); 
    	mobileLog.addIntProperty(DataManager.RECORD_FREQUENCY).notNull();
    	mobileLog.addStringProperty(DataManager.MOBILE_STATE).notNull();
    	mobileLog.addStringProperty(DataManager.WIFI_STATE).notNull();
    	mobileLog.addLongProperty(DataManager.TRANSMITTED_BYTE).notNull();
    	mobileLog.addLongProperty(DataManager.RECEIVED_BYTE).notNull();
    	mobileLog.addStringProperty(DataManager.APP_NAME).notNull();
    	mobileLog.addBooleanProperty(DataManager.IS_USING).notNull();
    }

    private static void addActivityLog(Schema schema){
    	Entity mobileLog = schema.addEntity("ActivityLog");
    	
    	mobileLog.addIdProperty();
    	mobileLog.addStringProperty(DataManager.DEVICE_ID).notNull();
    	mobileLog.addDateProperty(DataManager.START_TIME).notNull();
    	mobileLog.addDateProperty(DataManager.END_TIME).notNull();
    	mobileLog.addStringProperty(DataManager.ACTIVITY_NAME).notNull();     
    	
    	mobileLog.addBooleanProperty(DataManager.UPLOADED).notNull();
    	mobileLog.addStringProperty(DataManager.ACTIVITY_COMMENT);
    }
    
    /*
    private static void addNote(Schema schema) {
        Entity note = schema.addEntity("Note");
        note.addIdProperty();
        note.addStringProperty("text").notNull();
        note.addStringProperty("comment");
        note.addDateProperty("date");
    }

    private static void addCustomerOrder(Schema schema) {
        Entity customer = schema.addEntity("Customer");
        customer.addIdProperty();
        customer.addStringProperty("name").notNull();

        Entity order = schema.addEntity("Order");
        order.setTableName("ORDERS"); // "ORDER" is a reserved keyword
        order.addIdProperty();
        Property orderDate = order.addDateProperty("date").getProperty();
        Property customerId = order.addLongProperty("customerId").notNull().getProperty();
        order.addToOne(customer, customerId);

        ToMany customerToOrders = customer.addToMany(order, customerId);
        customerToOrders.setName("orders");
        customerToOrders.orderAsc(orderDate);
    }*/

}
