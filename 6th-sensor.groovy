/*
 * 6th-sensor.groovy
 *
 * Copyright (C) 2010 Antoine Mercadal <antoine.mercadal@inframonde.eu>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

definition(
    name: "6th Sensor",
    namespace: "primalmotion",
    author: "primalmotion",
    description: "Turn on lights when dark, deal with night path, and more",
    category: "Convenience",
    iconUrl: "http://icons.iconarchive.com/icons/graphicloads/medical-health/96/eye-icon.png",
    iconX2Url: "http://icons.iconarchive.com/icons/graphicloads/medical-health/96/eye-icon.png"
)

preferences
{
    section ("Sensors")
    {
        input "_lightsensordevice", "capability.illuminanceMeasurement", title: "Select the light sensor to control", required: true
        input "_motionsensordevice", "capability.motionSensor", title: "Select the motion sensor to control", required: true
    }

    section ("Night Light")
    {
        input "_nightlightsenabled", "boolean", title: "Enable Night Light", required: true
        input "_homemode", "mode", title: "When home is in mode", required: false
        input "_nightlights", "capability.switch", title: "Turn these lights on", multiple: true, required: false
        input "_luminancetrigger", "number", title: "When luminance is below: (default: 300lux)", required: false
        input "_nightlighttime", "number", title: "Turn them off after no motion for (default: 600s)", required: false
    }

    section ("Night Path")
    {
        input "_nightpathenabled", "boolean", title: "Enable Night Path", required: true
        input "_nightmode", "mode", title: "When home is in mode", required: false
        input "_pathligths", "capability.switch", title: "On motion, turn these lights on", multiple: true, required: false
        input "_pathtime", "number", title: "Turn them off after (default: 60s)", required: false
    }

    section ("Night Watch")
    {
        input "_nightwatchenabled", "boolean", title: "Enable Night Watch", required: true
        input "_awaymode", "mode", title: "Send alert on any motion when home is in this mode", required: false, defaultValue: "Away"
    }
}

def installed()
{
    initialize()
}

def updated()
{
    unsubscribe()
    initialize()
}

def initialize()
{
    final _luminancetrigger  = _luminancetrigger  ?: 300
    final _nightlighttime    = _nightlighttime    ?: 10 * 60
    final _pathtime          = _pathtime          ?: 60
    final _homemode          = _homemode          ?: "Home"
    final _nightmode         = _nightmode         ?: "Night"
    final _awaymode          = _awaymode          ?: "Away"

    log.debug("_luminancetrigger :${_luminancetrigger}")
    log.debug("_nightlighttime   :${_nightlighttime}")
    log.debug("_pathtime         :${_pathtime}")
    log.debug("_homemode         :${_homemode}")
    log.debug("_nightmode        :${_nightmode}")
    log.debug("_awaymode         :${_awaymode}")

    subscribe(_lightsensordevice, "illuminance", on_event)
    subscribe(_motionsensordevice, "motion.active", on_event)
}


def on_event(evt)
{
    log.debug("-------------------------")

    if (_nightlightsenabled && location.mode == _homemode)
        on_home_event(evt)

    else if (_nightpathenabled && location.mode == _nightmode)
        on_night_event(evt)

    else if (_nightwatchenabled && location.mode == _awaymode)
        on_away_event(evt)

    log.debug("-------------------------")
}

def on_home_event(evt)
{
    log.debug("home event: name: ${evt.name}, value: ${evt.value}")

    def is_dark = _lightsensordevice.currentIlluminance < _luminancetrigger

    if (evt.name == "motion" && evt.value == "active")
        is_dark ? turn_nightlights_on() : turn_nightlights_off()

    else if (evt.name == "illuminance" && !is_dark)
        turn_nightlights_off()
}

def on_night_event(evt)
{
    log.debug("night event: name: ${evt.name}, value: ${evt.value}")

    if (evt.name != "motion" || evt.value != "active")
        return

    turn_night_path_lights_on()

    unschedule("turn_night_path_lights_off")
    runIn(_pathtime, "turn_night_path_lights_off")
}

def on_away_event(evt)
{
    log.debug("away event: name: ${evt.name}, value: ${evt.value}")

    if (evt.name != "motion" || evt.value != "active" )
        return

    sendPush("Alert! Some movement has been detected but nobody's home!")
}

def turn_nightlights_on()
{
    log.debug(" - turn lights on")
    _set_lights_state(_nightlights, true)

    log.debug(" - canceling any secheduled light off.")
    unschedule("turn_nightlights_off")

    log.debug(" - schedulinng lights off in ${_nightlighttime} seconds")
    runIn(_nightlighttime, "turn_nightlights_off")
}

def turn_nightlights_off()
{
    log.debug(" - turn lights off")
    _set_lights_state(_nightlights, false)
}

def turn_night_path_lights_on()
{
    log.debug(" - turn path lights on")
    _set_lights_state(_pathligths, true)
}

def turn_night_path_lights_off()
{
    log.debug(" - turn path lights off")
    _set_lights_state(_pathligths, false)
}

def _set_lights_state(targets, state)
{
    for (l in targets)
    {
        if (state)
            l.on()
        else
            l.off()
    }
}