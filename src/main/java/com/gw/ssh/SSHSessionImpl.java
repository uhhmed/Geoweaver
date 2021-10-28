package com.gw.ssh;
/*

The MIT License (MIT)

Copyright (c) 2013 The Authors

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.PublicKey;
import java.text.Normalizer;
import java.util.List;

import com.gw.database.HostRepository;
import com.gw.database.ProcessRepository;
import com.gw.jpa.Environment;
import com.gw.jpa.History;
import com.gw.jpa.Host;
import com.gw.tools.EnvironmentTool;
import com.gw.tools.HistoryTool;
import com.gw.tools.ProcessTool;
import com.gw.utils.BaseTool;

import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.connection.channel.direct.Session.Shell;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;

/**
 * Geoweaver SSH session wrapper
 * @author JensenSun
 *
 */
@Service
@Scope("prototype")
public class SSHSessionImpl implements SSHSession {
	
	// @Autowired
	// HostTool ht;

    @Autowired
    HostRepository hostrepo;
	
	@Autowired
	BaseTool bt;
	
    @Autowired
    ProcessRepository processRepository;

    @Autowired
	EnvironmentTool et;
	
    protected final Logger   log = LoggerFactory.getLogger(getClass());
    
    private SSHClient        ssh; //SSHJ creates a new client
    
    private String 			 hostid;
    
    private Session          session; //SSHJ client creates SSHJ session
    
    private Shell            shell; //SSHJ session creates SSHJ shell
    
    private String           username;
    
    private String			 token; //add by Ziheng on 12 Sep 2018 - token of each execution
    
    private BufferedReader   input;
    
    private OutputStream     output;

    @Autowired
    private SSHLiveSessionOutput sessionsender;
    
    @Autowired
    private SSHCmdSessionOutput cmdsender;
    
    private Thread           thread;
    
    private String           host;
    
    private String           port;
    
    private boolean			 isTerminal;
    
    /**********************************************/
    /** section of the geoweaver history records **/
    /**********************************************/
//    private String			 history_input;
//    
//    private String			 history_output;
//    
//    private String			 history_begin_time;
//    
//    private String			 history_end_time;
//    
//    private String			 history_process;
//    
    
    private History          history;
    
    @Autowired
    private HistoryTool      history_tool;
    
    /**********************************************/
    /** end of history section **/
    /**********************************************/
    
    public SSHSessionImpl() {
    	
        // this id should be passed into this class in the initilizer
    	// this.history.setHistory_id(new RandomString(12).nextString()); //create a history id everytime the process is executed
    	
    }
    
    public String getHistory_process() {
		return history.getHistory_process();
	}

	public void setHistory_process(String history_process) {
		history.setHistory_process(history_process);
	}

	public String getHistory_id() {
		return history.getHistory_id();
	}
	
    public SSHClient getSsh() {
		return ssh;
	}
    
	public Session getSSHJSession() {
		return session;
	}
    
	public void setSSHJSession(Session session) {
		this.session = session;
	}

	public String getUsername() {
		return username;
	}

	public String getToken() {
		return token;
	}

	public String getHost() {
		return host;
	}

	public String getPort() {
		return port;
	}
	
	public boolean isTerminal() {
		
		return isTerminal;
	}
	
	public boolean login(String hostid, String password, String token, boolean isTerminal) {
		
		this.hostid = hostid;

        Host h = hostrepo.findById(hostid).get();
		
		return this.login(h.getIp(), h.getPort(), h.getUsername(), password, token, false);
		
	}

	@Override
    public boolean login(String host, String port, String username, String password, String token, boolean isTerminal) throws AuthenticationException {
        try {
            logout();
            // ssh.authPublickey(System.getProperty("user.name"));
            log.info("new SSHClient");
            ssh = new SSHClient(); //create a new SSH client
            log.info("verify all hosts");
            ssh.addHostKeyVerifier(new HostKeyVerifier() {
                public boolean verify(String arg0, int arg1, PublicKey arg2) {
                    return true; // don't bother verifying
                }
            });
            log.info("connecting");
            ssh.connect(host, Integer.parseInt(port));
            log.info("authenticating: {}", username);
            ssh.authPassword(username, password);
            log.info("starting session");
            session = ssh.startSession(); //SSH client creates new SSH session
            log.info("allocating PTY");
            session.allocateDefaultPTY(); 
            this.username = username;
            this.token = token;
            this.isTerminal = isTerminal;
            
            if(isTerminal) {
            	//shell
            	log.info("starting shell");
                shell = session.startShell(); //SSH session creates SSH Shell. if shell is null, it is in command mode.
                log.info("SSH session established");
                input = new BufferedReader(new InputStreamReader(shell.getInputStream()));
                output = shell.getOutputStream();
//                sender = new SSHSessionOutput(input, token);
                sessionsender.init(input, token);
                //moved here on 10/29/2018
                //all SSH shell sessions must have a output thread
                thread = new Thread(sessionsender);
                thread.setName("SSH output thread");
                log.info("starting sending thread");
                thread.start();
            }else {
            	//command
            	//do nothing here
            	
            }
            
        } catch (Exception e) {
        	e.printStackTrace();
            log.error(e.getMessage());
            finalize();
            throw new SSHAuthenticationException(e.getMessage(), e);
        }
        return true;

    }

    @Override
    public BufferedReader getSSHInput() {
        return input;
    }

    @Override
    public OutputStream getSSHOutput() {
        return output;
    }

    @Override
    protected void finalize() {
    	//stop the thread first
        try {
        	if(sessionsender!=null)
        		sessionsender.stop();
        	if(cmdsender!=null)
        		cmdsender.stop();
//        	thread.interrupt();
        } catch (Throwable e) {
        	e.printStackTrace();
        }
        try {
            shell.close(); //sshj shell
        } catch (Throwable e) {
        }
        try {
            session.close(); //sshj session
        } catch (Throwable e) {
        }
        try {
            ssh.disconnect(); //sshj client
            
        } catch (Throwable e) {
        }
        
        log.info("session finalized");
    }
    
    @Override
	public void saveHistory(String logs, String status) {
		history.setHistory_output(logs);
		history.setIndicator(status);
    	this.history_tool.saveHistory(history);
    	
	}
    
    public static String unaccent(String src) {
		return Normalizer
				.normalize(src, Normalizer.Form.NFD)
				.replaceAll("[^\\p{ASCII}]", "");
	}
    
    public static String escapeJupter(String json) {
    	
    	json  = StringEscapeUtils.escapeJava(json);
    	
    	return json;
    	
    }
    
    @Override
	public void runPython(String history_id, String python, String processid, boolean isjoin, String bin, String pyenv, String basedir, String token) {
    	
    	try {
    		
    		log.info("starting command: " + python);
    		
    		python = escapeJupter(python);
    		
//    		log.info("escaped command: " + python);
    		
//    		python = python.replaceAll("\\\n", ".");
//    		python = python.replace("\\n", "\\\\n");
    		
    		log.info("\n command: " + python);
    		
    		String cmdline = "";
    		
    		if(!bt.isNull(basedir)||"default".equals(basedir)) {
    			
    			cmdline += "cd " + basedir + "; ";
    			
    		}

    		//new version of execution in which all the python files are copied in the host
    		
    		cmdline += "mkdir " + token + ";";
    		
    		cmdline += "tar -xvf " + token + ".tar -C " + token + "/; ";
    		
    		cmdline += "cd "+ token + "/; ";
    		
//    		cmdline += "printf \"" + python + "\" > python-" + history_id + ".py; ";
    		
    		cmdline += "chmod +x *.py;";

    		String filename = processRepository.findById(processid).get().getName();//pt.getNameById(processid);
    		
    		filename = filename.trim().endsWith(".py")? filename: filename+".py";
    		
    		if(bt.isNull(bin)||"default".equals(bin)) {

//    			cmdline += "python python-" + history_id + ".py;";
    			cmdline += "python " + filename + "; ";
    			
    		}else {
    			
    			// if(!bt.isNull(pyenv)) cmdline += "source activate " + pyenv + "; "; //for demo only
    			
    			cmdline += bin + " " + filename + "; ";
    			
    		}
    		
    		cmdline += "cd ..; rm -R " + token + "*;"; //remove the code
    		
    		log.info(cmdline);
    		
    		Command cmd = session.exec(cmdline);
    		
            log.info("SSH command session established");
            
            input = new BufferedReader(new InputStreamReader(cmd.getInputStream()));
            
//            sender = new SSHCmdSessionOutput(input, token);
            cmdsender.init(input, token, history_id);
            
            //moved here on 10/29/2018
            //all SSH sessions must have a output thread
            
            thread = new Thread(cmdsender);
            
            thread.setName("SSH Command output thread");
            
            log.info("starting sending thread");
            
            thread.start();
            
            log.info("returning to the client..");
            
            if(isjoin) thread.join(7*24*60*60*1000); //longest waiting time - a week
	        
		} catch (Exception e) {
			
			e.printStackTrace();
			
		}
    	
	}
    
    @Override
	public void runJupyter(String history_id, String notebookjson, String processid, boolean isjoin, String bin, String pyenv, String basedir, String token) {
    	
    	try {
    		
    		log.info("starting command");
    		
    		String cmdline = "";
    		
    		if(!bt.isNull(basedir)||"default".equals(basedir)) {
    			
    			cmdline += "cd " + basedir + "; ";
    			
    		}
    		
    		notebookjson = escapeJupter(notebookjson);
    		
    		cmdline += "echo \"" + notebookjson + "\" > jupyter-" + history_id + ".ipynb; ";
    		
//    		if(!(BaseTool.isNull(bin)||"default".equals(bin))) {
//    			
//    			cmdline += "source activate " + pyenv + "; ";
//    			
//    		}
    		
    		if(bt.isNull(bin)||"default".equals(bin)) {

//    			cmdline += "python python-" + history_id + ".py;";
    			
//    			cmdline += "python " + filename + "; ";
    			
    		}else {
    			
//    			cmdline += "conda init; ";
    			
    			cmdline += "source activate " + pyenv + "; "; //for demo only
    			
//    			cmdline += bin + " " + filename + "; ";
    			
    		}
    		
    		cmdline += "jupyter nbconvert --to notebook --execute jupyter-" + history_id + ".ipynb;";
    		
    		cmdline += "rm ./jupyter-" + history_id + ".ipynb; "; // remove the script finally, leave no trace behind
    		
    		// cmdline += "echo \"==== Geoweaver Bash Output Finished ====\"";
    		
//    		cmdline += "./geoweaver-" + token + ".sh;";
    		
//    		cmdline += "cat ./jupyter-"+token+".ipynb | while read line\r\n" + 
//    				"do\r\n" + 
//    				"  echo \"$line\"\r\n" + 
//    				"done; "; // read the content of the result ipynb
    		
			
    		log.info(cmdline);
    		
    		Command cmd = session.exec(cmdline);
//            con.writer().print(IOUtils.readFully(cmd.getInputStream()).toString());
//            cmd.join(5, TimeUnit.SECONDS);
//            con.writer().print("\n** exit status: " + cmd.getExitStatus());
    		
            log.info("SSH command session established");
            
            input = new BufferedReader(new InputStreamReader(cmd.getInputStream()));
            
//            sender = new SSHSessionOutput(input, token);
//            sender = new SSHCmdSessionOutput(input, token);
            cmdsender.init(input, token, history_id);
            
            //moved here on 10/29/2018
            //all SSH sessions must have a output thread
            
            thread = new Thread(cmdsender);
            
            thread.setName("SSH Command output thread");
            
            log.info("starting sending thread");
            
            thread.start();
            
            log.info("returning to the client..");
            
            if(isjoin) thread.join(7*24*60*60*1000); //longest waiting time - a week
//	        
//	        output.write((cmd + '\n').getBytes());
//			
////	        output.flush();
//	        
//	        cmd = "./geoweaver-" + token + ".sh";
//	        		
//	        output.write((cmd + '\n').getBytes());
//			
////	        output.flush();
//	        	
//	        cmd = "echo \"==== Geoweaver Bash Output Finished ====\"";
//	        
//	        output.write((cmd + '\n').getBytes());
//	        output.flush();
	        
		} catch (Exception e) {
			
			e.printStackTrace();
			
		}
    	
	}

	@Override
	public void runBash(String history_id, String script, String processid, boolean isjoin, String token) {
    	
    	try {
    		
    		log.info("starting command");
    		
    		// script += "\n echo \"==== Geoweaver Bash Output Finished ====\"";
    		
    		String cmdline = "echo \"" 
    				+ script.replaceAll("\r\n", "\n").replaceAll("\\$", "\\\\\\$").replaceAll("\"", "\\\\\"") 
    				+ "\" > geoweaver-" + token + ".sh; ";
    		
    		cmdline += "chmod +x geoweaver-" + token + ".sh; ";
    		
    		cmdline += "./geoweaver-" + token + ".sh;";
    		
    		cmdline += "rm ./geoweaver-" + token + ".sh; "; //remove the script finally, leave no trace behind
			
    		log.info(cmdline);
    		
    		Command cmd = session.exec(cmdline);
//            con.writer().print(IOUtils.readFully(cmd.getInputStream()).toString());
//            cmd.join(5, TimeUnit.SECONDS);
//            con.writer().print("\n** exit status: " + cmd.getExitStatus());
    		
            log.info("SSH command session established");
            
            input = new BufferedReader(new InputStreamReader(cmd.getInputStream()));
            
//            sender = new SSHSessionOutput(input, token);
//            sender = new SSHCmdSessionOutput(input, token);
            cmdsender.init(input, token, history_id);
            
            //moved here on 10/29/2018
            //all SSH sessions must have a output thread
            
            thread = new Thread(cmdsender);
            
            thread.setName("SSH Command output thread");
            
            log.info("starting sending thread");
            
            thread.start();
            
            log.info("returning to the client..");
            
            if(isjoin) thread.join(7*24*60*60*1000); //longest waiting time - a week
//	        
//	        output.write((cmd + '\n').getBytes());
//			
////	        output.flush();
//	        
//	        cmd = "./geoweaver-" + token + ".sh";
//	        		
//	        output.write((cmd + '\n').getBytes());
//			
////	        output.flush();
//	        	
//	        cmd = "echo \"==== Geoweaver Bash Output Finished ====\"";
//	        
//	        output.write((cmd + '\n').getBytes());
//	        output.flush();
	        
		} catch (Exception e) {
			
			e.printStackTrace();
			
		}
    	
    	//feed the process code into the SSH session
    	

//		
//		Session.Command cmd = session.getSSHJSession().exec(executebash);
//		
//		String output = IOUtils.readFully(cmd.getInputStream()).toString();
//		
//		logger.info(output);
//		
//		//wait until the process execution is over
//		
//        cmd.join(5, TimeUnit.SECONDS);
//        
//		cmd.close();
//		
//		session.logout();
	}

	@Override
	public void runMultipleBashes(String history_id, String[] script, String processid) {
		// TODO Auto-generated method stub
		
		
	}

	@Override
    public void setWebSocketSession(WebSocketSession session) {
        if(!bt.isNull(sessionsender))this.sessionsender.setWebSocketSession(session); //connect WebSocket with SSH output thread
        if(!bt.isNull(cmdsender)) this.cmdsender.setWebSocketSession(session);
    }

    @Override
    public boolean logout() {
        log.info("logout: {}", username);
        try {
            // output.write("exit".getBytes());
            finalize();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return true;
    }

    void readWhereCondaInOneCommand(String hostid) throws IOException{

        List<Environment> old_envlist = et.getEnvironmentsByHostId(hostid);

        String cmdline = "source ~/.bashrc; whereis python; conda env list";
        
        log.info(cmdline);
    
        Command cmd = session.exec(cmdline);

        String output = IOUtils.readFully(cmd.getInputStream()).toString();

        System.out.println(output);
        //An Example:
        //  ## there might be some error messages here because of the source ~/.bashrc
        //  python: /usr/bin/python3.6m /usr/bin/python3.6 /usr/lib/python2.7 /usr/lib/python3.8 /usr/lib/python3.6 /usr/lib/python3.7 /etc/python2.7 /etc/python /etc/python3.6 /usr/local/lib/python3.6 /usr/include/python3.6m /usr/share/python
        //  bash: conda: command not found
        //  # conda environments:
        //  #
        //                           /home/zsun/anaconda3
        //                           /home/zsun/anaconda3/envs/ag
        //  base                  *  /root/anaconda3

        String[] lines = output.split("\n");
        int nextlineindex = 1;
        //Parse "whereis python"
        for(int i=0; i<lines.length; i++){

            if(lines[i].startsWith("python")){

                String pythonarraystr = lines[i].substring(8);
    
                String[] pythonarray = pythonarraystr.split(" ");
    
                for(String pypath : pythonarray){
    
                    if(!bt.isNull(pypath)){
    
                        pypath = pypath.trim();
    
                        et.addNewEnvironment(pypath, old_envlist, hostid, pypath);
    
                    }
    
                }

                nextlineindex = i+1;

                break;
    
            }
        }
        

        //parse Conda results
        if(!bt.isNull(lines[nextlineindex]) && lines[nextlineindex].startsWith("# conda")){ //pass if conda is not found

            for(int i=nextlineindex+1; i<lines.length; i++){

                if(!lines[i].startsWith("#")){ //pass comments

                    String[] vals = lines[i].split("\\s+");

					if(vals.length<2) continue;

					String bin = vals[vals.length-1]+"/bin/python"; //on linux python command is under bin folder

					String name = bt.isNull(vals[0])?bin:vals[0];

                    et.addNewEnvironment(bin, old_envlist, hostid, name);

                }

            }

        }
        

    }

    @Override
    public String readPythonEnvironment(String hostid, String password) {

        String resp = null;

        try {

           this.readWhereCondaInOneCommand(hostid);

        //    this.readConda();

           resp = et.getEnvironments(hostid);

        } catch (Exception e) {

            e.printStackTrace();

        } finally{

            finalize();
            // if(!bt.isNull(session))
            //     try {

            //         session.close();
            //     } catch (Exception e) {
            //         e.printStackTrace();
            //     }

        }

        

        return resp;
    }

}
