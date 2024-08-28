# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

from datetime import datetime


def pm_data_to_dict(pm_data, ctx):
    """
    Returns a dict representation of a PMData instance for serialization.

    Args:
        pm_data (PMData): PMData instance.
        ctx (SerializationContext): Metadata pertaining to the serialization
            operation (Passed by AvroSerializer)

    Returns:
        dict: Dict populated with user attributes to be serialized.
    """
    return dict(
        gnb_id=pm_data.gnb_id,
        cell_id=pm_data.cell_id,
        pm_mac_vol_dl_s_cell_ext=pm_data.pm_mac_vol_dl_s_cell_ext,
        pm_mac_vol_dl=pm_data.pm_mac_vol_dl,
        pm_mac_vol_dl_drb=pm_data.pm_mac_vol_dl_drb,
        pm_active_ue_dl_sum=pm_data.pm_active_ue_dl_sum,
        pm_mac_r_b_sym_avail_dl=pm_data.pm_mac_r_b_sym_avail_dl,
        pm_mac_r_b_sym_used_pdsch_type_a=pm_data.pm_mac_r_b_sym_used_pdsch_type_a,
        gnb_id_len=pm_data.gnb_id_len,
        s_n_c_i=pm_data.s_n_c_i,
        p_time=str(datetime.timestamp(pm_data.p_time)),
    )
