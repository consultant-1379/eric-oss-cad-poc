#!/usr/bin/env python
# coding: utf-8
#
# Copyright Ericsson (c) 2022

import uvicorn
import threading

from configs import logger


def start():
    logger.debug("Starting ARC optimizer server thread ...")
    arc_server = threading.Thread(target=start_server, args=())
    try:
        arc_server.start()
    except RuntimeError:
        logger.exception("Failed to start server thread - already started.")
    else:
        logger.debug("ARC optimizer server thread started ...")


def start_server():
    logger.info("*** ARC Server Started ***")
    uvicorn.run("api:app", host="0.0.0.0", port=5000, log_level="warning")
    logger.info("*** ARC Server Stopped ***")


if __name__ == "__main__":
    logger.debug("Starting main from __init__.py ..")
    try:
        start()
    except Exception:
        logger.exception("Failed to start server thread.")

# EOF
