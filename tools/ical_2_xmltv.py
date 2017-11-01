#!/usr/bin/env python
# -*- coding: utf8 -*-
from __future__ import unicode_literals

"""
author: luapmartin
description: a really straight forward, quick and dirty iCal to xmltv converter

requirements.txt

icalendar==3.11.7
pkg-resources==0.0.0
python-dateutil==2.6.1
python-xmltv==1.4.3
pytz==2017.2
six==1.11.0

"""

import icalendar
import xmltv

ical_file = open("./basic.ics", 'rb')
xmltv_file = open("./basic.xml", 'w')

my_cal = icalendar.Calendar.from_ical(ical_file.read())

w = xmltv.Writer()
list_of_programs = []
channel_id = "test_id"
channel_dict = {}


for a_program in my_cal.walk():
    # the first elements are descriptive of the calendar and the timezones
    if not channel_dict:
        channel_dict = {"id": channel_id,
                        "display-name": [a_program.get("X-WR-CALNAME").title()]
                        }
        continue
    if a_program.get("TZID") or a_program.get("TZNAME"):
        # skip the lines about timezone information
        continue
    start_time = a_program.get("DTSTART")
    if start_time:
        start_time = start_time.dt
        start_time = start_time.strftime(xmltv.date_format)
    end_time = a_program.get("DTEND")
    if end_time:
        end_time = end_time.dt
        end_time = end_time.strftime(xmltv.date_format)
    else:
        # if there is no end time the end time is set as the start time
        end_time = start_time
    summary = a_program.get("SUMMARY").title()
    description = a_program.get("DESCRIPTION")
    if description:
        description = description.title()
    prog_dict = {"channel": channel_id,
                 "start": start_time,
                 "stop": end_time,
                 "sub-title": [(description, u'')],
                 "title": [(summary, u'')]
                 }
    list_of_programs.append(prog_dict)

w.addChannel(channel_dict)
for a_program in list_of_programs:
    w.addProgramme(a_program)

w.write(xmltv_file, pretty_print=True)


"""
# example of iCal data parsed :
ical_file = open("../res/basic.ics", 'rb')
C = icalendar.Calendar.from_ical(ical_file.read())
for a in C.walk():
    for k,v in a.items():
        print k,v
    print "-"*20

# as we can see first lines are descriptive

PRODID -//Google Inc//Google Calendar 70.9054//EN
VERSION 2.0
CALSCALE GREGORIAN
METHOD PUBLISH
X-WR-CALNAME TWiT Live Schedule
X-WR-TIMEZONE America/Los_Angeles
X-WR-CALDESC Schedule of upcoming live broadcasts on http://twit.tv and http://live.twit.tv. Updated regularly.
--------------------
TZID America/Los_Angeles
X-LIC-LOCATION America/Los_Angeles
--------------------
TZOFFSETFROM <icalendar.prop.vUTCOffset object at 0x7f6e6e46da50>
TZOFFSETTO <icalendar.prop.vUTCOffset object at 0x7f6e6cc75610>
TZNAME PDT
DTSTART <icalendar.prop.vDDDTypes object at 0x7f6e6cc758d0>
RRULE vRecur({u'BYMONTH': [3], u'FREQ': [u'YEARLY'], u'BYDAY': [u'2SU']})
--------------------
TZOFFSETFROM <icalendar.prop.vUTCOffset object at 0x7f6e6cc75810>
TZOFFSETTO <icalendar.prop.vUTCOffset object at 0x7f6e6cc75910>
TZNAME PST
DTSTART <icalendar.prop.vDDDTypes object at 0x7f6e6cc75990>
RRULE vRecur({u'BYMONTH': [11], u'FREQ': [u'YEARLY'], u'BYDAY': [u'1SU']})
--------------------
DTSTART <icalendar.prop.vDDDTypes object at 0x7f6e6cde5450>
DTEND <icalendar.prop.vDDDTypes object at 0x7f6e6cc759d0>
DTSTAMP <icalendar.prop.vDDDTypes object at 0x7f6e6cd8e950>
UID enol3dbcud5rlmbsd95cuqoqog@google.com
RECURRENCE-ID <icalendar.prop.vDDDTypes object at 0x7f6e6cc75b50>
CREATED <icalendar.prop.vDDDTypes object at 0x7f6e6cc75b10>
DESCRIPTION 
LAST-MODIFIED <icalendar.prop.vDDDTypes object at 0x7f6e6cc75b90>
LOCATION 
SEQUENCE 2
STATUS CONFIRMED
SUMMARY this WEEK in TECH
TRANSP OPAQUE
--------------------
"""
