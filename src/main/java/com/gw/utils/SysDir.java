package com.gw.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 *Class SysDir.java
 *@author ziheng
 */

@Configuration
@ConfigurationProperties(prefix = "geoweaver")
public class SysDir {
	
	int worknumber = 1;
	
	String instantiationservletaddress0 = null; //WorkflowCore instantiation servlet 0 - bind LogicProcess to MessageType
	
	String instantiationservletaddress = null; //WorkflowCore instantiation servlet 1 - Transform LogicProcess and MessageType to BPEL Workflow
	
	String executionservletaddress = null;
	
	String registrationaddress = null;
	
	String NOTIFICATION_EMAIL  = null;
    
	String NOTIFICATION_EMAIL_SERVICE_URL = null;
    
	String CACHE_OPERATION = null;
    
	String CACHE_SERVICE_URL = null;
    
	String CACHE_DATA_URLPREFIX = null;
    
	String CSISS_CSW_URL = null;
    
	String prefixurl = null;
    
	String ncWMSURL = null; 
    
	String ncUsername = null; 
    
	String ncPassword = null;
    
	String covali_file_path = null;
	
	String geoweaver_file_path = null;
    
	String upload_file_path = null;
	
	String allowed_ssh_hosts = null;
	
	String allowed_ssh_clients = null;
	
	String secret_properties_path = null;
    
	String temp_file_path = null;

	String thredds_harvester_path = null;

	String database_driver = null;

	String database_url = null;
	
	String database_docker_url = null;

	String database_user = null;

	String database_password = null;
	
	String workspace = null;

//	static Properties readProperties(String path) {
//		Properties p = new Properties();
//
//		try {
//			FileInputStream fileIn = new FileInputStream(path);
//
//			p.load(fileIn);
//			fileIn.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//			System.exit(1);
//		}
//
//		return p;
//	}
	
	

//	static{
//		
//		//initialize from config file
////		try {
////			
////			BaseTool t = new BaseTool();
////			String configFile = t.getClassPath()+File.separator+"config.properties";
////			
////			Properties p = readProperties(configFile);
////
////			String secretConfigFile = p.getProperty("secret_properties_path");
////
////			Properties secrets = null;
////			
////			if(new File(secretConfigFile).exists())
////			
////				secrets = readProperties(secretConfigFile);
////			
////			else
////				
////				secrets = readProperties(t.getClassPath()+File.separator+secretConfigFile);
////			
////			String number = p.getProperty("workernumber");
////			
////			SysDir.worknumber = Integer.parseInt(number);
////			
////			instantiationservletaddress0 = p.getProperty("instantiationservletaddress0");
////			
////			instantiationservletaddress = p.getProperty("instantiationservletaddress");
////			
////			executionservletaddress = p.getProperty("executionservletaddress");
////			
////			registrationaddress = p.getProperty("registrationaddress");
////			
////			NOTIFICATION_EMAIL = p.getProperty("notify");
////			
////			NOTIFICATION_EMAIL_SERVICE_URL = p.getProperty("notificationserviceaddress");
////			
////			CACHE_OPERATION = p.getProperty("datacacheoperation");
////			
////			CACHE_SERVICE_URL = p.getProperty("datacacheserviceaddress");
////			
////			CSISS_CSW_URL = p.getProperty("csisscswurl");
////			
////			CACHE_DATA_URLPREFIX = p.getProperty("datacacheprefix");
////			
////			PREFIXURL = p.getProperty("prefixurl");
////			
////			ncWMSURL = p.getProperty("ncwmsurl");
////			
////			covali_file_path = p.getProperty("covali_file_path");
////			
//////			geoweaver_file_path = p.getProperty("geoweaver_file_path");
////			
////			upload_file_path = p.getProperty("upload_file_path");
////			
////			temp_file_path = p.getProperty("temp_file_path");
////
////			thredds_harvester_path = p.getProperty("thredds_harvester_path");
////
////			database_driver = p.getProperty("database_driver");
////
////			database_url = p.getProperty("database_url");
////
////			database_docker_url = p.getProperty("database_docker_url");
////			
////			workspace = normalizedPath(p.getProperty("workspace")).toString();
////			
////			// SECRET PROPERTIES
////
////			ncUsername = secrets.getProperty("ncwms_username");
////
////			ncPassword = secrets.getProperty("ncwms_password");
////
////			database_user = secrets.getProperty("database_user");
////
////			database_password = secrets.getProperty("database_password");
////			
////			//create the workspace folder if it doesn't exist
////			
////			File workspacefolder = new File(workspace);
////			
////			System.out.println("Check if the workspace folder exists. If not, create it." + workspace);
////			
//////			Files.createDirectories(workspace);
////			
////			if(!workspacefolder.exists()) {
////				
////				workspacefolder.mkdirs();
////				
////			}
////			
////		} catch (Exception e) {
////			
////			e.printStackTrace();
////			
////		}
//		
//	}

}