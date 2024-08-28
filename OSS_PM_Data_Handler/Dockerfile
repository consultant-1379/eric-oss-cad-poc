# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

ARG BASE_IMAGE="armdocker.rnd.ericsson.se/proj-ldc/common_base_os_release/sles"
ARG BASE_VERSION="4.2.0-24"
FROM ${BASE_IMAGE}:${BASE_VERSION}

ARG USER_ID=60577
ARG USER_NAME="eric-sdk"

RUN zypper addrepo -C -G -f https://arm.sero.gic.ericsson.se/artifactory/proj-ldc-repo-rpm-local/common_base_os/sles/4.2.0-24?ssl_verify=no COMMON_BASE_OS_SLES_REPO \
    && zypper install -l -y python310 \
    && zypper install -l -y python310-pip \
    && zypper clean --all

#install netcat for nc command
RUN zypper addrepo -C -G -f https://download.opensuse.org/repositories/network:utilities/SLE_15_SP3/network:utilities.repo \
    && zypper install -l -y netcat-openbsd \
    && zypper clean --all

RUN pip3.10 install --upgrade pip \
    && pip3.10 install --no-cache-dir pipenv==2022.8.5

ARG WORKDIR=/PM_Data_Handler
WORKDIR ${WORKDIR}
COPY ./Pipfile ${WORKDIR}
COPY ./Pipfile.lock ${WORKDIR}
COPY ./wait-for-kafka-broker-and-schema-registry.sh ${WORKDIR}
COPY ./pm_data_handler/ ${WORKDIR}/pm_data_handler

RUN echo "$USER_ID:x:$USER_ID:0:An Identity for $USER_NAME:/nonexistent:/bin/false" >>/etc/passwd
RUN echo "$USER_ID:!::0:::::" >>/etc/shadow

RUN pipenv sync --system

ENV PYTHONPATH "pm_data_handler"

USER $USER_ID

CMD ["./wait-for-kafka-broker-and-schema-registry.sh"]