package com.hni.pdmlink.transfer;

import com.google.common.io.Files;
import com.lowagie.text.Document;
import com.lowagie.text.pdf.PRAcroForm;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.SimpleBookmark;
import com.ptc.commons.lang.util.StringEscapeUtils;
import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.meta.common.OperationIdentifier;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.impl.InstanceBasedAttributeTypeIdentifier;
import com.ptc.core.meta.type.command.typemodel.common.GetSoftSchemaAttributesCommand;
import com.ptc.wvs.server.publish.PublishJob;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.logging.log4j.Logger;
import wt.change2.ChangeHelper2;
import wt.change2.WTChangeOrder2;
import wt.content.ApplicationData;
import wt.content.ContentHelper;
import wt.content.ContentHolder;
import wt.content.ContentItem;
import wt.content.ContentRoleType;
import wt.content.ContentServerHelper;
import wt.content.FormatContentHolder;
import wt.doc.WTDocument;
import wt.enterprise.RevisionControlled;
import wt.epm.EPMDocConfigSpec;
import wt.epm.EPMDocument;
import wt.epm.build.EPMBuildRule;
import wt.epm.familytable.CompatibleFamilyTables;
import wt.epm.familytable.EPMFamilyTableHelper;
import wt.epm.familytable.EPMSepFamilyTable;
import wt.epm.workspaces.EPMAsStoredConfig;
import wt.epm.workspaces.EPMAsStoredConfigSpec;
import wt.epm.workspaces.EPMAsStoredHelper;
import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.fc.ReferenceFactory;
import wt.fc.WTReference;
import wt.fc.collections.WTCollection;
import wt.fc.collections.WTHashSet;
import wt.fc.collections.WTKeyedMap;
import wt.inf.container.WTContained;
import wt.inf.container.WTContainer;
import wt.lifecycle.LifeCycleHelper;
import wt.lifecycle.LifeCycleHistory;
import wt.lifecycle.LifeCycleManaged;
import wt.lifecycle.LifeCycleState;
import wt.lifecycle.State;
import wt.log4j.LogR;
import wt.maturity.MaturityHelper;
import wt.maturity.PromotionNotice;
import wt.part.WTPart;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.queue.MethodArgument;
import wt.queue.QueueEntry;
import wt.representation.Representable;
import wt.representation.Representation;
import wt.representation.RepresentationContributor;
import wt.representation.RepresentationHelper;
import wt.session.SessionHelper;
import wt.session.SessionServerHelper;
import wt.type.Typed;
import wt.type.TypedUtilityServiceHelper;
import wt.util.LocalizableMessage;
import wt.util.WTException;
import wt.util.WTProperties;
import wt.vc.VersionControlException;
import wt.vc.VersionControlHelper;
import wt.vc.Versioned;
import wt.vc.config.ConfigSpec;
import wt.viewmarkup.DerivedImage;

public class HniTransfer {
   private Properties transferProperties = this.getTransferProperties();
   private static int[] whereIndicies = new int[]{0, 1};
   private static Logger logger = LogR.getLogger(HniTransfer.class.getName());

   private Properties getTransferProperties() {
      Properties var1 = new Properties();

      try {
         WTProperties var2 = WTProperties.getLocalProperties();
         if (var2 == null) {
            throw new Exception("Failed to read wt.properties");
         }

         String var3 = var2.getProperty("wt.home");
         if (var3 == null || var3.length() <= 0) {
            throw new Exception("Failed to read wt.home");
         }

         var3 = var3 + "/codebase/com/hni/pdmlink/transfer/transfer.properties";
         BufferedReader var4 = null;
         File var5 = new File(var3);
         if (!var5.exists()) {
            throw new Exception("Failed to find " + var3);
         }

         var4 = new BufferedReader(new FileReader(var5));

         String var6;
         while((var6 = var4.readLine()) != null) {
            var6 = var6.trim();
            if (var6.indexOf(35) != 0 && var6.indexOf(61) > 0) {
               String[] var7 = var6.split("=", 2);
               var1.put(var7[0].trim().toLowerCase(), var7[1].trim());
            }
         }

         var4.close();
      } catch (Exception var8) {
      }

      return var1;
   }

   public String getProperty(String var1, String var2) {
      String var3 = this.transferProperties.getProperty(var1.toLowerCase());
      return var3 != null && var3.length() > 0 ? var3 : var2;
   }

   private File getConfigFile() {
      WTProperties var1 = null;

      try {
         var1 = WTProperties.getLocalProperties();
      } catch (Exception var3) {
         logger.error("-->getConfigFile() Exception: " + var3.getMessage());
         var3.printStackTrace();
         return null;
      }

      if (var1 == null) {
         return null;
      } else {
         String var2 = var1.getProperty("wt.home");
         if (var2 != null && var2.length() > 0) {
            var2 = var2.replace('\\', '/');
            if (!var2.endsWith("/")) {
               var2 = var2 + "/";
            }

            var2 = var2 + "codebase/com/hni/pdmlink/transfer/fop.xconf";
            return new File(var2);
         } else {
            return null;
         }
      }
   }

   private File getXslFile(Persistable var1) {
      try {
         WTProperties var2 = WTProperties.getLocalProperties();
         if (var2 == null) {
            return null;
         }

         String var3 = var2.getProperty("wt.home");
         if (var3 == null || var3.length() <= 0) {
            return null;
         }

         var3 = var3.replace('\\', '/');
         if (!var3.endsWith("/")) {
            var3 = var3 + "/";
         }

         var3 = var3 + "codebase/com/hni/pdmlink/transfer/";
         String var4 = TypedUtilityServiceHelper.service.getExternalTypeIdentifier((Typed)var1);
         logger.trace("-->getXslFile() softType: " + var4);
         String var5 = this.getProperty("xsl_" + var4, (String)null);
         if (var5 != null && var5.length() > 0) {
            return new File(var3, var5);
         }
      } catch (Exception var6) {
         logger.error("-->getXslFile() Exception: " + var6.getMessage());
         var6.printStackTrace();
      }

      return null;
   }

   private String getBaseName(String var1) {
      int var2 = var1.indexOf(46);
      return var2 < 0 ? var1 : var1.substring(0, var2);
   }

   public String getDocNumber(Persistable var1) {
      if (var1 instanceof WTPart) {
         return ((WTPart)var1).getNumber();
      } else if (var1 instanceof WTDocument) {
         return ((WTDocument)var1).getNumber();
      } else if (var1 instanceof EPMDocument) {
         return ((EPMDocument)var1).getNumber();
      } else if (var1 instanceof WTChangeOrder2) {
         return ((WTChangeOrder2)var1).getNumber();
      } else {
         return var1 instanceof PromotionNotice ? ((PromotionNotice)var1).getNumber() : "";
      }
   }

   private String getDocState(Persistable var1) {
      if (var1 instanceof LifeCycleManaged) {
         LifeCycleManaged var2 = (LifeCycleManaged)var1;
         String var3 = "UNKNOWN";
         LifeCycleState var4 = var2.getState();
         if (var4 == null) {
            return var3;
         } else {
            State var5 = var4.getState();
            if (var5 != null) {
               var3 = var5.toString();
            }

            return var3;
         }
      } else {
         return "";
      }
   }

   private String getFileExt(RevisionControlled var1) {
      String var2 = this.getOriginalFilename(var1);
      int var3 = var2.lastIndexOf(46);
      return var3 < 0 ? "" : var2.substring(var3 + 1);
   }

   // Returns true if this is a SolidWorks drawing (.slddrw). Used to route
   // SolidWorks docs to the "single as-is PDF, no coversheet merge, no xml"
   // output path instead of the normal Creo drw/pdf flow.
   private boolean isSolidWorksDrawing(Persistable var1) {
      return var1 instanceof EPMDocument && this.getOriginalFilename(var1).toLowerCase().endsWith(".slddrw");
   }

   // Returns the subset of changeables that should go through the
   // coversheet-merge + xml flow (Creo .drw / WTDocument .pdf).
   @SuppressWarnings({ "unchecked", "rawtypes" })
   private List<RevisionControlled> getMergeableDrawings(List<RevisionControlled> var1) {
      List<RevisionControlled> var2 = new ArrayList();

      for (RevisionControlled var4 : var1) {
         if (!this.isSolidWorksDrawing(var4)) {
            var2.add(var4);
         }
      }

      return var2;
   }

   // Returns just the SolidWorks drawings from a changeables list - these
   // get only a single as-is pdf copied to the output directory.
   @SuppressWarnings({ "unchecked", "rawtypes" })
   private List<RevisionControlled> getStandaloneSolidWorksDrawings(List<RevisionControlled> var1) {
      List<RevisionControlled> var2 = new ArrayList();

      for (RevisionControlled var4 : var1) {
         if (this.isSolidWorksDrawing(var4)) {
            var2.add(var4);
         }
      }

      return var2;
   }

   private EPMDocument getEPMDocument(String var1, String var2) throws Exception {
      QuerySpec var3 = new QuerySpec(EPMDocument.class);
      var3.appendWhere(new SearchCondition(EPMDocument.class, "master>CADName", "=", var1.toLowerCase(), false), whereIndicies);
      QueryResult var4 = PersistenceHelper.manager.find(var3);

      while(var4.hasMoreElements()) {
         EPMDocument var5 = (EPMDocument)var4.nextElement();
         if (var5 != null) {
            var5 = (EPMDocument)PersistenceHelper.manager.refresh(var5);
            if (var5 != null && var5.isLatestIteration()) {
               String var6 = VersionControlHelper.getVersionIdentifier(var5).getValue();
               if (var2.equalsIgnoreCase(var6)) {
                  return var5;
               }
            }
         }
      }

      return null;
   }

   private String getOriginalFilename(Persistable var1) {
      if (var1 instanceof WTDocument) {
         ContentItem var2 = null;

         try {
            var2 = ((FormatContentHolder)ContentHelper.service.getContents((ContentHolder)var1)).getPrimary();
         } catch (Exception var4) {
         }

         String var3 = null;
         if (var2 != null && var2 instanceof ApplicationData) {
            var3 = ((ApplicationData)var2).getFileName();
         }

         return var3 == null ? "" : var3.toLowerCase();
      } else {
         return var1 instanceof EPMDocument ? ((EPMDocument)var1).getCADName().toLowerCase() : "";
      }
   }

   private boolean pendingPublishJobExists(RevisionControlled var1) {
      try {
         ReferenceFactory var2 = new ReferenceFactory();
         String var3 = var1.getPersistInfo().getObjectIdentifier().getStringValue();
         logger.trace("-->pendingPublishJobExists() wantedID: " + var3);
         QuerySpec var4 = new QuerySpec(QueueEntry.class);
         var4.appendWhere(new SearchCondition(QueueEntry.class, "targetClass", "=", "com.ptc.wvs.server.publish.PublishJob"), whereIndicies);
         var4.appendAnd();
         var4.appendOpenParen();
         var4.appendWhere(new SearchCondition(QueueEntry.class, "statusInfo.code", "=", "EXECUTING"), whereIndicies);
         var4.appendOr();
         var4.appendWhere(new SearchCondition(QueueEntry.class, "statusInfo.code", "=", "READY"), whereIndicies);
         var4.appendCloseParen();
         QueryResult var5 = PersistenceHelper.manager.find(var4);

         while(var5.hasMoreElements()) {
            QueueEntry var6 = (QueueEntry)var5.nextElement();
            Vector var7 = var6.getArgs();
            if (var7 != null) {
               for(int var8 = 0; var8 < var7.size(); ++var8) {
                  Object var9 = var7.elementAt(var8);
                  if (var9 instanceof MethodArgument) {
                     MethodArgument var10 = (MethodArgument)var9;
                     Object var11 = var10.getArg();
                     if (var11 != null && var11 instanceof PublishJob) {
                        PublishJob var12 = (PublishJob)var11;
                        String var13 = var12.getPersistableRef();
                        if (var13 != null) {
                           WTReference var14 = var2.getReference(var13);
                           if (var14 != null) {
                              Persistable var15 = var14.getObject();
                              if (var15 != null && var15 instanceof RevisionControlled) {
                                 String var16 = ((RevisionControlled)var15).getPersistInfo().getObjectIdentifier().getStringValue();
                                 if (var16 != null && var16.equalsIgnoreCase(var3)) {
                                    logger.trace("-->pendingPublishJobExists() match found: " + var16);
                                    return true;
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      } catch (Exception var17) {
         logger.error("-->pendingPublishJobExists() exception: " + var17.getMessage());
         var17.printStackTrace();
      }

      logger.trace("-->pendingPublishJobExists() no match found...");
      return false;
   }

   private void deletePendingPublishJob(RevisionControlled var1) {
      try {
         ReferenceFactory var2 = new ReferenceFactory();
         String var3 = var1.getPersistInfo().getObjectIdentifier().getStringValue();
         logger.trace("-->deletePendingPublishJob() wantedID: " + var3);
         QuerySpec var4 = new QuerySpec(QueueEntry.class);
         var4.appendWhere(new SearchCondition(QueueEntry.class, "targetClass", "=", "com.ptc.wvs.server.publish.PublishJob"), whereIndicies);
         var4.appendAnd();
         var4.appendWhere(new SearchCondition(QueueEntry.class, "statusInfo.code", "=", "READY"), whereIndicies);
         QueryResult var5 = PersistenceHelper.manager.find(var4);

         while(var5.hasMoreElements()) {
            QueueEntry var6 = (QueueEntry)var5.nextElement();
            Vector var7 = var6.getArgs();
            if (var7 != null) {
               for(int var8 = 0; var8 < var7.size(); ++var8) {
                  Object var9 = var7.elementAt(var8);
                  if (var9 instanceof MethodArgument) {
                     MethodArgument var10 = (MethodArgument)var9;
                     Object var11 = var10.getArg();
                     if (var11 != null && var11 instanceof PublishJob) {
                        PublishJob var12 = (PublishJob)var11;
                        String var13 = var12.getPersistableRef();
                        if (var13 != null) {
                           WTReference var14 = var2.getReference(var13);
                           if (var14 != null) {
                              Persistable var15 = var14.getObject();
                              if (var15 != null && var15 instanceof RevisionControlled) {
                                 String var16 = ((RevisionControlled)var15).getPersistInfo().getObjectIdentifier().getStringValue();
                                 if (var16 != null && var16.equalsIgnoreCase(var3)) {
                                    logger.trace("-->deletePendingPublishJob() match found: " + var16);

                                    try {
                                       var6 = (QueueEntry)PersistenceHelper.manager.refresh(var6);
                                       PersistenceHelper.manager.delete(var6);
                                       logger.debug("-->deletePendingPublishJob() isDeleted: " + PersistenceHelper.isDeleted(var6));
                                    } catch (Exception var18) {
                                       logger.debug("-->deletePendingPublishJob() Exception: " + var18.getMessage());
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      } catch (Exception var19) {
         logger.error("-->deletePendingPublishJob() exception: " + var19.getMessage());
         var19.printStackTrace();
      }

      logger.trace("-->deletePendingPublishJob() no match found...");
   }

   private boolean isValidRepresentationContributor(DerivedImage var1, RevisionControlled var2) {
      RepresentationContributor var3 = var1.getDerivedFrom();
      if (var3 != null) {
         long var4 = var3.getPersistInfo().getObjectIdentifier().getId();
         long var6 = var2.getPersistInfo().getObjectIdentifier().getId();
         logger.debug("-->isValidRepresentationContributor() derivedImageID: " + var4);
         logger.debug("-->isValidRepresentationContributor() revControlledID: " + var6);
         if (var4 == var6) {
            return true;
         }
      }

      return false;
   }

   private Representation findWTPartBasedRepresentation(EPMDocument var1) throws Exception {
      String var2 = VersionControlHelper.getVersionIdentifier(var1).getValue();
      String var3 = VersionControlHelper.getIterationIdentifier(var1).getValue();
      logger.debug("-->findWTPartBasedRepresentation() epmDoc: " + var1.getCADName() + " " + var2 + "." + var3);
      logger.debug("-->findWTPartBasedRepresentation() epmDoc: " + var1.getPersistInfo().getObjectIdentifier().getId());
      QuerySpec var4 = new QuerySpec(EPMBuildRule.class);
      Long var5 = new Long(VersionControlHelper.getBranchIdentifier(var1));
      var4.appendWhere(new SearchCondition(EPMBuildRule.class, "roleAObjectRef.key.branchId", "=", var5), whereIndicies);
      QueryResult var6 = PersistenceHelper.manager.find(var4);

      while(var6.hasMoreElements()) {
         EPMBuildRule var7 = (EPMBuildRule)var6.nextElement();
         if (var7.getBuildType() == 7) {
            WTPart var8 = (WTPart)var7.getBuildTarget();
            var2 = VersionControlHelper.getVersionIdentifier(var8).getValue();
            var3 = VersionControlHelper.getIterationIdentifier(var8).getValue();
            logger.debug("-->findWTPartBasedRepresentation() wtPart: " + var8.getNumber() + " " + var2 + "." + var3);
            Representation var9 = RepresentationHelper.service.getDefaultRepresentation(var8);
            if (var9 != null && var9 instanceof DerivedImage) {
               DerivedImage var10 = (DerivedImage)var9;
               if (this.isValidRepresentationContributor(var10, var1)) {
                  return var9;
               }
            }
         }
      }

      return null;
   }

   private Timestamp findModelTimestampForDrw(String var1, String var2) throws Exception {
      Timestamp var3 = null;

      try {
         EPMSepFamilyTable var4 = null;
         EPMDocument var5 = this.getEPMDocument(var1, var2);
         if (var5 == null) {
            return null;
         } else {
            logger.trace("-->findModelTimestampForDrw() epmDocument: " + var5.getIdentity());
            if (var5.getFamilyTableStatus() != 0) {
               logger.trace("-->findModelTimestampForDrw() appears to be family table");
               WTHashSet var6 = new WTHashSet(1);
               var6.add(var5);
               WTKeyedMap var7 = EPMFamilyTableHelper.manager.getCompatibleFamilyTables(var6, var5.getContainer());
               if (var7 != null && var7.size() > 0) {
                  logger.trace("-->findModelTimestampForDrw() compatible family table found");
                  CompatibleFamilyTables var8 = (CompatibleFamilyTables)var7.values().iterator().next();
                  WTCollection var9 = var8.getCompatibleFamilyTables();
                  if (var9 != null && var9.size() > 0) {
                     logger.trace("-->findModelTimestampForDrw() using latest compatible family table");
                     var4 = EPMFamilyTableHelper.getLatestFamilyTable(var9);
                  }
               }
            }

            QueryResult var11 = ContentHelper.service.getContentsByRole((ContentHolder)(var4 == null ? var5 : var4), ContentRoleType.PRIMARY);

            while(var11.hasMoreElements()) {
               Object var12 = var11.nextElement();
               if (var12 instanceof ApplicationData) {
                  ApplicationData var13 = (ApplicationData)var12;
                  Timestamp var14 = PersistenceHelper.getCreateStamp(var13);
                  if (var3 == null) {
                     var3 = var14;
                  } else if (var14 != null && var14.after(var3)) {
                     var3 = var14;
                  }
               }
            }

            logger.trace("-->findModelTimestampForDrw() contentTimestamp: " + var3);
            return var3;
         }
      } catch (Exception var10) {
         logger.error("-->findModelTimestampForDrw() exception: " + var10.getMessage());
         return null;
      }
   }

   private String getPrimaryContentFilename(RevisionControlled var1) {
      try {
         ContentHolder var2 = null;
         if (var1 instanceof WTDocument) {
            var2 = ContentHelper.service.getContents((WTDocument)var1);
         }

         if (var1 instanceof EPMDocument) {
            var2 = ContentHelper.service.getContents((EPMDocument)var1);
         }

         if (var2 == null) {
            return null;
         }

         Vector var3 = ContentHelper.getContentListAll(var2);
         if (var3 == null || var3.size() <= 0) {
            return null;
         }

         for(Object var5 : var3) {
            if (var5 instanceof ApplicationData) {
               ApplicationData var6 = (ApplicationData)var5;
               if (ContentRoleType.PRIMARY.equals(var6.getRole())) {
                  String var7 = var6.getFileName();
                  if (var7 != null && var7.trim().length() > 0) {
                     logger.trace("-->getPrimaryContentFilename() fileName: " + var7);
                     return var7.trim();
                  }
               }
            }
         }
      } catch (Exception var8) {
         logger.error("-->getPrimaryContentFilename() Exception: " + var8.getMessage());
      }

      return null;
   }

   private static ConfigSpec selectAsStoredEPMConfigSpec(EPMDocument var0) {
      EPMAsStoredConfig var1 = null;

      try {
         var1 = EPMAsStoredHelper.getAsStoredConfig(var0);
      } catch (Exception var5) {
         logger.error("-->selectAsStoredEPMConfigSpec() Exception" + var5.getMessage());
      }

      if (var1 == null) {
         logger.trace("->selectAsStoredEPMConfigSpec() epmAsStoredConfig is null");
         return null;
      } else {
         EPMDocConfigSpec var2 = null;

         try {
            EPMAsStoredConfigSpec var3 = EPMAsStoredConfigSpec.newEPMAsStoredConfigSpec(var1);
            var2 = EPMDocConfigSpec.newEPMDocConfigSpec(var3);
         } catch (Exception var4) {
            logger.error("-->selectAsStoredEPMConfigSpec() Exception" + var4.getMessage());
         }

         if (var2 == null) {
            logger.trace("->selectAsStoredEPMConfigSpec() epmDocConfigSpec is null");
         } else {
            logger.trace("->selectAsStoredEPMConfigSpec() epmDocConfigSpec is found");
         }

         return var2;
      }
   }

   private static PublishJob getPublishJob(Representation var0, long var1) {
      try {
         long var3 = System.currentTimeMillis() - 60000L * var1;
         Timestamp var5 = new Timestamp(var3);
         QuerySpec var6 = new QuerySpec(QueueEntry.class);
         var6.appendWhere(new SearchCondition(QueueEntry.class, "targetClass", "=", "com.ptc.wvs.server.publish.PublishJob"), whereIndicies);
         var6.appendAnd();
         var6.appendWhere(new SearchCondition(QueueEntry.class, "thePersistInfo.createStamp", ">", var5), whereIndicies);
         var6.appendAnd();
         var6.appendWhere(new SearchCondition(QueueEntry.class, "statusInfo.code", "=", "COMPLETED"), whereIndicies);
         QueryResult var7 = PersistenceHelper.manager.find(var6);

         while(var7.hasMoreElements()) {
            QueueEntry var8 = (QueueEntry)var7.nextElement();
            Vector var9 = var8.getArgs();
            if (var9 != null) {
               for(int var10 = 0; var10 < var9.size(); ++var10) {
                  Object var11 = var9.elementAt(var10);
                  if (var11 instanceof MethodArgument) {
                     MethodArgument var12 = (MethodArgument)var11;
                     Object var13 = var12.getArg();
                     if (var13 != null && var13 instanceof PublishJob) {
                        PublishJob var14 = (PublishJob)var13;
                        Representation var15 = var14.getCreatedRepresentation();
                        if (var0.equals(var15)) {
                           return var14;
                        }
                     }
                  }
               }
            }
         }
      } catch (Exception var16) {
         logger.error("-->getPublishJob() Exception: " + var16.getLocalizedMessage());
         var16.printStackTrace();
      }

      return null;
   }

   private Timestamp getPublishJobStartTime(Representation var1) {
      long var2 = 60L;

      try {
         var2 = Long.parseLong(this.getProperty("previousPublishMinutes", "60"));
      } catch (Exception var6) {
         logger.trace("-->isPdfViewableValid() Parse long exception: " + var6.getLocalizedMessage());
      }

      PublishJob var4 = null;
      if (var2 > 0L) {
         var4 = getPublishJob(var1, var2);
      }

      Timestamp var5 = null;
      if (var4 != null) {
         var5 = new Timestamp(var4.getStartTime());
      }

      return var5;
   }

   private boolean isPdfViewableValid(RevisionControlled var1, int var2) throws Exception {
      int var3 = 0;
      boolean var4 = SessionServerHelper.manager.setAccessEnforced(false);
      boolean var5 = false;

      try {
         Timestamp var6 = null;
         Timestamp var7 = null;
         String var8 = this.getPrimaryContentFilename(var1);
         String var9 = this.getDocNumber(var1);
         String var10 = VersionControlHelper.getVersionIdentifier(var1).getValue();
         String var11 = VersionControlHelper.getIterationIdentifier(var1).getValue();
         Representation var12 = RepresentationHelper.service.getDefaultRepresentation((Representable)var1);
         if (var12 == null && var1 instanceof EPMDocument && ((EPMDocument)var1).getCADName().toLowerCase().endsWith(".dwg")) {
            var12 = this.findWTPartBasedRepresentation((EPMDocument)var1);
         }

         String var13 = this.getProperty("forceViewableNewerThanStateChange", "true");
         if ("true".equalsIgnoreCase(var13)) {
            String var14 = this.getDocState(var1);
            QueryResult var15 = LifeCycleHelper.service.getHistory(var1);

            while(var15.hasMoreElements()) {
               LifeCycleHistory var16 = (LifeCycleHistory)var15.nextElement();
               String var17 = var16.getState().toString();
               Timestamp var18 = PersistenceHelper.getModifyStamp(var16);
               if (var14.equalsIgnoreCase(var17)) {
                  if (var7 == null) {
                     var7 = var18;
                  } else if (var18 != null && var18.after(var7)) {
                     var7 = var18;
                  }
               }
            }
         }

         String var33 = this.getProperty("forceDrawingNewerThanmodel", "false");
         boolean var34 = var1 instanceof EPMDocument && ((EPMDocument)var1).getCADName().toLowerCase().endsWith(".drw");
         if ("true".equalsIgnoreCase(var33) && var34) {
            String var35 = this.getBaseName(((EPMDocument)var1).getCADName().toLowerCase());
            var6 = this.findModelTimestampForDrw(var35 + ".prt", var10);
            Timestamp var38 = this.findModelTimestampForDrw(var35 + ".asm", var10);
            if (var6 == null) {
               var6 = var38;
            } else if (var38 != null && var38.after(var6)) {
               var6 = var38;
            }
         }

         logger.trace("-->isPdfViewableValid() revControlled: " + var9 + " " + var10 + "." + var11);
         if (var12 != null && var12 instanceof DerivedImage) {
            Timestamp var36 = this.getPublishJobStartTime(var12);
            var5 = var12.isOutOfDate();
            DerivedImage var39 = (DerivedImage)var12;
            ContentHolder var41 = ContentHelper.service.getContents(var39);
            RepresentationContributor var19 = var39.getDerivedFrom();
            if (var19 == null) {
               logger.trace("-->isPdfViewableValid() representationContributor: null");
            }

            if (var41 != null && var19 != null) {
               long var20 = var19.getPersistInfo().getObjectIdentifier().getId();
               long var22 = var1.getPersistInfo().getObjectIdentifier().getId();
               logger.trace("-->isPdfViewableValid() repContID: " + var20);
               logger.trace("-->isPdfViewableValid() revContID: " + var22);
               if (var20 == var22) {
                  logger.trace("-->isPdfViewableValid() repContID=revContID");
                  Vector var24 = ContentHelper.getContentListAll(var41);
                  if (var24 != null) {
                     logger.trace("-->isPdfViewableValid() vector.size: " + var24.size());

                     for(Object var26 : var24) {
                        if (var26 instanceof ApplicationData) {
                           ApplicationData var27 = (ApplicationData)var26;
                           String var28 = var39.getCADPartName();
                           logger.trace("-->isPdfViewableValid() cadPartName: " + var28);
                           if (var28 != null && var28.toLowerCase().endsWith(".pdf") && !var28.equalsIgnoreCase(var8)) {
                              logger.trace("-->isPdfViewableValid() ignoring derivedImage for PDF attachment: " + var28);
                           } else {
                              ContentRoleType var29 = var27.getRole();
                              String var30 = var27.getFileName().toLowerCase();
                              if (var30.endsWith(".pdf")) {
                                 logger.trace("-->isPdfViewableValid() docName: " + var30 + " role: " + var29.toString());
                                 Timestamp var31 = PersistenceHelper.getCreateStamp(var27);
                                 logger.trace("-->isPdfViewableValid() pdfTimestamp: " + var31);
                                 logger.trace("-->isPdfViewableValid() mdlTimestamp: " + var6);
                                 logger.trace("-->isPdfViewableValid() lfcTimestamp: " + var7);
                                 if (var6 != null && var31 != null && var6.after(var31)) {
                                    logger.trace("-->isPdfViewableValid() deleting drawing representation since it is older than model");
                                    RepresentationHelper.service.deleteRepresentation(var12, true);
                                 } else if (var7 != null && var31 != null && var7.after(var31)) {
                                    logger.trace("-->isPdfViewableValid() deleting representation since it is older than state change");
                                    RepresentationHelper.service.deleteRepresentation(var12, true);
                                 } else if (var7 != null && var36 != null && var7.after(var36)) {
                                    logger.error("-->isPdfViewableValid() *** HNI TRANSFER RACE CONDITION CAUGHT ***");
                                    logger.trace("-->isPdfViewableValid() deleting representation since its job was started before state change");
                                    RepresentationHelper.service.deleteRepresentation(var12, true);
                                 } else {
                                    ++var3;
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         logger.debug("-->isPdfViewableValid() representation.isOutOfDate(): " + var5);
         if ((var3 == 0 || var5) && var2 == 0) {
            String var37 = this.getProperty("skipPendingPublishJobLookup", "false");
            if ("true".equalsIgnoreCase(var37) || !this.pendingPublishJobExists(var1)) {
               String var40 = this.getProperty("deletePendingPublishJobs", "false");
               if ("true".equalsIgnoreCase(var40)) {
                  logger.debug("-->isPdfViewableValid() deleting pending publish jobs for " + var1.getDisplayIdentifier());
                  this.deletePendingPublishJob(var1);
               }

               logger.trace("-->isPdfViewableValid() emitReadyToPublishEvent");
               ConfigSpec var42 = selectAsStoredEPMConfigSpec((EPMDocument)var1);
               Vector var43 = new Vector(1);
               var43.addElement(var1);
               RepresentationHelper.service.emitReadyToPublishEvent(var43, var42, 1);
            }
         }
      } catch (Exception var32) {
         logger.error("-->isPdfViewableValid() exception: " + var32.getMessage());
         var32.printStackTrace();
      }

      SessionServerHelper.manager.setAccessEnforced(var4);
      if (var3 > 1) {
         throw new Exception("there was more than one applicable PDF viewable for this submission.");
      } else {
         return var3 == 1 && !var5;
      }
   }

   private String getIbaValue(Persistable var1, String var2) {
      logger.debug("-->getIbaValue() [" + var2 + "] " + this.getDocNumber(var1));

      try {
         Locale var3 = SessionHelper.getLocale();
         PersistableAdapter var4 = new PersistableAdapter(var1, (String)null, var3, (OperationIdentifier)null);
         var4.load(new String[]{var2});
         Object var5 = var4.get(var2);
         if (var5 instanceof String) {
            return (String)var5;
         }

         if (var5 instanceof Timestamp) {
            return this.getFormattedDate((Timestamp)var5);
         }
      } catch (Exception var6) {
         logger.debug("-->getIbaValue() Exception: " + var6.getLocalizedMessage());
      }

      return "";
   }

   @SuppressWarnings({ "rawtypes", "unchecked" })
   private List<String> getAttributeList(Persistable var1) throws Exception {
      logger.debug("-->getAttributeList() persistable: " + var1);
      ArrayList var2 = new ArrayList();
      TypeIdentifier var3 = TypedUtilityServiceHelper.service.getTypeIdentifier(var1);
      GetSoftSchemaAttributesCommand var4 = new GetSoftSchemaAttributesCommand();
      var4.setType_id(var3);

      for(Object var7obj : ((GetSoftSchemaAttributesCommand)var4.execute()).getAttributes()) {
         InstanceBasedAttributeTypeIdentifier var7 = (InstanceBasedAttributeTypeIdentifier)var7obj;
         String var8 = var7.getAttributeName();
         logger.debug("-->getAttributeList() ibaName: " + var8);
         var2.add(var8);
      }

      return var2;
   }

   private String getFormattedDate(Timestamp var1) {
      SimpleDateFormat var2 = new SimpleDateFormat("dd-MMM-yyyy");
      TimeZone var3 = TimeZone.getTimeZone("America/Chicago");
      Calendar var4 = Calendar.getInstance();
      var4.setTime(var1);
      var4.setTimeZone(var3);
      var2.setTimeZone(var3);
      return var2.format(var4.getTime());
   }

   private StringBuffer xmlNode(String var1, String var2, String var3) {
      return this.xmlNode(var1, var2, var3, false);
   }

   private StringBuffer xmlNode(String var1, String var2, String var3, boolean var4) {
      String var5 = var3;
      if (var3 != null) {
         var5 = StringEscapeUtils.escapeXml(var3);
      }

      StringBuffer var6 = new StringBuffer();
      if (!var4 || var5 != null && !"".equals(var5)) {
         var6.append(var1);
         var6.append("<").append(var2).append(">");
         var6.append(var5);
         var6.append("</").append(var2).append(">\n");
         return var6;
      } else {
         return var6;
      }
   }

   private String getIndent(int var1) {
      String var2 = "";

      for(int var3 = 0; var3 < var1; ++var3) {
         var2 = var2 + "\t";
      }

      return var2;
   }

   private String buildCncXml(RevisionControlled var1) throws VersionControlException {
      StringBuilder var2 = new StringBuilder();
      String var3 = this.getDocNumber(var1);
      if (var3.toLowerCase().endsWith(".pdf") || var3.toLowerCase().endsWith(".drw")) {
         var3 = var3.substring(0, var3.length() - 4);
      }

      String var4 = VersionControlHelper.getVersionIdentifier(var1).getValue();
      String var5 = this.getIbaValue(var1, "REV_SEQ");
      String var6 = this.getIbaValue(var1, "TARGET_STATE");
      logger.trace("-->buildCncXml() [" + var3 + "] [" + var4 + var5 + "] [" + var6 + "]");
      var2.append("\t<cnc_cnc>\n");
      var2.append(this.xmlNode(this.getIndent(2), "drw", var3));
      var2.append(this.xmlNode(this.getIndent(2), "rev", var4 + var5));
      var2.append(this.xmlNode(this.getIndent(2), "target_state", var6));
      var2.append("\t</cnc_cnc>\n");
      return var2.toString();
   }

   private List<RevisionControlled> getSortedChangeables(Persistable var1) {
      ArrayList var2 = new ArrayList();

      try {
         QueryResult var3 = null;
         if (var1 instanceof PromotionNotice) {
            PromotionNotice var4 = (PromotionNotice)var1;
            var3 = MaturityHelper.service.getPromotionTargets(var4);
         } else if (var1 instanceof WTChangeOrder2) {
            WTChangeOrder2 var7 = (WTChangeOrder2)var1;
            var3 = ChangeHelper2.service.getChangeablesAfter(var7, true);
         }

         while(var3 != null && var3.hasMoreElements()) {
            Object var8 = var3.nextElement();
            if (var8 instanceof WTDocument) {
               WTDocument var5 = (WTDocument)var8;
               if (this.getOriginalFilename(var5).endsWith(".pdf")) {
                  var2.add(var5);
               }
            } else if (var8 instanceof EPMDocument) {
               EPMDocument var9 = (EPMDocument)var8;
               String var10 = this.getOriginalFilename(var9);
               if (var10.endsWith(".drw") || var10.endsWith(".slddrw")) {
                  var2.add(var9);
               }
            } else {
               logger.debug("-->getSortedChangeables() unsupported type - " + var8);
            }
         }

         Collections.sort(var2, new HniTransfer$1(this));
      } catch (Exception var6) {
         logger.error("-->getSortedChangeables() Exception: " + var6.getLocalizedMessage());
         var6.printStackTrace();
      }

      return var2;
   }

   private String generateCoversheetXml(Persistable var1) throws Exception {
      String var2 = this.getProperty("coversheetCompanyIBA", "COVERSHEET_COMPANY");
      String var3 = this.getProperty("coversheetTitleIBA", "COVERSHEET_TITLE");
      StringBuilder var4 = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<linkAccessFile>\n");
      String var5 = "";
      String var6 = "";
      String var7 = "";
      if (var1 instanceof PromotionNotice) {
         PromotionNotice var8 = (PromotionNotice)var1;
         if (var8.getPromotionDate() != null) {
            var5 = this.getFormattedDate(var8.getPromotionDate());
         }

         if (var8.getLongDescription() != null) {
            var7 = var8.getLongDescription().getPlainText();
         }

         if (var7 != null && var7.length() <= 0 && var8.getDescription() != null) {
            var7 = var8.getDescription();
         }
      } else {
         if (!(var1 instanceof WTChangeOrder2)) {
            throw new Exception("primaryBusinessObject is not a supported type");
         }

         WTChangeOrder2 var17 = (WTChangeOrder2)var1;
         if (var17.getResolutionDate() != null) {
            var5 = this.getFormattedDate(var17.getResolutionDate());
         }

         if (var17.getNeedDate() != null) {
            var6 = this.getFormattedDate(var17.getNeedDate());
         }

         if (var17.getLongDescription() != null) {
            var7 = var17.getLongDescription().getPlainText();
         }

         if (var7 != null && var7.length() <= 0 && var17.getDescription() != null) {
            var7 = var17.getDescription();
         }
      }

      List<RevisionControlled> var18 = this.getSortedChangeables(var1);
      Iterator var9 = var18.iterator();

      while(var9.hasNext()) {
         var4.append(this.buildCncXml((RevisionControlled)var9.next()));
      }

      WTContainer var10 = ((WTContained)var1).getContainer();
      String var11 = this.getIbaValue(var10, var2);
      String var12 = this.getIbaValue(var10, var3);
      logger.trace("-->generateCoversheetXml() coversheetCompany: " + var11);
      logger.trace("-->generateCoversheetXml() coversheetTitle: " + var12);
      var4.append(this.xmlNode(this.getIndent(1), "coversheet_company", var11, true));
      var4.append(this.xmlNode(this.getIndent(1), "coversheet_title", var12, true));

      for(String var15 : this.getAttributeList(var1)) {
         String var16 = this.getIbaValue(var1, var15);
         logger.trace("-->generateCoversheetXml() " + var15 + "=" + var16);
         var4.append(this.xmlNode(this.getIndent(1), var15.toLowerCase(), var16, true));
      }

      logger.trace("-->generateCoversheetXml() rel_date=" + var5);
      var4.append(this.xmlNode(this.getIndent(1), "rel_date", var5, true));
      logger.trace("-->generateCoversheetXml() needdate=" + var6);
      var4.append(this.xmlNode(this.getIndent(1), "needdate", var6, true));
      logger.trace("-->generateCoversheetXml() description=" + var7);
      var4.append(this.xmlNode(this.getIndent(1), "description", var7, true));
      var4.append("</linkAccessFile>\n");
      return var4.toString();
   }

   private File createCoversheetPdf(Persistable var1, File var2, String var3) throws Exception {
      File var4 = new File(var3, this.getOutputFilename(var1, "_tmp.pdf"));
      FileOutputStream var5 = new FileOutputStream(var4);

      try {
         String var6 = this.generateCoversheetXml(var1);
         File var7 = this.getConfigFile();
         FopFactory var8 = FopFactory.newInstance(var7);
         Fop var9 = var8.newFop("application/pdf", var5);
         TransformerFactory var10 = TransformerFactory.newInstance();
         Transformer var11 = var10.newTransformer(new StreamSource(var2));
         StreamSource var12 = new StreamSource(new BufferedReader(new StringReader(var6)));
         SAXResult var13 = new SAXResult(var9.getDefaultHandler());
         var11.transform(var12, var13);
      } finally {
         var5.close();
      }

      return var4;
   }

   private String getOutputFilename(Persistable var1, String var2) throws Exception {
      String var3 = null;
      if (!(var1 instanceof WTChangeOrder2) && !(var1 instanceof PromotionNotice)) {
         if (var1 instanceof EPMDocument || var1 instanceof WTDocument) {
            String var10 = this.getBaseName(this.getOriginalFilename(var1));
            String var11 = VersionControlHelper.getVersionIdentifier((Versioned)var1).getValue();
            String var12 = this.getIbaValue(var1, "REV_SEQ");
            if (var12 == null || var12.length() <= 0) {
               var12 = ".";
            }

            String var7 = this.getDocState((RevisionControlled)var1);
            if (var7.equalsIgnoreCase("MAKEREADY")) {
               var7 = "MAKE READY";
            }

            var3 = var10 + "~" + var11 + var12 + "~" + var7;
         }
      } else {
         String var4 = this.getBaseName(this.getDocNumber(var1));
         String var5 = TypedUtilityServiceHelper.service.getExternalTypeIdentifier((Typed)var1);
         if (var5 != null && (var5.toUpperCase().endsWith("HNI_PROMOTION_REQUEST") || var5.toUpperCase().endsWith("HNI_CHANGE_NOTICE"))) {
            String var6 = this.getIbaValue(var1, "RELEASE_NO");
            logger.debug("-->getOutputFilename() release: " + var6);
            if (var6 != null && var6.trim().length() > 0) {
               var3 = var6.trim().replace(" - ", "-");
               var3 = var3.replace("\\", "").replace("/", "");
            }
         }

         if (var3 == null || var3.trim().length() <= 0) {
            var3 = var4;
         }

         var3 = var3.toLowerCase();
      }

      var3 = var3 + var2;
      logger.debug("-->getOutputFilename() resultFilename: " + var3);
      return var3;
   }

   private File getPdfViewable(RevisionControlled var1, String var2) {
      try {
         String var3 = this.getPrimaryContentFilename(var1);
         String var4 = this.getDocNumber(var1);
         Representation var5 = RepresentationHelper.service.getDefaultRepresentation((Representable)var1);
         if (var5 == null && var1 instanceof EPMDocument && ((EPMDocument)var1).getCADName().toLowerCase().endsWith(".dwg")) {
            var5 = this.findWTPartBasedRepresentation((EPMDocument)var1);
         }

         if (var5 == null || !(var5 instanceof DerivedImage)) {
            throw new WTException("Could not find a single default representation for " + var4);
         }

         String var6 = VersionControlHelper.getVersionIdentifier(var1).getValue();
         String var7 = VersionControlHelper.getIterationIdentifier(var1).getValue();
         logger.trace("-->getPdfViewable() revControlled: " + var4 + " " + var6 + "." + var7);
         DerivedImage var8 = (DerivedImage)var5;
         ContentHolder var9 = ContentHelper.service.getContents(var8);
         RepresentationContributor var10 = var8.getDerivedFrom();
         if (var10 == null) {
            logger.trace("-->getPdfViewable() representationContributor: null");
         } else {
            logger.trace("-->getPdfViewable() representationContributor: " + PersistenceHelper.getObjectIdentifier(var10).getStringValue());
         }

         if (var9 == null) {
            throw new WTException("Could not find a holder for " + var4);
         }

         Vector var11 = ContentHelper.getContentListAll(var9);
         if (var11 == null || var11.size() <= 0) {
            throw new WTException("Could not find the contents for " + var4);
         }

         for(Object var13 : var11) {
            if (var13 instanceof ApplicationData) {
               ApplicationData var14 = (ApplicationData)var13;
               String var15 = var8.getCADPartName();
               logger.trace("-->getPdfViewable() cadPartName: " + var15);
               if (var15 != null && var15.toLowerCase().endsWith(".pdf") && !var15.equalsIgnoreCase(var3)) {
                  logger.trace("-->getPdfViewable() ignoring derivedImage for pdf attachment: " + var15);
               } else {
                  ContentRoleType var16 = var14.getRole();
                  String var17 = var14.getFileName().toLowerCase();
                  if (var17.endsWith(".pdf")) {
                     logger.trace("-->getPdfViewable() docName: " + var17 + " role: " + var16.toString());
                     File var18 = new File(var2, this.getOutputFilename(var1, ".pdf"));
                     if (var18.exists()) {
                        return var18;
                     }

                     ContentServerHelper.service.writeContentStream(var14, var18.getAbsolutePath());
                     if (var18.exists()) {
                        return var18;
                     }

                     logger.trace("-->getPdfViewable() file not downloaded");
                  }
               }
            }
         }
      } catch (Exception var19) {
         logger.error("-->getPdfViewable() Exception: " + var19.getMessage());
      }

      return null;
   }

   private File getPrimaryContent(RevisionControlled var1, String var2) {
      try {
         ContentItem var3 = ((FormatContentHolder)ContentHelper.service.getContents((ContentHolder)var1)).getPrimary();
         if (var3 != null && var3 instanceof ApplicationData) {
            ApplicationData var4 = (ApplicationData)var3;
            logger.trace("-->getPrimaryContent() primaryContent: " + var4.getFileName());
            String var5 = this.getFileExt(var1);
            String var6 = this.getDocNumber(var1);
            String var7 = var6 + "." + var5;
            logger.trace("-->getPrimaryContent() outputName: " + var7);
            File var8 = new File(var2, this.getOutputFilename(var1, ".pdf"));
            if (var8.exists()) {
               return var8;
            }

            ContentServerHelper.service.writeContentStream(var4, var8.getAbsolutePath());
            if (var8.exists()) {
               return var8;
            }

            logger.trace("-->getNativeFile() file not downloaded");
         }
      } catch (Exception var9) {
         logger.error("-->getNativeViewable() exception: " + var9.getMessage());
      }

      return null;
   }

   private void mergePdfFiles(String[] var1, String var2) throws Exception {
      boolean var3 = false;
      int var4 = 0;
      PdfCopy var5 = null;
      Document var6 = null;

      try {
         ArrayList var7 = new ArrayList();

         for(int var18 = 0; var18 < var1.length; ++var18) {
            PdfReader var8 = new PdfReader(var1[var18]);
            var8.consolidateNamedDestinations();
            int var9 = var8.getNumberOfPages();
            List var10 = SimpleBookmark.getBookmark(var8);
            if (var10 != null) {
               if (var4 != 0) {
                  SimpleBookmark.shiftPageNumbers(var10, var4, (int[])null);
               }

               var7.addAll(var10);
            }

            var4 += var9;
            if (var18 == 0) {
               var6 = new Document(var8.getPageSizeWithRotation(1));
               var5 = new PdfCopy(var6, new FileOutputStream(var2));
               var6.open();
            }

            for(int var12 = 1; var12 <= var9; ++var12) {
               PdfImportedPage var11 = var5.getImportedPage(var8, var12);
               var5.addPage(var11);
            }

            PRAcroForm var19 = var8.getAcroForm();
            if (var19 != null) {
               var5.copyAcroForm(var8);
            }
         }

         if (!var7.isEmpty()) {
            var5.setOutlines(var7);
         }
      } catch (Exception var16) {
         logger.error("-->mergePdfFiles() Exception: " + var16.getMessage());
         var16.printStackTrace();
         throw var16;
      } finally {
         if (var6 != null) {
            var6.close();
         }

      }

   }

   private File getTempDir(String var1) throws Exception {
      String var2 = this.getProperty("tempDirectory", (String)null);
      if (var2 == null) {
         throw new Exception("tempDirectory must be specified in transfer.properties");
      } else {
         File var3 = new File(var2, var1);
         if (!var3.exists() && !var3.mkdirs()) {
            throw new Exception("Failed to create working directory " + var3.getAbsolutePath());
         } else {
            return var3;
         }
      }
   }

   private String[] getFilesToBeMerged(File var1, List<RevisionControlled> var2, String var3) throws Exception {
      StringBuilder var4 = new StringBuilder();
      String[] var5 = new String[var2.size() + 1];
      var5[0] = var3;

      for(int var6 = 0; var6 < var2.size(); ++var6) {
         File var7 = new File(var1, this.getOutputFilename((Persistable)var2.get(var6), ".pdf"));
         if (!var7.exists()) {
            if (var4.length() > 0) {
               var4.append(", ");
            }

            var4.append(var7.getAbsolutePath());
         } else {
            var5[var6 + 1] = var7.getAbsolutePath();
         }
      }

      if (var4.length() > 0) {
         throw new Exception("Failed to find pdf(s): " + var4.toString());
      } else {
         return var5;
      }
   }

   private File createDocumentXml(Persistable var1, String var2) throws Exception {
      StringBuilder var3 = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<drawing_list>\n\t<drawing>\n");
      String var4 = this.getOriginalFilename(var1);

      for(int var5 = var4.lastIndexOf(46); var5 > 0; var5 = var4.lastIndexOf(46)) {
         var4 = var4.substring(0, var5);
      }

      var3.append(this.xmlNode(this.getIndent(2), "part_no", var4));
      logger.trace("-->createDocumentXml() part_no=" + var4);
      String var6 = VersionControlHelper.getVersionIdentifier((Versioned)var1).getValue();
      var3.append(this.xmlNode(this.getIndent(2), "revision", var6));
      logger.trace("-->createDocumentXml() revision=" + var6);

      for(String var9 : this.getAttributeList(var1)) {
         String var10 = this.getIbaValue(var1, var9);
         logger.trace("-->createDocumentXml() " + var9 + "=" + var10);
         if (var9.equalsIgnoreCase("part_no")) {
            logger.trace("-->createDocumentXml() skipping part_no, was set previously");
         } else {
            if (var9.equalsIgnoreCase("rev_seq") && (var10 == null || var10.length() <= 0)) {
               logger.trace("-->createDocumentXml() empty rev_seq, settings to '.'");
               var10 = ".";
            }

            var3.append(this.xmlNode(this.getIndent(2), var9.toLowerCase(), var10));
         }
      }

      String var15 = this.getDocState(var1);
      if (var15.equalsIgnoreCase("MAKEREADY")) {
         var15 = "MAKE READY";
      }

      var3.append(this.xmlNode(this.getIndent(2), "rel_level", var15));
      logger.trace("-->createDocumentXml() rel_level=" + var15);
      var3.append("\t</drawing>\n</drawing_list>");
      File var16 = new File(var2, this.getOutputFilename(var1, ".xml"));
      FileWriter var11 = null;

      try {
         var11 = new FileWriter(var16);
         var11.write(var3.toString());
      } finally {
         if (var11 != null) {
            var11.close();
         }

      }

      return var16;
   }

   private boolean downloadPdf(RevisionControlled var1, Set<String> var2, File var3, int var4) throws Exception {
      boolean var5 = true;
      String var6 = this.getOutputFilename(var1, ".pdf");
      if (var2.contains(var6.toLowerCase())) {
         logger.debug("-->downloadPdf() found pdf " + var6);
         return true;
      } else {
         if (var1 instanceof WTDocument) {
            String var7 = this.getOriginalFilename(var1);
            if (!var7.toLowerCase().endsWith(".pdf")) {
               return true;
            }

            File var8 = this.getPrimaryContent(var1, var3.getAbsolutePath());
            if (var8 == null || !var8.exists()) {
               String var10001 = var8 == null ? "null" : var8.getAbsolutePath();
               logger.debug("-->downloadPdf() failed to download pdf " + var10001);
               var5 = false;
            }
         } else if (var1 instanceof EPMDocument) {
            if (this.isPdfViewableValid(var1, var4)) {
               File var9 = this.getPdfViewable(var1, var3.getAbsolutePath());
               if (var9 == null || !var9.exists()) {
                  String var10 = var9 == null ? "null" : var9.getAbsolutePath();
                  logger.debug("-->downloadPdf() failed to download " + var10);
                  var5 = false;
               }
            } else {
               logger.debug("-->downloadPdf() pdf viewable is invalid");
               var5 = false;
            }
         }

         return var5;
      }
   }

   private boolean downloadXml(RevisionControlled var1, Set<String> var2, File var3) throws Exception {
      boolean var4 = true;
      File var5 = this.createDocumentXml(var1, var3.getAbsolutePath());
      if (var5 == null || !var5.exists()) {
         String var10001 = var5 == null ? "null" : var5.getAbsolutePath();
         logger.debug("-->validateObject() failed to create xml " + var10001);
         var4 = false;
      }

      return var4;
   }

   private boolean copyFile(File var1, File var2) {
      try {
         if (var1.exists()) {
            Files.copy(var1, var2);
         }

         return var2.exists();
      } catch (IOException var4) {
         logger.error("-->copyFile() IOException: " + var4.getLocalizedMessage());
         return false;
      }
   }

   private void copyAssociatedFiles(List<RevisionControlled> var1, File var2, String var3) throws Exception {
      StringBuilder var4 = new StringBuilder();

      for(RevisionControlled var6 : var1) {
         String var7 = this.getOutputFilename(var6, ".pdf");
         String var8 = this.getOutputFilename(var6, ".xml");
         File var9 = new File(var2, var7);
         File var10 = new File(var2, var8);
         File var11 = new File(var3, var7);
         File var12 = new File(var3, var8);
         if (!this.copyFile(var9, var11)) {
            if (var4.length() > 0) {
               var4.append(", ");
            }

            var4.append(var9.getAbsolutePath());
         }

         if (!this.copyFile(var10, var12)) {
            if (var4.length() > 0) {
               var4.append(", ");
            }

            var4.append(var10.getAbsolutePath());
         }
      }

      if (var4.length() > 0) {
         throw new Exception("Failed to copy the following files: " + var4.toString());
      }
   }

   // SolidWorks drawings only get their single as-is pdf copied to the
   // output directory - no coversheet merge, no xml.
   private void copySolidWorksPdfs(List<RevisionControlled> var1, File var2, String var3) throws Exception {
      StringBuilder var4 = new StringBuilder();

      for(RevisionControlled var6 : var1) {
         String var7 = this.getOutputFilename(var6, ".pdf");
         File var9 = new File(var2, var7);
         File var11 = new File(var3, var7);
         if (!this.copyFile(var9, var11)) {
            if (var4.length() > 0) {
               var4.append(", ");
            }

            var4.append(var9.getAbsolutePath());
         }
      }

      if (var4.length() > 0) {
         throw new Exception("Failed to copy the following SolidWorks pdf(s): " + var4.toString());
      }
   }

   private void cleanupTempDir(File var1) {
      File[] var2 = var1.listFiles();

      for(int var3 = 0; var3 < var2.length; ++var3) {
         var2[var3].delete();
      }

      var1.delete();
   }

   public String getEmailMsg(Persistable var1) {
      String var2 = "";

      try {
         String var3 = this.getDocNumber(var1);
         List<RevisionControlled> var4 = this.getSortedChangeables(var1);
         var2 = "\r\n" + var4.size() + " Documents from Release Number ";
         var2 = var2 + var3 + " were successfully generated and\r\n";
         var2 = var2 + "placed in the queue to be published in the viewer at http://eng-view . They should be\r\n";
         var2 = var2 + "available within 30 minutes of the generation of this e-mail.\r\n";

         for(RevisionControlled var6 : var4) {
            var2 = var2 + this.getOutputFilename(var6, ".pdf") + "\r\n";
         }
      } catch (Exception var7) {
         logger.error("-->getEmailMsg() Exception: " + var7.getLocalizedMessage());
         var7.printStackTrace();
      }

      return var2;
   }

   public String validateObject(Object var1, int var2) throws Exception {
      Object var3 = null;
      String var4 = "";
      QueryResult var10;
      if (var1 instanceof WTChangeOrder2) {
         WTChangeOrder2 var5 = (WTChangeOrder2)var1;
         Logger var10000 = logger;
         LocalizableMessage var10001 = var5.getDisplayIdentifier();
         var10000.debug("-->validateObject() changeNotice: " + var10001 + "   tryCount: " + var2);
         var10 = ChangeHelper2.service.getChangeablesAfter(var5);
         var4 = this.getDocNumber(var5);
      } else {
         if (!(var1 instanceof PromotionNotice)) {
            throw new Exception("Invalid object type - " + var1);
         }

         PromotionNotice var12 = (PromotionNotice)var1;
         Logger var15 = logger;
         LocalizableMessage var16 = var12.getDisplayIdentifier();
         var15.debug("-->validateObject() promotionNotice: " + var16 + "   tryCount: " + var2);
         var10 = MaturityHelper.service.getPromotionTargets(var12);
         var4 = this.getDocNumber(var12);
      }

      File var13 = this.getTempDir(var4);
      File[] var6 = var13.listFiles();
      HashSet var7 = new HashSet();

      for(int var8 = 0; var8 < var6.length; ++var8) {
         var7.add(var6[var8].getName().toLowerCase());
      }

      boolean var14 = true;

      while(var10.hasMoreElements()) {
         RevisionControlled var9 = (RevisionControlled)var10.nextElement();
         logger.debug("-->validateObject() processing: " + var9.getDisplayIdentifier());
         String var17 = this.getOriginalFilename(var9).toLowerCase();
         if (!var17.endsWith(".drw") && !var17.endsWith(".pdf") && !var17.endsWith(".slddrw")) {
            logger.debug("-->validateObject() skipping non drw/pdf/slddrw file");
         } else {
            if (!this.downloadPdf(var9, var7, var13, var2)) {
               var14 = false;
            }

            if (var17.endsWith(".slddrw")) {
               logger.debug("-->validateObject() skipping xml creation for SolidWorks drawing: " + var9.getDisplayIdentifier());
            } else if (!this.downloadXml(var9, var7, var13)) {
               var14 = false;
            }
         }
      }

      if (var14) {
         return "Success";
      } else {
         return null;
      }
   }

   public String transferObject(Object var1, String var2) throws Exception {
      if (var1 instanceof WTChangeOrder2) {
         WTChangeOrder2 var3 = (WTChangeOrder2)var1;
         logger.debug("-->transferObject() changeNotice: " + var3.getDisplayIdentifier());
         String var4 = this.getDocNumber(var3);
         WTContainer var5 = var3.getContainer();
         String var6 = this.getIbaValue(var5, var2);
         logger.debug("-->transferObject() outputPath: " + var6);
         File var7 = new File(var6);
         if (var7 == null || !var7.exists()) {
            throw new Exception("Output directory doesn't exist: " + var6);
         }

         File var8 = this.getTempDir(var4);
         List<RevisionControlled> var9 = this.getSortedChangeables(var3);
         List<RevisionControlled> var23 = this.getMergeableDrawings(var9);
         List<RevisionControlled> var24 = this.getStandaloneSolidWorksDrawings(var9);
         File var10 = this.getXslFile(var3);
         if (var10 == null) {
            throw new Exception("Failed to find xsl file");
         }

         File var11 = this.createCoversheetPdf(var3, var10, var8.getAbsolutePath());
         if (var11 == null || !var11.exists()) {
            throw new Exception("Failed to create coversheet for " + var4);
         }

         String[] var12 = this.getFilesToBeMerged(var8, var23, var11.getAbsolutePath());
         this.mergePdfFiles(var12, (new File(var7, this.getOutputFilename(var3, ".pdf"))).getAbsolutePath());
         this.copyAssociatedFiles(var23, var8, var6);
         this.copySolidWorksPdfs(var24, var8, var6);
         this.cleanupTempDir(var8);
      } else {
         if (!(var1 instanceof PromotionNotice)) {
            throw new Exception("Invalid object type - " + var1);
         }

         PromotionNotice var13 = (PromotionNotice)var1;
         logger.debug("-->transferObject() promotionNotice: " + var13.getDisplayIdentifier());
         String var14 = this.getDocNumber(var13);
         WTContainer var15 = var13.getContainer();
         String var16 = this.getIbaValue(var15, var2);
         logger.debug("-->transferObject() outputPath: " + var16);
         File var17 = new File(var16);
         if (var17 == null || !var17.exists()) {
            throw new Exception("Output directory doesn't exist: " + var16);
         }

         File var18 = this.getTempDir(var14);
         List<RevisionControlled> var19 = this.getSortedChangeables(var13);
         List<RevisionControlled> var25 = this.getMergeableDrawings(var19);
         List<RevisionControlled> var26 = this.getStandaloneSolidWorksDrawings(var19);
         File var20 = this.getXslFile(var13);
         if (var20 != null) {
            File var21 = this.createCoversheetPdf(var13, var20, var18.getAbsolutePath());
            if (var21 == null || !var21.exists()) {
               throw new Exception("Failed to create coversheet for " + var14);
            }

            String[] var22 = this.getFilesToBeMerged(var18, var25, var21.getAbsolutePath());
            this.mergePdfFiles(var22, (new File(var17, this.getOutputFilename(var13, ".pdf"))).getAbsolutePath());
         } else {
            logger.debug("-->transferObject() no xsl file found. Not creating coversheet");
         }

         this.copyAssociatedFiles(var25, var18, var16);
         this.copySolidWorksPdfs(var26, var18, var16);
         this.cleanupTempDir(var18);
      }

      return "Success";
   }
}
