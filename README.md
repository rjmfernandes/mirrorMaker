# MirrorMaker Local Setup

Run:

```bash
docker compose up -d
```

It will run two separate deployments of Kafka on same docker network. Including:

- zookeeper1
- broker1
- schema-registry1
- connect1
- control-center1
- zookeeper2
- broker2
- schema-registry2
- connect2
- control-center2

You can access each control-center under:
- (east) http://localhost:9021/clusters
- (west) http://localhost:9022/clusters

Also guarantee that on your /etc/hosts file you have the mappings:

```
127.0.0.1 broker1
127.0.0.1 broker2
```

Wait for both clusters to show up as healthy on corresponding control-center instances.

For creating the MirrorMaker connector from east to west we use the file east-to-west-source.json (on west target connect2 instance):

```bash
curl -X PUT -H "Content-Type: application/json" \
  -d @./east-to-west-source.json \
  localhost:8084/connectors/east-to-west-source/config
```

Check connector configured on the control-center2 (west): 

```bash
curl localhost:8084/connectors
```

You can also check the status of the connector with:

```bash
curl localhost:8084/connectors/east-to-west-source/status
```

Once status is available it will also be reflected on corresponding control-center2:
http://localhost:9022/clusters 

Check also the topics listed on west deployment and see no custom topics created yet on west (broker2) deployment:

```bash
kafka-topics --bootstrap-server localhost:9093 \
  --list
```

Create a topic on east deployment (broker1):

```bash
kafka-topics --bootstrap-server localhost:9092 \
   --create --topic inventory \
   --partitions 5 --replication-factor 1
```

Then list topics on west (broker2) and see topic already mirrored:

```bash
kafka-topics --bootstrap-server localhost:9093 \
  --list
```

Start two shells next to each other and in first one start a producer on east kafka (broker1):

```bash
kafka-console-producer --bootstrap-server localhost:9092 \
  --topic inventory
```

And on the other parallel shell start the consumer on broker2 (west):

```bash
kafka-console-consumer --bootstrap-server localhost:9093 \
  --topic inventory \
  --from-beginning
```

Check how the messages you create on east kafka get replicated to west kafka.

## MirrorCheckpointConnector - consumer applications

We create a new connector:

```bash
curl -X PUT -H "Content-Type: application/json" \
  -d @east-to-west-checkpoint.json \
  localhost:8083/connectors/east-to-west-checkpoint/config
```

We can again double check the status of connector:

```bash
curl localhost:8084/connectors
curl localhost:8084/connectors/east-to-west-checkpoint/status
```

We can now create a consumer group in the east (broker1):

```bash
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic inventory --from-beginning \
  --group my-group
```

If we list topics we can see the connector has created a topic east-kafka.checkpoints.internal with a single partition in the west/broker2 deployment:

```bash
kafka-topics --bootstrap-server localhost:9093 \
  --list
```

If we run again against the east/broker1 in one shell:

```bash
kafka-console-producer --bootstrap-server localhost:9092 \
  --topic inventory
```

And against the other also for the east/broker1:

```bash
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic inventory --from-beginning \
  --group my-group
```

We should see the new created messages show up  on the consumer.

If we stop the last consumer and run the same now against the west/broker2:

```bash
kafka-console-consumer --bootstrap-server localhost:9093 \
  --topic inventory --from-beginning \
  --group my-group
```

For new created messages we should see it pop up on the consumer for the west/broker2 which picks up from the 
last offset of eat/broker1.

If you check offsets on each deployment they should match:

```bash
kafka-consumer-groups --bootstrap-server localhost:9092 \
--describe --group my-group
kafka-consumer-groups --bootstrap-server localhost:9093 \
--describe --group my-group
```