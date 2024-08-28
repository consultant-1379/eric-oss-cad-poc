## ------------------------------------------------------
## AI-Powered Inter-DU coordination rApp
## This is the deployment makefile made for preparing and
## deploying the Arc Optimization and the Arc Flow
## applications that make part of the rApp.
## Please refer to the README.md to find more information
## on how to use this file.
## ------------------------------------------------------

## ----------------------------------
## * Main commands *
## help: Show the help for this project
## launch_local: Launch the local application
## relaunch_local: Stop current running application and recreates all images and containers.
## relaunch_local_test: Same as relaunch_local but including integration tests.
## compose_down: Stop all containers
## arc_container_reload: Stop, recreate arc optimization image and start it again.
## compose_rebuild_[cts/ncmp]: Stop the containers and recreate cts or ncmp and launch it again.
## ----------------------------------
##
# -----------------------------------
# Properties
# -----------------------------------

SHELL := /bin/bash
REPOROOT=$(shell git rev-parse --show-toplevel)

## -----------------------------------
## General
## -----------------------------------
help:	## Show this help
	@sed -ne '/@sed/!s/## //p' $(MAKEFILE_LIST)

check_requirements: ## Check the environment requirements for the application to work correctly
ifeq ($(shell uname -s),Linux)
ifeq ($(shell grep '^ID_LIKE' /etc/os-release | sed -e 's,^ID_LIKE=,,' -e 's,,,'),debian)
ifneq ($(shell apt list -a dos2unix | grep -i "installed"),)
	@echo "dos2unix found"
else
	@echo "dos2unix not found, installing it..."
	@sudo apt install dos2unix
endif
else
	@echo "The launched script is for Debian systems. Make sure it is adapted to your system ($(shell grep '^NAME' /etc/os-release | sed -e 's,^NAME=",,' -e 's,",,'))"
	@echo "Make sure you have dos2unix installed"
endif
	dos2unix ${REPOROOT}/env/setupUbuntu
	sh ${REPOROOT}/env/setupUbuntu
else ifeq ($(shell uname -s),Darwin)
ifneq ($(shell which brew | grep "brew"),)
	@echo "brew found"
ifneq ($(shell brew list | grep "dos2unix"),)
	@echo "dos2unix found"
else
	@echo "dos2unix not found, installing it..."
	@brew install dos2unix
endif
	dos2unix ${REPOROOT}/env/setupMac
	sh ${REPOROOT}/env/setupMac
else
	@echo "Homebrew was not found, please make sure it is correctly installed"
endif
else
	@echo "You are working on an incompatible system."
endif

launch_local: compose_launch flow_launch_local ## Create and launch the whole rApp
ifeq ($(shell uname -s),Linux)
	@echo -e "\nOpening the flow automation server http://localhost:8282/#flow-automation/flowcatalog in your default browser"
	@sleep 2s
	@xdg-open http://localhost:8282/#flow-automation/flowcatalog
else ifeq ($(shell uname -s),Darwin)
	@echo -e "\nOpening the flow automation server http://localhost:8282/#flow-automation/flowcatalog in your default browser"
	@sleep 2s
	@open http://localhost:8282/#flow-automation/flowcatalog
else
	@echo "Switch to a Linux or Mac system"
endif

launch_local_test: compose_launch arc_test flow_launch_local_test ## Create and launch the whole rApp and perform integration tests

relaunch_local: ## Recreate and launch the whole rApp. Set RECREATE_IMAGES=true to recreate images again
ifeq ($(RECREATE_IMAGES),true)
relaunch_local: remove_local launch_local
else
relaunch_local: compose_down launch_local
endif

relaunch_local_test: ## Recreate and launch the whole rApp and perform integration tests. Set RECREATE_IMAGES=true to recreate images again
ifeq ($(RECREATE_IMAGES),true)
relaunch_local_test: remove_local launch_local_test
else
relaunch_local_test: compose_down launch_local_test
endif

remove_local: compose_down	## Stop running containers and removes all images created in this project
ifneq ($(shell docker image ls armdocker.rnd.ericsson.se/proj_oss_releases/enm/fa-service -q),)
	docker rmi armdocker.rnd.ericsson.se/proj_oss_releases/enm/fa-service
else
	@echo "No fa-service image found."
endif
ifneq ($(shell docker image ls armdocker.rnd.ericsson.se/proj_oss_releases/enm/fa-db -q),)
	docker rmi armdocker.rnd.ericsson.se/proj_oss_releases/enm/fa-db
else
	@echo "No fa-db image found."
endif
ifneq ($(shell docker image ls armdocker.rnd.ericsson.se/proj_oss_releases/enm/fa-access-control -q),)
	docker rmi armdocker.rnd.ericsson.se/proj_oss_releases/enm/fa-access-control
else
	@echo "No fa-access-control image found."
endif
ifneq ($(shell docker image ls armdocker.rnd.ericsson.se/proj_oss_releases/enm/fa-ui -q),)
	docker rmi armdocker.rnd.ericsson.se/proj_oss_releases/enm/fa-ui
else
	@echo "No fa-ui image found."
endif
ifneq ($(shell docker image ls arcapp -q),)
	docker rmi arcapp
else
	@echo "No arcapp image found."
endif
ifneq ($(shell docker image ls ctsproxy -q),)
	docker rmi ctsproxy
else
	@echo "No ctsproxy image found."
endif
ifneq ($(shell docker image ls ncmpsim -q),)
	docker rmi ncmpsim
else
	@echo "No ncmpsim image found."
endif

remove_image:	## Removes a specific image. Set IMAGE=the name of the specific image you want to remove.
ifdef IMAGE
ifneq ($(shell docker image ls $(IMAGE) -q),)
	docker rmi $(IMAGE)
else
	@echo "no $(IMAGE) image was found"
endif
else
	@echo "please specify the image you want to remove"
endif

## -----------------------------------
## ARC Optimization
## -----------------------------------

arc_reload: arc_container_remove arc_container_up	## Reload the container including the ARC Optimization service

arc_test_reload: arc_container_remove arc_test arc_container_up	## Reload the container including the ARC Optimization service

arc_container_stop:	## Stop the 'arcoptimization' container
	docker-compose stop arcoptimization

arc_container_remove: arc_container_stop ## Removes the 'arcoptimization' container
ifneq ($(shell docker container ls -a | grep "arcoptimization"),)
	docker container rm arcoptimization
else
	@echo "No arcoptimization container found."
endif
ifneq ($(shell docker image ls arcapp -q),)
	docker rmi arcapp
else
	@echo "No arcapp image found."
endif

arc_container_up:	## Load the 'arcoptimization' container (recreates if an image already exists)
	docker-compose up --build --force-recreate --no-deps -d arcoptimization

arc_test:  ## Perform unit testing and code style check
	pylint ${REPOROOT}/ArcSwU/ArcSrv/* || echo ""
	source $(shell cd ${REPOROOT}/ArcSwU/ && pipenv --venv)/bin/activate && \
	cd ${REPOROOT}/ArcSwU/ArcSrv/ && \
	python3.10 -m unittest -v

## ---------------------------------
## Local Environment Setup
## ---------------------------------

compose_login:	## Login to the Ericsson image repository
	@echo "Logging in to armdocker.rnd.ericsson.se..."
	docker login armdocker.rnd.ericsson.se

compose_launch: compose_build compose_up	## Build docker compose images and start the containers

compose_relaunch: compose_down compose_build compose_up	## Stop existing containers, recreate images and start the containers

compose_build: compose_login	## Build docker compose images
	docker-compose build --force-rm

compose_up: ## Start the containers for this project
	docker-compose up -d
	@echo "Giving some time for FA to startup..."
	@sleep 90s

compose_down:	## Stop and remove running containers for this project
	docker-compose down -v --remove-orphans

compose_stop_ncmp:	## Stop and remove running ncmp container
	docker-compose stop ncmpsim

compose_remove_ncmp: compose_stop_fajboss arc_container_stop compose_stop_ncmp	## Remove ncmpsim image
ifneq ($(shell docker container ls -a | grep "ncmpsim"),)
	docker container rm ncmpsim
else
	@echo "No ctsproxy container found."
endif
ifneq ($(shell docker image ls ncmpsim -q),)
	docker rmi ncmpsim
else
	@echo "No ncmpsim image found."
endif

compose_up_ncmp:	## Recreate and launch ncmp container
	docker-compose up --build --force-recreate --no-deps -d ncmpsim

compose_rebuild_ncmp: compose_remove_ncmp compose_up	## Stop, recreate and run ncmp container

compose_stop_fajboss: ## Stop running fa jboss server
	docker-compose stop fa_jboss

compose_stop_cts:	## Stop running cts container
	docker-compose stop ctsproxy

compose_remove_cts: compose_stop_fajboss compose_stop_cts	## Remove ctsproxy image
ifneq ($(shell docker container ls -a | grep "ctsproxy"),)
	docker container rm ctsproxy
else
	@echo "No ctsproxy container found."
endif
ifneq ($(shell docker image ls ctsproxy -q),)
	docker rmi ctsproxy
else
	@echo "No ctsproxy image found."
endif

compose_up_cts:	## Recreate and launch cts container
	docker-compose up --build --force-recreate --no-deps -d ctsproxy

compose_rebuild_cts: compose_remove_cts compose_up	## Stop, recreate and run cts container

## ----------------------------------
## ARC Flow
## ----------------------------------

flow_launch_local: flow_install flow_upload_local	## Package and upload locally the ARC Flow

flow_launch_local_test: flow_install_test flow_upload_local	## Package, test and upload locally the ARC Flow

flow_relaunch_local: flow_delete_local flow_launch_local	## Delete locally the existing ARC Flow, and then upload it again

flow_relaunch_local_test: flow_delete_local flow_launch_local_test	## Delete the existing ARC Flow, then test it and finally upload it locally again

flow_install:	## Create the flow zip file and performs unit tests
	cd ${REPOROOT}/flowAutomation/arc-automation-flow/ && mvn clean install

flow_install_test:	## Install and perform unit tests, as well as integration tests on ARC FLOW. Note: All rApp docker images must be up for tests to run correctly.
	cd ${REPOROOT}/flowAutomation && mvn clean install

flow_upload_local:	## Upload the ARC Flow zip to the Flow Automation server
	curl --insecure --request POST 'http://localhost:8282/flowautomation/v1/flows' --cookie cookie.txt -H 'Accept: application/json' -H 'content-type: multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW' -F 'flow-package=@${REPOROOT}/flowAutomation/arc-automation-flow/target/arc-automation-flow-1.0.0-SNAPSHOT.zip'

flow_delete_local:	## Remove the ARC Flow zip to the Flow Automation server
	curl --insecure --request DELETE --cookie cookie.txt 'http://localhost:8282/flowautomation/v1/flows/com.ericsson.oss.flow.arc-automation?force=true' -H 'Content-Type: application/json'

flow_reset_build_number:	## Removes the build number file to reset the build number in the generated zip file.
ifeq '$(shell ls ${REPOROOT}/flowAutomation/arc-automation-flow | grep "buildNumber.properties")' 'buildNumber.properties'
	@echo "Build number file found. Removing it..."
	rm ${REPOROOT}/flowAutomation/arc-automation-flow/buildNumber.properties
else
	@echo "buildNumber.properties file not found"
endif
