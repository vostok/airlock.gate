# Airlock Gate
Service for sending application events.
Three main event types:

 - Log events
 - Metric events
 - Tracing events

## Installation

1. Add settings files to /etc/kontur/airlock-gate (linux) or c:\ProgramData\kontur\airlock-gate (windows), see samples at ./airlockgate/src/main/resources. If settings file is not found then it will be loaded from resources.    
2. run command: `make run-java-gate`

Settings file | description
--------| -----
apikeys.properties | Key-value list. Key is api key. Value is project name (used for topic name)
app.properties | application settings
log4j.properties | logging settings
producer.properties | kafka settings, see https://github.com/edenhill/librdkafka/blob/master/CONFIGURATION.md

## Api methods

### Ping
**Url**: /ping

**Description**: Return 200 http code and that's all

### Send

**Url**: /send

**Description**: Send data to message broker

**Method**: POST

**Body**: Serialized [Airlock Message](#airlockmessage)

**Headers**

Name        | Type  |
------------|-------|
apikey      | string|


## Message structure

Assume little endian for primitive types.

### AirlockMessage
Description        | Type  | Size (bytes)
-------------------|-------|------
Version            | short | 2
List of EventGroup | list  | *

### List of objects
Description        | Type  | Size (bytes)
-------------------|-------|------
Size of list       | int   | 4
Object 1           |       | *
...                |       | *
Object N           |       | *

### EventGroup
Description          | Type   | Size (bytes)
---------------------|--------|------
Event Type           | string | *
List of EventRecords | list   | *

### EventRecord
Description              | Type  | Size (bytes)
-------------------------|-------|------
Timestamp (milliseconds) | long  | 8
Data                     | byte array| *

### Byte array
Description        | Type  | Size (bytes)
-------------------|-------|------
Size of array      | int   | 4
Bytes              | byte[]| n (size of array)


