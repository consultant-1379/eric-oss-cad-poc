#!/usr/bin/env python
# coding: utf-8
#
# Copyright Ericsson (c) 2022
import datetime
from threading import Event, Thread

from entities.api_response import ResponseStatus

task_thread = Thread()
task_stop_event = Event()
task_opt_id = "None"


def stop_event_is_set(document):
    if task_stop_event.is_set():
        document.update(
            status=ResponseStatus.OPTIMIZATION_FINISHED,
            optimization_end_date=datetime.datetime.now(),
        )
        return True
    return False


# EOF
