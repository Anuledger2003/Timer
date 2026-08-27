package com.fishbowl.pdmlink.linkaccess.robot;

import com.hni.pdmlink.transfer.HniTransfer;
import org.apache.logging.log4j.Logger;
import wt.fc.Persistable;
import wt.log4j.LogR;

public class WorkflowTriggerCreator {
   private String result;
   private String trgGroupPath;
   private String emailMessage = "";
   private String triggerMessage;
   private static Logger logger = LogR.getLogger(WorkflowTriggerCreator.class.getName());

   public String getResult() {
      return this.result;
   }

   public String getTriggerMessage() {
      return this.triggerMessage;
   }

   public String getTriggerGroupPath() {
      return this.trgGroupPath;
   }

   public String getEmailMessage(String var1) {
      boolean var2 = var1 != null && var1.length() > 0;
      boolean var3 = this.emailMessage != null && this.emailMessage.length() > 0;
      return var2 && !var3 ? var1 : this.emailMessage;
   }

   public static WorkflowTriggerCreator synchronizeTriggerWithContextIBA(Object var0, String var1, String var2, String var3, String var4, String var5) {
      WorkflowTriggerCreator var6 = new WorkflowTriggerCreator();
      HniTransfer var7 = new HniTransfer();
      Integer var15 = 0;
      Integer var9 = 30;

      try {
         var9 = Integer.parseInt(var7.getProperty("maxValidateCount", "30"));
      } catch (Exception var12) {
      }

      if (var2 != null && var2.length() >= 1) {
         var15 = Integer.parseInt(var2);
         ++var15;
         logger.trace("synchronizeTriggerWithContextIBA() attempt " + var15);
         var6.trgGroupPath = var15.toString();
      } else {
         logger.trace("synchronizeTriggerWithContextIBA() first run");
         var6.trgGroupPath = "0";
      }

      if (var4 == null) {
         logger.trace("synchronizeTriggerWithContextIBA() phase one...");

         try {
            var6.result = var7.validateObject(var0, var15);
            if (var15 > var9 && (var6.result == null || !var6.result.equalsIgnoreCase("Success"))) {
               var6.result = "Failure";
            }
         } catch (Exception var14) {
            var6.result = "Failure";
            var6.triggerMessage = var14.getMessage();
            logger.error("-->synchronizeTriggerWithContextIBA() Exception: " + var6.triggerMessage);
         }
      } else {
         logger.trace("synchronizeTriggerWithContextIBA() phase two...");

         try {
            var6.result = var7.transferObject(var0, var4);
            if (var15 > var9 && (var6.result == null || !var6.result.equalsIgnoreCase("Success"))) {
               var6.result = "Failure";
            }

            var6.emailMessage = var7.getEmailMsg((Persistable)var0);
         } catch (Exception var13) {
            var6.result = "Failure";
            var6.triggerMessage = var13.getMessage();
            logger.error("-->synchronizeTriggerWithContextIBA() Exception: " + var6.triggerMessage);
         }
      }

      return var6;
   }
}
