# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

from entities.database_collection_models import (
    GnbduPairs,
    Gnbdus,
    Optimization,
    TargetGnbdus,
)
import logging
from mongoengine import OperationError
from mongoengine.queryset.visitor import Q

logger = logging.getLogger(__name__)


def parse_target_ghbdus(selected_nodes):
    target_gnbdus_list = []
    gnb_id_list = selected_nodes["gnb_id_list"]
    for index, bb in enumerate(gnb_id_list):
        if "gnb_id" in bb:
            target_gnbdus = TargetGnbdus(gnbdu_id=bb["gnb_id"])
            target_gnbdus_list.append(target_gnbdus)
        else:
            logger.warn("gnb_id does not exist in the bb at gnb_id_list[", index, "]")
    return target_gnbdus_list


def parse_gnb_pairs_constraint_list(node_pairs_list):
    gnb_pairs_list = []
    if "gnb_pairs_list" in node_pairs_list:
        gnb_pair_list = node_pairs_list["gnb_pairs_list"]
        for index, bb_pair in enumerate(gnb_pair_list):
            if "p_gnbdu_id" in bb_pair and "s_gnbdu_id" in bb_pair:
                gnb_pairs_list.append(
                    GnbduPairs(
                        p_gnbdu_id=bb_pair["p_gnbdu_id"],
                        s_gnbdu_id=bb_pair["s_gnbdu_id"],
                    )
                )
            else:
                logger.warning(
                    f"Either p_gnbdu_id or s_gnbdu_id or both is/are missing in the bb_pair at gnb_pairs_list[{index}]"
                )
    else:
        logger.warning("gnb_pairs_list does not exist in the provided node pairs list")
    return gnb_pairs_list


def persist_gnbdus(selected_nodes):
    logger.debug("Saving gnbdus into the database.")
    # Name and FDN fields are supposed to be updated later.
    gnb_id_list = selected_nodes["gnb_id_list"]
    for index, bb in enumerate(gnb_id_list):
        if "gnb_id" in bb and "cm_handle" in bb:
            gnbdu = Gnbdus(
                gnbdu_id=bb["gnb_id"],
                cm_handle=bb["cm_handle"],
                name=str(bb["gnb_id"]),
                fdn=str(bb["gnb_id"]),
            )
            gnbdu_already_existing = Gnbdus.objects(Q(gnbdu_id=bb["gnb_id"]))
            if not gnbdu_already_existing:
                gnbdu.save()
                logger.debug(
                    "gnbdu object has been saved in the database successfully."
                )
            else:
                logger.warning("Gnbdu object already exists in the database")
        else:
            logger.warning(
                "Either gnb_id or cm_handle or both is/are missing in the bb at gnb_id_list[",
                index,
                "]",
            )


def persist_newly_created_optimization_instance(
    creation_date,
    optimization_status,
    selected_nodes,
    unwanted_node_pairs,
    mandatory_node_pairs,
):
    logger.info("Saving optimization into the database.")
    optimization = Optimization(
        creation_date=creation_date,
        status=optimization_status,
        target_gnbdus=parse_target_ghbdus(selected_nodes),
        restricted_links=parse_gnb_pairs_constraint_list(unwanted_node_pairs),
        mandatory_links=parse_gnb_pairs_constraint_list(mandatory_node_pairs),
    )
    try:
        optimization.save()
        logger.info("Optimization has been saved in the database successfully.")
        return optimization.id
    except OperationError as e:
        logger.error("Optimization document cannot be saved in the database.")
        raise e


def get_optimization_by_id(_id, collection_name):
    if collection_name == "Optimization":
        return Optimization.objects.with_id(_id)
