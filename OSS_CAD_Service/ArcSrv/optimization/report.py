# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

import logging
from functools import cache
from minio import Minio
from minio.commonconfig import GOVERNANCE
from datetime import datetime, timedelta
from io import BytesIO
from configs import object_store_config as storage_config
from configs import optimization_config
from minio.retention import Retention

import pandas as pd

logger = logging.getLogger(__name__)


@cache
def establish_object_storage_connection():
    return Minio(storage_config.host, access_key=storage_config.access_key,
                 secret_key=storage_config.secret_key, secure=False)


def save_in_bucket(bb_link_df, mandatory_bb_links, task_opt_id):
    minio_client = establish_object_storage_connection()
    fdate = datetime.now()
    file_name = 'CAD_Optimization_' + task_opt_id + '_' + fdate.strftime('%m%d%Y_%H%M%S') + '.csv'
    bucket_name = optimization_config.bucket_name
    found = minio_client.bucket_exists(bucket_name)

    if not found:
        minio_client.make_bucket(bucket_name, object_lock=True)
    else:
        logger.debug("Bucket %s already exists", bucket_name)

    bb_link_df['Mandatory Pair'] = False
    dfnew = bb_link_df
    dfnew['Mandatory Pair'] = bb_link_df.gNbs.isin(mandatory_bb_links)

    df = pd.DataFrame(dfnew)
    df = df.drop('gNbs', axis=1)
    df = df.reindex(columns=['gNb0', 'gNb1', 'usability', 'linkUsed', 'Mandatory Pair'])
    df = df.rename(columns={'gNb0': 'Primary gNodeB ID', 'gNb1': 'Secondary gNodeB ID', 'usability': 'Usability',
                            'linkUsed': 'Link used'})

    csv_bytes = df.to_csv(index=False).encode('utf-8')
    csv_buffer = BytesIO(csv_bytes)

    date = datetime.now().replace(
        hour=0, minute=0, second=0, microsecond=0,
    ) + timedelta(days=int(optimization_config.retention_policy))
    minio_client.put_object(bucket_name, file_name, data=csv_buffer, length=len(csv_bytes),
                            content_type='application/csv',
                            retention=Retention(GOVERNANCE, date),
                            legal_hold=True, )

    logger.debug("%s is successfully uploaded ", file_name)
