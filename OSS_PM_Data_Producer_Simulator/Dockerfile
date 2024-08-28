# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

ARG PYTHON_VERSION=3.8
ARG PYTHON_IMAGE=gaia-python-cbo
ARG USER_ID=60577
ARG USER_NAME="eric-sdk"
FROM sekidocker.rnd.ki.sw.ericsson.se/proj-gaia/${PYTHON_IMAGE}:${PYTHON_VERSION}

USER root

#install pipenv
RUN python -m pip install --no-cache-dir pipenv==2022.8.5

#install netcat for nc command
RUN zypper addrepo -C -G -f https://download.opensuse.org/repositories/network:utilities/SLE_15_SP3/network:utilities.repo \
    && zypper install -l -y netcat-openbsd \
    && zypper clean --all

RUN env
#install all dependencies
ARG WORKDIR=/PM_Data_Producer_Simulator
WORKDIR ${WORKDIR}
COPY ./Pipfile ${WORKDIR}
COPY ./Pipfile.lock ${WORKDIR}
COPY ./wait-for-kafka-broker-and-schema-registry.sh ${WORKDIR}
COPY ./pm_data_producer_simulator ${WORKDIR}/pm_data_producer_simulator

#Give execution permission to the script
RUN chmod a+x wait-for-kafka-broker-and-schema-registry.sh

RUN pipenv install --deploy

RUN echo "$USER_ID:x:$USER_ID:0:An Identity for $USER_NAME:/$WORKDIR:/bin/false" >>/etc/passwd
RUN echo "$USER_ID:!::0:::::" >>/etc/shadow

#start app as user
USER $USER_ID
CMD ["./wait-for-kafka-broker-and-schema-registry.sh"]
