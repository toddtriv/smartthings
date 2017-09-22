/**
 *  MIMOlite device type for garage door button, including power failure indicator.  Be sure mimolite has jumper removed before
 *  including the device to your hub, and tap Config to ensure power alarm is subscribed.
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
 *
 *  Original Author: John Constantelos (jscgs350) 
 *  Orignal Code: https://github.com/constjs/jcdevhandlers/blob/master/devicetypes/jscgs350/my-mimolite-garage-door-controller.src/my-mimolite-garage-door-controller.groovy
 *  Modified By: Todd Trivette
 */
metadata {	
	definition (name: "MiMOlite Device Type", namespace: "toddtriv", author: "John Constantelos") {
        capability "Momentary"
        capability "Relay Switch"
		capability "Polling"
        capability "Refresh"
        capability "Switch"
        capability "Sensor"
        capability "Contact Sensor"
        capability "Configuration"
		capability "Actuator"
		capability "Door Control"
		capability "Garage Door Control"
        capability "Health Check"
        
		attribute "powered", "string"
        attribute "contactState", "string"       
        
		command "on"
		command "off"
        command "open"
        command "close"
	}

	// UI tile definitions 
	tiles(scale: 2) {
		multiAttributeTile(name:"actuate", type: "generic", width: 6, height: 4){
			tileAttribute ("device.door", key: "PRIMARY_CONTROL") {
                attributeState("unknown", label:'${name}', action:"refresh.refresh", icon:"st.doors.garage.garage-open", backgroundColor:"#e86d13")
                attributeState("closed", label:'${name}', action:"door control.open", icon:"st.doors.garage.garage-closed", backgroundColor:"#00a0dc", nextState:"opening")
                attributeState("open", label:'${name}', action:"door control.close", icon:"st.doors.garage.garage-open", backgroundColor:"#e86d13", nextState:"closing")
                attributeState("opening", label:'${name}', icon:"st.doors.garage.garage-opening", backgroundColor:"#ffD700")
                attributeState("closing", label:'${name}', icon:"st.doors.garage.garage-closing", backgroundColor:"#ffD700")
            }
            tileAttribute ("device.contactState", key: "SECONDARY_CONTROL") {
                attributeState("default", label:'${currentValue}', icon: "https://raw.githubusercontent.com/constjs/jcdevhandlers/master/img/icon-garage1.png")
            }
		}
        standardTile("switch", "device.switch", width: 1, height: 1, canChangeIcon: true) {
            state "off", label: 'Off', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "on"
            state "on", label: 'On', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821", nextState: "off"
        }
        standardTile("contact", "device.contact", inactiveLabel: false) {
			state "open", label: '${name}', icon: "st.doors.garage.garage-open", backgroundColor: "#e86d13"
			state "closed", label: '${name}', icon: "st.doors.garage.garage-closed", backgroundColor: "#00A0DC"
		}
        standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'Refresh', action:"refresh.refresh", icon:"st.secondary.refresh-icon"
		}
        standardTile("powered", "device.powered", width: 2, height: 2, inactiveLabel: false) {
			state "powerOn", label: "Power On", icon: "st.switches.switch.on", backgroundColor: "#79b821"
			state "powerOff", label: "Power Off", icon: "st.switches.switch.off", backgroundColor: "#ff0000"
		}
		standardTile("configure", "device.configure", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
		}
        standardTile("blankTile", "statusText", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
			state "default", label:'', icon:"http://cdn.device-icons.smartthings.com/secondary/device-activity-tile@2x.png"
		}  
        standardTile("statusText", "statusText", inactiveLabel: false, decoration: "flat", width: 5, height: 1) {
			state "statusText", label:'${currentValue}', backgroundColor:"#ffffff"
		} 
		main (["contact"])
		details(["actuate", "blankTile", "statusText", "powered", "refresh", "configure"])
    }
}

def updated(){				
    response( configure() )
    log.info "$device.displayName was updated...."
}

def parse(String description) {	
	def result = null
    def POWER_LOSS_CODE = "7105"
    def cmd = zwave.parse(description, [0x72: 1, 0x86: 1, 0x71: 1, 0x30: 1, 0x31: 3, 0x35: 1, 0x70: 1, 0x85: 1, 0x25: 1, 0x03: 1, 0x20: 1, 0x84: 1])
            
    if (cmd.CMD == POWER_LOSS_CODE) {				//Mimo sent a power loss report
    	log.warn "$device.displayName lost power"
    	sendEvent(name: "powered", value: "powerOff", descriptionText: "$device.displayName lost power")
    } else {
    	sendEvent(name: "powered", value: "powerOn", descriptionText: "$device.displayName regained power")
    }
    
	if (cmd) {
    	log.debug "creating Zwave Event for $cmd"
		result = createEvent( zwaveEvent(cmd) )
	}
	
    def timeString = new Date().format("MM/dd/yy h:mm a", location.timeZone)
    sendEvent(name: "statusText", value: "Last Updated: "+ timeString)
	return result
}

def sensorValueEvent(Short value) {	
    sendEvent(name: "contact", value: (value == 0 ? "closed" : "open"))
    sendEvent(name: "door", value: (value == 0 ? "closed" : "open"))
    sendEvent(name: "switch", value: (value == 0 ? "off" : "on"))
    sendEvent(name: "contactState", value: (value == 0 ? "Door is closed, tap to open" : "Door is open, tap to close"))	
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
	// log.trace "NOT IMPLEMENTED: zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd)" 	
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd){		
	sensorValueEvent(cmd.value)
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
	// log.trace "NOT IMPLEMENTED: zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd)"         
}

def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd){	
	sensorValueEvent(cmd.sensorValue)
}

def zwaveEvent(physicalgraph.zwave.commands.alarmv1.AlarmReport cmd){
    // we caught this up in the parse method
    // log.trace "NOT IMPLEMENTED: zwaveEvent(physicalgraph.zwave.commands.alarmv1.AlarmReport cmd)" 
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	// Handles all Z-Wave commands we aren't interested in
	[:]
}

def on() {
	open()
}

def off() {
	close()
}

def open() {
	if (device.currentValue("contact") != "open") {
		log.info "Sending ACTUATE event to open $device.displayName"
		push()
	}
	else {
		log.info "Not opening $device.displayName since it is already open"
	}
}

def close() {
	if (device.currentValue("contact") != "closed") {
		log.info "Sending ACTUATE event to close $device.displayName"
		push()
	}
	else {
		log.info "Not closing door since it is already $device.displayName"
	}
}

def push() {
	log.info "Executing ACTUATE for $device.displayName per user request"
	delayBetween([
		zwave.basicV1.basicSet(value: 0xFF).format(),
		zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.sensorBinaryV1.sensorBinaryGet().format(),
        zwave.basicV1.basicGet().format(),
		zwave.alarmV1.alarmGet().format() 
	],1000)
}

def poll() {
	log.debug "Polling $device.displayName...."
	refresh()
}

// PING is used by Device-Watch in attempt to reach the Device
def ping() {
	log.debug "Pinging $device.displayName...."
	refresh()
}

def refresh() {	
	delayBetween([
		zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.sensorBinaryV1.sensorBinaryGet().format(),
        zwave.basicV1.basicGet().format(),
		zwave.alarmV1.alarmGet().format() 
	],500)
}

def configure() {
	log.info "Configuring $device.displayName...." //setting up to monitor power alarm and actuator duration      
	
    // The interval in minutes that Device-Watch pings if no device events received
	def checkDeviceIntervalInMinutes = 30; 
    sendEvent(name: "checkInterval", value: (checkDeviceIntervalInMinutes * 60), displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])	
    
	delayBetween([
		zwave.associationV1.associationSet(groupingIdentifier:3, nodeId:[zwaveHubNodeId]).format(),
        zwave.configurationV1.configurationSet(parameterNumber: 11, configurationValue: [25], size: 1).format(),
        zwave.configurationV1.configurationGet(parameterNumber: 11).format()
	],100)
}
