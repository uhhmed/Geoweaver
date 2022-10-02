package com.gw.commands;

import org.springframework.stereotype.Component;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gw.jpa.History;
import com.gw.ssh.SSHSessionImpl;
import com.gw.tools.HistoryTool;
import com.gw.tools.WorkflowTool;
import com.gw.utils.BaseTool;
import com.gw.utils.BeanTool;
import com.gw.utils.RandomString;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Component
@Command(name = "workflow")
public class RunWorkflowCommand  implements Runnable {

    static class ImportedWorkflowArgs {
        @Option(names = "-importMode", required = false) boolean importMode;
        @Option(names = { "--workflowfile" }, description = "workflow package or path to workflow.json to run")
        String workflowZipOrPathToJson;
        @Option(names = { "--add-hosts" }, description = "hosts to run on")
        String[] hostStrings;
        @Option(names = { "--add-environments" }, description = "environments to run on")
        String[] envs;
        @Option(names = { "--add-passwords" }, description = "passwords to the target hosts")
        String[] passes;
    }

    static class ExistingWorkflowArgs {
        @Option(names = "-existingMode", required = false) boolean existingMode;
        @Parameters(index = "0", description = "workflow id to run")
        String workflowId;
        @Option(names = { "-h", "--hosts" }, description = "hosts to run on")
        String[] hostStrings;
        @Option(names = { "-e", "--environments" }, description = "environments to run on")
        String[] envs;
        @Option(names = { "-p", "--passwords" }, description = "passwords to the target hosts")
        String[] passes;
    }

    static class Args {
        @ArgGroup(exclusive = false, multiplicity = "1", heading = "Imported Workflow mode args%n")
        ImportedWorkflowArgs ImportedArgs;

        @ArgGroup(exclusive = false, multiplicity = "1", heading = "Existing Workflow mode args%n")
        ExistingWorkflowArgs ExistingArgs;
    }

    @ArgGroup(exclusive = true, multiplicity = "1")
    Args args;

    

    public void run() {
        
        WorkflowTool wt = BeanTool.getBean(WorkflowTool.class);
        HistoryTool ht = BeanTool.getBean(HistoryTool.class);
        BaseTool bt = BeanTool.getBean(BaseTool.class);
        String historyId = new RandomString(18).nextString();

        if (args.ImportedArgs != null) {
        
            System.out.println("workflow zip or path to json: " + args.ImportedArgs.workflowZipOrPathToJson);

            Path toCopy = new File(args.ImportedArgs.workflowZipOrPathToJson).toPath();
            Path target = new File(bt.getFileTransferFolder() + toCopy.getFileName()).toPath();
            
            try {
                Files.copy(toCopy, target, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Copy path: " + toCopy);
                System.out.println("target path: " + target);

                String resp = wt.precheck(target.getFileName().toString());
                System.out.println("Response of /precheck: "+ resp);
    
                ObjectMapper mapper = new ObjectMapper();
                Map<String, String> map = mapper.readValue(resp, Map.class);
                
                
                String wid = String.valueOf(map.get("id"));

                resp = wt.saveWorkflowFromFolder(wid, toCopy.getFileName().toString());

                System.out.println("saved workflow ID: " + wid);

                String response = wt.execute(historyId, wid, "one", args.ImportedArgs.hostStrings, 
                args.ImportedArgs.passes, args.ImportedArgs.envs, "xxxxxxxxxx");

            } catch (Exception e) {
                e.printStackTrace();
            }
            
        }
        
        if (args.ExistingArgs != null) {
            
            System.out.println(String.format("Running workflow %s", args.ExistingArgs.workflowId));


            if(BaseTool.isNull(args.ExistingArgs.envs)) args.ExistingArgs.envs = new String[]{"default_option"};

            if(BaseTool.isNull(args.ExistingArgs.hostStrings)) args.ExistingArgs.hostStrings = new String[]{"10001"};

            String response = wt.execute(historyId, args.ExistingArgs.workflowId, "one", args.ExistingArgs.hostStrings, 
            args.ExistingArgs.passes, args.ExistingArgs.envs, "xxxxxxxxxx");

            System.out.println(String.format("The workflow has been kicked off.\nHistory Id: %s", historyId));

            System.out.println("Waiting for it to finish");

            History hist = ht.getHistoryById(historyId);;

            try {
            
                while(true){
            
                    TimeUnit.SECONDS.sleep(2);
            
                    hist = ht.getHistoryById(historyId);
            
                    if(ht.checkIfEnd(hist)) break;
                
                }
    
            } catch (InterruptedException e) {
                
                e.printStackTrace();
            
            }
    
    
            System.out.println(String.format("Total time cost: %o seconds", 
                               BaseTool.calculateDuration(hist.getHistory_begin_time(), hist.getHistory_end_time())));
            
                               
            System.out.println(String.format("Execution is over. Final status: %s.", hist.getIndicator()));

        }
        

    }
    
}
