ERICSSON © 2022

# CAD Optimization Service

This service handles instances for both optimization and node configuration. It holds the information about the status
of each instance and its results.

---

# Table of Contents

* [About The Project](#about-the-project)
    * [Introduction](#introduction)
    * [Code Structure](#code-structure)
* [Getting Started](#getting-started)
    * [Requirements](#requirements)
    * [Installation](#installation-for-python-usage)
    * [Run Using Python](#run-using-python)
    * [Run Using Docker](#run-using-docker)
    * [Unit Tests For CAD Optimization](#unit-tests-for-cad-optimization)
    * [Code Formatting And Fix](#code-style-check-and-fix)
* [Support](#support)
    * [Project Contributors](#project-contributors)
* [License](#license)
* [Important Notes](#important-notes)

---

# About The Project

## Introduction

Every time an optimization or partner configuration is needed, CAD Optimization service is contacted to perform these
actions. First, an optimization instance will be created, and its progress state is followed by the service as it gets
handled at each step of the optimization process. Then, after optimization is done, the service allows for partner
configuration of a selection of the resulting node pairs from the optimization.

CAD Optimization interacts with both CAD PM Data Handler and NCMP Proxy services to provide its exposed functions. CAD
PM Data Handler provides the needed performance measurements needed to predict cell load needed to compute the optimal
pairs. NCMP Proxy is contacted by CAD Optimization in order to retrieve the UE Measurements and to configure the
partners in the network.
![CAD Optimization Deployment Diagram](../resources/cad_optimization_component_diagram_1_0.png)
> [!TODO] Complete this section

---

## Code Structure

- **`root`**: Contains the files to create the docker image for this service. It includes required libraries.
- **`ArcSrv`**: Contains the service launch point (`main.py`). This folder also presents the module structure for the
  CAD Optimization.
    - **`api`**: Contains the service end-point declarations.
    - **`configs`**: Holds the configuration files for the correct execution of CAD Optimization.
    - **`database`**: Holds the code related to database connection.
    - **`entities`**: Holds entities used along the optimization process. It can contain database models as well.
    - **`helper`**: Utilitary classes (e.g. logging)
    - **`optimization`**: Holds the logic behind gNodeB E5 links optimization (starting at `build_solution.py`)
    - **`resources`**: The place for additional files used throughout the service that are not code.
    - **`services`**: Exposes the functionality for CAD Optimization separated by concerns (database, kpi,
      optimization).
    - **`test`**: Contains the test files for the available functionalities in CAD Optimization.

---

# Getting Started

> [!WARNING]  
> It is strongly recommended to use the Docker Compose file in root folder to build the entire CAD environment as
> services are interconnected. The following scripts are for single component development only.

## Requirements

Executing the code in this project properly requires that your environment has the packages listed in
the [Pipfile](./Pipfile) installed.

If you are running a Linux environment, you can go ahead and follow the installation steps.

If you are running a Windows environment, please use WSL2 to install a Ubuntu environment that allows to use correctly
this code. After installing a Ubuntu environment with Python and Docker you will be able to run the installation steps
below.

---

## Installation For Python Usage

```shell
# Change working directory to `[REPO]/ArcSwU`
cd ArcSwU

# Install pipenv and create virtual environment with python packages:
python3.10 -m pip install --no-cache-dir pipenv
pipenv install --deploy --dev

# Install missing python packages in your local env:
pipenv lock -r --dev > requirements.txt
pip install -r requirements.txt
rm -f requirements.txt
```

### Run Using Python

```shell
python3 ArcSrv/main.py
```

### Run Using Docker

The `Dockerfile` already contains all needed installation and running actions, just create the image using the following
script:

```shell
# Build image:
sudo docker build -t cadoptimization .

# Start container:
sudo docker run -d -p 5000:5000 cadoptimization
```

### Unit tests for CAD Optimization

The following testing scripts are used in the local python. If you are using docker, you need to access to the running
container first.

```shell
# Change directory to `[REPO]/ArcSwU`
cd ArcSwU
# Launch virtual environment
pipenv shell
# Change directory to `[REPO]/ArcSwU/ArcSrv`
cd ArcSrv
# Run unit tests
python3.10 -m unittest -v
# Run unit tests with coverage and omit tests from html report 
# (optional flags are: `--source ./<dir> -- branch --settings=test_XXXX --parallel=1`)
coverage run --source ./test --omit ./test/__init__.py -m unittest -v
# To omit tests
# coverage run --omit ./test/*.py -m unittest -v

# Generate a coverage html report
coverage html
# Or generate a complete coverage report separately
# coverage report

# Exit virtual environment
exit
```

### Code Style Check and Fix

```shell
# Change directory to `[REPO]/ArcSwU`
cd ArcSwU
# Launch virtual environment
pipenv shell
# Change directory to `[REPO]/ArcSwU/ArcSrv`
cd ArcSrv
# Run tox
tox -e isort,flake8,black
# Exit virtual environment
exit
```

---

### CAD Optimization Restful endpoints documentation
After starting all docker images using the docker-compose file or after starting the cad-optimization and the NCMP simulator images, you can test the CAD Optimization APIs using this cad-optimization swagger UI 
[http://localhost:5000/](http://localhost:5000/) 
- To create an optimization, you can use **POST /optimizations/** with the request body that contains selected nodes to optimize their E5
connections, unwantedNodePairs, the list of node pairs Id that is not to be used for ARC and mandatoryNodePairs, the list of node pairs to add to the BB partner to set up
- Using the `optimization_id` from create optimization request response body, you can start the optimization using **POST /optimizations/{optimization_id}/start** request
- To check the optimization status, you can use **GET /optimizations/{optimization_id}/start** request; in the response body, you will find the status of the optimization and the result
if the status is optimization finished, the result field will contain the resulted BB partners
- To stop an ongoing optimization, you can use **POST /optimizations/{optimization_id}/stop** request
- To retrieve mocked KPIs value, you can use **POST /kpis/** request
- To start a configuration instance, you can use **POST /configurations/** request
- Using the `configuration_id` from create configuration request, you can check the configuration status with **GET /configurations/{configuration_id}/status** request

---

# Support

The following persons have contributed to the code in this repository. Please contact them for further information.

## Project contributors

[Papa Demba](mailto:papa.demba.diallo@ericsson.com)  
[Wassim Derbel](mailto:wassim.derbel@ericsson.com)  
[Qasim Khalid](mailto:qasim.khalid@ericsson.com)  
[Laurent Mariko](mailto:laurent.mariko@ericsson.com)  
[Wojciech Rakow](mailto:wojciech.xx.rakow@ericsson.com)  
[Zohra Zribi](mailto:zohra.zribi@ericsson.com)  
[![](https://img.shields.io/badge/gmail-%23DD0031.svg?&style=for-the-badge&logo=gmail&logoColor=white)](mailto:papa.demba.diallo@ericsson.com,wassim.derbel@ericsson.com,qasim.khalid@ericsson.com,laurent.mariko@ericsson.com,wojciech.xx.rakow@ericsson.com,zohra.zribi@ericsson.com)

---

# License

This project is not yet licensed - see the [LICENSE.md](./LICENSE.md) file for details

---

# Important Notes

> 📝 Internal navigation doesn't work in Markdown preview for PyCharm IDE with versions lower than
> 2022.2.3 [Link](https://youtrack.jetbrains.com/issue/IDEA-213085/Internal-navigation-doesnt-work-in-Markdown-preview)
