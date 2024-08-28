# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

from datetime import datetime


class PMData(object):
    gnb_id: int
    cell_id: int
    pm_mac_vol_dl_s_cell_ext: int
    pm_mac_vol_dl: int
    pm_mac_vol_dl_drb: int
    pm_active_ue_dl_sum: int
    pm_mac_r_b_sym_avail_dl: int
    pm_mac_r_b_sym_used_pdsch_type_a: int
    s_n_c_i: int
    gnb_id_len: int
    p_time: datetime

    """
    PMData record

    Attributes:
        gnb_id (int): Id of the gnb
        cell_id (int): Id of the cell
        pm_mac_vol_dl_s_cell_ext (int): PM Counter
        pm_mac_vol_dl (int): PM Counter
        pm_mac_vol_dl_drb (int): PM Counter
        pm_active_ue_dl_sum (int): PM Counter
        pm_mac_r_b_sym_avail_dl (int): PM Counter
        pm_mac_r_b_sym_used_pdsch_type_a (int): PM Counter
        s_n_c_i (long): PM Counter
        gnb_id_length (int): gnb_id in bits
        p_time (datetime): The date and time of production of this PMData
    """

    def __init__(
        self,
        gnb_id,
        cell_id,
        pm_mac_vol_dl_s_cell_ext,
        pm_mac_vol_dl,
        pm_mac_vol_dl_drb,
        pm_active_ue_dl_sum,
        pm_mac_r_b_sym_avail_dl,
        pm_mac_r_b_sym_used_pdsch_type_a,
        gnb_id_len,
        s_n_c_i,
        p_time=datetime.now(),
    ):
        self.gnb_id = gnb_id
        self.cell_id = cell_id
        self.pm_mac_vol_dl_s_cell_ext = pm_mac_vol_dl_s_cell_ext
        self.pm_mac_vol_dl = pm_mac_vol_dl
        self.pm_mac_vol_dl_drb = pm_mac_vol_dl_drb
        self.pm_active_ue_dl_sum = pm_active_ue_dl_sum
        self.pm_mac_r_b_sym_avail_dl = pm_mac_r_b_sym_avail_dl
        self.pm_mac_r_b_sym_used_pdsch_type_a = pm_mac_r_b_sym_used_pdsch_type_a
        self.gnb_id_len = gnb_id_len
        self.s_n_c_i = s_n_c_i
        self.p_time = p_time
