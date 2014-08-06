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

1. Run "mvn clean install" and find the IbmWebSphereMsgBrokerMonitor.zip file in the "target" folder. You can also download the IbmWebSphereMsgBrokerMonitor.zip from [AppDynamics Exchange][].
2. Unzip IbmWebSphereMsgBrokerMonitor.zip as IbmWebSphereMsgBrokerMonitor.

## Configuration ##

There are two types on configuration needed 

 1. On the WebSphere Message Broker
     
    To get resource statistics from the broker, enable the resource statistics on WMB (WebSphere Message Broker) first. There are two ways that you can enable statistics . 

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
    
    The extension uses subscribes to a particular topic in a queue manager's queue. Please confirm that you have all of the following in the IBM WebSphere explorer
        
        a. A Queue Manager. 
        b. A TCP Listener on some port controlled/managed by Queue Manager. This port will be the port which we will use to subscribe to the statistics from the extension.
    
    If the queue manager and/or tcp listener is not present, please create them.
      
 2. On the appdynamics extension

###Note
Please make sure to not use tab (\t) while editing yaml files. You may want to validate the yaml file using a yaml validator http://yamllint.com/

## Custom Dashboard ##
![]()

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
