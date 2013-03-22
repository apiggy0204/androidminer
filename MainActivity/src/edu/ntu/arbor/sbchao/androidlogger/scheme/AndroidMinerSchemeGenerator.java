package edu.ntu.arbor.sbchao.androidlogger.scheme;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Property;
import de.greenrobot.daogenerator.Schema;
import de.greenrobot.daogenerator.ToMany;
import edu.ntu.arbor.sbchao.androidlogger.logmanager.DataManager;

public class AndroidMinerSchemeGenerator {

	public static void main(String[] args) throws Exception {
        Schema schema = new Schema(1, "edu.ntu.arbor.sbchao.androidlogger.scheme");

        addMobileLog(schema);
        
        new DaoGenerator().generateAll(schema, ".");
    }

    private static void addMobileLog(Schema schema){
    	Entity mobileLog = schema.addEntity("MobileLog");
    	
    	mobileLog.addIdProperty();
    	mobileLog.addStringProperty(DataManager.DEVICE_ID).notNull();
    	mobileLog.addDateProperty(DataManager.TIME).notNull();
    	mobileLog.addIntProperty("dayOfWeek").notNull();
    	mobileLog.addIntProperty("hourOfDay").notNull(); 
    	mobileLog.addIntProperty(DataManager.RECORD_FREQUENCY).notNull();
    	mobileLog.addIntProperty(DataManager.BAT_STATUS).notNull();
    	mobileLog.addDoubleProperty(DataManager.BAT_PERCENTAGE).notNull();
    	mobileLog.addIntProperty(DataManager.GPS_PROVIDER_STATUS).notNull();
    	mobileLog.addIntProperty(DataManager.NETWORK_PROVIDER_STATUS).notNull();    	
    	mobileLog.addStringProperty(DataManager.WIFI_STATE).notNull();
    	mobileLog.addStringProperty(DataManager.MOBILE_STATE).notNull();
    	mobileLog.addStringProperty(DataManager.PROCESS_CURRENT_PACKAGE).notNull();
    	mobileLog.addBooleanProperty(DataManager.IS_LOW_MEMORY).notNull();
    	
    	mobileLog.addDoubleProperty(DataManager.LOC_ACCURACY);
    	mobileLog.addDoubleProperty(DataManager.LOC_LATITUDE);
    	mobileLog.addDoubleProperty(DataManager.LOC_LONGITUDE);
    	mobileLog.addStringProperty(DataManager.LOC_PROVIDER);
    	mobileLog.addDoubleProperty(DataManager.LOC_SPEED);
    	
    	 	    	
    	
    }
    
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
    }

}
