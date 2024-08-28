#!/bin/bash
# COPYRIGHT Ericsson 2023
#
# The copyright to the computer program(s) herein is the property of
# Ericsson Inc. The programs may be used and/or copied only with written
# permission from Ericsson Inc. or in accordance with the terms and
# conditions stipulated in the agreement/contract under which the
# program(s) have been supplied.

#This script control running the PM Data Handler App in order to wait for Kafka & Schema Registry servers to be up!

isEnvironmentAvailable=0
numberOfTrials=0

while [ $isEnvironmentAvailable -eq 0 ]
do
  ((numberOfTrials++))
  kafkaBrokerPortConnection=$(nc -zv broker 29092 2>&1)
  schemaRegistryPortConnection=$(nc -zv schema-registry 8081 2>&1)
  if [[ $kafkaBrokerPortConnection == *"succeeded!"* ]] && [[ $schemaRegistryPortConnection == *"succeeded!"* ]]; then
    isEnvironmentAvailable=1
    echo "$(date +"%m/%d/%Y %r") ~> wait-for-kafka-broker-and-schema-registry.sh | Trial N°${numberOfTrials} | Kafka Broker and Schema Registry are up!"
    echo "$(date +"%m/%d/%Y %r") ~> wait-for-kafka-broker-and-schema-registry.sh | Trial N°${numberOfTrials} | Launching PM Data Handler"
    sleep 15 # wait until the producer sim creates the topic.
    python3.10 -m pm_data_handler
  else
    echo "$(date +"%m/%d/%Y %r") ~> wait-for-kafka-broker-and-schema-registry.sh | Trial N°${numberOfTrials} | FAILED"
      if [[ $numberOfTrials -ge 20 ]]; then
        echo "$(date +"%m/%d/%Y %r") ~> wait-for-kafka-broker-and-schema-registry.sh | Maximum number of trials exceeded | EXITED "
        echo "$(date +"%m/%d/%Y %r") ~> PM Data Handler will not be launched, please check the availability of kafka broker & schema registry!"
        break
      fi
    echo "$(date +"%m/%d/%Y %r") ~> wait-for-kafka-broker-and-schema-registry.sh | Trial N°$((numberOfTrials+1)) | SCHEDULED in 6s ..."
    sleep 6 # Sleep 6 seconds before next trial!
  fi


done
