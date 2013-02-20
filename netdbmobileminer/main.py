#!/usr/bin/env python
#
# Copyright 2007 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
from datetime import datetime
from google.appengine.api import users
from google.appengine.ext import db
from google.appengine.ext.webapp.util import run_wsgi_app
from google.appengine.runtime import apiproxy_errors
import json
import logging
import webapp2
import wsgiref.handlers


class MobileLog(db.Model):
    deviceId = db.StringProperty()	
        
    recordTime = db.DateTimeProperty()
    recordFreq = db.IntegerProperty()

    #batLevel   = db.IntegerProperty()
    #batScale   = db.IntegerProperty()
    #batVoltage = db.IntegerProperty()
    batStatus  = db.IntegerProperty()
    #batPlugged = db.IntegerProperty()
    batPercentage = db.FloatProperty()

    #isGPSProviderEnabled = db.BooleanProperty()	
    #isNetworkProviderEnabled = db.BooleanProperty()
    GPSProviderStatus = db.IntegerProperty()
    networkProviderStatus = db.IntegerProperty()
    locAccuracy = db.FloatProperty()
    locProvider = db.StringProperty( choices = set(["network", "gps"]) )
    #locAltitude = db.FloatProperty()
    locLatitude = db.FloatProperty()
    locLongitude = db.FloatProperty()
    locSpeed = db.FloatProperty()    

    #callState = db.IntegerProperty()
    #inNumber = db.StringProperty()

    #connectivity = db.BooleanProperty()	
    #activeNetworkType = db.IntegerProperty()
    #isMobileAvailable = db.BooleanProperty()		
    #isMobileConnected = db.BooleanProperty()	
    #isMobileFailover = db.BooleanProperty()
    #isMobileRoaming = db.BooleanProperty()	
    mobileState = db.StringProperty( choices = set(["CONNECTED", "CONNECTING", "DISCONNECTED", "DISCONNECTING", "SUSPENDED", "UNKNOWN"]) )
    #isWifiAvailable = db.BooleanProperty()			
    #isWifiConnected = db.BooleanProperty()
    #isWifiFailover = db.BooleanProperty()	
    #isWifiRoaming = db.BooleanProperty()	
    wifiState = db.StringProperty( choices = set(["CONNECTED", "CONNECTING", "DISCONNECTED", "DISCONNECTING", "SUSPENDED", "UNKNOWN"]) )
        
    processCurrentClass = db.StringProperty()
    #processCurrentPackage = db.StringProperty()	
    #availMem = db.IntegerProperty()
    isLowMemory = db.BooleanProperty()	
	
	

class MainHandler(webapp2.RequestHandler):
		
    def get(self):
        try:
            #Only for testing
            _deviceId = self.request.get('deviceId')					
            mobileLog = MobileLog(deviceId = _deviceId)			
            mobileLog.put()
            self.response.write(mobileLog.deviceId)						
				
        except apiproxy_errors.DeadlineExceededError:
            self.response.clear()
            self.response.set_status(500)
            self.response.out.write("This operation could not be completed in time...")
		
    def post(self):
        try :	
            
            _deviceId = self.request.get('deviceId')	
            rawTimeStr = self.request.get('time')        
            mobileLog = MobileLog( key_name = _deviceId + rawTimeStr ) #Avoid repeated uploads of a record

            _deviceId = self.request.get('deviceId')	
            mobileLog.deviceId = _deviceId
            
            _time     = datetime.strptime(self.request.get('time'), "%Y/%m/%d %H:%M:%S")
            mobileLog.recordTime = _time

            try:
                _recordFreq = int(self.request.get('recordFreq'))
                mobileLog.recordFreq = _recordFreq
            except ValueError:
                pass
                
            #_batLevel   = int(self.request.get('batLevel'))
            #_batScale   = int(self.request.get('batScale'))
            #_batVoltage = int(self.request.get('batVoltage'))

            try:
                _batStatus  = int(self.request.get('batStatus'))
                mobileLog.batStatus = _batStatus
            except ValueError:
                pass
                
            #_batPlugged = int(self.request.get('batPlugged'))

            try:
                _batPercentage = float(self.request.get('batPercentage'))
                mobileLog.batPercentage = _batPercentage
            except ValueError:
                pass


            #_isGPSProviderEnabled = bool(self.request.get('isGPSProviderEnabled'))
            #_isNetworkProviderEnabled = bool(self.request.get('isNetworkProviderEnabled'))

            try:
                _GPSProviderStatus = int(self.request.get('GPSProviderStatus'))
                mobileLog.GPSProviderStatus = _GPSProviderStatus
            except ValueError:
                pass
                
            try:
                _networkProviderStatus = int(self.request.get('networkProviderStatus'))
                mobileLog.networkProviderStatus = _networkProviderStatus
            except ValueError:
                pass
                
            try:
               _locAccuracy = float(self.request.get('locAccuracy'))
               mobileLog.locAccuracy = _locAccuracy
            except ValueError:
                pass

            _locProvider = self.request.get('locProvider')
            if _locProvider != "null":
                mobileLog.locProvider = _locProvider
            #_locAltitude = float(self.request.get('locAltitude'))

            try:
                _locLatitude = float(self.request.get('locLatitude'))
                mobileLog.locLatitude = _locLatitude
            except ValueError:
                pass
                
            try:
                _locLongitude = float(self.request.get('locLongitude'))
                mobileLog.locLongitude = _locLongitude
            except ValueError:
                pass
                
            try:
                _locSpeed = float(self.request.get('locSpeed'))
                mobileLog.locSpeed = _locSpeed
            except ValueError:
                pass


            #_callState = int(self.request.get('callState'))
            #_inNumber = self.request.get('incomingNumber')

            #_connectivity = bool(self.request.get('connectivity'))
            #_activeNetworkType = int(self.request.get('activeNetworkType'))
            #_isMobileAvailable = bool(self.request.get('isMobileAvailable'))
            #_isMobileConnected = bool(self.request.get('isMobileConnected'))
            #_isMobileFailover = bool(self.request.get('isMobileFailover'))
            #_isMobileRoaming = bool(self.request.get('isMobileRoaming'))
            _mobileState = self.request.get('mobileState')
            mobileLog.mobileState = _mobileState
            #_isWifiAvailable = bool(self.request.get('isWifiAvailable'))
            #_isWifiConnected = bool(self.request.get('isWifiConnected'))	
            #_isWifiFailover = bool(self.request.get('isWifiFailover'))
            #_isWifiRoaming = bool(self.request.get('isWifiRoaming'))
            _wifiState = self.request.get('wifiState')
            mobileLog.wifiState = _wifiState
            #_processCurrentClass = self.request.get('processCurrentClass')
            #mobileLog.processCurrentClass = _processCurrentClass
            _processCurrentPackage = self.request.get('processCurrentPackage')
            mobileLog.processCurrentPackage = _processCurrentPackage

            #_availMem = int(self.request.get('availMem'))
            try:
                _isLowMemory = bool(self.request.get('isLowMemory'))
                mobileLog.isLowMemory = _isLowMemory
            except ValueError:
                pass


            """
            mobileLog.batStatus = _batStatus
            mobileLog.batPercentage = _batPercentage
            mobileLog.isGPSProviderEnabled = _isGPSProviderEnabled               
            mobileLog.isNetworkProviderEnabled = _isNetworkProviderEnabled
            mobileLog.GPSProviderStatus = _GPSProviderStatus
            mobileLog.networkProviderStatus = _networkProviderStatus
            mobileLog.locAccuracy = _locAccuracy
            mobileLog.locProvider = _locProvider
            #mobileLog.locAltitude = _locAltitude
            mobileLog.locLatitude = _locLatitude
            mobileLog.locLongitude = _locLongitude
            mobileLog.locSpeed = _locSpeed
            #mobileLog.callState = _callState
            #mobileLog.inNumber = _inNumber
            mobileLog.connectivity = _connectivity
            mobileLog.activeNetworkType = _activeNetworkType
            mobileLog.isMobileAvailable = _isMobileAvailable
            mobileLog.isMobileConnected = _isMobileConnected
            mobileLog.isMobileFailover = _isMobileFailover
            mobileLog.isMobileRoaming = _isMobileRoaming
            mobileLog.mobileState = _mobileState
            mobileLog.isWifiAvailable = _isWifiAvailable
            mobileLog.isWifiConnected = _isWifiConnected
            mobileLog.isWifiFailover = _isWifiFailover
            mobileLog.isWifiRoaming = _isWifiRoaming
            mobileLog.wifiState = _wifiState
            mobileLog.processCurrentClass = _processCurrentClass
            mobileLog.processCurrentPackage = _processCurrentPackage
            #mobileLog.availMem = _availMem
            mobileLog.isLowMemory = _isLowMemory			
            """
                        

            """
            mobileLog = MobileLog(deviceId = _deviceId, recordTime = _time, recordFreq = _recordFreq, batLevel = _batLevel, 
                batScale = _batScale, batVoltage = _batVoltage, batStatus = _batStatus,
                batPlugged = _batPlugged, batPercentage = _batPercentage, isGPSProviderEnabled = _isGPSProviderEnabled,
                isNetworkProviderEnabled = _isNetworkProviderEnabled, GPSProviderStatus = _GPSProviderStatus,
                networkProviderStatus = _networkProviderStatus, locAccuracy = _locAccuracy,
                locProvider = _locProvider, locAltitude = _locAltitude, locLatitude = _locLatitude,
                locLongitude = _locLongitude, locSpeed = _locSpeed, 			
                callState = _callState, inNumber = _inNumber, connectivity = _connectivity,
                activeNetworkType = _activeNetworkType, isMobileAvailable = _isMobileAvailable,
                isMobileConnected = _isMobileConnected, isMobileFailover = _isMobileFailover,
                isMobileRoaming = _isMobileRoaming, mobileState = _mobileState,
                isWifiAvailable = _isWifiAvailable, isWifiConnected = _isWifiConnected,
                isWifiFailover = _isWifiFailover, isWifiRoaming = _isWifiRoaming,
                wifiState = _wifiState,	processCurrentClass = _processCurrentClass,
                processCurrentPackage = _processCurrentPackage, availMem = _availMem,
                isLowMemory = _isLowMemory)
            """

            mobileLog.put()			
			
        except apiproxy_errors.DeadlineExceededError:
            self.response.clear()
            self.response.set_status(500)
            self.response.out.write("This operation could not be completed in time...")
			
	
			
app = webapp2.WSGIApplication([
    ('/', MainHandler)
], debug=True)

def main():
    run_wsgi_app(app)

if __name__ == "__main__":
    main()