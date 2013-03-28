package edu.ntu.arbor.sbchao.androidlogger.scheme;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.DaoConfig;
import de.greenrobot.dao.Property;

import edu.ntu.arbor.sbchao.androidlogger.scheme.NetworkLog;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table NETWORK_LOG.
*/
public class NetworkLogDao extends AbstractDao<NetworkLog, Long> {

    public static final String TABLENAME = "NETWORK_LOG";

    /**
     * Properties of entity NetworkLog.<br/>
     * Can be used for QueryBuilder and for referencing column names.
    */
    public static class Properties {
        public final static Property Id = new Property(0, Long.class, "id", true, "_id");
        public final static Property DeviceId = new Property(1, String.class, "deviceId", false, "DEVICE_ID");
        public final static Property Time = new Property(2, java.util.Date.class, "time", false, "TIME");
        public final static Property DayOfWeek = new Property(3, int.class, "dayOfWeek", false, "DAY_OF_WEEK");
        public final static Property HourOfDay = new Property(4, int.class, "hourOfDay", false, "HOUR_OF_DAY");
        public final static Property RecordFreq = new Property(5, int.class, "recordFreq", false, "RECORD_FREQ");
        public final static Property MobileState = new Property(6, String.class, "mobileState", false, "MOBILE_STATE");
        public final static Property WifiState = new Property(7, String.class, "wifiState", false, "WIFI_STATE");
        public final static Property TransmittedByte = new Property(8, long.class, "transmittedByte", false, "TRANSMITTED_BYTE");
        public final static Property ReceivedByte = new Property(9, long.class, "receivedByte", false, "RECEIVED_BYTE");
        public final static Property AppName = new Property(10, String.class, "appName", false, "APP_NAME");
        public final static Property IsUsing = new Property(11, boolean.class, "isUsing", false, "IS_USING");
    };


    public NetworkLogDao(DaoConfig config) {
        super(config);
    }
    
    public NetworkLogDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(SQLiteDatabase db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "'NETWORK_LOG' (" + //
                "'_id' INTEGER PRIMARY KEY ," + // 0: id
                "'DEVICE_ID' TEXT NOT NULL ," + // 1: deviceId
                "'TIME' INTEGER NOT NULL ," + // 2: time
                "'DAY_OF_WEEK' INTEGER NOT NULL ," + // 3: dayOfWeek
                "'HOUR_OF_DAY' INTEGER NOT NULL ," + // 4: hourOfDay
                "'RECORD_FREQ' INTEGER NOT NULL ," + // 5: recordFreq
                "'MOBILE_STATE' TEXT NOT NULL ," + // 6: mobileState
                "'WIFI_STATE' TEXT NOT NULL ," + // 7: wifiState
                "'TRANSMITTED_BYTE' INTEGER NOT NULL ," + // 8: transmittedByte
                "'RECEIVED_BYTE' INTEGER NOT NULL ," + // 9: receivedByte
                "'APP_NAME' TEXT NOT NULL ," + // 10: appName
                "'IS_USING' INTEGER NOT NULL );"); // 11: isUsing
    }

    /** Drops the underlying database table. */
    public static void dropTable(SQLiteDatabase db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "'NETWORK_LOG'";
        db.execSQL(sql);
    }

    /** @inheritdoc */
    @Override
    protected void bindValues(SQLiteStatement stmt, NetworkLog entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
        stmt.bindString(2, entity.getDeviceId());
        stmt.bindLong(3, entity.getTime().getTime());
        stmt.bindLong(4, entity.getDayOfWeek());
        stmt.bindLong(5, entity.getHourOfDay());
        stmt.bindLong(6, entity.getRecordFreq());
        stmt.bindString(7, entity.getMobileState());
        stmt.bindString(8, entity.getWifiState());
        stmt.bindLong(9, entity.getTransmittedByte());
        stmt.bindLong(10, entity.getReceivedByte());
        stmt.bindString(11, entity.getAppName());
        stmt.bindLong(12, entity.getIsUsing() ? 1l: 0l);
    }

    /** @inheritdoc */
    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }    

    /** @inheritdoc */
    @Override
    public NetworkLog readEntity(Cursor cursor, int offset) {
        NetworkLog entity = new NetworkLog( //
            cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // id
            cursor.getString(offset + 1), // deviceId
            new java.util.Date(cursor.getLong(offset + 2)), // time
            cursor.getInt(offset + 3), // dayOfWeek
            cursor.getInt(offset + 4), // hourOfDay
            cursor.getInt(offset + 5), // recordFreq
            cursor.getString(offset + 6), // mobileState
            cursor.getString(offset + 7), // wifiState
            cursor.getLong(offset + 8), // transmittedByte
            cursor.getLong(offset + 9), // receivedByte
            cursor.getString(offset + 10), // appName
            cursor.getShort(offset + 11) != 0 // isUsing
        );
        return entity;
    }
     
    /** @inheritdoc */
    @Override
    public void readEntity(Cursor cursor, NetworkLog entity, int offset) {
        entity.setId(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setDeviceId(cursor.getString(offset + 1));
        entity.setTime(new java.util.Date(cursor.getLong(offset + 2)));
        entity.setDayOfWeek(cursor.getInt(offset + 3));
        entity.setHourOfDay(cursor.getInt(offset + 4));
        entity.setRecordFreq(cursor.getInt(offset + 5));
        entity.setMobileState(cursor.getString(offset + 6));
        entity.setWifiState(cursor.getString(offset + 7));
        entity.setTransmittedByte(cursor.getLong(offset + 8));
        entity.setReceivedByte(cursor.getLong(offset + 9));
        entity.setAppName(cursor.getString(offset + 10));
        entity.setIsUsing(cursor.getShort(offset + 11) != 0);
     }
    
    /** @inheritdoc */
    @Override
    protected Long updateKeyAfterInsert(NetworkLog entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }
    
    /** @inheritdoc */
    @Override
    public Long getKey(NetworkLog entity) {
        if(entity != null) {
            return entity.getId();
        } else {
            return null;
        }
    }

    /** @inheritdoc */
    @Override    
    protected boolean isEntityUpdateable() {
        return true;
    }
    
}