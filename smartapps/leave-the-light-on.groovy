/**
 *  Leave The Light On
 *
 *  Copyright 2017 Todd Trivette
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Leave The Light On",
    namespace: "toddtriv",
    author: "Todd Trivette",
    description: "Turn on a device when a device is not home and it gets dark",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("When this device is away..."){
		input "myMobileDevice", "capability.presenceSensor", required: true
	}
	section("Turn something on if it's dark..."){
		input "switch1", "capability.switch", multiple: true
	}
	section ("Sunset offset (optional)...") {
		input "sunsetOffsetValue", "text", title: "HH:MM", required: false
		input "sunsetOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"]
	}
}

def installed() {	
	subscribe(myMobileDevice, "presence", presenceHandler)    
}

def updated() {	
	unsubscribe()
	subscribe(myMobileDevice, "presence", presenceHandler)
}

def presenceHandler(evt) {		    
    def isAway = (myMobileDevice.currentPresence=="not present")
    def switchIsOn = (switch1.currentSwitch[0]=="on")
          
    if(switchIsOn || (!isAway && !switchIsOn)){    	
    	log.info "No need to turn on the $switch1.label device. Stop checking...."
    	unschedule(presenceHandler)
    	return
    }  
                 
    def now = new Date()
    def actualSunsetTime = getSunriseAndSunset().sunset;
    def offsetSunsetTime = getSunriseAndSunset(sunsetOffset: getSunsetOffset()).sunset 
	       		   
	if(isAway && (now >= offsetSunsetTime)) {
		switch1.on()
		log.info "$switch1.label device was turned on because $myMobileDevice.label is not home"
	} 
    else{
    	log.info "Not time to turn on the $switch1.label device yet, keep checking...."
    	runEvery10Minutes(presenceHandler)
    }
}

private getSunsetOffset() {	    
	if (sunsetOffsetDir == null)
    	return null
      
	sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : null              
}
