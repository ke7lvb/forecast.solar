metadata {
  definition(
    name: "Forecast.Solar_2",
    namespace: "ke7lvb",
    author: "Ryan Lundell",
    importUrl: "https://raw.githubusercontent.com/ke7lvb/forecast.solar/2-Panel-Groups/forecast.solar.groovy",
  ) {
    capability "Refresh"
    capability "PowerMeter"

    attribute "estimatedWattHoursToday", "number"
    attribute "estimatedWattHoursTomorrow", "number"
    attribute "estimatedWattHoursTwoDays", "number"
  }
  preferences {
    input name: "logEnable", type: "bool", title: "Enable Info logging", defaultValue: true, description: ""
    input name: "lat", type: "string", title: "Latitude", description: "", required: true
    input name: "lng", type: "string", title: "Longitude", description: "", required: true
    input name: "dec", type: "string", title: "Declination", description: "0-90", required: true
    input name: "az", type: "string", title: "Azimuth Group 1", description: "", required: true
    input name: "az2", type: "string", title: "Azimuth Group 2", description: "", required: true
    input name: "kwp", type: "string", title: "Kilowatt Production Group 1", description: "", required: true
    input name: "kwp2", type: "string", title: "Kilowatt Production Group 2", description: "", required: true
    input name: "damping", type: "number", title: "Damping", description: "", defaultValue: "0"
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

def version() {
  return "1.0.1"
}

def installed() {
  if (logEnable) log.info "Driver installed"

  state.version = version()
}

def uninstalled() {
  unschedule(refresh)
  if (logEnable) log.info "Driver uninstalled"
}

def updated() {
  if (logEnable) log.info "Settings updated"
  if (settings.refresh_interval != "0") {
    //refresh()
    if (settings.refresh_interval == "24") {
      schedule("11 10 2 ? * * *", refresh, [overwrite: true])
    } else {
      schedule("11 10 */${settings.refresh_interval} ? * * *", refresh, [overwrite: true])
    }
  }else{
    unschedule(refresh)
  }
  state.version = version()
}

import groovy.json.JsonOutput;
def refresh() {
  today = new Date().format('yyyy-MM-dd')
  tomorrow = new Date().next().format("yyyy-MM-dd")
  twoDays = new Date().plus(2).format("yyyy-MM-dd")
  
  host = "https://api.forecast.solar/estimate/${lat}/${lng}/${dec}/${az}/${kwp}?damping=${damping}"
  if (logEnable) log.info host
  groupOneEstimatee = httpGet([uri: host]){resp -> def respData = resp.data}
  log.info groupOneEstimatee
  estimatedWattHoursToday1 = groupOneEstimatee.result.watt_hours_day[today] ?: 0
  estimatedWattHoursTomorrow1 = groupOneEstimatee.result.watt_hours_day[tomorrow] ?: 0
  estimatedWattHoursTwoDays1 = groupOneEstimatee.result.watt_hours_day[twoDays] ?: 0
  
  
  host2 = "https://api.forecast.solar/estimate/${lat}/${lng}/${dec}/${az2}/${kwp2}?damping=${damping}"
  if (logEnable) log.info host2
  groupTwoEstimate = httpGet([uri: host2]){resp -> def respData = resp.data}
  estimatedWattHoursToday2 = groupTwoEstimate.result.watt_hours_day[today] ?: 0
  estimatedWattHoursTomorrow2 = groupTwoEstimate.result.watt_hours_day[tomorrow] ?: 0
  estimatedWattHoursTwoDays2 = groupTwoEstimate.result.watt_hours_day[twoDays] ?: 0
  
  
  state.estimatedWattHoursToday = estimatedWattHoursToday1 + estimatedWattHoursToday2
  sendEvent(name: "power", value: state.estimatedWattHoursToday)
  state.estimatedWattHoursTomorrow = estimatedWattHoursTomorrow1 + estimatedWattHoursTomorrow2
  sendEvent(name: "estimatedWattHoursTomorrow", value: state.estimatedWattHoursTomorrow)
  state.estimatedWattHoursTwoDays = estimatedWattHoursTwoDays1 + estimatedWattHoursTwoDays2
  sendEvent(name: "estimatedWattHoursTwoDays", value: state.estimatedWattHoursTwoDays)
  now = new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'")
  state.lastUpdate = timeToday(now, location.timeZone)
}
