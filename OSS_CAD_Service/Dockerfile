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
    && zypper install -l -y python310 python310-pip curl zip \
    && zypper clean --all \
    && zypper rr COMMON_BASE_OS_SLES_REPO

RUN pip3.10 install --upgrade pip \
    && pip3.10 install --no-cache-dir pipenv==2022.8.5

ARG WORKDIR=/arc_app
WORKDIR ${WORKDIR}
COPY ./OSS_CAD_Service/Pipfile ${WORKDIR}
COPY ./OSS_CAD_Service/Pipfile.lock ${WORKDIR}
COPY ./flows/arc-automation-flow/src/main/resources/ ${WORKDIR}/flow
COPY ./OSS_CAD_Service/ArcSrv/ ${WORKDIR}/ArcSrv

EXPOSE 5000

RUN echo "$USER_ID:x:$USER_ID:0:An Identity for $USER_NAME:/nonexistent:/bin/false" >>/etc/passwd
RUN echo "$USER_ID:!::0:::::" >>/etc/shadow

RUN pipenv sync --system

ENV PYTHONPATH "ArcSrv"

USER $USER_ID
ENTRYPOINT ["python3.10", "ArcSrv/main.py"]
