# 01 - Data Ingestion

## System Requirements
The following project should work as expected on UNIX Derived OSes, such as Linux and macOS, but it has only been tested on macOS.

In order to execute the program, only a bash script is provided, by understanding the content of such script users might be able to run this project on Windows as well.

## Architecture Overview

All the infrastructure to run the code or query the data is deployed using Docker containers.
This choice was made to guarantee that the project works seamlessly on every host OS supporting Docker, preventing anomalies caused by OS-related factors.

The following containers are used:
* Postgres, used as RDBMS where the output data is stored
* One spark-master container, used to process the input datasets and to publish the output data into Postgres. (Due to hardware limitations, it was not possible to simulate a Spark cluster by spawning also worker nodes.)
* PgAdmin, used to provide the user a UI to query the output data (Alternatively any SQL client can be used, such as [DBeaver](https://www.dbeaver.io))

All these containers and their configuration are defined in the `docker-compose.yml` file.

## Tools Overview

In order to implement the data processing, Apache Spark was the chosen framework.
Being around since seven years and with a quite large user community, Apache Spark is one of the best tools to perform data processing, it allows to perform fast calculations by caching datasets directly in memory and by distributing the workload among a cluster of worker nodes, when cluster mode is enabled.  

Other than Apache Spark, it was necessary to use the following dependencies:
* os-lib: A practical Scala library to perform filesystem operations.
* upickle: A Scala library to easily (de)serialize JSON data. 
* postgresql jdbc driver: JDBC Driver used to let Spark communicate with and send data to the target Postgres DB.

## Project Overview

The aim of this project is to provide insights about air quality, with one dataset at disposal, containing different air quality measurement at county level for each state.
Users should be able to query the processed dataset in order to get some other insights of their choice.

## How To Run
### 1. Download and install Docker
Installing Docker is required to run this project. Installation guides and further informations can be found on [their website](https://www.docker.com/products/docker-desktop).

### 2. Run The Program 

Once Docker is installed, the program can be executed using the `cli.sh` bash script.

First, let's start the containers by using
```shell script
./cli.sh start
```

In order to check if the Docker containers are being started properly, the following command can be used in another terminal window to show logs
```shell script
./cli.sh logs
```

Next, the actual data processing can be started with
```shell script
./cli.sh process_data
```

Services can be accessed using the following links:
* **Spark UI:** http://localhost:4040
* **PgAdmin:** http://localhost:5050
    * username: pgadmin4@pgadmin.org
    * password: admin

#### RDBMS Connection
##### PGAdmin
**Important:** When accessing PgAdmin for the first time, it is necessary to setup a new connection to the postgres database.

Here are the connection details:
* **host:** postgres_container
* **username:** postgres
* **password:** changeme
* **port:** 5432

##### SQL Client (e.g. DBeaver)
**Important:** When accessing your RDBMS for the first time, it is necessary to setup a new connection to the postgres database.

Here are the connection details:
* **host:** localhost
* **username:** postgres
* **password:** changeme
* **port:** 5432

Once the processing and data querying is completed, the Docker containers can be stopped with
```shell script
./cli.sh stop
```

It is also possible to run all the unit tests by using
```shell script
./cli.sh test
```

## Outputs Overview

The Spark code writes two tables to the Postgres database:

* measures(measureid", measurename, measuretype)
* air_quality_reports(measureid, stratificationlevel, statename, countyname, reportyear, value, unit, unitname, dataorigin, monitoronly)

## Algorithm Overview
All the implementation of the algorithm is inside the `it.zevektor.datatest.dataset.DatasetFile` class.

The very first step consists of loading the JSON dataset file.

### 1. Get Dataset Columns 
After loading the JSON file, the content of the `view->meta->columns` is parsed in order to get the list of columns composing the dataset and some information on their format.

There are some useful fields that will be used later:
* `fieldName` contains the string used as column name in the schema
* `dataTypeName` gives some hint on the data type contained in the column, some examples are: `text`, `number`, `meta_data`
* `description` when available, this field contains a human-readable description of what is contained in the column.
* `format->noCommas` when this field is set to `false` and `dataTypeName` is `numeric`, it means this column should be treated as a double.

### 2. Get Dataset Records
Once the columns list is parsed, the `data` field of the JSON file is parsed, since it contains all the records of the dataset, represented as a 2-D array.
A first basic check verifies that each nested array contains `|columns|` elements, which means that the record has all the columns. Any eventual incomplete records are removed from the dataset in this stage (users will get a warning in the application logs).


### 3. Represent Records as a Spark DataFrame
The final step of the algorithm takes the columns list and the records and creates a Spark DataFrame, used to create the two tables that are written to Postgres.

In a first moment, all the columns are represented as Strings, then some of the columns are converted to a numeric type if the column had `dataTypeName` equal to `number`:
* If the column name ends with `id`, it is casted to `Long`.
* If the column `format->noCommas` field is set to `false`, the column is casted to `Double`.
* If the column `format->noCommas` field is not set, the column is casted to `Long`.


### 4. Results and Improvements
With more domain knowledge on each measurement type, it would be easier to understand if there were any anomalies and ensure a better quality of the data:

For example, some measurements have their value equal to zero, which can be a legitimate value for some measurements (e.g. pm2.5 measurements in some deserted Alaskan counties), on the other hand zero can also be a value meaning there is no data available (some measurements in several counties have meaningful values until a certain year, then "no measurement" is represented with zero).