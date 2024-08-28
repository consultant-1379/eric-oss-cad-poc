# coding: utf-8
#
# Copyright Ericsson (c) 2022

from pm_data_handler.entities.cad_pm_data import PMData
from pm_data_handler.entities.pm_data_handler_logger import PMDataHandlerLogger
from pm_data_handler.utils.meta import SingletonMeta


class PMDataTransformer(object, metaclass=SingletonMeta):
    """
    PM Data Transformer

    Desc:
        The transformer will transform the deserialized value to an instance of cad needed PMData

     TODO:
         what is still not clear is how we can convert cell_id to gnb_id in case gnb_id
         is not available att all in the message
     TODO [ Sync with producer team ] the gnb_id may not be available
     TODO [ Investigate about how we can do such mapping from cell_id to gnb_id
    """

    @staticmethod
    def extract_cad_pm_data_from_full_pm_data(
        self,
        full_pm_data_record: dict,
    ) -> PMData:
        # TODO : To be re-checked based on return of producer team
        # TODO : Explanation -> Revisit calculation and keep/add only needed ones depending on final message schema.
        if "gnb_id" in full_pm_data_record:
            cell_id, gnb_id = PMDataTransformer.handle_exiting_gnb_id_case(
                full_pm_data_record
            )
        else:
            if "cell_id" in full_pm_data_record:
                cell_id, gnb_id = PMDataTransformer.handle_exiting_cell_id_case(
                    full_pm_data_record
                )
            else:
                cell_id, gnb_id = PMDataTransformer.handler_non_existing_cell_id_case(
                    full_pm_data_record, self
                )

        try:
            # Retrieving the targeted PM 6 counters
            pm_mac_vol_dl_s_cell_ext = full_pm_data_record["pm_mac_vol_dl_s_cell_ext"]
            pm_mac_vol_dl = full_pm_data_record["pm_mac_vol_dl"]
            pm_mac_vol_dl_drb = full_pm_data_record["pm_mac_vol_dl_drb"]
            pm_active_ue_dl_sum = full_pm_data_record["pm_active_ue_dl_sum"]
            pm_mac_r_b_sym_avail_dl = full_pm_data_record["pm_mac_r_b_sym_avail_dl"]
            pm_mac_r_b_sym_used_pdsch_type_a = full_pm_data_record[
                "pm_mac_r_b_sym_used_pdsch_type_a"
            ]
        except KeyError as key_error:
            error_msg = f"{key_error} was not found"
            PMDataHandlerLogger().get_logger("dev").warning(error_msg)
            raise KeyError(error_msg)

        return PMData(
            gnb_id,
            cell_id,
            pm_mac_vol_dl_s_cell_ext,
            pm_mac_vol_dl,
            pm_mac_vol_dl_drb,
            pm_active_ue_dl_sum,
            pm_mac_r_b_sym_avail_dl,
            pm_mac_r_b_sym_used_pdsch_type_a,
        )

    @staticmethod
    def handler_non_existing_cell_id_case(full_pm_data_record, self):
        if "gnb_id_len" in full_pm_data_record and "s_n_c_i" in full_pm_data_record:
            gnb_id = self.retrieve_gnb_id_from_s_n_c_i_and_gnb_id_len(
                full_pm_data_record["gnb_id_len"],
                full_pm_data_record["s_n_c_i"],
            )
            cell_id = full_pm_data_record["s_n_c_i"] - gnb_id
        else:
            error_msg = (
                "gnb_id_len & s_n_c_i not found in received pm record | can't calculate corresponding "
                "gnb_id and cell_id"
            )
            PMDataHandlerLogger().get_logger("dev").warning(error_msg)
            raise KeyError(error_msg)
        return cell_id, gnb_id

    @staticmethod
    def handle_exiting_cell_id_case(full_pm_data_record):
        cell_id = full_pm_data_record["cell_id"]
        if "s_n_c_i" in full_pm_data_record:
            gnb_id = full_pm_data_record["s_n_c_i"] - cell_id
        else:
            error_msg = "s_n_c_i not found in received pm record | can't calculate corresponding gnb_id"
            PMDataHandlerLogger().get_logger("dev").warning(error_msg)
            raise KeyError(error_msg)
        return cell_id, gnb_id

    @staticmethod
    def handle_exiting_gnb_id_case(full_pm_data_record):
        gnb_id = full_pm_data_record["gnb_id"]
        if "cell_id" in full_pm_data_record:
            cell_id = full_pm_data_record["cell_id"]
        else:
            if "s_n_c_i" in full_pm_data_record:
                cell_id = full_pm_data_record["s_n_c_i"] - gnb_id
            else:
                error_msg = "s_n_c_i not found in received pm record | can't calculate corresponding cell_id"
                PMDataHandlerLogger().get_logger("dev").warning(error_msg)
                raise KeyError(error_msg)
        return cell_id, gnb_id

    @staticmethod
    def retrieve_gnb_id_from_s_n_c_i_and_gnb_id_len(gnb_id_len, s_n_c_i):
        total_cells_ids = 2 ** (36 - gnb_id_len)
        gnb_mask = (2**gnb_id_len - 1) * total_cells_ids
        gnb_id = (gnb_mask & s_n_c_i) / total_cells_ids
        return gnb_id
