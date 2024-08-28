#!/usr/bin/env python
# coding: utf-8
#
# Copyright Ericsson (c) 2022

"""
Summary: Evaluate BB connections made by build_solution.py
Description:
        - Use the created solution to calculate the total value.
        - Returns the summed values of the resulting configuration data-frame
params:
    bb_link_value_list ([]) :  A BB-BB link list with aggregated usability
"""


def calculate_arc_value(bb_link_value_list):
    return bb_link_value_list.query("linkUsed == True")["usability"].sum()


# EOF
