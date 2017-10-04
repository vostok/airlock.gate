# Airlock Gate [![Build Status](https://travis-ci.org/vostok-project/airlock.svg?branch=master)](https://travis-ci.org/vostok-project/airlock)

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
203  | Request body format is valid. API key is valid for some, but not all, provided routing keys. Messages with disallowed routing keys had been dropped.
400  | Request body is empty, or request body format is invalid.
403  | Request body format is valid. API key is invalid for all provided routing keys. All messages had been dropped. 

#### Body

Binary-serialized message. Assume little endian for primitive types.

##### AirlockMessage

Description        | Type  | Size (bytes)
-------------------|-------|-------------
Version            | short | 2
List of EventGroup | list  | *

##### List of objects

Description  | Type  | Size (bytes)
-------------|-------|-------------
Size of list | int   | 4
Object 1     |       | *
...          |       | *
Object N     |       | *

##### EventGroup

Description          | Type   | Size (bytes)
---------------------|--------|-------------
Event Routing Key    | string | *
List of EventRecords | list   | *

##### EventRecord

Description                   | Type       | Size (bytes)
------------------------------|------------|-------------
Unix Timestamp (milliseconds) | long       | 8
Data                          | byte array | *

##### Byte array

Description        | Type  | Size (bytes)
-------------------|-------|-------------
Size of array      | int   | 4
Bytes              | byte[]| n (size of array)
