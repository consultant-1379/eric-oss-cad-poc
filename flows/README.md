ERICSSON © 2022

# CAD Flow - CAD rApp

---

# Table of Contents

* [About this project](#about-this-project)
    * [Introduction](#introduction)
    * [Code Structure](#code-structure)
    * [Class Loading](#class-loading)
* [Getting Started](#getting-started)
    * [Requirements](#requirements)
    * [Setup](#setup)
    * [Running](#running)
* [Support](#support)
    * [Project contributors](#project-contributors)
* [License](#license)
* [Important Notes](#important-notes)

---

# About This Project

## Introduction
CAD Flow is developed on the Flow Automation service provided by EIAP. It interacts with the user to retrieve operator’s input and configuration that is used to perform the network optimization.
![CAD rApp Component Diagram](../resources/cad_component_diagram_1_0.png)

CAD Flow consists of two phases: setup and execute.

## Code Structure

> [!TODO] Complete this section

## Class Loading
In order to use classes that are present in `arc-automation-flow/src/main/resources/groovy/constants`, `arc-automation-flow/src/main/resources/groovy/exceptions and arc-automation-flow/src/main/resources/groovy/utils` in the groovy scripts, it is necessary to use a custom class loader to load these classes.
It is very important to read this [documentation](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/ATP/Flow+Automation+Script+Compilation) and watch this [video](https://ericsson-my.sharepoint.com/:v:/p/daniel_felipe_gonzalez_obando/EQgx27gl5dVEsUNtCeMc730B2rDrcbkhoVZi9BtSO2hI0g) before proceeding with class loaders in order to fully understand why class loaders are being used in the groovy scripts and to have a clearer view on the limitations of flow automation in terms of using explicit classes.

The following steps could serve as a guide for using the custom class loaders to load these classes:

- First convert the contents of the class to string using the `getStringFromResourceFile` method which takes the parameters process instance execution and the file path of the class to be loaded.
- Create a new custom class loader with this code snippet: `new GroovyClassLoader(getClass().getClassLoader())`
- Parse the string to the class loader using the parseClass method of the class loader in order to load the class
- Before loading a class, please check to see if this class uses another explicit class. If it does, the class used internally should be loaded first before loading the class
- You could check any of the scripts or this [script](../flowAutomation/arc-automation-flow/src/main/resources/groovy/Optimization.groovy) to see how the custom class loading is done

# Getting Started

> [!WARNING]
> It is strongly recommended to use the Docker Compose file in root folder to build the entire CAD environment as
> services are interconnected. The following scripts are for single component development only.

## Requirements

> [!WARNING]
> It is strongly recommended to use the Docker Compose file in root folder to build the entire CAD environment as
> services are interconnected. The following scripts are for single component development only.

- A Linux environment (WSL in windows)
- Docker (with docker compose) to launch Flow Automation services.
- Java 8
- Maven

## Setup

> [!INFO]
> There exists an integrated setup script available in the [root](../README.md) of the project (`/Makefile`). Please try
> using that script over these more complex instructions.

First, make sure to log in to Ericsson Docker registry:

```shell
docker login armdocker.rnd.ericsson.se
```

Once logged in, you can build the images (from the repo root folder):

```shell
docker-compose build
```

Then start the containers:

```shell
docker-compose up
```

When completed you can check if the service is up by opening in a web
browser [http://localhost:8282/#flow-automation](http://localhost:8282/#flow-automation).
You should see an interface with no available flows in it.
To compile CAD Flow you will need to change to the project folder:

```shell
cd /flowAutomation
```

And then run the deployment command in maven:

```shell
mvn clean install
```

Once built, you can upload the generated zip file in `/flowAutomation/arc-flow-automation/target` either directly on the
web browser by clicking 'View Flow Catalog' and then 'Import Flow', or via terminal by executing (make sure to
adjust `repo_path` to your repo local path):

```shell
curl --insecure --request POST 'http://localhost:8282/flowautomation/v1/flows' --cookie cookie.txt -H 'Accept: application/json' -H 'content-type: multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW' -F 'flow-package=@[repo_path]/flowAutomation/arc-automation-flow/target/arc-automation-flow-1.0.0-SNAPSHOT.zip'
```

## Running

Running an instance of CAD Flow is simple, just go to [Flow Catalog](http://localhost:8282/#flow-automation/flowcatalog)
and select the Carrier Aggregation Deployment flow. Then click, the 'Start' button to configure and run a new instance.

---

# Support

The following persons have contributed to the code in this repository. Please contact them for further information.

## Project contributors

[Peiliang Chang](mailto:peiliang.chang@ericsson.com)  
[Papa Demba](mailto:papa.demba.diallo@ericsson.com)  
[Wassim Derbel](mailto:wassim.derbel@ericsson.com)  
[Daniel Gonzalez](mailto:daniel.felipe.gonzalez.obando@ericsson.com)  
[Qasim Khalid](mailto:qasim.khalid@ericsson.com)  
[Nizar Louhichi](mailto:nizar.louhichi@ericsson.com)  
[Laurent Mariko](mailto:laurent.mariko@ericsson.com)  
[Stephen Sarpong Antwi](mailto:stephen.sarpong.antwi@ericsson.com)  
[Wojciech Rakow](mailto:wojciech.xx.rakow@ericsson.com)  
[Mats Zachrison](mailto:mats.zachrison@ericsson.com)  
[Zohra Zribi](mailto:zohra.zribi@ericsson.com)  
[![](https://img.shields.io/badge/gmail-%23DD0031.svg?&style=for-the-badge&logo=gmail&logoColor=white)](mailto:peiliang.chang@ericsson.com,papa.demba.diallo@ericsson.com,wassim.derbel@ericsson.com,daniel.felipe.gonzalez.obando@ericsson.com,qasim.khalid@ericsson.com,nizar.louhichi@ericsson.com,laurent.mariko@ericsson.com,stephen.sarpong.antwi@ericsson.com,wojciech.xx.rakow@ericsson.com,mats.zachrison@ericsson.com,zohra.zribi@ericsson.com)

---

# License

This project is not yet licensed - see the [LICENSE.md](./LICENSE.md) file for details

---

# Important Notes

> 📝 Internal navigation doesn't work in Markdown preview for PyCharm IDE with versions lower than
> 2022.2.3 [Link](https://youtrack.jetbrains.com/issue/IDEA-213085/Internal-navigation-doesnt-work-in-Markdown-preview)