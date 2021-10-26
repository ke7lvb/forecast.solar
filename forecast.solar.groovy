metadata {
	definition(
name: "Forecast.Solar",
namespace: "ke7lvb",
author: "Ryan Lundell",
	){
		capability "Refresh"
		capability "PowerMeter"
        		
		attribute "estimatedWattHoursToday","number"
        attribute "estimatedWattHoursTomorrow","number"
	}
	preferences {
		input name: "logEnable", type: "bool", title: "Enable Info logging", defaultValue: true, description: ""
        input name: "lat", type: "string", title: "Latitude", description: "", required: true
        input name: "lng", type: "string", title: "Longitude", description: "", required: true
        input name: "dec", type: "string", title: "Declination", description: "0-90", required: true
        input name: "az", type: "string", title: "Azimuth", description: "-180 - 180", required: true
        input name: "kwp", type: "string", title: "Kilowatt Production", description: "", required: true
        input("refresh_interval", "enum", title: "How often to refresh the battery data", options: [
            0: "Do NOT update",
            1: "1 Hour",
            3: "3 Hours",
            8: "8 Hours",
            12: "12 Hours",
            24: "Daily",
    ], required: true, defaultValue: "8")
	}
}

def version(){ return "1.0.0" }

def installed(){
	if(logEnable) log.info "Driver installed"
	
	state.version = version()
}

def uninstalled() {
	unschedule(refresh)
	if(logEnable) log.info "Driver uninstalled"
}

def updated() {
  unschedule(refresh)
  if (logEnable) log.info "Settings updated"
  if (settings.refresh_interval != "0") {
    //refresh()
    if (settings.refresh_interval == "24") {
      schedule("10 10 2 ? * * *", refresh)
    } else {
      schedule("10 10 */${settings.refresh_interval} ? * * *", refresh)
    }
  }
  state.version = version()
}
import groovy.json.JsonOutput;
def refresh() {
    today = new Date().format( 'yyyy-MM-dd' )
    tomorrow = new Date().next().format("yyyy-MM-dd")
    host = "https://api.forecast.solar/estimate/${lat}/${lng}/${dec}/${az}/${kwp}"
    httpGet([uri: host]) {
        resp -> def respData = resp.data.result
        state.estimatedWattHoursToday = respData.watt_hours_day[today]
        sendEvent(name: "power", value: state.estimatedWattHoursToday)
        state.estimatedWattHoursTomorrow = respData.watt_hours_day[tomorrow]
        sendEvent(name: "estimatedWattHoursTomorrow", value: state.estimatedWattHoursTomorrow)
        state.JSON = JsonOutput.toJson(resp.data)
        state.lastUpdate = new Date()
    }
}
