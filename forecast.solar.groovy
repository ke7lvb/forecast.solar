metadata {
  definition(
    name: "Forecast.Solar",
    namespace: "ke7lvb",
    author: "Ryan Lundell",
    importUrl: "https://raw.githubusercontent.com/ke7lvb/forecast.solar/main/forecast.solar.groovy",
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
    input name: "az", type: "string", title: "Azimuth", description: "", required: true
    input name: "kwp", type: "string", title: "Kilowatt Production", description: "", required: true
    input name: "damping", type: "number", title: "Damping", description: ""
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
  return "1.0.4"
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
      schedule("10 10 2 ? * * *", refresh, [overwrite: true])
    } else {
      schedule("10 10 */${settings.refresh_interval} ? * * *", refresh, [overwrite: true])
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
  httpGet([uri: host]) {
    resp -> def respData = resp.data
      state.estimatedWattHoursToday = respData.result.watt_hours_day[today]
      sendEvent(name: "power", value: state.estimatedWattHoursToday)
      state.estimatedWattHoursTomorrow = respData.result.watt_hours_day[tomorrow]
      sendEvent(name: "estimatedWattHoursTomorrow", value: state.estimatedWattHoursTomorrow)
      state.estimatedWattHoursTwoDays = respData.result.watt_hours_day[twoDays]
      sendEvent(name: "estimatedWattHoursTwoDays", value: state.estimatedWattHoursTwoDays)
      state.JSON = JsonOutput.toJson(respData)
      now = new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'")
      state.lastUpdate = timeToday(now, location.timeZone)
  }
}
