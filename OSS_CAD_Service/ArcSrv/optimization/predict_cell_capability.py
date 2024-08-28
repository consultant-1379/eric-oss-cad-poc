#!/usr/bin/env python
# coding: utf-8
#
# Copyright Ericsson (c) 2022
#
# ARC Automation - Prediction of Cell Capability
# Use stored ML model to predict the capability of sCells in ARC Automation
# Basic functionality:
#   - Read in data samples
#   - Read the stored regression model for prediction of cell load during set prediction time interval
#   - Calculate cell capability estimate
#   - Return results

# Some ideas to try out:
# -> Include min(prediction, availableRBSymbols)? To avoid predicting more than possible, but how to know maximum value?

import pandas as pd

"""
Summary: Read in the model data, normalize capacity data, and calculate the predicted capacity value
Description:
        - Read the CSV file with model and normalization data:
        - Read the dataframe in csv file
        - Extract the parameters for the model (intercept and coefficients)
        - Extract the parameters for the normalization
params:
    file_name(String) : A csv file name
    separator(Character) : A character that separates normalization data
"""


def get_model(file_name, separator):
    df = pd.read_csv(file_name, sep=separator)
    return df.loc[0]["intercept"], df.loc[0]["RBSymFree"], df.loc[1]["RBSymFree"]


"""
Summary: Normalize columns to be included in feature set
Description:
        - Normalize each column individually
        - Use normalization parameters from model creation
        - Normalized value = value / normalizeConstant
params:
    df([]) : A data frame that holds col1, RBSymFree, col3 data arrays
    normalize_columns(String[]) :  A column list of data
    normalize_constants(int[]) : A list of constants
"""


def normalize_data(df, normalize_columns, normalize_constants):
    df_final = df.copy()
    for index, col in enumerate(normalize_columns):
        df_final[col + "Norm"] = [
            (value / normalize_constants[index]) for value in df_final[col]
        ]
    return df_final


"""
Summary: Entry point for the prediction
Description:
        - Predict the average available cell capacity for the next time interval:
        - Use the simple regression read from the stored model
params:
    model_file_name(String) : A file path
    model_separator(Character) : A character that separates cell availability data
    df_capacity_data([]) :  A data frame that holds col1, RBSymFree, col3 data arrays
"""


def add_predicted_cell_capacity(model_file_name, model_separator, df_capacity_data):
    intercept, coefficient_0, normalize_constant_0 = get_model(
        model_file_name, model_separator
    )
    df_capacity_norm = normalize_data(
        df_capacity_data.copy(), ["RBSymFree"], [normalize_constant_0]
    )
    df_capacity_pred = df_capacity_norm.copy()
    df_capacity_pred["predictedCapacity"] = (
        intercept + coefficient_0 * df_capacity_norm["RBSymFreeNorm"]
    )
    return df_capacity_pred


# EOF
