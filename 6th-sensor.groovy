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
    iconUrl: "http://icons.iconarchive.com/icons/graphicloads/android-settings/128/light-icon.png",
    iconX2Url: "http://icons.iconarchive.com/icons/graphicloads/android-settings/128/light-icon.png"
)

preferences
{
    section ("Sensors")
    {
        input "lightsensordevice", "capability.illuminanceMeasurement", title: "Select the light sensor to control", required: true
        input "motionsensordevice", "capability.motionSensor", title: "Select the motion sensor to control", required: true
    }

    section ("Night Light")
    {
        input "nightlightingenabled", "boolean", title: "Enable Night Light", required: true
        input "nightlights", "capability.switch", title: "On illuminance, turn these lights on", multiple: true, required: false
        input "luminancetrigger", "number", title: "If luminance is below: (300 default)", required: false
        input "homemode", "mode", title: "When home is in mode", required: false
    }

    section ("Night Path")
    {
        input "nightpathenabled", "boolean", title: "Enable Night Path", required: true
        input "pathligths", "capability.switch", title: "On motion, turn these lights on", multiple: true, required: false
        input "pathtime", "number", title: "For that amount of seconds", required: false
        input "nightmode", "mode", title: "When home is in mode", required: false
    }

    section ("Night Watch")
    {
        input "nightwatchenabled", "boolean", title: "Enable Night Watch", required: true
        input "awaymode", "mode", title: "Send alert on any motion when home is in this mode", required: false, defaultValue: "Away"
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

def configure

def initialize()
{
    subscribe(lightsensordevice, "illuminance", on_event)
    subscribe(motionsensordevice, "motion", on_event)
}


def on_event(evt)
{
    if (nightlightingenabled && location.mode == homemode)
        on_home_event()

    else if (nightpathenabled && location.mode == nightmode && evt.name == "motion" && evt.value == "active")
        on_night_event()

    else if (nightwatchenabled && location.mode == awaymode && evt.name == "motion" && evt.value == "active")
        on_away_event()
}

def on_home_event()
{
    log.debug("Home mode. checking lux value: ${lightsensordevice.currentIlluminance}")

    if (lightsensordevice.currentIlluminance < (luminancetrigger ?: 300))
    {
        log.debug("lux value below luminancetrigger: turning lights on")
        turn_lights_on()
    }
    else
    {
        log.debug("lux value above luminancetrigger: turning lights off")
        turn_lights_off()
    }
}

def on_night_event()
{
    log.debug("Night mode. Lighthing up Night Path")

    turn_night_path_lights_on()
    runIn(pathtime ?: 60, "turn_night_path_lights_off", [overwrite: true])
}

def on_away_event()
{
    log.debug("Away mode. Sending  message")

    sendPush("Alert! Some movement has been detected but nobody's home!")
}

def turn_lights_on()
{
    _set_lights_state(nightlights, true, 100)
}

def turn_lights_off()
{
    _set_lights_state(nightlights, false, 100)
}

def turn_night_path_lights_on()
{
    _set_lights_state(pathligths, true, 20)
}

def turn_night_path_lights_off()
{
    _set_lights_state(pathligths, false, 100)
}

def _set_lights_state(targets, state, dim)
{
    for (l in targets)
    {
        l.setLevel(dim)
        if (state)
            l.on()
        else
            l.off()
    }
}