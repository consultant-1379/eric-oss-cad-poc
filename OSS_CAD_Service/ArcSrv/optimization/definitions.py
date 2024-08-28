#!/usr/bin/env python
# coding: utf-8
#
# Copyright Ericsson (c) 2022
#
# ARC Automation - Constant definitions
# File holding all constants for the ARC application
#
# Functionality and script layout:
#   - Pseudo-constants defined, all capital letters
#   - To use in other files, 'import definitions' and then use CONSTANT with 'definitions.CONSTANT'
#   - Not very elegant, and not protecting against manipulating these 'constants'

# Parameter Definitions:

# Set all parameters that can be tweaked or set by other functions or conditions:

# A lowest level of capability for including the P-S cell relation, skip otherwise:
CAPABILITY_LIMIT = 0.1

# Set max number of BB partners per direction:
MAX_BB_PARTNERS = 6

# Set max number of external cells per partner and per direction:
MAX_EXTERNAL_CELLS_SECONDARY_GNB = 10

# NCI - Total hex digits = 9, most significant portion to gNB-ID given by attribute gnb_idLength, rest to cell-ID:
TOTAL_NCI_DIGITS = 9
DIGITS_FOR_GNB_ID = 7  # This should be given by gnb_idLength
DIGITS_FOR_CELL_ID = TOTAL_NCI_DIGITS - DIGITS_FOR_GNB_ID

RESULTS_DIRECTORY = "./ArcSrv/resources/"

# File name for PM data
PM_DATA_FILE = RESULTS_DIRECTORY + "pmDataFromLte.csv"

# File name for prediction model to be used:
MODEL_FILENAME = RESULTS_DIRECTORY + "modelParametersLinRegr_60.csv"

# Separator:
MODEL_SEPARATOR = ","

# EOF
