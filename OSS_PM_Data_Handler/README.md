ERICSSON © 2022

# PM Data Handler - CAD rApp

This service is responsible for collecting the metrics of nodes currently under optimization in CAD rApp and provide
statistics from this collected data.

---

# Table of Contents

* [About the project](#about-the-project)
    * [Introduction](#introduction)
    * [Code Structure](#code-structure)
    * [Component](#component)
    * [Project](#project)
    * [Role](#role)
    * [Architecture](#architecture)
* [Getting Started](#getting-started)
    * [Techs & Libraries & Keywords](#techs--libraries--keywords)
    * [Dev packages](#dev-packages)
    * [Installation](#installation)
    * [Unit Tests](#unit-tests)
    * [Code Formatting](#code-formatting)
* [Support](#support)
    * [Project contributors](#project-contributors)
    * [Kafka Team](#kafka-team)
      * [Kafka Schema Registry Team](#kafka-schema-registry-team)
      * [Kafka Messages filtering support](#kafka-messages-filtering-support)
    * [CI/CD](#cicd)
* [License](#license)
* [Important Notes](#important-notes)

---

# About the project

## Introduction

This component is in charge of multiple tasks, but mainly it is in charge of:

- Provide an API to allow subscribing and unsubscribing gNodeBs to collect PM counters from them.
- Provide an API to retrieve, aggregate and return PM counters for a provided list of gNodeBs and a specific time
  interval.
- Collecting published needed PM counters from EIAP Kafka DMM Cluster
- Connect to schema registry to fetch the latest schema to be able to deserialize and read received data.
- Storing the needed records in a MongoDB time-series Collection (Collection existence check & creation are also
  included). This process happens after filtering messages based on MO & gnbId source of PM counters.

![CAD PM Data Handler component diagram](../resources/pm_handler_component_diagram_1_0.png)

## Code Structure

Implementing an OOP Architecture, The PM Data Handler is extensible and easily customizable since it is including a
centralized package for configuration.

## Component

CAD PM Data Handler

## Project

Carrier Aggregation Deployment (CAD)

## Role

- Collecting published 6 needed pm counters from EIAP Kafka DMM Cluster
- This component is connected to schema registry to fetch the latest schema to be able to deserialize and
  read received data.
- After filtering based on MO & gnbId source of pm counters, this module is storing the needed records in a
  MongoDB time-series Collection (Collection existence check & creation are also included).

## Architecture

Implementing an OOP Architecture, The PM Data Handler is extensible and easily customizable since
it is including a centralized package for configuration.

---

# Getting Started

## Techs & Libraries & Keywords

- Docker
- Docker compose
- Python v3.9
- requests lib v2.27.1
- fastavro lib v1.5.4
- confluent-kafka lib v1.9.0
- pymongo lib v4.2.0
- Kafka Batch streaming
- Schema Registry
- Data Pipeline
- Data Handling
- Avro serialization

## Dev packages

- tox v3.24.1
- coverage v6.3.2
- unittest-xml-reporting v3.2.0

## Installation

- docker-compose up
- That's it, you can check what is happening inside the cluster by tracking docker logs of the different
  involved services in the `docker-compose.yml`

## Unit Tests

How to run unit tests for the PM data handler app:

Change directory to _<REPO>/PM_Data_Handler_
```shell 
cd PM_Data_Handler
```
Change directory to _<REPO>/PM_Data_Handler/pm_data_handler_
```shell
cd pm_data_handler
```
Launch virtual environment
```shell
pipenv shell
```
Run unit tests
```shell
python -m unittest -v
```
Run unit tests with coverage and omit tests from html report
```shell
coverage run --source ./test --omit ./test/__init__.py -m unittest -v
````
or
```shell
coverage run --omit ./test/*.py -m unittest -v
```
Generate a coverage html report
```shell
coverage html
```
Generate a complete coverage report separately
```shell
coverage report -m
```
Exit virtual environment
```shell
exit
```
## Code Formatting

Tox library is taking care of all needed pre-testing verifications (e.g. Respecting PEP8 Naming convention, Detecting
unused libraries ...)

How to reformat the code and check for code style issues using _tox_ library :

Change directory to <REPO>/PM_Data_Handler
```shell
cd PM_Data_Handler
```
Change directory to <REPO>/PM_Data_Handler/pm_data_handler
```shell
cd pm_data_handler
```
Launch virtual environment
```shell
pipenv shell
```
Run tox
```shell
tox -e isort,flake8,black
```
Exit virtual environment
```shell
 exit
```
---

# Support

Tell people where they can go to for help. It can be any combination of an issue tracker, a chat room, an email address,
etc.

## Project contributors

[Wassim Derbel](mailto:wassim.derbel@ericsson.com)  
[Ikbel Benabdessamad](mailto:ikbel.benabdessamad@ericsson.com)  
[Stephen Sarpong Antwi](mailto:stephen.sarpong.antwi@ericsson.com)  
[Zohra Zribi](mailto:zohra.zribi@ericsson.com)  
[![](https://img.shields.io/badge/gmail-%23DD0031.svg?&style=for-the-badge&logo=gmail&logoColor=white)](mailto:wassim.derbel@ericsson.com,ikbel.benabdessamad@ericsson.com,zohra.zribi@ericsson.com,stephen.sarpong.antwi@ericsson.com)

## Kafka Team

### Main contacts

[Hussain Kaneka](mailto:hussain.kanekal@ericsson.com)  
[Susanta Mohanty](mailto:susanta.mohanty@wipro.com)  
[Jayashree E P](mailto:jayashree.e.p@ericsson.com)  
[Lakshmi Gururajrao](mailto:lakshmi.gururajrao.ext@ericsson.com)  
[![](https://img.shields.io/badge/gmail-%23DD0031.svg?&style=for-the-badge&logo=gmail&logoColor=white)](mailto:hussain.kanekal@ericsson.com,lakshmi.gururajrao.ext@ericsson.com,jayashree.e.p@ericsson.com,susanta.mohanty@wipro.com)

### Kafka Schema Registry Team

[Team Techcoders](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/pages/viewpage.action?spaceKey=IDUN&title=Team+Techcoders)  
[![](https://img.shields.io/badge/gmail-%23DD0031.svg?&style=for-the-badge&logo=gmail&logoColor=white)](mailto:ramesh.arumugam.ext@ericsson.com,shahazad.khanam.ext@ericsson.com,arjun.singh.chauhan.ext@ericsson.com,subhijith.tharayil.ext@ericsson.com,karthikeyan.kj.ext@ericsson.com,srinivas.e.v.ext@ericsson.com)

### Kafka Messages filtering support

[Team Powerhouse](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/IDUN/Team+Powerhouse)  
[![](https://img.shields.io/badge/gmail-%23DD0031.svg?&style=for-the-badge&logo=gmail&logoColor=white)](mailto:susanta.mohanty.ext@ericsson.com,prasad.thutta.ext@ericsson.com,dinesh.tiwari.ext@ericsson.com,veeramani.v.ext@ericsson.com,harinadha.reddy.ext@ericsson.com,srinivas.annam.ext@ericsson.com,kandakatla.raju.ext@ericsson.com,shivangi.rani.ext@ericsson.com)

## CI/CD

### Main contacts

[Nizar Louhichi](mailto:nizar.louhichi@ericsson.com)  
[![](https://img.shields.io/badge/gmail-%23DD0031.svg?&style=for-the-badge&logo=gmail&logoColor=white)](mailto:nizar.louhichi@ericsson.com)

---

# License

This project is not yet licensed - see the [LICENSE.md](./LICENSE.md) file for details

---

# Important Notes
> 📝 All the  **PM-Data-Handler** resources can be found on this [Link](https://ericsson.sharepoint.com/:f:/s/MR9644ERANAutomation/ElfFlchAsGpEmH_wQSDk44sBvXjKD6fyWyhCPTwnbCz0hg?e=9qzyr3) 

> 📝 Internal navigation doesn't work in Markdown preview for PyCharm IDE with versions lower than
> 2022.2.3 [Link](https://youtrack.jetbrains.com/issue/IDEA-213085/Internal-navigation-doesnt-work-in-Markdown-preview)

> 📝 _Get the full coverage in SonarQube along with the current status of the
project_ [Sonar-Link](https://sonarqube.lmera.ericsson.se/sessions/new?return_to=%2Fdashboard%3Fid%3Drapp-ai-inter-bb-coordination)

> 📝 For the APIs implementation please check
> this [Link](https://ericsson.sharepoint.com/:b:/r/sites/MR9644ERANAutomation/Shared%20Documents/Resources/Knowledge%20Sharing%20Sessions/PM%20Handler%20Solution/4%20PM%20Persister%20DBMS%20Benchmark%20tests/PM%20Data%20Storage%20Techs%20Benchmark%20Tests.pdf?csf=1&web=1&e=GSzeb4)
> , while focusing on these two sections: **_Calculate AVG PM Query Time MongoDB_** and **_Calculate AVG KPIs Query Time
MongoDB_**

> 📝 Kafka team informed us that the schema would be static, and this can be changed thus keeping the synchronization is necessary


> 📝 Dealing with json/avro errors.   
> As soon as the read data format doesn’t match the
> expected schema, an exception should be thrown. the error should get treated
> as a fatal PM Data error – log the error as good as possible, report back to the user something like “Error reading KPI
> and cell data”, and then continue the optimization along the lines of “no PM data available” or “missing PM data” which
> I believe we have UCs for