# Airlock Gate
Service for sending application events.
Three main event types:

 - Log events
 - Metric events
 - Tracing events

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

Assume big endian for primitive types.

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
Description          | Type  | Size (bytes)
---------------------|-------|------
Event Type           | short | 4
List of EventRecords | list  | *

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


