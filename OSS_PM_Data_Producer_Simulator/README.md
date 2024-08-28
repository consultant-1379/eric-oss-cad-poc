ERICSSON © 2022

# PM Data Producer Mock for CAD rApp

A module simulating the PM messages sent by EIAP Kafka service.

---

# Table of Contents

* [About The Project](#about-the-project)
    * [Introduction](#introduction)
    * [Project Structure](#project-structure)
* [Getting Started](#getting-started)
    * [Requirements](#requirements)
    * [Setup](#setup)
* [Support](#support)
    * [Project contributors](#project-contributors)
* [License](#license)
* [Important Notes](#important-notes)

---

# About The Project

## Introduction

This module has been created to simulate what the Kafka PM producer would make available as performance measurements (
PM) counters for different cells. The data produced by this simulator has been adjusted to present the needed PM
counters with an amount calculated to be close to a real scenario.

> [!INFO]
> You can for now launch the producer and test it with the consumer prototype for now. Check
> ticket [AIPIC-343](https://gerrit-review.gic.ericsson.se/c/msrbs/rapp-ai-inter-bb-coordination/+/94281)

## Project Structure

> [!TODO] Complete this section

---

# Getting Started

## Requirements

In order to run the PM data producer mock the following environment is necessary:

- A Linux environment with docker installed and access to internet.

## Setup

If needed, shut down existing containers:

```shell
docker-compose down -v --remove-orphans
```

Then, turn on containers again (now including the producer mock):

```shell
docker-compose up
```

Finally, you can check the logs and status of the producer simulator:

```shell
docker logs pm-data-producer-sim
```

When you see this logs:

```
pm-data-producer-sim    | Setting up connections to kafka broker and schema registry | DONE
pm-data-producer-sim    | Producer | Successfully connected to the environment | DONE
pm-data-producer-sim    | Producer | Initialized successfully | READY
pm-data-producer-sim    | Importing PM Simulation Data source file | IN PROGRESS ...
pm-data-producer-sim    | Importing PM Simulation Data source file | DONE
pm-data-producer-sim    | Producer | Simulator is launched successfully | LAUNCHED
```

You are ready to go, now you can run the consumer prototype to test data transfer, go to the folder that contains
consumer.py of [AIPIC-343]:
`python consumer.py localhost:9092 1 pm_data`
An output example of the serialized PM generated is as follows:

```
b'\x00\x00\x00\x00\x01\xe0\x9c\xa0\xa7\x98\x01\x9e\xcf\xa9\xe6U\x82\x90\xc3 "1654066617.155899'
% pm_data [0] at offset 4374 with key b'3be4b82e-d1dd-4988-8946-88a5b7dfb98b':
```

---

# Support

The following persons have contributed to the code in this repository. Please contact them for further information.

## Project contributors

[Ikbel Benabdessamad](mailto:ikbel.benabdessamad@ericsson.com)  
[Peiliang Chang](mailto:peiliang.chang@ericsson.com)  
[Wassim Derbel](mailto:wassim.derbel@ericsson.com)  
[Qasim Khalid](mailto:qasim.khalid@ericsson.com)  
[Laurent Mariko](mailto:laurent.mariko@ericsson.com)  
[Mats Zachrison](mailto:mats.zachrison@ericsson.com)  
[![](https://img.shields.io/badge/gmail-%23DD0031.svg?&style=for-the-badge&logo=gmail&logoColor=white)](mailto:ikbel.benabdessamad@ericsson.com,peiliang.chang@ericsson.com,papa.demba.diallo@ericsson.com,wassim.derbel@ericsson.com,daniel.felipe.gonzalez.obando@ericsson.com,qasim.khalid@ericsson.com,nizar.louhichi@ericsson.com,laurent.mariko@ericsson.com,stephen.sarpong.antwi@ericsson.com,wojciech.xx.rakow@ericsson.com,mats.zachrison@ericsson.com,zohra.zribi@ericsson.com)

---

# License

This project is not yet licensed - see the [LICENSE.md](./LICENSE.md) file for details

---

# Important Notes

> 📝 Internal navigation doesn't work in Markdown preview for PyCharm IDE with versions lower than
> 2022.2.3 [Link](https://youtrack.jetbrains.com/issue/IDEA-213085/Internal-navigation-doesnt-work-in-Markdown-preview)