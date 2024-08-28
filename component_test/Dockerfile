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

RUN pip3.10 install --no-cache-dir pipenv==2022.3.23

ARG WORKDIR=/component_test
WORKDIR ${WORKDIR}
COPY ./Pipfile ${WORKDIR}
COPY ./Pipfile.lock ${WORKDIR}
COPY ./optimization/ ${WORKDIR}/optimization

RUN echo "$USER_ID:x:$USER_ID:0:An Identity for $USER_NAME:/nonexistent:/bin/false" >>/etc/passwd
RUN echo "$USER_ID:!::0:::::" >>/etc/shadow

RUN pipenv sync --system

ENV PYTHONPATH "optimization"

USER $USER_ID

CMD ["python3.10", "-m", "optimization"]
