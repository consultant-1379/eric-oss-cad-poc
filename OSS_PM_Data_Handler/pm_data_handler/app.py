# coding: utf-8
#
# Copyright Ericsson (c) 2022
from pm_data_handler.entities.pm_data_handler import PMDataHandler


# PM Data Handler Module Launcher method
def launch():
    # Instantiating PM Data Handler
    pm_data_handler = PMDataHandler()
    # Run the module
    pm_data_handler.run()
