#!/usr/bin/env python
# coding: utf-8
#
# Copyright Ericsson (c) 2022
import logging
import random

from entities.api_response import ResponseStatus
from entities.kpi_retrieve_post_parameters import (
    KpiRetrievePostResponse,
    KpiRetrievePostResponseCls,
    KpiRetrievePostResponseResult,
)
from fastapi import status

logger = logging.getLogger(__name__)


def retrieve_kpi(request_body_as_json):
    selected_gnb_output = {}
    response_orm = KpiRetrievePostResponseCls(
        status=ResponseStatus.FAILED,
        kpi_data={},
        result=KpiRetrievePostResponseResult.KPI_RETRIEVE_POST_MISSING_DATA_400,
    )
    response = KpiRetrievePostResponse.from_orm(response_orm)
    response_code = status.HTTP_400_BAD_REQUEST
    if "gnb_list" in request_body_as_json:
        gnb_id_list = request_body_as_json["gnb_list"]
        for bb in gnb_id_list:
            if "gnb_id" in bb and "cm_handle" in bb:
                selected_gnb_output[bb["gnb_id"]] = bb["cm_handle"]
        if selected_gnb_output:
            response_orm = KpiRetrievePostResponseCls(
                status=ResponseStatus.SUCCESS,
                kpi_data={
                    "KPI1": random.randint(1, 1000) + 1,
                    "KPI2": random.randint(1, 1000) + 2,
                    "KPI3": random.randint(1, 1000) + 3,
                },
                result=KpiRetrievePostResponseResult.KPI_RETRIEVE_POST_SUCCESS_200,
            )
            response = KpiRetrievePostResponse.from_orm(response_orm)
            response_code = status.HTTP_200_OK

    return response, response_code


# EOF
