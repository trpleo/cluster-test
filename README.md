1. cd <project root>
2. sbt compile
3. docker pull rabbitmq:latest
4. docker run -d --hostname my-rabbit --name some-rabbit -p 5672:5672 -p 15672:15672 -p 8080:15672 rabbitmq:3-management
5. http://localhost:15672
6. Start nodes:
    * ./scripts/node-1.sh
    * ./scripts/node-2.sh
    * ./scripts/node-3.sh

To get info about the cluster:
curl -X GET localhost:8500/cluster/members/

For further info, see:
https://developer.lightbend.com/docs/akka-management/current/cluster-http-management.html
