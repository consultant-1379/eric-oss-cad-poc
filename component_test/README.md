ERICSSON © 2022

# CAD Optimization component tests

This folder contains component tests for the CAD Optimization and the necessary framework to execute those tests.

---

# Table of contents

* [About the project](#about-the-project)
    * [Introduction](#introduction)
	* [Code structure](#project-structure)
* [Getting started](#getting-started)
    * [Requirements](#requirements)
	* [Build images](#build-images)
	* [Start containers](#start-containers)
	* [View results](#view-results)
	* [Tear down](#tear-down)
* [Support](#support)
    * [Project contributors](#project-contributors)

---

# About the project
## Introduction

To verify the Optimization component of the rApp Carrier Aggregation Deployment, a test suite should be deployed,
using simulated data with known expected outcome. In order to execute this suite a framework is needed to
read the input data, convert it to proper format, execute tests, display results, etc. This framework
along with the test suite is a separate Docker container. The Software Under Test (SUT), ie. CAD Optimization
is a Docker container as well in the exactly same form as when deployed within rApp. The interaction with SUT
shall be done using its interfaces to Flow Automation, NCMP, and PM Handler components,
which shall be simulated by this Framework.

## Code structure

- **`root`**: Contains files to create and start Docker images for the Framework and CAD Optimization
- **`optimization`**: Contains \_\_main__.py which is an entry point for the Framework Docker container starting it up
    - **`apis`**: Contains interface stubs needed to interact with CAD Optimization
	    - **`flow_automation`**: Interface stub to Flow Automation component
	- **`entities`**: Contains files representing Framework core functions
	- **`tests`**: Contains files with the test source code and utilities needed to setup Pytest

# Getting started

> [!WARNING]
> It is strongly recommended to use the docker-compose.yml file in root folder to build and start needed components
> in correct order.

## Requirements

Executing the code in this project properly requires that your environment has the packages listed
in the [Pipfile](./Pipfile) installed.

If you are running a Linux environment, you can go ahead and follow the instruction steps.

If you are running a Windows environment, please use WSL2 to install a Ubuntu environment that allows to use correctly
this code. After installing a Ubuntu environment with Python and Docker you will be able to run
the installation steps below.

---

## Build images

Build images for CAD Optimization and Component Test Framework.

The following command should be executed from the root directory.

```shell
docker-compose build --force-rm
```

## Start containers

Start CAD Optimization first and then Component Test Framework.

The following command should be executed from the root directory.

```shell
docker-compose up -d
```

## View results

Currently no logging service has been implemented. Test verdict can be checked in
Component Test Framework container logs.

*Pytest return code: 0* <- means all executed tests has passed. Other return code means something went wrong.
More details can be found in logs.

```shell
docker logs opt_comp_test
```

## Tear down

Stop CAD Optimization and Component Test Framework.

The following command should be executed from the root directory.

```shell
docker-compose down -v --remove-orphans
```

# Support

The following persons have contributed to the code in this project. Please contact them for further information.

## Project contributors

[Wojciech Rakow](mailto:wojciech.xx.rakow@ericsson.com)
