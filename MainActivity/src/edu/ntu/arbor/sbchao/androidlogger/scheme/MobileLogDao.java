package edu.ntu.arbor.sbchao.androidlogger.scheme;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.DaoConfig;
import de.greenrobot.dao.Property;

import edu.ntu.arbor.sbchao.androidlogger.scheme.MobileLog;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table MOBILE_LOG.
*/
public class MobileLogDao extends AbstractDao<MobileLog, Long> {

    public static final String TABLENAME = "MOBILE_LOG";

    /**
     * Properties of entity MobileLog.<br/>
     * Can be used for QueryBuilder and for referencing column names.
    */
    public static class Properties {
        public final static Property Id = new Property(0, Long.class, "id", true, "_id");
        public final static Property DeviceId = new Property(1, String.class, "deviceId", false, "DEVICE_ID");
        public final static Property Time = new Property(2, java.util.Date.class, "time", false, "TIME");
        public final static Property RecordFreq = new Property(3, int.class, "recordFreq", false, "RECORD_FREQ");
        public final static Property BatStatus = new Property(4, int.class, "batStatus", false, "BAT_STATUS");
        public final static Property BatPct = new Property(5, double.class, "batPct", false, "BAT_PCT");
        public final static Property GpsStatus = new Property(6, int.class, "gpsStatus", false, "GPS_STATUS");
        public final static Property NetworkStatus = new Property(7, int.class, "networkStatus", false, "NETWORK_STATUS");
        public final static Property WifiState = new Property(8, String.class, "wifiState", false, "WIFI_STATE");
        public final static Property MobileState = new Property(9, String.class, "mobileState", false, "MOBILE_STATE");
        public final static Property ProcessCurrentPackage = new Property(10, String.class, "processCurrentPackage", false, "PROCESS_CURRENT_PACKAGE");
        public final static Property IsLowMemory = new Property(11, boolean.class, "isLowMemory", false, "IS_LOW_MEMORY");
        public final static Property LocAcc = new Property(12, Double.class, "locAcc", false, "LOC_ACC");
        public final static Property Lat = new Property(13, Double.class, "lat", false, "LAT");
        public final static Property Lon = new Property(14, Double.class, "lon", false, "LON");
        public final static Property LocProvider = new Property(15, String.class, "locProvider", false, "LOC_PROVIDER");
        public final static Property Speed = new Property(16, Double.class, "speed", false, "SPEED");
    };


    public MobileLogDao(DaoConfig config) {
        super(config);
    }
    
    public MobileLogDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(SQLiteDatabase db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "'MOBILE_LOG' (" + //
                "'_id' INTEGER PRIMARY KEY ," + // 0: id
                "'DEVICE_ID' TEXT NOT NULL ," + // 1: deviceId
                "'TIME' INTEGER NOT NULL ," + // 2: time
                "'RECORD_FREQ' INTEGER NOT NULL ," + // 3: recordFreq
                "'BAT_STATUS' INTEGER NOT NULL ," + // 4: batStatus
                "'BAT_PCT' REAL NOT NULL ," + // 5: batPct
                "'GPS_STATUS' INTEGER NOT NULL ," + // 6: gpsStatus
                "'NETWORK_STATUS' INTEGER NOT NULL ," + // 7: networkStatus
                "'WIFI_STATE' TEXT NOT NULL ," + // 8: wifiState
                "'MOBILE_STATE' TEXT NOT NULL ," + // 9: mobileState
                "'PROCESS_CURRENT_PACKAGE' TEXT NOT NULL ," + // 10: processCurrentPackage
                "'IS_LOW_MEMORY' INTEGER NOT NULL ," + // 11: isLowMemory
                "'LOC_ACC' REAL," + // 12: locAcc
                "'LAT' REAL," + // 13: lat
                "'LON' REAL," + // 14: lon
                "'LOC_PROVIDER' TEXT," + // 15: locProvider
                "'SPEED' REAL);"); // 16: speed
    }

    /** Drops the underlying database table. */
    public static void dropTable(SQLiteDatabase db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "'MOBILE_LOG'";
        db.execSQL(sql);
    }

    /** @inheritdoc */
    @Override
    protected void bindValues(SQLiteStatement stmt, MobileLog entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
        stmt.bindString(2, entity.getDeviceId());
        stmt.bindLong(3, entity.getTime().getTime());
        stmt.bindLong(4, entity.getRecordFreq());
        stmt.bindLong(5, entity.getBatStatus());
        stmt.bindDouble(6, entity.getBatPct());
        stmt.bindLong(7, entity.getGpsStatus());
        stmt.bindLong(8, entity.getNetworkStatus());
        stmt.bindString(9, entity.getWifiState());
        stmt.bindString(10, entity.getMobileState());
        stmt.bindString(11, entity.getProcessCurrentPackage());
        stmt.bindLong(12, entity.getIsLowMemory() ? 1l: 0l);
 
        Double locAcc = entity.getLocAcc();
        if (locAcc != null) {
            stmt.bindDouble(13, locAcc);
        }
 
        Double lat = entity.getLat();
        if (lat != null) {
            stmt.bindDouble(14, lat);
        }
 
        Double lon = entity.getLon();
        if (lon != null) {
            stmt.bindDouble(15, lon);
        }
 
        String locProvider = entity.getLocProvider();
        if (locProvider != null) {
            stmt.bindString(16, locProvider);
        }
 
        Double speed = entity.getSpeed();
        if (speed != null) {
            stmt.bindDouble(17, speed);
        }
    }

    /** @inheritdoc */
    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }    

    /** @inheritdoc */
    @Override
    public MobileLog readEntity(Cursor cursor, int offset) {
        MobileLog entity = new MobileLog( //
            cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // id
            cursor.getString(offset + 1), // deviceId
            new java.util.Date(cursor.getLong(offset + 2)), // time
            cursor.getInt(offset + 3), // recordFreq
            cursor.getInt(offset + 4), // batStatus
            cursor.getDouble(offset + 5), // batPct
            cursor.getInt(offset + 6), // gpsStatus
            cursor.getInt(offset + 7), // networkStatus
            cursor.getString(offset + 8), // wifiState
            cursor.getString(offset + 9), // mobileState
            cursor.getString(offset + 10), // processCurrentPackage
            cursor.getShort(offset + 11) != 0, // isLowMemory
            cursor.isNull(offset + 12) ? null : cursor.getDouble(offset + 12), // locAcc
            cursor.isNull(offset + 13) ? null : cursor.getDouble(offset + 13), // lat
            cursor.isNull(offset + 14) ? null : cursor.getDouble(offset + 14), // lon
            cursor.isNull(offset + 15) ? null : cursor.getString(offset + 15), // locProvider
            cursor.isNull(offset + 16) ? null : cursor.getDouble(offset + 16) // speed
        );
        return entity;
    }
     
    /** @inheritdoc */
    @Override
    public void readEntity(Cursor cursor, MobileLog entity, int offset) {
        entity.setId(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setDeviceId(cursor.getString(offset + 1));
        entity.setTime(new java.util.Date(cursor.getLong(offset + 2)));
        entity.setRecordFreq(cursor.getInt(offset + 3));
        entity.setBatStatus(cursor.getInt(offset + 4));
        entity.setBatPct(cursor.getDouble(offset + 5));
        entity.setGpsStatus(cursor.getInt(offset + 6));
        entity.setNetworkStatus(cursor.getInt(offset + 7));
        entity.setWifiState(cursor.getString(offset + 8));
        entity.setMobileState(cursor.getString(offset + 9));
        entity.setProcessCurrentPackage(cursor.getString(offset + 10));
        entity.setIsLowMemory(cursor.getShort(offset + 11) != 0);
        entity.setLocAcc(cursor.isNull(offset + 12) ? null : cursor.getDouble(offset + 12));
        entity.setLat(cursor.isNull(offset + 13) ? null : cursor.getDouble(offset + 13));
        entity.setLon(cursor.isNull(offset + 14) ? null : cursor.getDouble(offset + 14));
        entity.setLocProvider(cursor.isNull(offset + 15) ? null : cursor.getString(offset + 15));
        entity.setSpeed(cursor.isNull(offset + 16) ? null : cursor.getDouble(offset + 16));
     }
    
    /** @inheritdoc */
    @Override
    protected Long updateKeyAfterInsert(MobileLog entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }
    
    /** @inheritdoc */
    @Override
    public Long getKey(MobileLog entity) {
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
