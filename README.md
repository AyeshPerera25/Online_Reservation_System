# Online_Reservation_System

1. Introduction.

   The Online Reservation System is a highly available and scalable distributed system implemented to provide a digital platform for fulfilling the seller’s and customer’s business requirements. Each two party have provided their portals to log and attend to their task.
   
   a. Sellers Portal.
   
      Sellers Portal is the client software which implemented for the sellers to log in with the Reservation Server and interact with their business requirements. Throughout the seller’s portal, they can
   
         • View their listed items and reservations.
         • Add a new item to the system.
         • Update an added item.
         • Remove item.
   
      First seller needs to select the server to which the software needs to connect from the suggested servers list which are available at the moment. According to this design seller can switch the servers and connect to the highly available server without entering the connecting details. Then he enters the ID and username to log into the system to identify his action throughout the system and access the services provided by the software.

     ![image](https://github.com/AyeshPerera25/Online_Reservation_System/blob/development/Images/img.png)


   b. Customer Portal.

     Customer Portal is the implemented software for the customer to access the system and fulfil their requirement from the system. The client portal provides services like,
   
       •	View all the listed items on the system.
       •	Make a reservation on the selected Item.
   
     Similar to the Sellers Portal, the customer first needs to select the preferred server from the available which suggested by the system. After that, he needs to enter the logging details to identify the customer and his request throughout the system.
   
     ![image](https://github.com/AyeshPerera25/Online_Reservation_System/blob/development/Images/img_1.png)

2. Architecture.
   
   2.1. Design Architecture.
   
   ![image](https://github.com/AyeshPerera25/Online_Reservation_System/blob/development/Images/img_2.png?raw=true)

  2.2. Components.
  
  Implemented system designed with the following main components and JAVA JDK version 11 has been used for developing the system.
  
  a. Reservation Server.
  
  This component is the backbone of the system, and it mainly handles all the service requests provided by the system while managing the database. Each server contains an in-memory Hash Map as a database and it’s always in sync with all the Hash Map in every other server running on the cluster. Each reservation sever has responsible for ensuring system consistency, fault tolerance and reliability therefore all work as Master and Slave hierarchy in the cluster with the help of Distributed Master Lock, Transaction Participant/Coordinator and Naming Server Client.
  
  With the help of Distributed Master Lock, one server gets elected as a Master and it achieves the Master Lock and becomes a Transaction Coordinator. The rest of the servers in the cluster become Slave servers and become Transaction Participants. Therefore, all the Slave servers work on the cluster under the Master server instruction because of that system has able to keep its data consistency and integrity.With the help of the Naming Server Client, all the Reservation Servers and their connection data are used to register in separate servers therefore in case of a failure on one server in the cluster can be replaced by the next upcoming server and continue the failed server work. That ensures the reliability of the system.
  
  All the client-server and between-server communications are done using the gRPC protocol.
  
  b. Distributed Master Lock.
  
  This component’s main goal is to keep contact with the Apache Zookeeper and help the Reservation server to become the Master or Slave server. With the help of Distributed Master Lock, each server is able to coordinate their receiving request among the Master server and Slave servers and that ensures the reliability of data synchronization among the servers. This Master Lock is implemented to acquire by selecting the shortest ZNode holder registered in the Zookeeper server. Therefore, only one Master gets selected at a time and in the case of the Master server frailer next shortest ZNode holder becomes the next Master. Therefore, system consistency and reliability are ensured when receiving large amounts of concurrent requests because every request is managed under the supervision of the Master server.
  
  c. Transaction Participant/Coordinator.

  The main goal of implementing this component is to gain the distributed transaction ability to the system. With the help of Apache Zookeeper, this component is able to function the Two-Phase commit within the transaction. The Master server becomes the Transaction Coordinator and others become the Transaction Participant. The coordinator starts the transaction before sending the request to others and checks the vote for commit which receivers submit to the Zookeeper under child-node created under the transaction Znode. When the participant receives the request sent from the coordinator it places a vote commit otherwise place a vote abort. After checking all the votes Coordinator places a Global commit to inform receivers to process the received request and update the local DB in case all votes as commit, if there is a single vote abort then places Global Abort to discard the request. This technique reduces the possibility to message losses while communicating critical requests with each other. Therefore overall, this increases the system data consistency and communication reliability within the servers.
  
  d. Name Service Client.
  
  Name Service Client helps to each Reservation server to get registered on the Etcd server and keep their connecting details like IP and Port data in a centralized system. Therefore, any server or client can be able to get the other server connecting details and initiate the connection with the required server. The best part is with the help of Zookeeper and the Etcd server, in case when one server gets crashed or is dropped from the cluster next upcoming server can get its place by getting failed server connection details. This helps to increase the system’s reliability by a considerable amount.
  
  e. Seller and Customer clients.
  
  The main goal of these components is to provide accessibility to the services provided by the reservation system for each party. Its gets server connection details from the Etcd server and do the communication with the main system by using gRPC communication protocol.

  2.3. Distributed System.
  
  A distributed system architecture is a design technique where various components of a system are used to get spread across many interconnected nodes to achieve a single goal by working as one. Other than handling the workload on a single centralized server, a distributed system is used to coordinate its workload across its spread nodes by communicating with each other. When a single node fails another node can get its place and continue its functionality without giving any downtime. Due to that following advantages like scalability, fault tolerance, workload coordination, consistency, and reliability can be achieved on the distributed system architecture. When implementing a system according to the distributed system architecture following challenges like,
  
    • Communication and coordination across the nodes. • Data synchronization among the nodes.
    • Consistency of the data.
    • Decrees the fault tolerance.
    
  2.4. Communication.
  
  Communication between the nodes and clients is one main factor to focus on when designing a distributed system. System workload distribution mainly depends on the communication techniques used in the system and the reliability and consistency of the communication method directly impact the overall system reliability and consistency. Like single node systems, intercommunication cannot be performed using the node shared memory location references, distributed systems require communication between each node over the network. For that, there are many techniques like Rest or WebSocket connections, messaging queues, Remote method invocation etc.
  
  This Reservation Server project implemented to used gRPC communication framework to establish inter-node communication. gRPC is a remote procedure call (RPC) communication framework developed by Google to ensure efficient and reliable communication methods for distributed systems. It uses HTTP/2 as the base protocol for communication therefore it gained abilities like bidirectional streaming, flow control, and header compression. To make it easier to implement the messages serialization and deserialization according to support the gRPC framework, Protobuf is used to generate required code implementations during the project.
  
  In the implemented system, stubs are implemented on each node and client according to the service required to invoke. When sending the request to invoke the service, the first sender parses the request to the local stub created relevant to the service. The stub builds and serialize the request and then passes it to the local operating system. The local operating system sends the serialized request to the remote machine over the network and when it is received to the remote machine, its operating system passes the request to the relevant service stub implemented on the target client or node. The remote stub then deserialize the request and parses it to the service required to invoke and the result gets sent as a response in the same way.

  ![image](https://github.com/AyeshPerera25/Online_Reservation_System/blob/development/Images/img_3.png)

  2.5. Coordination.
  
   Node coordination is another main part of the distributed system. This feature needs to be improved and structured when implementing features like node synchronization and minimizing fault tolerance. In this part describe the node coordination on scaling and node failing.
   
   In a distributed system, its node needs to scale horizontally according to the workload and demand. Each node has a unique network address to call and those need to be shared nodes to work as a cluster. It’s very inefficient and burden to maintain the all network connection details of every node within each node because every time a new node is added or removed from the system this network connection data set needs to be updated. Therefore, centralized network connection data registry needs to be maintained which contains the connection data of every node in the cluster. Therefore, any new node can update its connection data to the registry and any node which needs to connect to another node can easily get the relevant connection data it.
   
   In this project Etcd server has been used as a naming registry service to hold each server connection data. Therefore, all the server and clients can connect their required servers by getting the connection details from the Etcd naming registry server.
   
   With the help of Zookeeper and the Etcd server, the implemented system can be able to identify its position on the server cluster when adding a new server node to the cluster. When starting the system, the initial server node takes the 11436 port under the ‘server1’ name and registers to the Etcd server and also get connected to the Zookeeper to competing the Master lock. Then the second server node gets started, its checks the registered ports on the connection detail at Etcd server and the availability of those ports with the Zookeeper child nodes data which is related to the Master and Slave servers. Then the second server node takes the next available port and gets registered as the first server.

   ![image](https://github.com/AyeshPerera25/Online_Reservation_System/blob/development/Images/img_4.png?raw=true)

   When a server node get failed next starting server node takes its place by taking the failed node name and port. Therefore, it ensures server availability on the system.

   ![image](https://github.com/AyeshPerera25/Online_Reservation_System/blob/development/Images/img_5.png?raw=true)

   2.6. Synchronization.
   
   The biggest challenge which distributed systems need to overcome is synchronization. When every node receives service requests concurrently it can result conflicts in the process due to many requests trying to access the same record and update it. To overcome this, the system needs to allow requests to get processed according to the first come first served basis and for that, every node needs to get in sync with each other and their work with relative to the time. But considering the local time on each node could not be exactly equal because those nodes could be run on different servers. Therefore, the time on selected one node in the cluster is considered as a global clock and handles the request under the supervision of the selected node. Therefore implemented system uses a Master/Slave configuration with the help of Apache Zookeeper to coordinate the service request handling among the nodes.
   
   Every node contains a Distributed Master Lock instance, and their main goal is to keep in connect with Zookeeper and registered as a child node. Zookeeper works as a coordination platform in the Distributed system, and it is a reliable and efficient open- source program designed by Apache. Zookeeper maintains the child node as a path in its hierarchical namespace under the Root Zoned. When a new child node gets added to the system Zookeeper register its path under Znode with sequentially named order.

   ![image](https://github.com/AyeshPerera25/Online_Reservation_System/blob/development/Images/img_6.png?raw=true)

   Users can add Watchers to Zookeeper to keep in track with the changes in this Znode and its child node. In any change occurs to the targeted node, the watcher can implement to invoke any process to perform.

   ![image](https://github.com/AyeshPerera25/Online_Reservation_System/blob/development/Images/img_7.png?raw=true)
   
   Zookeeper keeps in track with the availability of each component registered as a child node and If any component get exits from the system or get failed, Zookeeper remove the relevant child node related to the component with after the given time out. Therefore, Zookeeper can use to track the availability of the system nodes also.
   
   By using these techniques on Zookeeper, the Distributed Master Lock instance of each server node sorts out the Master Lock instance which has registered in the shortest child node path. Then the shortest child node path Master Lock instance elects its server node as a Master and Others become Slave. If the elected Master server fails then the next shortest child node path registered Master Lock instance makes its server node instance as the new Master.
   
   When a new slave server node is added to the system, its first job is to get connected with the available Master server and sync its in-memory database with the Master’s local database. Then the new Slave server is ready to accept service requests from the others.
  
   When the Slave server gets a service request it re-sends to the Master server. The Master server picks the request sent from the Slave server and distributes it to all the available Slave servers. Slave servers then update their local DB with the request sent from the Master server. During this process, all the requests received to the system coordinate by from the Master server therefore the system works synchronized with relative to one global clock.

   2.7. Consistency.
   
   Proper distributed system needs to ensure the high availability of the data and its consistency. For that system needs to maintain replicated data sources as a backup in case of any failure. Therefore, when one node get failed the next new node which takes its place can recover the failed node data and continue its work. In the implemented system used the primary based sequential consistency model to coordinate nodes in-memory databases from the process described in the Synchronization part. Therefore, the Master node’s role is to coordinate the data insert to each node in-memory DB and the Slave server only can perform the data read service requests on its own.
   
   As described in the Synchronization part, Master/Slave coordination helps to get synced with other in-memory databases therefore, every node has its replicated database on each other. As a result of that implemented system has much higher data availability with higher consistency.

   ![image](https://github.com/AyeshPerera25/Online_Reservation_System/blob/development/Images/img_8.png?raw=true)

   2.8. Fault tolerance.
   
   Fault tolerance in the distributed system indicates the ability of the system to provide its services at the failure of the system component. These failures can occur due to corrupted data, implementation bugs or hardware failure. Redundancy and replication are the main fault tolerance risk mitigation strategies used to integrate into a distributed system. Redundancy is the process of replicating system components across numerous nodes to ensure that they continue to function even if one of them fails. Replication keeps several copies of data or services across nodes, which improves data consistency and availability.
 
   To ensure data consistency and fault tolerance, the implemented reservation system used distributed transaction commits. This approach ensures atomicity, meaning a commit update either happens successfully on all participant servers or fails entirely. The two- phase commit protocol is used to implement the distributed transaction logic and it ensures that all nodes either commit the update or roll back any changes upon the vote on each participant server.
   
   In the implemented system Master Server acts as a Transaction coordinator and Slave servers act as Transaction participants. When the slave server received a new service request it re directed to the Master server. After receiving the request sent from Slave servers, the Master server starts a transaction by creating a Zoned on the Zookeeper under the transaction ID. Then it sends to the request back to Slave servers with the transaction ID and waits to get the vote from the Slave servers. After receiving the request from the Master, Slave servers are used to create child nodes under the Transaction ID named Znode in Zookeeper and add ‘vote commit’ on their representing child node data if the slave server manages to handle the request without any issue otherwise place a ‘vote abort.’ Then wait by watching Znode data get updated to ‘Global Commit’ or ‘Global Abort’ by from Master transaction coordinator. After receiving all the votes from every slave node, the Master branch updates the Znode data to ‘Global Commit’ when it has not received any ‘vote abort’ from slave nodes and tells slave servers to process the service request and update the local DB. Otherwise, it places ‘Global Abort’ and tells the Slave servers to discard the service request and initiate rollback.


4. System Behavior.
   
   4.1. When an item is Add/Update/Remove to the system.

      ![image](https://github.com/AyeshPerera25/Online_Reservation_System/blob/development/Images/img_27.png?raw=true)

      • This sequential diagram demonstrates the process of the system when services like add item, update item, and remove item.

      • First Seller send a service request to the available server node.

      • Then the slave server node redirects the request to the Master server node.

      • The master node validate the request and creates a root Znode in the Zookeeper under
the transaction ID.

      • Then Master redistributes the service request to Slave nodes with Transaction ID.

      • The slave node creates child nodes under the Root Znode using the transaction ID after receiving the request from the Master and updating their child nodes data with ‘VOTE COMMIT’ if the request is received successfully otherwise update ‘VOTE ABORT’.

      • Master collects the voting data from the child node under the Transaction Znode and checks any ‘VOTE ABORT’ there. If it is available update the Root Znode data as ‘GLOBAL ABORT’ otherwise update as ‘GLOBAL COMMIT’.

      • Every node gets an update from the Root Znode data change using Watchers and if the update is ‘GLOBAL COMMIT’, then process the request and update the local DB otherwise discard the request.

      • Finally, a response was sent from the Master to Slave then Seller.

   4.2. A reservation is confirmed for a given item.

      ![image](https://github.com/AyeshPerera25/Online_Reservation_System/blob/development/Images/img_28.png?raw=true)

      • This sequential diagram demonstrates the process of the system when the customer places a reservation on a selected item.

      • First customer loads the listed item data from the connected server node and places a reservation request on the selected item.

      • The connected server received the reservation request and redirected it to the Master node.

      • The master node validates the request and creates a root Znode in the Zookeeper under the transaction ID.

      • Then Master redistributes the reservation request to Slave nodes with Transaction ID.

      • The slave node creates child nodes under the Root Znode using the transaction ID after receiving the request from the Master and updating their child nodes data with ‘VOTE COMMIT’ if the request is received successfully otherwise update ‘VOTE ABORT’.

      • Master collects the voting data from the child node under the Transaction Znode and checks any ‘VOTE ABORT’ there. If it is available update the Root Znode data as ‘GLOBAL ABORT’ otherwise update as ‘GLOBAL COMMIT’.

      • Every node gets an update from the Root Znode data change using Watchers and if the update is ‘GLOBAL COMMIT’, then process the request and update the reservation to the item and insert to the local DB otherwise discard the request.

      • Finally, a response was sent from the Master to Slave then Seller.

   4.3. When two customers place a reservation for the same item on the same dates at the same time.

      ![image](https://github.com/AyeshPerera25/Online_Reservation_System/blob/development/Images/img_29.png?raw=true)

      • This sequential diagram demonstrates the process of the system when two customers place a reservation on the same selected item in the same date.

      • As demonstrated in the previous both requests get directed to the Master server by from the Slave nodes.

      • Due to the synchronization on the server in service handler implementation one of the two request processing thread able to acquire the thread lock and other thread goes to the Block state. Assume request 1 processing thread gets the thread lock and continues the process.

      • Request 1 gets validate and continuous the process normally as mentioned on previously according to the two-phase commit protocol.

      • After the master sends the response related to request 1, the Request 2 processing thread comes to the running state and starts processing the request.

      • But in the validation, it gets rejected because the system already has a reservation on the selected item on the same date.

      • The Master sends the failed response related to request 2 and its passes to the Slave server to then the customer.

   4.4. One node exits the system.

      • When the Master node exit the system every other Slave nodes try to aqure the master lock.

      • Therefore each Distributed Master Lock instance on each Slave node instances checks their child node path registered under the root Znode in the Zookeeper and sorts out the shortest child node path.

      • The Distributed Master Lock instance which has the shortest child node path used to select its server instance as a Master and others become Slave servers again.

      • Other Distributed Lock instances are used to save the selected Master server data to use in service request coordination.

      • If the Slave node exit from the system next upcoming server is used to replace its position with the help of Etcd and Zookeeper servers. (As mentioned in section 2.5)
   
   4.5. One node joins the system or restarts.

      • When a node joins the system it’s looking to get the next available port to join the server cluster.

      • For that, its first gets the server register details from the Etcd server and check the availability on each registered port with Zookeeper Distributed Master Lock child nodes data. If it found a port which un available on Zookeeper, new server used to get start using the selected port.

      • If all the port on the Etcd register is currently available according to the zookeeper, a new server is used to take the next available new port and register under the new server’s name in the Etcd server.

      • Then it used to connect with the available master node in the system and sync its in- memory database with the master node.

      • After getting synced with the master, the new server get available to accept the request on the server cluster.
   
6. Discussion.
   
   The online Reservation System demonstrated in this report is implemented with distributed architecture to achieve the qualities like high availability, high scalability, consistency, and reliability. With interconnected multiple server nodes at its core, the application efficiently handles client requests concurrently, avoiding the potential for any single point of failure. Throughout the system's development, we are focused to integrating the key features of the distributed system which are synchronization, consistency, communication, naming and fault tolerance.
   
   When establishing those key features on the system, We have used frameworks and components like gRPC, Apache Zookeeper, Etcd server, Protobuf and many other libraries.
   
   Also designed system get integrated with modern design techniques like Master/Slave coordination, Distributed Master Locks, and Distributed Transactions with two-phase commit.
   
6. Improvements.
  
   For further improvement of this system, we can consider the following,
      • Design a user interface to the Seller and Customer clients.
      • Use the proper database for the system.
      • Introduce the microservice architecture to the system.
      • Integrated to the Docker to simplify the system deployment and horizontal scaling.
      • Integrate a proper login to the customer and seller
