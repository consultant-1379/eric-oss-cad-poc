# coding: utf-8
#
# Copyright Ericsson (c) 2022
from datetime import datetime


class PMMetadata(object):
    gnb_id: int
    cell_id: int
    """
    PM Metadata class definition

    Desc:
        This class define the metadata of a performance measurements record

    Attributes:
        gnb_id (int): Id of gNodeB containing corresponding cell
        cell_id (int): Id of the cell

    """

    def __init__(self, gnb_id, cell_id):
        self.gnb_id = gnb_id
        self.cell_id = cell_id

    """
    PM Metadata to_dict() method

    Desc:
        Returns a dict representation of a PM MetaData instance for serialization.

    Returns:
        dict: Dict populated with PM Metadata attributes to be serialized.

    """

    def to_dict(self):
        return dict(gnb_id=self.gnb_id, cell_id=self.cell_id)


# TODO: Logging activities and Exception Handling
class PMData(object):
    metadata: PMMetadata
    timestamp: datetime
    pm_mac_vol_dl_s_cell_ext: int
    pm_mac_vol_dl: int
    pm_mac_vol_dl_drb: int
    pm_active_ue_dl_sum: int
    pm_mac_r_b_sym_avail_dl: int
    pm_mac_r_b_sym_used_pdsch_type_a: int
    """
    PM Data Needed record for CAD class definition

    Desc:
        This class define the pm counters and metadata that needs to be persisted in the DB

    Attributes:
        metadata (PMMetadata): PM Record Metadata holding gnb_id & cell_id values
        timestamp (datetime): [Auto generated with UTC Date/Time] The reception date & time of the pm record
        pm_mac_vol_dl_s_cell_ext (int): Performance measurement
        pm_mac_vol_dl (int): Performance measurement
        pm_mac_vol_dl_drb (int): Performance measurement
        pm_active_ue_dl_sum (int): Performance measurement
        pm_mac_r_b_sym_avail_dl (int): Performance measurement
        pm_mac_r_b_sym_used_pdsch_type_a (int): Performance measurement

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
    ):
        self.metadata = PMMetadata(gnb_id, cell_id)
        self.timestamp = datetime.utcnow()
        self.pm_mac_vol_dl_s_cell_ext = pm_mac_vol_dl_s_cell_ext
        self.pm_mac_vol_dl = pm_mac_vol_dl
        self.pm_mac_vol_dl_drb = pm_mac_vol_dl_drb
        self.pm_active_ue_dl_sum = pm_active_ue_dl_sum
        self.pm_mac_r_b_sym_avail_dl = pm_mac_r_b_sym_avail_dl
        self.pm_mac_r_b_sym_used_pdsch_type_a = pm_mac_r_b_sym_used_pdsch_type_a

    """
    PM Data to_dict() method

    Desc:
        Returns a dict representation of a PM Data record instance for serialization.

    Returns:
        dict: Dict populated with PM Data record attributes to be serialized.

    """

    def to_dict(self):
        return dict(
            metadata=self.metadata.to_dict(),
            timestamp=self.timestamp,
            pm_mac_vol_dl_s_cell_ext=self.pm_mac_vol_dl_s_cell_ext,
            pm_mac_vol_dl=self.pm_mac_vol_dl,
            pm_mac_vol_dl_drb=self.pm_mac_vol_dl_drb,
            pm_active_ue_dl_sum=self.pm_active_ue_dl_sum,
            pm_mac_r_b_sym_avail_dl=self.pm_mac_r_b_sym_avail_dl,
            pm_mac_r_b_sym_used_pdsch_type_a=self.pm_mac_r_b_sym_used_pdsch_type_a,
        )
