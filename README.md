# P4DB: On-the-fly Debugging of the Programmable Data Plane

P4Db is a general debugging platform to troubleshoot runtime bugs of P4 programs.

This repository is the P4DB debugging platform running on [ONOS](http://onosproject.org/). So if you want to use the P4DB debugging platform, it's better for readers to have an intimate knowledge of ONOS.

## Prerequisites

#### Dependencies
 
1. Install git, jdk and maven 

Install git.
```bash
$ sudo apt-get install git
```

Install jdk.
```bash
$ sudo apt-get install software-properties-common -y
$ sudo add-apt-repository ppa:webupd8team/java -y
$ sudo apt-get update
$ sudo apt-get install oracle-java8-installer oracle-java8-set-default -y
```

Install maven.
```bash
$ cd; mkdir Downloads Applications
$ cd Downloads
$ wget http://archive.apache.org/dist/karaf/3.0.5/apache-karaf-3.0.5.tar.gz
$ wget http://archive.apache.org/dist/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz
$ tar -zxvf apache-karaf-3.0.5.tar.gz -C ../Applications/
$ tar -zxvf apache-maven-3.3.9-bin.tar.gz -C ../Applications/
```

#### Installation of ONOS 

#### Installation of dependent modules


## Install

## Run

## Modules

There are as many as 7 modules in the repository. Three of them, i.e. api, driver and provider, are cloned from the ONOS repository. And they are modified to fix some bugs in the original repository. The remains four are developed by us to to constitute the main framework of P4DB.

Their functions are listed as below:


## Notice 

The debugging is still under extensive developing. Furthermore, we will release the stable version as soon as we finish the development in the near future. This open-source version of P4DB is aimed at helping people deeply understand how P4Db work.
