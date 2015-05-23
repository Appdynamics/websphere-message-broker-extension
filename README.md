ibm-websphere-msg-broker-monitor
================================
The IBM Integration Bus, formerly known as the IBM WebSphere Message Broker Family, provides a variety of options for implementing a 
universal integration foundation based on an enterprise service bus (ESB). Implementations help to enable connectivity and transformation 
in heterogeneous IT environments for businesses of any size, in any industry and covering a range of platforms including cloud and z/OS.


## Metrics Provided ##

This extension extracts only resource level statistics. The list of metrics that it extracts can be found here

http://www-01.ibm.com/support/knowledgecenter/SSMKHH_9.0.0/com.ibm.etools.mft.doc/bn43250_.htm?lang=en

## Dependencies ##

- IBM WebSphere Message Broker.
- AppDynamics Machine Agent in HttpListener mode.  


## Installation ##

   ### To compile this project successfully, please copy the jar files from the <IBM_INSTALL_DIR>\WebSphere MQ\java\lib directory in the lib dir. On Windows, by default this is under ﻿C:\Program Files (x86)\IBM\WebSphere MQ\java\lib ###

1. Run "mvn clean install" and find the IbmWebSphereMsgBrokerMonitor.zip file in the "target" folder. You can also download the IbmWebSphereMsgBrokerMonitor.zip from [AppDynamics Exchange][].
2. Unzip IbmWebSphereMsgBrokerMonitor.zip as IbmWebSphereMsgBrokerMonitor.

## Configuration ##

There are two configurations needed 

 1. On the WebSphere Message Broker
     
    To get resource statistics from the broker first you will have to enable the resource statistics on WMB (WebSphere Message Broker). There are two ways that you can enable statistics . 

        a. You can run "mqsichangeresourcestats" by running it in IBM Integration Console (in WMB 9.0) or  IBM WMB Command Console (in previous versions). 
        A sample command can be 
        
        ```      
            mqsichangeresourcestats BrokerA -c active -e default 
                
            where, BrokerA -> The broker name.
                   default -> The execution group.
        ```
                       
           Please follow the below documentation to get more familiar with the mqsichangeresourcestats command. 
            http://www-01.ibm.com/support/knowledgecenter/SSMKHH_9.0.0/com.ibm.etools.mft.doc/bj43320_.htm?lang=en
    
        b. You can also enable resource statistics from IBM WebSphere MQ Explorer. 
            - Open IBM WebSphere MQ Explorer
            - Click on Integration Node i.e. Broker Name.
            - Right click on the execution group which you want statistics for and start resource statistics. 
      
    Once you have started the resource statistics by any of the above two approaches, you can confirm it by viewing the statistics as follows 
     
        - Open IBM WebSphere MQ Explorer
        - Click on Integration Node i.e. Broker Name.
        - Right click on the execution group which you want statistics for and view resource statistics.   
      
    The resource metrics get published every 20 seconds to a topic. For eg. for the above command, the statistics will get published on this topic $SYS/Broker/BrokerA/ResourceStatistics/default
          
    For more details, please follow the IBM documentation mentioned here  http://www-01.ibm.com/support/knowledgecenter/SSMKHH_9.0.0/com.ibm.etools.mft.doc/aq20080_.htm?lang=en
    
    The extension subscribes to a particular topic in a queue manager's queue. Please confirm that you have all of the following in the IBM WebSphere MQ explorer
        
        a. A Queue Manager. 
        b. A TCP Listener on some port controlled/managed by Queue Manager. This port will be the port which we will use to subscribe to the statistics from the extension.
    
    If the queue manager and/or tcp listener is not present, please create them.
      
 2. On the appdynamics extension
 
    Configure the config.yml file in IbmWebSphereMsgBrokerMonitor directory
 
    ```
        ﻿host: "localhost"
        port: 2414
        
        clientId: "wmb_appd_ext"

        userID: ""
        password: ""

        queueManager: ""
        channelName: ""
        
        # Topic for all or a particular execution group belonging to a broker.
        resourceStatTopics:
            - name: "$SYS/Broker/+/ResourceStatistics/#"
              subscriberName: "allResources"
        
        # sleep time in seconds
        sleepTime: 20
        
        #Machine agent url
        machineAgentUrl: "http://localhost:8293/machineagent/metrics?"
        
        #Number of threads to make parallel http calls
        numberThreads : 10
        
        # Thread time out in seconds
        threadTimeout : 3
        
        #prefix used to show up metrics in AppDynamics
        metricPrefix:  "Custom Metrics|WMB|"
        
    ```
    
    In the above config, port # is the port on which the TCP listener was started in the IBM WebSphere MQ Explorer. 
    By default, the extension will get statistics for all the execution groups on a particular broker. The "#" in the resourceStatTopics.name
    signifies that. If you want to view statistics for selected execution groups, you can configure as below
     
    ```
        # Topic for all or a particular execution group belonging to a broker.
        resourceStatTopics:
            - name: "$SYS/Broker/BrokerA/ResourceStatistics/execGroupA"
              subscriberName: "execGroupA"
            - name: "$SYS/Broker/BrokerA/ResourceStatistics/execGroupB"
              subscriberName: "execGroupB"
    ```
       
###Note
Please make sure to not use tab (\t) while editing yaml files. You may want to validate the yaml file using a yaml validator http://yamllint.com/

For older version of IBM WMB,if there are compatibility issues, try replacing the jar files in the lib folder with the old ones. The jar files can be found in the MQSI directory
\IBM\WebSphere MQ\java\lib directory.

## Execution ##

For this extension, the appdynamics machine agent needs to be started in http listener mode. After making the necessary configurations in the machine 
agent's controller-info.xml, please use the below command to start the machine agent
    
    ```
        java -Dmetric.http.listener=true -Dmetric.http.listener.port=<port_number> -jar machineagent.jar
        
        If you do not specify the optional metric.http.listener.port, it defaults to 8293.
    ```

The extension runs as a stand alone JVM and it subscribes to the configured topics on the broker's queue on the configured port. 
After making the necessary changes in the config.yaml file, to start the extension 
    
    ```
        a. cd into the unzipped IbmWebSphereMsgBrokerMonitor directory. 
        b. Run the following command 
           On Windows : 
                
                java -Dlog4j.configuration=file:.\conf\log4j.xml -cp "ibm-websphere-msg-broker-extension.jar;<IBM_INSTALL_DIR>\WebSphere MQ\java\lib\*" com.appdynamics.extensions.wmb.WmbMonitor
                
                By default the <IBM_INSTALL_DIR> is under ﻿C:\Program Files (x86)\IBM\WebSphere MQ\java\lib
    
           On Unix or Linux : 
           
                java -Dlog4j.configuration=file:./conf/log4j.xml -cp "ibm-websphere-msg-broker-extension.jar:<IBM_INSTALL_DIR>\WebSphere MQ\java\lib/*" com.appdynamics.extensions.wmb.WmbMonitor
    
    ```
 

## Custom Dashboard ##
![](https://raw.githubusercontent.com/Appdynamics/ibm-websphere-msg-broker-monitor/master/ibm-wmb.png)

## Contributing ##

Always feel free to fork and contribute any changes directly via [GitHub][].

## Community ##

Find out more in the [AppDynamics Exchange][].

## Support ##

For any questions or feature request, please contact [AppDynamics Center of Excellence][].

**Version:** 1.0.0
**Controller Compatibility:** 3.7+
**IBM WebSphere Message Broker Version Tested On:** 9.0.0.2


[Github]: https://github.com/Appdynamics/ibm-websphere-msg-broker-monitor
[AppDynamics Exchange]: http://community.appdynamics.com/t5/AppDynamics-eXchange/idb-p/extensions
[AppDynamics Center of Excellence]: mailto:ace-request@appdynamics.com
