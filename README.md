# Airlock Gate [![Build Status](https://travis-ci.org/vostok/airlock.gate.svg?branch=master)](https://travis-ci.org/vostok/airlock.gate)

A proxy between your Vostok-instrumented applications and Kafka.

## How to run

Just `make`, if you are on a normal OS. Otherwise, look inside the `Makefile` for correct commands.

## API methods

### Ping

**Url**: `/ping`

**Method**: `GET`

**Description**: Does nothing. Returns `200 OK`.

### Send

**Url**: `/send`

**Method**: `POST`

**Description**: Relays messages to Kafka.

#### Request headers

Name   | Type
-------|-------
apikey | string

#### Response codes

Code | Meaning
-----|--------
200  | Request body format is valid. API key is valid for all provided routing keys. Messages had been put into an internal buffer, but not necessarily into Kafka yet.
203  | Request is valid, but some event groups have routing keys that are either forbidden for this apikey or contain characters other than [A-Za-z0-9.-]. Messages with disallowed routing keys had been dropped.
400  | Request body is empty, or request body format is invalid, or request is valid, but all event groups have routing keys that are either forbidden for this apikey or contain characters other than [A-Za-z0-9.-].

#### Body

Binary-serialized message. Assume little endian for primitive types.

##### List *(datatype)*

Description  | Type  | Size (bytes)
-------------|-------|-------------
Size of list | int   | 4
Object 1     |       | *
...          |       | *
Object N     |       | *

##### Byte array *(datatype)*

Description        | Type   | Size (bytes)
-------------------|--------|-------------
Size of array      | int    | 4
Bytes              | byte[] | n (size of array)

##### String *(datatype)*

Description        | Type   | Size (bytes)
-------------------|--------|-------------
Size of string     | int    | 4
Bytes (UTF-8)      | byte[] | n (size of string)

##### AirlockMessage *(root object)*

Description         | Type  | Size (bytes)
--------------------|-------|-------------
Version             | short | 2
List of EventGroups | list  | *

##### EventGroup

Description          | Type   | Size (bytes)
---------------------|--------|-------------
Routing key          | string | *
List of EventRecords | list   | *

##### EventRecord

Description                   | Type       | Size (bytes)
------------------------------|------------|-------------
Unix Timestamp (milliseconds) | long       | 8
Data                          | byte array | *

