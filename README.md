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
   
   2.1. Design Architecture
   
   ![image](https://github.com/AyeshPerera25/Online_Reservation_System/blob/development/Images/img_2.png?raw=true)

  2.2. Components
  
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

  

  

