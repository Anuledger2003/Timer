package com.hni.pdmlink.action;

import com.ptc.core.meta.type.mgmt.server.impl.AttributeMappingRecord;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import org.apache.logging.log4j.Logger;
import wt.access.AccessControlHelper;
import wt.access.AccessPermission;
import wt.admin.AdminDomainRef;
import wt.admin.AdministrativeDomain;
import wt.admin.AdministrativeDomainHelper;
import wt.change2.ChangeActivityIfc;
import wt.change2.ChangeHelper2;
import wt.change2.ChangeRecord2;
import wt.change2.Changeable2;
import wt.change2.ChangeableIfc;
import wt.change2.WTChangeActivity2;
import wt.change2.WTChangeOrder2;
import wt.content.ApplicationData;
import wt.content.ContentHelper;
import wt.content.ContentItem;
import wt.content.FormatContentHolder;
import wt.doc.WTDocument;
import wt.enterprise.RevisionControlled;
import wt.epm.EPMAuthoringAppType;
import wt.epm.EPMDocument;
import wt.epm.attributes.EPMParameterMap;
import wt.epm.util.EPMHelper;
import wt.fc.IdentityHelper;
import wt.fc.ObjectReference;
import wt.fc.ObjectVector;
import wt.fc.PersistenceHelper;
import wt.fc.PersistenceServerHelper;
import wt.fc.QueryResult;
import wt.fc.collections.WTArrayList;
import wt.fc.collections.WTCollection;
import wt.fc.collections.WTHashSet;
import wt.fc.collections.WTKeyedMap;
import wt.fc.collections.WTValuedHashMap;
import wt.folder.Cabinet;
import wt.folder.CabinetBased;
import wt.folder.Folder;
import wt.folder.FolderEntry;
import wt.folder.FolderHelper;
import wt.folder.SubFolder;
import wt.folder.SubFolderIdentity;
import wt.folder.SubFolderReference;
import wt.iba.definition.AttributeDefinitionReference;
import wt.iba.definition.litedefinition.AbstractAttributeDefinizerNodeView;
import wt.iba.definition.litedefinition.AttributeDefDefaultView;
import wt.iba.definition.litedefinition.AttributeDefNodeView;
import wt.iba.definition.litedefinition.AttributeOrgNodeView;
import wt.iba.definition.litedefinition.BooleanDefView;
import wt.iba.definition.litedefinition.FloatDefView;
import wt.iba.definition.litedefinition.IntegerDefView;
import wt.iba.definition.litedefinition.StringDefView;
import wt.iba.definition.service.IBADefinitionHelper;
import wt.iba.definition.service.IBADefinitionService;
import wt.iba.value.DefaultAttributeContainer;
import wt.iba.value.IBAHolder;
import wt.iba.value.IBAHolderReference;
import wt.iba.value.litevalue.AbstractValueView;
import wt.iba.value.litevalue.BooleanValueDefaultView;
import wt.iba.value.litevalue.FloatValueDefaultView;
import wt.iba.value.litevalue.IntegerValueDefaultView;
import wt.iba.value.litevalue.StringValueDefaultView;
import wt.iba.value.service.IBAValueHelper;
import wt.inf.container.WTContainer;
import wt.inf.container.WTContainerRef;
import wt.inf.library.WTLibrary;
import wt.lifecycle.LifeCycleManaged;
import wt.lifecycle.LifeCycleServerHelper;
import wt.lifecycle.LifeCycleState;
import wt.lifecycle.State;
import wt.lifecycle.Transition;
import wt.log4j.LogR;
import wt.maturity.MaturityBaseline;
import wt.maturity.MaturityHelper;
import wt.maturity.PromotionNotice;
import wt.org.WTPrincipal;
import wt.org.WTPrincipalReference;
import wt.org.WTUser;
import wt.ownership.Ownable;
import wt.ownership.OwnershipHelper;
import wt.ownership.OwnershipServerHelper;
import wt.part.WTPart;
import wt.pdmlink.PDMLinkProduct;
import wt.pom.Transaction;
import wt.project.Role;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.representation.RepresentationHelper;
import wt.session.SessionHelper;
import wt.session.SessionServerHelper;
import wt.team.Team;
import wt.team.TeamHelper;
import wt.units.service.MeasurementSystemDefaultView;
import wt.util.WTContext;
import wt.util.WTException;
import wt.util.WTProperties;
import wt.vc.ControlBranch;
import wt.vc.Iterated;
import wt.vc.Mastered;
import wt.vc.VersionControlHelper;
import wt.vc.VersionControlServerHelper;
import wt.vc.baseline.Baseline;
import wt.vc.baseline.BaselineHelper;
import wt.vc.config.OwnershipIndependentLatestConfigSpec;
import wt.vc.wip.WorkInProgressHelper;
import wt.vc.wip.WorkInProgressState;
import wt.vc.wip.Workable;
import wt.workflow.engine.WfEngineHelper;
import wt.workflow.engine.WfProcess;
import wt.workflow.engine.WfState;
import wt.workflow.engine.WfVotingEventAudit;
import wt.workflow.work.WfAssignedActivity;

public class ReleaseSystemHelper {
   private static Logger logger = LogR.getLogger(ReleaseSystemHelper.class.getName());
   private String errorMsg;
   static int[] whereIndicies = new int[]{0};
   static final String CN_AUTO_MOVE_IBA = "AUTO_MOVE";
   static final String CN_CHECKER_IBA = "CHECKER";
   static final String CN_DISPOSITION_IBA = "DISPOSITION";
   static final String CN_MDS_ECR_IBA = "MDS_ECR";
   static final String CN_ORIGINATOR_IBA = "ORIGINATOR";
   static final String CN_PASS_NUMBER_IBA = "PASS_NUMBER";
   static final String CN_PROD_LINE_IBA = "PROD_LINE";
   static final String CN_REASON_IBA = "REASON";
   static final String CN_TARGET_PROD_DATE_IBA = "TARGET_PROD_DATE";
   static final String CN_TARGET_STATE_IBA = "TARGET_STATE";
   static final String CN_SUBMIT_DATE_IBA = "SUBMIT_DATE";
   static final String CN_RELEASE_NO_IBA = "RELEASE_NO";
   static final String CN_PROJECT_IBA = "PROJECT";
   static final String CONTAINER_NEXT_PROJECT_IBA = "NEXT_PROJECT";
   static final String CONTAINER_GROUP_NUMBER_IBA = "GROUP_NUMBER";
   static final String DOC_PART_NO_IBA = "PART_NO";
   static final String PROJ_XML_DESC_IBA = "DESC";
   static final String PROJ_XML_RTP_IBA = "RTP";
   static final String DRW_REV_SEQ_IBA = "REV_SEQ";
   static final String DRW_WHO_IBA = "WHO";
   static final String DRW_REL_WHO_IBA = "REL_WHO";
   static final String DRW_PDM_EC_NO_IBA = "PDM_EC_NO";
   static final String DRW_REL_IBA = "REL";
   static final String DRW_REL_DATE_IBA = "REL_DATE";
   static final String SUFFIX_DONE = "-done";
   static final String SUFFIX_PROCESSING = "-processing";

   public static String getFormattedDate(Timestamp var0) {
      SimpleDateFormat var1 = new SimpleDateFormat("dd-MMM-yyyy");
      TimeZone var2 = TimeZone.getTimeZone("America/Chicago");
      Calendar var3 = Calendar.getInstance();
      var3.setTime(var0);
      var3.setTimeZone(var2);
      var1.setTimeZone(var2);
      return var1.format(var3.getTime());
   }

   public static WfProcess getChangeNoticeProcess(WTChangeOrder2 var0) {
      WfProcess var1 = null;

      try {
         QueryResult var2 = WfEngineHelper.service.getAssociatedProcesses(var0, (WfState)null, (WTContainerRef)null);

         while(var2.hasMoreElements()) {
            WfProcess var3 = (WfProcess)var2.nextElement();
            if (var3 != null) {
               if (var1 == null) {
                  var1 = var3;
               } else if (PersistenceHelper.getCreateStamp(var3).after(PersistenceHelper.getCreateStamp(var1))) {
                  var1 = var3;
               }
            }
         }
      } catch (WTException var4) {
         var4.printStackTrace();
      }

      return var1;
   }

   public static Team getChangeNoticeTeam(WTChangeOrder2 var0) {
      WfProcess var1 = getChangeNoticeProcess(var0);
      return var1 == null ? null : (Team)var1.getTeamId().getObject();
   }

   public static Map getChangeNoticeTeamMap(WTChangeOrder2 var0) {
      HashMap var1 = null;
      Team var2 = getChangeNoticeTeam(var0);
      if (var2 == null) {
         return null;
      } else {
         try {
            var1 = TeamHelper.service.findAllParticipantsByRole(var2);
         } catch (WTException var4) {
            var4.printStackTrace();
         }

         return var1;
      }
   }

   public static String getActivityComments(Object var0) {
      String var1 = "";

      try {
         if (var0 instanceof ObjectReference) {
            var0 = ((ObjectReference)var0).getObject();
            if (var0 != null && var0 instanceof WfAssignedActivity) {
               WfAssignedActivity var2 = (WfAssignedActivity)var0;
               var1 = String.valueOf(var2.getKey());
            }
         }

         if (var0 instanceof String) {
            long var10 = Long.parseLong((String)var0);
            QuerySpec var4 = new QuerySpec(WfVotingEventAudit.class);
            var4.appendWhere(new SearchCondition(WfVotingEventAudit.class, "activityKey", "=", var10), whereIndicies);
            QueryResult var5 = PersistenceHelper.manager.find(var4);

            while(var5.hasMoreElements()) {
               var0 = var5.nextElement();
               if (var0 instanceof WfVotingEventAudit) {
                  WfVotingEventAudit var6 = (WfVotingEventAudit)var0;
                  String var7 = var6.getUserComment();
                  var1 = var1 + var7 + "\r\n";
               }
            }
         }
      } catch (Exception var8) {
      }

      logger.error("-->getActivityComments() result: " + var1);
      return var1;
   }

   private static int getLocation(CabinetBased var0) throws WTException {
      if (FolderHelper.inPersonalCabinet(var0)) {
         return !PersistenceHelper.isEquivalent(FolderHelper.service.getPersonalCabinet((WTPrincipal)null), FolderHelper.getCabinetReference(var0).getObject()) ? 2 : 1;
      } else {
         return 3;
      }
   }

   private static Ownable takeOwnership(Ownable var0) throws WTException {
      if (!PersistenceHelper.isPersistent(var0)) {
         return var0;
      } else {
         if (var0 instanceof CabinetBased) {
            int var1 = getLocation((CabinetBased)var0);
            if (var1 == 3 || var1 == 2) {
               var0 = (Ownable)PersistenceHelper.manager.refresh(var0);
               OwnershipServerHelper.service.changeOwner(var0, SessionHelper.manager.getPrincipal(), true);
            }
         }

         return var0;
      }
   }

   private static Ownable releaseOwnership(Ownable var0) throws WTException {
      if (!PersistenceHelper.isPersistent(var0)) {
         return var0;
      } else {
         if (var0 instanceof CabinetBased && var0 instanceof FolderEntry) {
            var0 = (Ownable)PersistenceHelper.manager.refresh(var0);
            int var1 = getLocation((CabinetBased)var0);
            if (var1 == 3) {
               OwnershipServerHelper.service.changeOwner(var0, (WTPrincipal)null, true);
            } else if (var1 == 2) {
               Folder var2 = FolderHelper.getFolder((FolderEntry)var0);
               WTPrincipal var3 = OwnershipHelper.getOwner((Ownable)var2);
               OwnershipServerHelper.service.changeOwner(var0, var3, true);
            }
         }

         return var0;
      }
   }

   public static void printObjectInfo(Iterated var0, String var1) {
      if (var0 instanceof EPMDocument) {
         EPMDocument var2 = (EPMDocument)var0;
         String var3 = "";

         try {
            var3 = VersionControlHelper.getVersionIdentifier(var2).getValue();
            var3 = var3 + "." + VersionControlHelper.getIterationIdentifier(var2).getValue();
         } catch (Exception var7) {
         }

         WorkInProgressState var4 = var2.getCheckoutInfo().getState();
         String var5 = var2.getContainerReference().getName();
         logger.error("-->" + var1 + ": " + var2.getCADName() + " " + var3 + " " + var4 + " " + var5);
      }

      if (var0 instanceof WTDocument) {
         WTDocument var8 = (WTDocument)var0;
         String var10 = "";

         try {
            var10 = VersionControlHelper.getVersionIdentifier(var8).getValue();
            var10 = var10 + "." + VersionControlHelper.getIterationIdentifier(var8).getValue();
         } catch (Exception var6) {
         }

         WorkInProgressState var12 = var8.getCheckoutInfo().getState();
         String var13 = var8.getContainerReference().getName();
         logger.error("-->" + var1 + ": " + var8.getNumber() + " " + var10 + " " + var12 + " " + var13);
      }

   }

   public static Iterated getLatestIteration(Mastered var0) throws WTException {
      try {
         QueryResult var1 = VersionControlHelper.service.allIterationsOf(var0);
         ObjectVector var2 = new ObjectVector();

         while(var1.hasMoreElements()) {
            Object var3 = null;

            try {
               var8 = (Iterated)var1.nextElement();
               printObjectInfo(var8, "initialResult");
            } catch (Exception var5) {
               logger.debug("-->getLatestIteration() nextElement() Exception: " + var5.getLocalizedMessage());
               continue;
            }

            if (var8 == null) {
               logger.debug("-->getLatestIteration() iterated is null");
            } else {
               try {
                  WTContainer var4 = null;
                  if (var8 instanceof WTPart) {
                     var4 = ((WTPart)var8).getContainer();
                  }

                  if (var8 instanceof WTDocument) {
                     var4 = ((WTDocument)var8).getContainer();
                  }

                  if (var8 instanceof EPMDocument) {
                     var4 = ((EPMDocument)var8).getContainer();
                  }

                  if (var4 != null && !isProductOrLibrary(var4)) {
                     printObjectInfo(var8, "skippedNonPDM");
                     continue;
                  }
               } catch (Exception var6) {
                  logger.error("-->getLatestIteration() getContainer() Exception: " + var6.getLocalizedMessage());
                  continue;
               }

               if (var8 instanceof Workable && WorkInProgressHelper.isWorkingCopy((Workable)var8)) {
                  printObjectInfo(var8, "workingCopyReplacement");
               } else {
                  var2.addElement(var8);
               }
            }
         }

         QueryResult var9 = new QueryResult(var2);
         Iterated var11 = null;

         for(QueryResult var10 = (new OwnershipIndependentLatestConfigSpec()).process(var9); var10.hasMoreElements(); var11 = (Iterated)var10.nextElement()) {
         }

         return var11;
      } catch (Exception var7) {
         logger.error("-->getLatestIteration() exception: " + var7.getMessage());
         return null;
      }
   }

   private static boolean isProductOrLibrary(WTContainer var0) {
      if (var0 instanceof PDMLinkProduct) {
         return true;
      } else {
         return var0 instanceof WTLibrary;
      }
   }

   public static String getReleaseProp(Properties var0, String var1) throws WTException {
      String var2 = var0.getProperty(var1);
      if (var2 != null && var2.length() > 0) {
         return var2;
      } else {
         throw new WTException(var1 + " not set in codebase/com/hni/pdmlink/release.properties");
      }
   }

   public static Properties getReleaseProperties() throws WTException {
      WTProperties var0 = null;

      try {
         var0 = WTProperties.getLocalProperties();
      } catch (Exception var7) {
      }

      if (var0 == null) {
         throw new WTException("Failed to read wt.properties");
      } else {
         String var1 = var0.getProperty("wt.home");
         if (var1 != null && var1.length() > 0) {
            var1 = var1 + "/codebase/com/hni/pdmlink/release.properties";
            BufferedReader var2 = null;
            Properties var3 = new Properties();
            File var4 = new File(var1);
            if (!var4.exists()) {
               throw new WTException("Failed to find " + var1);
            } else {
               try {
                  var2 = new BufferedReader(new FileReader(var4));

                  String var5;
                  while((var5 = var2.readLine()) != null) {
                     var5 = var5.trim();
                     if (var5.indexOf(35) != 0 && var5.indexOf(61) > 0) {
                        String[] var6 = var5.split("=", 2);
                        var3.put(var6[0].trim(), var6[1].trim());
                     }
                  }

                  var2.close();
               } catch (Exception var8) {
               }

               return var3;
            }
         } else {
            throw new WTException("Failed to read wt.home");
         }
      }
   }

   public static SubFolder getChangeNoticeFolder(WTContainerRef var0) throws WTException {
      logger.error("-->ReleaseSystemHelper.getChangeNoticeFolder() containerRef: " + var0.getName());
      Properties var1 = getReleaseProperties();
      String var2 = var1.getProperty("changeNoticeFolder");
      if (var2 == null) {
         throw new WTException("-->ReleaseSystemHelper.getChangeNoticeFolder() changeNoticeFolder not set in codebase/com/hni/pdmlink/release.properties");
      } else {
         if (!var2.startsWith("/")) {
            var2 = "/" + var2;
         }

         var2 = "/Default" + var2;
         logger.error("-->ReleaseSystemHelper.getChangeNoticeFolder() changeNoticeFolder: " + var2);
         SubFolder var3 = null;

         try {
            var3 = (SubFolder)FolderHelper.service.getFolder(var2, var0);
         } catch (Exception var5) {
         }

         if (var3 == null) {
            throw new WTException("-->ReleaseSystemHelper.getChangeNoticeFolder() <" + var2 + "> where the Release Objects should reside was not found");
         } else {
            return var3;
         }
      }
   }

   public static SubFolder getProjectReleaseFolder(WTChangeOrder2 var0, String var1) throws WTException {
      String var2 = var0.getName();
      logger.error("-->ReleaseSystemHelper.getProjectReleaseFolder() changeNotice: " + var2);
      Properties var3 = getReleaseProperties();
      String var4 = var3.getProperty("baseProjectFolder");
      if (var4 == null) {
         throw new WTException("-->ReleaseSystemHelper.getProjectReleaseFolder() baseProjectFolder not set in codebase/com/hni/pdmlink/release.properties");
      } else {
         if (!var4.startsWith("/")) {
            var4 = "/" + var4;
         }

         var4 = "/Default" + var4;
         if (!var4.endsWith("/")) {
            var4 = var4 + "/";
         }

         if (var2.indexOf(45) < 0) {
            throw new WTException("-->ReleaseSystemHelper.getProjectReleaseFolder() Change Notice <" + var2 + "> appears to be in an invalid folder");
         } else {
            String[] var5 = var2.split("-");
            if (var5.length < 3) {
               throw new WTException("-->ReleaseSystemHelper.getProjectReleaseFolder() Change Notice <" + var2 + "> appears to be in an invalid folder");
            } else {
               var4 = var4 + var5[0] + "-" + var5[1] + "/" + var5[2];
               if (var1 != null) {
                  var4 = var4 + var1;
               }

               logger.error("-->ReleaseSystemHelper.getProjectReleaseFolder() projectFolder: " + var4);
               SubFolder var6 = null;

               try {
                  var6 = (SubFolder)FolderHelper.service.getFolder(var4, var0.getContainerReference());
               } catch (Exception var8) {
               }

               if (var6 == null) {
                  throw new WTException("-->ReleaseSystemHelper.getProjectReleaseFolder() <" + var4 + "> where the Project Objects should reside was not found");
               } else {
                  return var6;
               }
            }
         }
      }
   }

   public static SubFolder getFolderFromName(WTContainerRef var0, String var1) throws WTException {
      if (!var1.startsWith("/")) {
         var1 = "/" + var1;
      }

      for(var1 = "/Default" + var1; var1.endsWith("/"); var1 = var1.substring(0, var1.length() - 1)) {
      }

      logger.error("-->ReleaseSystemHelper.getFolderFromName() folderPath: " + var1);
      SubFolder var2 = null;

      try {
         var2 = (SubFolder)FolderHelper.service.getFolder(var1, var0);
      } catch (Exception var4) {
      }

      if (var2 == null) {
         throw new WTException("-->ReleaseSystemHelper.getFolderFromName() folder <" + var1 + "> not found");
      } else {
         return var2;
      }
   }

   public static String getIbaFromObject(IBAHolder var0, String var1) {
      String var2 = null;

      try {
         Locale var3 = WTContext.getContext().getLocale();
         var0 = IBAValueHelper.service.refreshAttributeContainer(var0, (Object)null, var3, (MeasurementSystemDefaultView)null);
         DefaultAttributeContainer var4 = (DefaultAttributeContainer)var0.getAttributeContainer();
         AbstractValueView[] var5 = var4.getAttributeValues();

         for(int var6 = 0; var6 < var5.length; ++var6) {
            String var7 = var5[var6].getDefinition().getName();
            if (var7.equals(var1)) {
               if (var5[var6] instanceof StringValueDefaultView) {
                  StringValueDefaultView var8 = (StringValueDefaultView)var5[var6];
                  var2 = var8.getValue();
               } else if (var5[var6] instanceof IntegerValueDefaultView) {
                  IntegerValueDefaultView var11 = (IntegerValueDefaultView)var5[var6];
                  var2 = Long.toString(var11.getValue());
               } else if (var5[var6] instanceof BooleanValueDefaultView) {
                  BooleanValueDefaultView var12 = (BooleanValueDefaultView)var5[var6];
                  var2 = var12.isValue() ? "True" : "False";
               } else if (var5[var6] instanceof FloatValueDefaultView) {
                  FloatValueDefaultView var13 = (FloatValueDefaultView)var5[var6];
                  var2 = Double.toString(var13.getValue());
               }
            }
         }
      } catch (Exception var9) {
         var9.printStackTrace();
         logger.error("-->ReleaseSystemHelper.getIbaFromObject() WTException: " + var9.getMessage());
      }

      return var2;
   }

   private static AttributeDefDefaultView processAttributeOrgNodeView(AttributeOrgNodeView var0, String var1) throws WTException, RemoteException {
      IBADefinitionService var2 = IBADefinitionHelper.service;
      AbstractAttributeDefinizerNodeView[] var3 = var2.getAttributeChildren(var0);

      for(int var4 = 0; var4 < var3.length; ++var4) {
         if (var3[var4] instanceof AttributeOrgNodeView) {
            AttributeDefDefaultView var5 = processAttributeOrgNodeView((AttributeOrgNodeView)var3[var4], var1);
            if (var5 != null) {
               return var5;
            }
         }

         if (var3[var4] instanceof AttributeDefNodeView) {
            AttributeDefDefaultView var6 = var2.getAttributeDefDefaultView((AttributeDefNodeView)var3[var4]);
            if (var6.getName().equalsIgnoreCase(var1)) {
               return var6;
            }
         }
      }

      return null;
   }

   private static AttributeDefDefaultView findAttributeDefDefaultView(String var0) throws WTException, RemoteException {
      IBADefinitionService var1 = IBADefinitionHelper.service;
      AttributeOrgNodeView[] var2 = var1.getAttributeOrganizerRoots();

      for(int var3 = 0; var3 < var2.length; ++var3) {
         AttributeDefDefaultView var4 = processAttributeOrgNodeView(var2[var3], var0);
         if (var4 != null) {
            return var4;
         }
      }

      return null;
   }

   public static boolean setIbaOnDocument(WTDocument var0, String var1, String var2) {
      boolean var3 = false;
      if (var2 == null) {
         var2 = "";
      }

      try {
         var0 = (WTDocument)takeOwnership(var0);
         var3 = true;
         Locale var5 = WTContext.getContext().getLocale();
         IBAHolder var4 = IBAValueHelper.service.refreshAttributeContainer(var0, (Object)null, var5, (MeasurementSystemDefaultView)null);
         DefaultAttributeContainer var6 = (DefaultAttributeContainer)var4.getAttributeContainer();
         AbstractValueView[] var7 = var6.getAttributeValues();

         for(int var8 = 0; var8 < var7.length; ++var8) {
            String var9 = var7[var8].getDefinition().getName();
            if (var9.equals(var1)) {
               if (var7[var8] instanceof StringValueDefaultView) {
                  StringValueDefaultView var10 = (StringValueDefaultView)var7[var8];
                  var10.setValue(var2);
                  var6.updateAttributeValue(var10);
               } else if (var7[var8] instanceof BooleanValueDefaultView) {
                  BooleanValueDefaultView var24 = (BooleanValueDefaultView)var7[var8];
                  if (!var2.equalsIgnoreCase("True") && !var2.equalsIgnoreCase("Yes")) {
                     if (!var2.equalsIgnoreCase("False") && !var2.equalsIgnoreCase("No") && !var2.equalsIgnoreCase("0")) {
                        var24.setValue(true);
                     } else {
                        var24.setValue(false);
                     }
                  } else {
                     var24.setValue(true);
                  }

                  var6.updateAttributeValue(var24);
               } else if (var7[var8] instanceof FloatValueDefaultView) {
                  FloatValueDefaultView var25 = (FloatValueDefaultView)var7[var8];
                  var25.setValue(Double.valueOf(var2));
                  var6.updateAttributeValue(var25);
               } else {
                  if (!(var7[var8] instanceof IntegerValueDefaultView)) {
                     throw new WTException("Unsupported attribute type");
                  }

                  IntegerValueDefaultView var26 = (IntegerValueDefaultView)var7[var8];
                  var26.setValue((long)Integer.valueOf(var2));
                  var6.updateAttributeValue(var26);
               }

               var0.getCheckoutInfo().setState(WorkInProgressState.WORKING);
               IBAValueHelper.service.updateIBAHolder(var0, (Object)null, (Locale)null, (MeasurementSystemDefaultView)null);
               var0.getCheckoutInfo().setState(WorkInProgressState.CHECKED_IN);
               PersistenceServerHelper.manager.update(var0);
               ControlBranch var27 = VersionControlServerHelper.getControlBranch(var0);
               if (var27 != null) {
                  var27.setUntrustedBusinessFields(var0);
                  PersistenceServerHelper.manager.update(var27);
               }

               var0 = (WTDocument)releaseOwnership(var0);
               return true;
            }
         }

         AttributeDefDefaultView var18 = findAttributeDefDefaultView(var1);
         if (var18 == null) {
            throw new WTException("Attribute not defined");
         } else {
            if (var18 instanceof StringDefView) {
               StringValueDefaultView var19 = new StringValueDefaultView((StringDefView)var18);
               var19.setValue(var2);
               var6.addAttributeValue(var19);
            } else if (var18 instanceof BooleanDefView) {
               BooleanValueDefaultView var20 = new BooleanValueDefaultView((BooleanDefView)var18);
               if (!var2.equalsIgnoreCase("True") && !var2.equalsIgnoreCase("Yes")) {
                  var20.setValue(false);
               } else {
                  var20.setValue(true);
               }

               var6.addAttributeValue(var20);
            } else if (var18 instanceof FloatDefView) {
               FloatValueDefaultView var21 = new FloatValueDefaultView((FloatDefView)var18);
               var21.setValue(Double.valueOf(var2));
               var6.addAttributeValue(var21);
            } else {
               if (!(var18 instanceof IntegerDefView)) {
                  throw new WTException("Unsupported attribute type");
               }

               IntegerValueDefaultView var22 = new IntegerValueDefaultView((IntegerDefView)var18);
               var22.setValue((long)Integer.valueOf(var2));
               var6.addAttributeValue(var22);
            }

            var0.getCheckoutInfo().setState(WorkInProgressState.WORKING);
            IBAValueHelper.service.updateIBAHolder(var0, (Object)null, (Locale)null, (MeasurementSystemDefaultView)null);
            var0.getCheckoutInfo().setState(WorkInProgressState.CHECKED_IN);
            PersistenceServerHelper.manager.update(var0);
            ControlBranch var23 = VersionControlServerHelper.getControlBranch(var0);
            if (var23 != null) {
               var23.setUntrustedBusinessFields(var0);
               PersistenceServerHelper.manager.update(var23);
            }

            var0 = (WTDocument)releaseOwnership(var0);
            return true;
         }
      } catch (Exception var12) {
         var12.printStackTrace();
         logger.error("-->ReleaseSystemHelper.setIbaOnDocument() WTException: " + var12.getMessage());

         try {
            if (var3) {
               var0 = (WTDocument)releaseOwnership(var0);
            }
         } catch (Exception var11) {
         }

         return false;
      }
   }

   private static String getAttributeMapping(AttributeDefinitionReference var0, String var1) throws Exception {
      long var2 = 0L;
      String var4 = null;
      if (var0 == null) {
         return null;
      } else {
         QuerySpec var5 = new QuerySpec(AttributeMappingRecord.class);
         QueryResult var6 = PersistenceHelper.manager.find(var5);

         while(var6.hasMoreElements()) {
            AttributeMappingRecord var7 = (AttributeMappingRecord)var6.nextElement();
            AttributeDefinitionReference var8 = var7.getAttributeDefinition();
            if (var8.equals(var0) && var7.getContext().equalsIgnoreCase(var1)) {
               long var9 = var7.getTypeDefinitionReference().getObjectId().getId();
               if (var9 > var2) {
                  var4 = var7.getValue();
                  var2 = var9;
               }
            }
         }

         return var4;
      }
   }

   public static boolean setIbaOnDocument(EPMDocument var0, String var1, String var2) {
      boolean var3 = false;
      if (var2 == null) {
         var2 = "";
      }

      try {
         var0 = (EPMDocument)takeOwnership(var0);
         var3 = true;
         WTArrayList var5 = new WTArrayList();
         Locale var6 = WTContext.getContext().getLocale();
         IBAHolder var4 = IBAValueHelper.service.refreshAttributeContainer(var0, (Object)null, var6, (MeasurementSystemDefaultView)null);
         DefaultAttributeContainer var7 = (DefaultAttributeContainer)var4.getAttributeContainer();
         AbstractValueView[] var8 = var7.getAttributeValues();
         IBAHolderReference var9 = IBAHolderReference.newIBAHolderReference(var4);
         EPMAuthoringAppType var10 = var0.getAuthoringApplication();

         for(int var11 = 0; var11 < var8.length; ++var11) {
            String var12 = var8[var11].getDefinition().getName();
            if (var12.equals(var1)) {
               if (var8[var11] instanceof StringValueDefaultView) {
                  StringValueDefaultView var13 = (StringValueDefaultView)var8[var11];
                  var13.setValue(var2);
                  var7.updateAttributeValue(var13);
               } else if (var8[var11] instanceof BooleanValueDefaultView) {
                  BooleanValueDefaultView var28 = (BooleanValueDefaultView)var8[var11];
                  if (!var2.equalsIgnoreCase("True") && !var2.equalsIgnoreCase("Yes")) {
                     if (!var2.equalsIgnoreCase("False") && !var2.equalsIgnoreCase("No") && !var2.equalsIgnoreCase("0")) {
                        var28.setValue(true);
                     } else {
                        var28.setValue(false);
                     }
                  } else {
                     var28.setValue(true);
                  }

                  var7.updateAttributeValue(var28);
               } else if (var8[var11] instanceof FloatValueDefaultView) {
                  FloatValueDefaultView var29 = (FloatValueDefaultView)var8[var11];
                  var29.setValue(Double.valueOf(var2));
                  var7.updateAttributeValue(var29);
               } else {
                  if (!(var8[var11] instanceof IntegerValueDefaultView)) {
                     throw new Exception("Unsupported attribute type");
                  }

                  IntegerValueDefaultView var30 = (IntegerValueDefaultView)var8[var11];
                  var30.setValue((long)Integer.valueOf(var2));
                  var7.updateAttributeValue(var30);
               }

               var0.getCheckoutInfo().setState(WorkInProgressState.WORKING);
               IBAValueHelper.service.updateIBAHolder(var0, (Object)null, (Locale)null, (MeasurementSystemDefaultView)null);
               var0.getCheckoutInfo().setState(WorkInProgressState.CHECKED_IN);
               PersistenceServerHelper.manager.update(var0);
               ControlBranch var31 = VersionControlServerHelper.getControlBranch(var0);
               if (var31 != null) {
                  var31.setUntrustedBusinessFields(var0);
                  PersistenceServerHelper.manager.update(var31);
               }

               var0 = (EPMDocument)releaseOwnership(var0);
               return true;
            }
         }

         AttributeDefDefaultView var22 = findAttributeDefDefaultView(var1);
         if (var22 == null) {
            throw new WTException("Attribute not defined");
         } else {
            if (var22 instanceof StringDefView) {
               StringValueDefaultView var23 = new StringValueDefaultView((StringDefView)var22);
               var23.setValue(var2);
               var7.addAttributeValue(var23);
               AttributeDefinitionReference var32 = AttributeDefinitionReference.newAttributeDefinitionReference(var22);
               String var14 = getAttributeMapping(var32, var10.toString());
               if (var14 != null) {
                  var5.add(EPMParameterMap.newEPMParameterMap(var9, var32, var14));
               }
            } else if (var22 instanceof BooleanDefView) {
               BooleanValueDefaultView var24 = new BooleanValueDefaultView((BooleanDefView)var22);
               if (!var2.equalsIgnoreCase("True") && !var2.equalsIgnoreCase("Yes")) {
                  if (!var2.equalsIgnoreCase("False") && !var2.equalsIgnoreCase("No") && !var2.equalsIgnoreCase("0")) {
                     var24.setValue(true);
                  } else {
                     var24.setValue(false);
                  }
               } else {
                  var24.setValue(true);
               }

               var7.addAttributeValue(var24);
            } else if (var22 instanceof FloatDefView) {
               FloatValueDefaultView var25 = new FloatValueDefaultView((FloatDefView)var22);
               var25.setValue(Double.valueOf(var2));
               var7.addAttributeValue(var25);
            } else {
               if (!(var22 instanceof IntegerDefView)) {
                  throw new Exception("Unsupported attribute type");
               }

               IntegerValueDefaultView var26 = new IntegerValueDefaultView((IntegerDefView)var22);
               var26.setValue((long)Integer.valueOf(var2));
               var7.addAttributeValue(var26);
            }

            var0.getCheckoutInfo().setState(WorkInProgressState.WORKING);
            IBAValueHelper.service.updateIBAHolder(var0, (Object)null, (Locale)null, (MeasurementSystemDefaultView)null);
            if (!var5.isEmpty()) {
               PersistenceHelper.manager.store(var5);
            }

            var0.getCheckoutInfo().setState(WorkInProgressState.CHECKED_IN);
            PersistenceServerHelper.manager.update(var0);
            ControlBranch var27 = VersionControlServerHelper.getControlBranch(var0);
            if (var27 != null) {
               var27.setUntrustedBusinessFields(var0);
               PersistenceServerHelper.manager.update(var27);
            }

            var0 = (EPMDocument)releaseOwnership(var0);
            return true;
         }
      } catch (Exception var16) {
         var16.printStackTrace();
         logger.error("-->ReleaseSystemHelper.setIbaOnDocument() WTException: " + var16.getMessage());

         try {
            if (var3) {
               var0 = (EPMDocument)releaseOwnership(var0);
            }
         } catch (Exception var15) {
         }

         return false;
      }
   }

   private static IBAHolder setIbaOnObject(IBAHolder var0, String var1, String var2) {
      if (var2 == null) {
         var2 = "";
      }

      try {
         Locale var3 = WTContext.getContext().getLocale();
         var0 = IBAValueHelper.service.refreshAttributeContainer(var0, (Object)null, var3, (MeasurementSystemDefaultView)null);
         DefaultAttributeContainer var4 = (DefaultAttributeContainer)var0.getAttributeContainer();
         AbstractValueView[] var5 = var4.getAttributeValues();

         for(int var6 = 0; var6 < var5.length; ++var6) {
            String var7 = var5[var6].getDefinition().getName();
            if (var7.equals(var1)) {
               if (var5[var6] instanceof StringValueDefaultView) {
                  StringValueDefaultView var8 = (StringValueDefaultView)var5[var6];
                  var8.setValue(var2);
                  var4.updateAttributeValue(var8);
               } else if (var5[var6] instanceof BooleanValueDefaultView) {
                  BooleanValueDefaultView var18 = (BooleanValueDefaultView)var5[var6];
                  if (!var2.equalsIgnoreCase("True") && !var2.equalsIgnoreCase("Yes")) {
                     if (!var2.equalsIgnoreCase("False") && !var2.equalsIgnoreCase("No") && !var2.equalsIgnoreCase("0")) {
                        var18.setValue(true);
                     } else {
                        var18.setValue(false);
                     }
                  } else {
                     var18.setValue(true);
                  }

                  var4.updateAttributeValue(var18);
               } else if (var5[var6] instanceof FloatValueDefaultView) {
                  FloatValueDefaultView var19 = (FloatValueDefaultView)var5[var6];
                  var19.setValue(Double.valueOf(var2));
                  var4.updateAttributeValue(var19);
               } else {
                  if (!(var5[var6] instanceof IntegerValueDefaultView)) {
                     throw new WTException("Unsupported attribute type");
                  }

                  IntegerValueDefaultView var20 = (IntegerValueDefaultView)var5[var6];
                  var20.setValue((long)Integer.valueOf(var2));
                  var4.updateAttributeValue(var20);
               }

               var0 = IBAValueHelper.service.updateIBAHolder(var0, (Object)null, (Locale)null, (MeasurementSystemDefaultView)null);
               return var0;
            }
         }

         AttributeDefDefaultView var13 = findAttributeDefDefaultView(var1);
         if (var13 == null) {
            throw new WTException("Attribute not defined");
         } else {
            if (var13 instanceof StringDefView) {
               StringValueDefaultView var14 = new StringValueDefaultView((StringDefView)var13);
               var14.setValue(var2);
               var4.addAttributeValue(var14);
            } else if (var13 instanceof BooleanDefView) {
               BooleanValueDefaultView var15 = new BooleanValueDefaultView((BooleanDefView)var13);
               if (!var2.equalsIgnoreCase("True") && !var2.equalsIgnoreCase("Yes")) {
                  var15.setValue(false);
               } else {
                  var15.setValue(true);
               }

               var4.addAttributeValue(var15);
            } else if (var13 instanceof FloatDefView) {
               FloatValueDefaultView var16 = new FloatValueDefaultView((FloatDefView)var13);
               var16.setValue(Double.valueOf(var2));
               var4.addAttributeValue(var16);
            } else {
               if (!(var13 instanceof IntegerDefView)) {
                  throw new WTException("Unsupported attribute type");
               }

               IntegerValueDefaultView var17 = new IntegerValueDefaultView((IntegerDefView)var13);
               var17.setValue((long)Integer.valueOf(var2));
               var4.addAttributeValue(var17);
            }

            var0 = IBAValueHelper.service.updateIBAHolder(var0, (Object)null, (Locale)null, (MeasurementSystemDefaultView)null);
            return var0;
         }
      } catch (Exception var9) {
         var9.printStackTrace();
         logger.error("-->ReleaseSystemHelper.setIbaOnObject() WTException: " + var9.getMessage());
         return null;
      }
   }

   public static WTChangeOrder2 setIbaOnChangeNotice(WTChangeOrder2 var0, String var1, String var2) throws WTException {
      logger.error("-->ReleaseSystemHelper.setIbaOnChangeNotice() " + var1 + "=" + var2);
      var0 = (WTChangeOrder2)setIbaOnObject(var0, var1, var2);
      if (var0 == null) {
         throw new WTException("setIbaOnChangeNotice failed for " + var1);
      } else {
         return var0;
      }
   }

   public static WTContainer setIbaOnContainer(WTContainer var0, String var1, String var2) throws WTException {
      logger.error("-->ReleaseSystemHelper.setIbaOnContainer() " + var1 + "=" + var2);
      var0 = (WTContainer)setIbaOnObject((IBAHolder)var0, var1, var2);
      if (var0 == null) {
         throw new WTException("setIbaOnContainer failed for " + var1);
      } else {
         return var0;
      }
   }

   private static AdminDomainRef getDomainRefOfFolder(Folder var0) {
      AdminDomainRef var1 = null;
      if (var0 instanceof SubFolder) {
         var1 = ((SubFolder)var0).getDomainRef();
      } else {
         var1 = ((Cabinet)var0).getDomainRef();
      }

      return var1;
   }

   public static int releaseSystemFolderDepth(Object var0) {
      try {
         if (var0 instanceof SubFolder) {
            SubFolder var1 = (SubFolder)PersistenceHelper.manager.refresh((SubFolder)var0);
            Properties var2 = getReleaseProperties();
            String var3 = var2.getProperty("baseProjectFolder");
            if (var3 == null) {
               logger.error("-->ReleaseSystemHelper.releaseSystemFolderDepth() baseProjectFolder not set in codebase/com/hni/pdmlink/release.properties");
               return -1;
            }

            var3 = "/Default/" + var3;
            if (!var3.endsWith("/")) {
               var3 = var3 + "/";
            }

            String var4 = var1.getFolderPath();
            if (!var4.endsWith("/")) {
               var4 = var4 + "/";
            }

            if (!var4.startsWith(var3)) {
               return -1;
            }

            String var5 = getIbaFromObject((IBAHolder)var1.getContainer(), "NEXT_PROJECT");
            if (var5 != null && var5.length() > 0) {
               String var6 = var4.substring(var3.length());
               int var7 = 0;

               for(int var8 = 0; var8 < var6.length(); ++var8) {
                  if (var6.charAt(var8) == '/') {
                     ++var7;
                  }
               }

               WTPrincipal var17 = SessionHelper.getPrincipal();
               AdminDomainRef var9 = getDomainRefOfFolder(var1);
               String var10 = var2.getProperty("releaseForAccessControl", "PRELIMINARY");
               State var11 = State.toState(var10);
               String var12 = var2.getProperty("permissionForAccessControl", "10");
               AccessPermission var13 = AccessPermission.toAccessPermission(var12);
               String var14 = var2.getProperty("classForAccessControl", "wt.epm.EPMDocument");
               if (!AccessControlHelper.manager.hasAccess(var17, var14, var9, var11, var13)) {
                  return -1;
               }

               return var7;
            }

            logger.error("-->ReleaseSystemHelper.releaseSystemFolderDepth() No container attribute specified for the next project number");
            return -1;
         }
      } catch (Exception var15) {
         var15.printStackTrace();
         logger.error("ReleaseSystemHelper.releaseSystemFolderDepth() WTException: " + var15.getMessage());
      }

      return -1;
   }

   private static AdminDomainRef findDomainRef(long var0, boolean var2) throws WTException {
      QuerySpec var3 = new QuerySpec(AdministrativeDomain.class);
      var3.appendWhere(new SearchCondition(AdministrativeDomain.class, "containerReference.key.id", "=", var0), whereIndicies);
      var3.appendAnd();
      var3.appendWhere(new SearchCondition(AdministrativeDomain.class, "name", "=", var2 ? "Secure" : "Default", true), whereIndicies);
      QueryResult var4 = PersistenceHelper.manager.find(var3);
      if (var4.hasMoreElements()) {
         AdministrativeDomain var5 = (AdministrativeDomain)var4.nextElement();
         AdminDomainRef var6 = AdminDomainRef.newAdminDomainRef(var5);
         return var6;
      } else {
         return null;
      }
   }

   public String getErrorMsg() {
      return this.errorMsg;
   }

   public String preApprovalSteps(Object var1) {
      this.errorMsg = "";
      logger.error("-->ReleaseSystemHelper.preApprovalSteps() Called");

      try {
         WTChangeOrder2 var2 = (WTChangeOrder2)var1;
         SubFolder var3 = getProjectReleaseFolder(var2, (String)null);
         if (var3 == null) {
            throw new Exception("Failed to determine release folder");
         }

         String var4 = var3.getName();
         String var5 = var4 + "-processing";
         SubFolderIdentity var7 = (SubFolderIdentity)var3.getIdentificationObject();
         var7.setName(var5);
         var3 = (SubFolder)IdentityHelper.service.changeIdentity(var3, var7);
         if (!var3.getName().equalsIgnoreCase(var5)) {
            throw new Exception("Release folder not renamed to: " + var5);
         }

         WTContainer var8 = var3.getContainer();
         long var9 = PersistenceHelper.getObjectIdentifier(var8).getId();
         AdminDomainRef var11 = findDomainRef(var9, true);
         if (var11 == null) {
            throw new Exception("Failed to determine secure domain");
         }

         AdministrativeDomainHelper.manager.changeAdministrativeDomain(var3, var11, false);
      } catch (Exception var12) {
         this.errorMsg = var12.getMessage();
         var12.printStackTrace();
      }

      if (this.errorMsg != null && this.errorMsg.length() > 0) {
         logger.error("-->ReleaseSystemHelper.preApprovalSteps() errorMsg: " + this.errorMsg);
         return "Failure";
      } else {
         logger.error("-->ReleaseSystemHelper.preApprovalSteps() Succeeded");
         return "Success";
      }
   }

   public static void postRejectionSteps(Object var0) {
      logger.error("-->ReleaseSystemHelper.postRejectionSteps() Called");

      try {
         WTChangeOrder2 var1 = (WTChangeOrder2)var0;
         SubFolder var2 = getProjectReleaseFolder(var1, "-processing");
         if (var2 == null) {
            throw new Exception("Failed to determine release folder");
         }

         String var3 = var2.getName();
         String var4 = var3.substring(0, var3.length() - "-processing".length());
         SubFolderIdentity var6 = (SubFolderIdentity)var2.getIdentificationObject();
         var6.setName(var4);
         var2 = (SubFolder)IdentityHelper.service.changeIdentity(var2, var6);
         if (!var2.getName().equalsIgnoreCase(var4)) {
            throw new Exception("Release folder not renamed to: " + var4);
         }

         WTContainer var7 = var2.getContainer();
         long var8 = PersistenceHelper.getObjectIdentifier(var7).getId();
         AdminDomainRef var10 = findDomainRef(var8, false);
         if (var10 == null) {
            throw new Exception("Failed to determine default domain");
         }

         AdministrativeDomainHelper.manager.changeAdministrativeDomain(var2, var10, true);
      } catch (Exception var11) {
         var11.printStackTrace();
         logger.error("-->ReleaseSystemHelper.postRejectionSteps() errorMsg: " + var11.getMessage());
         return;
      }

      logger.error("-->ReleaseSystemHelper.postRejectionSteps() Succeeded");
   }

   public static void restoreLifecycles(Object var0) {
      logger.error("-->ReleaseSystemHelper.restoreLifecycles() Called");

      try {
         WTChangeOrder2 var1 = (WTChangeOrder2)var0;
         ChangeHelper2.service.saveChangeOrder((WTChangeOrder2)PersistenceHelper.manager.refresh(var1));
         WTPrincipalReference var2 = var1.getCreator();
         QuerySpec var3 = new QuerySpec(WTChangeActivity2.class);
         var3.appendSearchCondition(new SearchCondition(WTChangeActivity2.class, "master>number", "=", var1.getNumber(), false));
         QueryResult var4 = PersistenceHelper.manager.find(var3);
         if (!var4.hasMoreElements()) {
            throw new Exception("Unable to find the Change Activity with this Change Notice. Please contact your Windchill administrators.");
         }

         WTChangeActivity2 var5 = (WTChangeActivity2)PersistenceHelper.manager.refresh((WTChangeActivity2)var4.nextElement());
         var5 = (WTChangeActivity2)getLatestIteration(var5.getMaster());
         QueryResult var6 = PersistenceHelper.manager.navigate(var5, "theChangeable2", ChangeRecord2.class, false);

         while(var6.hasMoreElements()) {
            ChangeRecord2 var7 = (ChangeRecord2)var6.nextElement();
            String var8 = var7.getDescription();
            if (var8 != null) {
               ChangeableIfc var9 = var7.getChangeableIfc();
               RevisionControlled var10 = (RevisionControlled)var9;
               Logger var10000 = logger;
               String var10001 = var10.getName();
               var10000.error("-->ReleaseSystemHelper.restoreLifecycles() setting " + var10001 + " to " + var8);
               setLifecycle(var10, var8, var2);
            }
         }
      } catch (Exception var11) {
         var11.printStackTrace();
         logger.error("-->ReleaseSystemHelper.restoreLifecycles() errorMsg: " + var11.getMessage());
         return;
      }

      logger.error("-->ReleaseSystemHelper.restoreLifecycles() Succeeded");
   }

   public static void setChangeActivityState(Object var0, String var1) {
      logger.error("-->ReleaseSystemHelper.setChangeActivityState() Called");

      try {
         WTChangeOrder2 var2 = (WTChangeOrder2)var0;
         ChangeHelper2.service.saveChangeOrder((WTChangeOrder2)PersistenceHelper.manager.refresh(var2));
         WTPrincipalReference var3 = var2.getCreator();
         QuerySpec var4 = new QuerySpec(WTChangeActivity2.class);
         var4.appendSearchCondition(new SearchCondition(WTChangeActivity2.class, "master>number", "=", var2.getNumber(), false));
         QueryResult var5 = PersistenceHelper.manager.find(var4);
         if (!var5.hasMoreElements()) {
            throw new Exception("Unable to find the Change Activity with this Change Notice. Please contact your Windchill administrators.");
         }

         WTChangeActivity2 var6 = (WTChangeActivity2)PersistenceHelper.manager.refresh((WTChangeActivity2)var5.nextElement());
         var6 = (WTChangeActivity2)getLatestIteration(var6.getMaster());
         setLifecycle(var6, var1, var3);
         Logger var10000 = logger;
         String var10001 = var6.getName();
         var10000.error("-->ReleaseSystemHelper.setChangeActivityState() setting " + var10001 + " to " + var1);
      } catch (Exception var7) {
         var7.printStackTrace();
         logger.error("-->ReleaseSystemHelper.setChangeActivityState() errorMsg: " + var7.getMessage());
         return;
      }

      logger.error("-->ReleaseSystemHelper.setChangeActivityState() Succeeded");
   }

   public static int setLifecycle(RevisionControlled var0, String var1, WTPrincipalReference var2) {
      int var3 = 0;

      try {
         var3 = setLifecycleInternal((RevisionControlled)PersistenceHelper.manager.refresh(var0), var1, var2);
      } catch (Exception var7) {
         try {
            Thread.sleep(2000L);
            var3 = setLifecycleInternal((RevisionControlled)PersistenceHelper.manager.refresh(var0), var1, var2);
         } catch (Exception var6) {
            return 1;
         }
      }

      return var3;
   }

   private static int setLifecycleInternal(RevisionControlled var0, String var1, WTPrincipalReference var2) {
      Transaction var3 = null;

      try {
         var0 = (RevisionControlled)PersistenceHelper.manager.refresh(var0);
      } catch (Exception var10) {
      }

      LifeCycleState var4 = var0.getState();
      if (var4 == null) {
         logger.error("-->ReleaseSystemHelper.setLifecycle() failed to get lifecyclestate");
         return 1;
      } else {
         String var5 = var4.getState().toString();
         if (var5 == null) {
            logger.error("-->ReleaseSystemHelper.setLifecycle() failed to get current state");
            return 1;
         } else {
            logger.error("-->ReleaseSystemHelper.setLifecycle() curState: " + var5);
            if (var5.equalsIgnoreCase(var1)) {
               return 0;
            } else {
               boolean var6 = SessionServerHelper.manager.setAccessEnforced(false);

               try {
                  var3 = new Transaction();
                  var3.start();
                  WTPrincipal var7 = SessionHelper.manager.getPrincipal();
                  SessionHelper.manager.setPrincipal(var2.getName());
                  var0 = (RevisionControlled)takeOwnership(var0);
                  State var8 = State.toState(var1);
                  if (var8 == null) {
                     throw new Exception("failed to create toState");
                  }

                  var0 = (RevisionControlled)LifeCycleServerHelper.service.setState(var0, var8);
                  var0.getCheckoutInfo().setState(WorkInProgressState.CHECKED_IN);
                  PersistenceServerHelper.manager.update(var0);
                  ControlBranch var9 = VersionControlServerHelper.getControlBranch(var0);
                  if (var9 != null) {
                     var9.setUntrustedBusinessFields(var0);
                     PersistenceServerHelper.manager.update(var9);
                  }

                  var0 = (RevisionControlled)releaseOwnership(var0);
                  SessionHelper.manager.setPrincipal(var7.getName());
                  var3.commit();
                  var3 = null;
                  RepresentationHelper.service.emitReadyToPublishEvent(var0);
               } catch (Exception var11) {
                  if (var3 != null) {
                     var3.rollback();
                  }

                  logger.error("-->ReleaseSystemHelper.setLifecycle() exception: " + var11.getMessage());
                  var11.printStackTrace();
                  SessionServerHelper.manager.setAccessEnforced(var6);
                  return 1;
               }

               SessionServerHelper.manager.setAccessEnforced(var6);
               return 0;
            }
         }
      }
   }

   public static int setLifecycle(WTChangeActivity2 var0, String var1, WTPrincipalReference var2) {
      int var3 = 0;

      try {
         var3 = setLifecycleInternal((WTChangeActivity2)PersistenceHelper.manager.refresh(var0), var1, var2);
      } catch (Exception var7) {
         try {
            Thread.sleep(2000L);
            var3 = setLifecycleInternal((WTChangeActivity2)PersistenceHelper.manager.refresh(var0), var1, var2);
         } catch (Exception var6) {
            return 1;
         }
      }

      return var3;
   }

   private static int setLifecycleInternal(WTChangeActivity2 var0, String var1, WTPrincipalReference var2) {
      Transaction var3 = null;
      LifeCycleState var4 = var0.getState();
      if (var4 == null) {
         logger.error("-->ReleaseSystemHelper.setLifecycle() failed to get lifecyclestate");
         return 1;
      } else {
         String var5 = var4.getState().toString();
         if (var5 == null) {
            logger.error("-->ReleaseSystemHelper.setLifecycle() failed to get current state");
            return 1;
         } else {
            logger.error("-->ReleaseSystemHelper.setLifecycle() curState: " + var5);
            if (var5.equalsIgnoreCase(var1)) {
               return 0;
            } else {
               boolean var6 = SessionServerHelper.manager.setAccessEnforced(false);

               try {
                  var3 = new Transaction();
                  var3.start();
                  WTPrincipal var7 = SessionHelper.manager.getPrincipal();
                  SessionHelper.manager.setPrincipal(var2.getName());
                  var0 = (WTChangeActivity2)takeOwnership(var0);
                  State var8 = State.toState(var1);
                  if (var8 == null) {
                     throw new Exception("failed to create toState");
                  }

                  var0 = (WTChangeActivity2)LifeCycleServerHelper.service.setState(var0, var8);
                  PersistenceServerHelper.manager.update(var0);
                  ControlBranch var9 = VersionControlServerHelper.getControlBranch(var0);
                  if (var9 != null) {
                     var9.setUntrustedBusinessFields(var0);
                     PersistenceServerHelper.manager.update(var9);
                  }

                  var0 = (WTChangeActivity2)releaseOwnership(var0);
                  SessionHelper.manager.setPrincipal(var7.getName());
                  var3.commit();
                  var3 = null;
                  RepresentationHelper.service.emitReadyToPublishEvent(var0);
               } catch (Exception var10) {
                  if (var3 != null) {
                     var3.rollback();
                  }

                  logger.error("-->ReleaseSystemHelper.setLifecycle() exception: " + var10.getMessage());
                  var10.printStackTrace();
                  SessionServerHelper.manager.setAccessEnforced(var6);
                  return 1;
               }

               SessionServerHelper.manager.setAccessEnforced(var6);
               return 0;
            }
         }
      }
   }

   private String getRevisionSequence(RevisionControlled var1, String var2) throws WTException {
      int var3 = 0;
      logger.error("-->ReleaseSystemHelper.getRevisionSequence() Called for: " + var1.getName());
      if (!var2.equalsIgnoreCase("FORMAL") && !var2.equalsIgnoreCase("OBSOLETE")) {
         String var4 = VersionControlHelper.getVersionIdentifier(var1).getValue();
         logger.error("-->ReleaseSystemHelper.getRevisionSequence() currentRevision: " + var4);
         QueryResult var5 = VersionControlHelper.service.allIterationsOf(var1.getMaster(), true);

         while(var5.hasMoreElements()) {
            LifeCycleManaged var6 = (LifeCycleManaged)var5.nextElement();
            if (var6 instanceof RevisionControlled) {
               RevisionControlled var7 = (RevisionControlled)var6;
               String var8 = VersionControlHelper.getVersionIdentifier(var7).getValue();
               String var9 = VersionControlHelper.getIterationIdentifier(var7).getValue();
               if (var4.equalsIgnoreCase(var8)) {
                  String var10 = var7.getState().toString();
                  String var11 = getIbaFromObject((IBAHolder)var7, "REV_SEQ");
                  logger.error("-->ReleaseSystemHelper.getRevisionSequence() thisIteration: " + var8 + "." + var9 + " state: " + var10 + " rev_seq: " + var11);
                  if (var11 != null) {
                     int var12 = var11.lastIndexOf(46);
                     if (var12 >= 0) {
                        var11 = var11.substring(var12 + 1);
                     }

                     int var13 = 0;

                     try {
                        if (var11.length() > 0) {
                           var13 = Integer.parseInt(var11);
                        }

                        if (var3 < var13) {
                           var3 = var13;
                           logger.error("-->ReleaseSystemHelper.getRevisionSequence() updating maxOldRevSeq to: " + var13);
                        }
                     } catch (Exception var15) {
                     }
                  }
               }
            }
         }

         String var16 = String.valueOf(var3 + 1);
         if (var16.length() == 1) {
            var16 = "0" + var16;
         }

         var16 = "." + var16;
         logger.error("-->ReleaseSystemHelper.getRevisionSequence() revisionSequence: " + var16);
         return var16;
      } else {
         logger.error("-->ReleaseSystemHelper.getRevisionSequence() FORMAL and OBSOLETE are assigned '.'");
         return ".";
      }
   }

   public static String getWTDocFilename(WTDocument var0) {
      String var1 = "";

      try {
         ContentItem var2 = ((FormatContentHolder)ContentHelper.service.getContents(var0)).getPrimary();
         if (var2 != null) {
            ApplicationData var3 = (ApplicationData)var2;
            var1 = var3.getFileName();
         }
      } catch (Exception var4) {
      }

      return var1;
   }

   public String postApprovalSteps(Object var1) {
      this.errorMsg = "";
      Transaction var2 = null;
      logger.error("-->ReleaseSystemHelper.postApprovalSteps() Called");

      try {
         WTChangeOrder2 var3 = (WTChangeOrder2)var1;
         logger.error("-->ReleaseSystemHelper.postApprovalSteps() " + var3.getDisplayIdentifier());
         String var4 = getIbaFromObject(var3, "RELEASE_NO");
         String var5 = getFormattedDate(var3.getResolutionDate()).toUpperCase();
         String var6 = getIbaFromObject(var3, "CHECKER");
         String var7 = getIbaFromObject(var3, "ORIGINATOR");
         String var8 = getIbaFromObject(var3, "MDS_ECR");
         String var9 = getIbaFromObject(var3, "TARGET_STATE");
         logger.debug("-->ReleaseSystemHelper.postApprovalSteps() RELEASE_NO: " + var4);
         logger.debug("-->ReleaseSystemHelper.postApprovalSteps() resolution date: " + var5);
         logger.debug("-->ReleaseSystemHelper.postApprovalSteps() CHECKER: " + var6);
         logger.debug("-->ReleaseSystemHelper.postApprovalSteps() ORIGINATOR: " + var7);
         logger.debug("-->ReleaseSystemHelper.postApprovalSteps() MDS_ECR: " + var8);
         logger.debug("-->ReleaseSystemHelper.postApprovalSteps() TARGET_STATE: " + var9);
         var9 = var9.replaceAll(" ", "").toUpperCase();
         WTPrincipalReference var10 = var3.getCreator();
         var2 = new Transaction();
         var2.start();
         QueryResult var11 = ChangeHelper2.service.getChangeActivities(var3);

         label121:
         while(var11.hasMoreElements()) {
            ChangeActivityIfc var12 = (ChangeActivityIfc)var11.nextElement();
            QueryResult var13 = ChangeHelper2.service.getChangeablesAfter(var12);

            while(true) {
               Changeable2 var14;
               label92:
               while(true) {
                  if (!var13.hasMoreElements()) {
                     continue label121;
                  }

                  var14 = (Changeable2)var13.nextElement();
                  if (!(var14 instanceof EPMDocument)) {
                     break;
                  }

                  EPMDocument var15 = (EPMDocument)var14;
                  if (setLifecycle((RevisionControlled)var15, var9, var10) != 0) {
                     throw new Exception("Unable to set new lifecycle state");
                  }

                  if (var15.getCADName().toLowerCase().endsWith(".drw")) {
                     QueryResult var16 = BaselineHelper.service.getBaselines(var15);
                     WTHashSet var17 = new WTHashSet(var16.size());
                     WTHashSet var18 = new WTHashSet(var16.size());
                     var18.addAll(var16);
                     Iterator var19 = var18.persistableIterator();

                     while(var19.hasNext()) {
                        Baseline var20 = (Baseline)var19.next();
                        Logger var10000 = logger;
                        String var10001 = var20.getClass().getName();
                        var10000.error("-->ReleaseSystemHelper.postApprovalSteps().thisBaseline class: " + var10001 + " name: " + var20.toString());
                        if (var20 instanceof MaturityBaseline) {
                           MaturityBaseline var21 = (MaturityBaseline)PersistenceHelper.manager.refresh(var20);
                           var10000 = logger;
                           var10001 = var15.getIdentity();
                           var10000.error("-->ReleaseSystemHelper.postApprovalSteps().removeFromBaseline: " + var10001 + " maturityBaseline: " + var21.toString());
                           BaselineHelper.service.removeFromBaseline(var15, var21);
                           var17.add(var21);
                        }
                     }

                     setIbaOnDocument(var15, "REL", var4);
                     setIbaOnDocument(var15, "REL_DATE", var5);
                     setIbaOnDocument(var15, "REL_WHO", var6);
                     setIbaOnDocument(var15, "WHO", var7);
                     setIbaOnDocument(var15, "PDM_EC_NO", var8);
                     String var30 = this.getRevisionSequence(var15, var9);
                     if (var30 == null || var30.length() <= 0) {
                        var30 = ".";
                     }

                     logger.debug("-->ReleaseSystemHelper.postApprovalSteps() epmDoc: " + var15.getDisplayIdentifier());
                     logger.debug("-->ReleaseSystemHelper.postApprovalSteps() REV_SEQ: " + var30);
                     setIbaOnDocument(var15, "REV_SEQ", var30);
                     Iterator var33 = var17.persistableIterator();

                     while(true) {
                        if (!var33.hasNext()) {
                           break label92;
                        }

                        Baseline var36 = (Baseline)PersistenceHelper.manager.refresh((Baseline)var33.next());
                        Logger var40 = logger;
                        String var45 = var15.getIdentity();
                        var40.error("-->ReleaseSystemHelper.postApprovalSteps().addToBaseline: " + var45 + " baseline: " + var36.toString());
                        BaselineHelper.service.addToBaseline(var15, var36);
                     }
                  }
               }

               if (var14 instanceof WTDocument) {
                  WTDocument var26 = (WTDocument)var14;
                  if (setLifecycle((RevisionControlled)var26, var9, var10) != 0) {
                     throw new Exception("Unable to set new lifecycle state");
                  }

                  if (getWTDocFilename(var26).toLowerCase().endsWith(".pdf")) {
                     QueryResult var27 = BaselineHelper.service.getBaselines(var26);
                     WTHashSet var28 = new WTHashSet(var27.size());
                     WTHashSet var29 = new WTHashSet(var27.size());
                     var29.addAll(var27);
                     Iterator var31 = var29.persistableIterator();

                     while(var31.hasNext()) {
                        Baseline var34 = (Baseline)var31.next();
                        Logger var41 = logger;
                        String var46 = var34.getClass().getName();
                        var41.error("-->ReleaseSystemHelper.postApprovalSteps().thisBaseline class: " + var46 + " name: " + var34.toString());
                        if (var34 instanceof MaturityBaseline) {
                           MaturityBaseline var37 = (MaturityBaseline)PersistenceHelper.manager.refresh(var34);
                           var41 = logger;
                           var46 = var26.getIdentity();
                           var41.error("-->ReleaseSystemHelper.postApprovalSteps().removeFromBaseline: " + var46 + " maturityBaseline: " + var37.toString());
                           BaselineHelper.service.removeFromBaseline(var26, var37);
                           var28.add(var37);
                        }
                     }

                     setIbaOnDocument(var26, "REL", var4);
                     setIbaOnDocument(var26, "REL_DATE", var5);
                     setIbaOnDocument(var26, "REL_WHO", var6);
                     setIbaOnDocument(var26, "WHO", var7);
                     setIbaOnDocument(var26, "PDM_EC_NO", var8);
                     String var32 = this.getRevisionSequence(var26, var9);
                     if (var32 == null || var32.length() <= 0) {
                        var32 = ".";
                     }

                     logger.debug("-->ReleaseSystemHelper.postApprovalSteps() wtDoc: " + var26.getDisplayIdentifier());
                     logger.debug("-->ReleaseSystemHelper.postApprovalSteps() REV_SEQ: " + var32);
                     setIbaOnDocument(var26, "REV_SEQ", var32);
                     Iterator var35 = var28.persistableIterator();

                     while(var35.hasNext()) {
                        Baseline var38 = (Baseline)PersistenceHelper.manager.refresh((Baseline)var35.next());
                        Logger var43 = logger;
                        String var48 = var26.getIdentity();
                        var43.error("-->ReleaseSystemHelper.postApprovalSteps().addToBaseline: " + var48 + " baseline: " + var38.toString());
                        BaselineHelper.service.addToBaseline(var26, var38);
                     }
                  }
               }
            }
         }

         var2.commit();
         var2 = null;
      } catch (Exception var22) {
         if (var2 != null) {
            var2.rollback();
         }

         this.errorMsg = var22.getMessage();
         var22.printStackTrace();
      }

      if (this.errorMsg != null && this.errorMsg.length() > 0) {
         logger.error("-->ReleaseSystemHelper.postApprovalSteps() errorMsg: " + this.errorMsg);
         return "Failure";
      } else {
         logger.error("-->ReleaseSystemHelper.postApprovalSteps() Succeeded");
         return "Success";
      }
   }

   private static int getFolderContentsCount(SubFolder var0) throws WTException {
      int var1 = 0;
      WTHashSet var2 = new WTHashSet();
      var2.add(var0);
      WTKeyedMap var3 = FolderHelper.service.getFolderToContentsMap(var2, FolderEntry.class, true);
      Iterator var4 = var3.wtKeySet().persistableIterator();

      while(var4.hasNext()) {
         WTCollection var5 = (WTCollection)var3.get(var4.next());
         if (var5 != null) {
            for(Iterator var6 = var5.persistableIterator(); var6.hasNext(); ++var1) {
               var6.next();
            }
         }
      }

      return var1;
   }

   public String folderCleanupSteps(Object var1) {
      this.errorMsg = "";
      logger.error("-->ReleaseSystemHelper.folderCleanupSteps() Called");

      try {
         Properties var2 = getReleaseProperties();
         Boolean var3 = Boolean.parseBoolean(var2.getProperty("deleteProjectFolder", "false"));
         Integer var4 = Integer.parseInt(var2.getProperty("sleepBeforeDelete", "15"));
         Integer var5 = Integer.parseInt(var2.getProperty("sleepAfterDelete", "15"));
         WTChangeOrder2 var6 = (WTChangeOrder2)var1;
         SubFolder var7 = getProjectReleaseFolder(var6, "-processing");
         if (var7 == null) {
            throw new Exception("Failed to determine release folder");
         }

         String var8 = var7.getName();
         String var9 = var8.substring(0, var8.length() - "-processing".length());
         var9 = var9 + "-done";
         SubFolderIdentity var11 = (SubFolderIdentity)var7.getIdentificationObject();
         var11.setName(var9);
         var7 = (SubFolder)IdentityHelper.service.changeIdentity(var7, var11);
         if (!var7.getName().equalsIgnoreCase(var9)) {
            throw new Exception("Release folder not renamed to: " + var9);
         }

         WTContainer var12 = var7.getContainer();
         long var13 = PersistenceHelper.getObjectIdentifier(var12).getId();
         AdminDomainRef var15 = findDomainRef(var13, false);
         if (var15 == null) {
            throw new Exception("Failed to determine default domain");
         }

         AdministrativeDomainHelper.manager.changeAdministrativeDomain(var7, var15, true);
         String var16 = getIbaFromObject(var6, "AUTO_MOVE");
         String var17 = getIbaFromObject(var6, "RELEASE_NO");
         if (var16.equalsIgnoreCase("True")) {
            SubFolderReference var18 = var7.getParentFolder();
            if (var18 == null) {
               throw new Exception("Could not get parent folder reference");
            }

            SubFolder var19 = (SubFolder)var18.getObject();
            if (var19 == null) {
               throw new Exception("Could not get parent folder object");
            }

            logger.error("-->ReleaseSystemHelper.folderCleanupSteps() parentFolder: " + var19.getFolderPath());
            Properties var20 = getReleaseProperties();
            SubFolder var21 = getFolderFromName(var6.getContainerReference(), getReleaseProp(var20, "formalFolderPath"));
            SubFolder var22 = getFolderFromName(var6.getContainerReference(), getReleaseProp(var20, "obsoleteFolderPath"));
            QueryResult var23 = ChangeHelper2.service.getChangeActivities(var6);

            while(var23.hasMoreElements()) {
               ChangeActivityIfc var24 = (ChangeActivityIfc)var23.nextElement();
               QueryResult var25 = ChangeHelper2.service.getChangeablesAfter(var24);

               while(var25.hasMoreElements()) {
                  Changeable2 var26 = (Changeable2)var25.nextElement();
                  if (var26 instanceof RevisionControlled) {
                     RevisionControlled var27 = (RevisionControlled)var26;
                     State var28 = var27.getLifeCycleState();
                     SubFolder var29 = var19;
                     if (var28.toString().equalsIgnoreCase("Formal")) {
                        var29 = var21;
                     } else if (var28.toString().equalsIgnoreCase("Obsolete")) {
                        var29 = var22;
                     }

                     WTValuedHashMap var30 = new WTValuedHashMap();
                     var30.put(var27, var29);
                     EPMHelper.moveToAnotherContainer(var30);
                  }
               }
            }

            if (getFolderContentsCount(var7) > 0) {
               String var10001 = var7.getName();
               this.errorMsg = "The Release folder " + var10001 + " from project " + var17;
               this.errorMsg = this.errorMsg + " is not empty and therefore was not automatically removed by the Release System.";
               this.errorMsg = this.errorMsg + " Please empty the folder and remove it manually.";
            } else if (var3) {
               if (var4 != null && var4 > 0) {
                  Thread.sleep((long)(var4 * 1000));
               }

               var7 = (SubFolder)PersistenceHelper.manager.refresh(var7);
               PersistenceHelper.manager.delete(var7);
               if (var5 != null && var5 > 0) {
                  Thread.sleep((long)(var5 * 1000));
               }
            } else {
               logger.info("-->ReleaseSystemHelper.folderCleanupSteps() not deleting " + var7.getName());
            }
         }
      } catch (Exception var31) {
         this.errorMsg = var31.getMessage();
         var31.printStackTrace();
      }

      if (this.errorMsg != null && this.errorMsg.length() > 0) {
         logger.error("-->ReleaseSystemHelper.folderCleanupSteps() errorMsg: " + this.errorMsg);
         return "Failure";
      } else {
         logger.error("-->ReleaseSystemHelper.folderCleanupSteps() Succeeded");
         return "Success";
      }
   }

   private static Team removeMembersFromRole(Team var0, String var1) {
      logger.error("-->ReleaseSystemHelper.removeMembersFromRole() " + var1);

      try {
         Role var2 = Role.toRole(var1);
         Enumeration var3 = var0.getPrincipalTarget(var2);

         while(var3.hasMoreElements()) {
            WTPrincipalReference var4 = (WTPrincipalReference)var3.nextElement();
            if (var4 != null) {
               var0 = (Team)PersistenceHelper.manager.refresh(var0);
               var0.deletePrincipalTarget(var2, var4.getPrincipal());
            }
         }

         return (Team)PersistenceHelper.manager.refresh(var0);
      } catch (WTException var5) {
         logger.error("-->ReleaseSystemHelper.removeMembersFromRole() Exception: " + var5.getMessage());
         var5.printStackTrace();
         return var0;
      }
   }

   public static void removeAllButUser(Object var0, String var1) {
      try {
         Object var2 = var0;
         if (var0 instanceof ObjectReference) {
            var2 = ((ObjectReference)var0).getObject();
         }

         if (!(var2 instanceof WfAssignedActivity)) {
            logger.error("-->ReleaseSystemHelper.assignUserToRole() " + var2 + " is not instanceof WfAssignedActivity");
            return;
         }

         WfAssignedActivity var3 = (WfAssignedActivity)var2;
         var3 = (WfAssignedActivity)PersistenceHelper.manager.refresh(var3);
         WfProcess var4 = var3.getParentProcess();
         Team var5 = (Team)var4.getTeamId().getObject();
         var5 = (Team)PersistenceHelper.manager.refresh(var5);
         WTPrincipal var6 = SessionHelper.manager.getPrincipal();
         var5 = removeMembersFromRole(var5, var1);
         Logger var10000 = logger;
         String var10001 = var6.getName();
         var10000.error("-->ReleaseSystemHelper.assignUserToRole() adding " + var10001 + " to " + var1);
         var5.addPrincipal(Role.toRole(var1), var6);
      } catch (Exception var7) {
         logger.error("-->ReleaseSystemHelper.assignUserToRole() Exception: " + var7.getMessage());
         var7.printStackTrace();
      }

   }

   private static String parseName(String var0) {
      String[] var1 = var0.split(",");
      String var2 = "";
      String var3 = "";
      if (var1.length == 2) {
         var3 = var1[0].trim();
         var2 = var1[1].trim();
      } else if (var1.length == 1) {
         var3 = var1[0].trim();
      }

      String var4 = "";
      if (var2.length() > 0) {
         var4 = var2.substring(0, 1) + ".";
      }

      var4 = var4 + var3;
      return var4.toUpperCase();
   }

   public String populateAttributes(Object var1) {
      this.errorMsg = "";
      Transaction var2 = null;

      try {
         var2 = new Transaction();
         var2.start();
         Timestamp var3 = new Timestamp(System.currentTimeMillis());
         String var4 = getFormattedDate(var3);
         if (var4 != null) {
            var4 = var4.toUpperCase();
         }

         if (var1 instanceof PromotionNotice) {
            PromotionNotice var31 = (PromotionNotice)var1;
            logger.error("-->ReleaseSystemHelper.populateAttributes() " + var31.getDisplayIdentifier());
            String var32 = var31.getMaturityState().toString();
            if (var32.equalsIgnoreCase("MAKEREADY")) {
               var32 = "MAKE READY";
            }

            logger.debug("-->ReleaseSystemHelper.populateAttributes() targetState: " + var32);
            WTPrincipalReference var33 = var31.getCreator();
            WTUser var34 = (WTUser)var33.getPrincipal();
            String var35 = var34.getName();
            if (var35 != null) {
               var35 = var35.toUpperCase();
            }

            String var36 = getIbaFromObject(var31, "PROJECT");
            String var39 = var31.getNumber();
            String var42 = "";
            String var45 = "";
            if (var36 != null && var36.length() > 0) {
               var42 = var36 + "-";
               var45 = var36 + "-";
            }

            var42 = var42 + var39;
            var42 = var42.toUpperCase();
            var45 = var45 + var39;
            var45 = var45.toUpperCase();
            logger.debug("-->ReleaseSystemHelper.populateAttributes() releaseNo: " + var42);
            logger.debug("-->ReleaseSystemHelper.populateAttributes() submitDate: " + var4);
            logger.debug("-->ReleaseSystemHelper.populateAttributes() formattedName: " + var35);
            setIbaOnObject(var31, "RELEASE_NO", var42);
            setIbaOnObject(var31, "SUBMIT_DATE", var4);
            setIbaOnObject(var31, "ORIGINATOR", var35);
            QueryResult var48 = MaturityHelper.service.getPromotionTargets(var31);

            while(var48.hasMoreElements()) {
               Object var49 = var48.nextElement();
               if (var49 instanceof EPMDocument) {
                  EPMDocument var50 = (EPMDocument)var49;
                  String var52 = VersionControlHelper.getIterationIdentifier(var50).getValue();
                  QueryResult var55 = BaselineHelper.service.getBaselines(var50);
                  WTHashSet var57 = new WTHashSet(var55.size());
                  WTHashSet var60 = new WTHashSet(var55.size());
                  var60.addAll(var55);
                  Iterator var64 = var60.persistableIterator();

                  while(var64.hasNext()) {
                     Baseline var69 = (Baseline)var64.next();
                     Logger var90 = logger;
                     String var101 = var69.getClass().getName();
                     var90.error("-->ReleaseSystemHelper.populateAttributes().thisBaseline class: " + var101 + " name: " + var69.toString());
                     if (var69 instanceof MaturityBaseline) {
                        MaturityBaseline var74 = (MaturityBaseline)PersistenceHelper.manager.refresh(var69);
                        var90 = logger;
                        var101 = var50.getIdentity();
                        var90.error("-->ReleaseSystemHelper.populateAttributes().removeFromBaseline: " + var101 + " maturityBaseline: " + var74.toString());
                        BaselineHelper.service.removeFromBaseline(var50, var74);
                        var57.add(var74);
                     }
                  }

                  String var65 = this.getRevisionSequence(var50, var32.replaceAll(" ", "").toUpperCase());
                  if (var65 == null || var65.length() <= 0) {
                     var65 = ".";
                  }

                  logger.debug("-->ReleaseSystemHelper.populateAttributes() epmDoc: " + var50.getDisplayIdentifier());
                  logger.debug("-->ReleaseSystemHelper.populateAttributes() REV_SEQ: " + var65);
                  setIbaOnDocument(var50, "REV_SEQ", var65);
                  setIbaOnDocument(var50, "REL", var45);
                  setIbaOnDocument(var50, "REL_DATE", var4);
                  setIbaOnDocument(var50, "TARGET_STATE", var32);
                  setIbaOnDocument(var50, "WHO", var35);
                  Iterator var70 = var57.persistableIterator();

                  while(var70.hasNext()) {
                     Baseline var75 = (Baseline)PersistenceHelper.manager.refresh((Baseline)var70.next());
                     Logger var92 = logger;
                     String var103 = var50.getIdentity();
                     var92.error("-->ReleaseSystemHelper.populateAttributes().addToBaseline: " + var103 + " baseline: " + var75.toString());
                     BaselineHelper.service.addToBaseline(var50, var75);
                  }
               }

               if (var49 instanceof WTDocument) {
                  WTDocument var51 = (WTDocument)var49;
                  QueryResult var53 = BaselineHelper.service.getBaselines(var51);
                  WTHashSet var56 = new WTHashSet(var53.size());
                  WTHashSet var58 = new WTHashSet(var53.size());
                  var58.addAll(var53);
                  Iterator var61 = var58.persistableIterator();

                  while(var61.hasNext()) {
                     Baseline var66 = (Baseline)var61.next();
                     Logger var93 = logger;
                     String var104 = var66.getClass().getName();
                     var93.error("-->ReleaseSystemHelper.populateAttributes().thisBaseline class: " + var104 + " name: " + var66.toString());
                     if (var66 instanceof MaturityBaseline) {
                        MaturityBaseline var71 = (MaturityBaseline)PersistenceHelper.manager.refresh(var66);
                        var93 = logger;
                        var104 = var51.getIdentity();
                        var93.error("-->ReleaseSystemHelper.populateAttributes().removeFromBaseline: " + var104 + " maturityBaseline: " + var71.toString());
                        BaselineHelper.service.removeFromBaseline(var51, var71);
                        var56.add(var71);
                     }
                  }

                  setIbaOnDocument(var51, "TARGET_STATE", var32);
                  String var62 = this.getRevisionSequence(var51, var32);
                  if (var62 == null || var62.length() <= 0) {
                     var62 = ".";
                  }

                  logger.debug("-->ReleaseSystemHelper.populateAttributes() wtDoc: " + var51.getDisplayIdentifier());
                  logger.debug("-->ReleaseSystemHelper.populateAttributes() REV_SEQ: " + var62);
                  setIbaOnDocument(var51, "REV_SEQ", var62);
                  Iterator var67 = var56.persistableIterator();

                  while(var67.hasNext()) {
                     Baseline var72 = (Baseline)PersistenceHelper.manager.refresh((Baseline)var67.next());
                     Logger var95 = logger;
                     String var106 = var51.getIdentity();
                     var95.error("-->ReleaseSystemHelper.populateAttributes().addToBaseline: " + var106 + " baseline: " + var72.toString());
                     BaselineHelper.service.addToBaseline(var51, var72);
                  }
               }
            }
         } else if (!(var1 instanceof WTChangeOrder2)) {
            logger.error("-->ReleaseSystemHelper.populateReleaseNo() unsupported type - " + var1);
         } else {
            WTChangeOrder2 var5 = (WTChangeOrder2)var1;
            logger.error("-->ReleaseSystemHelper.populateAttributes() " + var5.getDisplayIdentifier());
            WTPrincipalReference var6 = var5.getIterationInfo().getCreator();
            WTUser var7 = (WTUser)var6.getPrincipal();
            String var8 = var7.getName();
            if (var8 != null) {
               var8 = var8.toUpperCase();
            }

            String var9 = getIbaFromObject(var5, "PROJECT");
            String var10 = var5.getNumber();
            String var11 = "";
            String var12 = "";
            if (var9 != null && var9.length() > 0) {
               var11 = var9 + "-";
               var12 = var9 + "-";
            }

            var11 = var11 + var10;
            var11 = var11.toUpperCase();
            var12 = var12 + var10;
            var12 = var12.toUpperCase();
            logger.debug("-->ReleaseSystemHelper.populateAttributes() releaseNo: " + var11);
            logger.debug("-->ReleaseSystemHelper.populateAttributes() submitDate: " + var4);
            logger.debug("-->ReleaseSystemHelper.populateAttributes() formattedName: " + var8);
            setIbaOnObject(var5, "RELEASE_NO", var11);
            setIbaOnObject(var5, "SUBMIT_DATE", var4);
            setIbaOnObject(var5, "ORIGINATOR", var8);
            QueryResult var13 = ChangeHelper2.service.getChangeActivities(var5);

            label222:
            while(var13.hasMoreElements()) {
               ChangeActivityIfc var14 = (ChangeActivityIfc)var13.nextElement();
               QueryResult var15 = ChangeHelper2.service.getChangeablesAfter(var14, false);

               while(true) {
                  ChangeRecord2 var16;
                  String var54;
                  while(true) {
                     if (!var15.hasMoreElements()) {
                        continue label222;
                     }

                     var16 = (ChangeRecord2)var15.nextElement();
                     Transition var17 = var16.getTargetTransition();
                     var54 = "";
                     if (Transition.CHANGE.equals(var17)) {
                        var54 = "FORMAL";
                        break;
                     }

                     if (Transition.OBSOLESCENCE.equals(var17)) {
                        var54 = "OBSOLETE";
                        break;
                     }

                     logger.error("-->ReleaseSystemHelper.populateTargetState() unsupported transition - " + var17);
                  }

                  logger.debug("-->ReleaseSystemHelper.populateAttributes() targetState: " + var54);
                  Changeable2 var19 = var16.getChangeable2();
                  if (var19 instanceof EPMDocument) {
                     EPMDocument var20 = (EPMDocument)var19;
                     String var21 = VersionControlHelper.getIterationIdentifier(var20).getValue();
                     QueryResult var22 = BaselineHelper.service.getBaselines(var20);
                     WTHashSet var23 = new WTHashSet(var22.size());
                     WTHashSet var24 = new WTHashSet(var22.size());
                     var24.addAll(var22);
                     Iterator var25 = var24.persistableIterator();

                     while(var25.hasNext()) {
                        Baseline var26 = (Baseline)var25.next();
                        Logger var10000 = logger;
                        String var10001 = var26.getClass().getName();
                        var10000.error("-->ReleaseSystemHelper.populateAttributes().thisBaseline class: " + var10001 + " name: " + var26.toString());
                        if (var26 instanceof MaturityBaseline) {
                           MaturityBaseline var27 = (MaturityBaseline)PersistenceHelper.manager.refresh(var26);
                           var10000 = logger;
                           var10001 = var20.getIdentity();
                           var10000.error("-->ReleaseSystemHelper.populateAttributes().removeFromBaseline: " + var10001 + " maturityBaseline: " + var27.toString());
                           BaselineHelper.service.removeFromBaseline(var20, var27);
                           var23.add(var27);
                        }
                     }

                     String var78 = this.getRevisionSequence(var20, var54.replaceAll(" ", "").toUpperCase());
                     if (var78 == null || var78.length() <= 0) {
                        var78 = ".";
                     }

                     logger.debug("-->ReleaseSystemHelper.populateAttributes() epmDoc: " + var20.getDisplayIdentifier());
                     logger.debug("-->ReleaseSystemHelper.populateAttributes() REV_SEQ: " + var78);
                     setIbaOnDocument(var20, "REV_SEQ", var78);
                     setIbaOnDocument(var20, "REL", var12);
                     setIbaOnDocument(var20, "REL_DATE", var4);
                     setIbaOnDocument(var20, "TARGET_STATE", var54);
                     Iterator var81 = var23.persistableIterator();

                     while(var81.hasNext()) {
                        Baseline var84 = (Baseline)PersistenceHelper.manager.refresh((Baseline)var81.next());
                        Logger var86 = logger;
                        String var97 = var20.getIdentity();
                        var86.error("-->ReleaseSystemHelper.populateAttributes().addToBaseline: " + var97 + " baseline: " + var84.toString());
                        BaselineHelper.service.addToBaseline(var20, var84);
                     }
                  }

                  if (var19 instanceof WTDocument) {
                     WTDocument var59 = (WTDocument)var19;
                     QueryResult var63 = BaselineHelper.service.getBaselines(var59);
                     WTHashSet var68 = new WTHashSet(var63.size());
                     WTHashSet var73 = new WTHashSet(var63.size());
                     var73.addAll(var63);
                     Iterator var76 = var73.persistableIterator();

                     while(var76.hasNext()) {
                        Baseline var79 = (Baseline)var76.next();
                        Logger var87 = logger;
                        String var98 = var79.getClass().getName();
                        var87.error("-->ReleaseSystemHelper.populateAttributes().thisBaseline class: " + var98 + " name: " + var79.toString());
                        if (var79 instanceof MaturityBaseline) {
                           MaturityBaseline var82 = (MaturityBaseline)PersistenceHelper.manager.refresh(var79);
                           var87 = logger;
                           var98 = var59.getIdentity();
                           var87.error("-->ReleaseSystemHelper.populateAttributes().removeFromBaseline: " + var98 + " maturityBaseline: " + var82.toString());
                           BaselineHelper.service.removeFromBaseline(var59, var82);
                           var68.add(var82);
                        }
                     }

                     setIbaOnDocument(var59, "TARGET_STATE", var54);
                     String var77 = this.getRevisionSequence(var59, var54);
                     if (var77 == null || var77.length() <= 0) {
                        var77 = ".";
                     }

                     logger.debug("-->ReleaseSystemHelper.populateAttributes() wtDoc: " + var59.getDisplayIdentifier());
                     logger.debug("-->ReleaseSystemHelper.populateAttributes() REV_SEQ: " + var77);
                     setIbaOnDocument(var59, "REV_SEQ", var77);
                     Iterator var80 = var68.persistableIterator();

                     while(var80.hasNext()) {
                        Baseline var83 = (Baseline)PersistenceHelper.manager.refresh((Baseline)var80.next());
                        Logger var89 = logger;
                        String var100 = var59.getIdentity();
                        var89.error("-->ReleaseSystemHelper.populateAttributes().addToBaseline: " + var100 + " baseline: " + var83.toString());
                        BaselineHelper.service.addToBaseline(var59, var83);
                     }
                  }
               }
            }
         }

         var2.commit();
         var2 = null;
      } catch (Exception var28) {
         if (var2 != null) {
            var2.rollback();
         }

         this.errorMsg = var28.getMessage();
         var28.printStackTrace();
      }

      if (this.errorMsg != null && this.errorMsg.length() > 0) {
         logger.error("-->ReleaseSystemHelper.populateAttributes() errorMsg: " + this.errorMsg);
         return "Failure";
      } else {
         logger.error("-->ReleaseSystemHelper.populateAttributes() Succeeded");
         return "Success";
      }
   }

   private static String getNumber(RevisionControlled var0) {
      String var1 = "UNKNOWN";
      if (var0 instanceof EPMDocument) {
         var1 = ((EPMDocument)var0).getNumber();
      } else if (var0 instanceof WTDocument) {
         var1 = ((WTDocument)var0).getNumber();
      } else if (var0 instanceof WTPart) {
         var1 = ((WTPart)var0).getNumber();
      }

      return var1;
   }

   public static void validateChangeTask(Object var0) throws WTException {
      StringBuffer var1 = new StringBuffer();
      if (var0 == null) {
         throw new WTException("primaryBusinessObject is null");
      } else if (!(var0 instanceof WTChangeActivity2)) {
         throw new WTException("primaryBusinessObject is not a WTChangeActivity2, it is " + var0.getClass().getName());
      } else {
         WTChangeActivity2 var2 = (WTChangeActivity2)var0;
         QueryResult var3 = ChangeHelper2.service.getChangeablesAfter(var2, false);

         while(var3.hasMoreElements()) {
            ChangeRecord2 var4 = (ChangeRecord2)var3.nextElement();
            Transition var5 = var4.getTargetTransition();
            logger.error("-->validateChangeTask() transition - " + var5);
            Changeable2 var6 = var4.getChangeable2();
            if (var6 instanceof RevisionControlled) {
               RevisionControlled var7 = (RevisionControlled)var6;
               if (var5 == null) {
                  var1.append("[" + getNumber(var7) + "] does not have a Release Target set\n");
               }

               String var8 = var7.getState().toString();
               if (!"MAKEREADY".equals(var8) && !"FORMAL".equals(var8) && !"PREOBSOLETE".equals(var8)) {
                  var1.append("[" + getNumber(var7) + "] is not at state 'Make Ready', 'Formal' or 'Pre-Obsolete'\n");
               }
            }
         }

         if (var1.length() > 0) {
            throw new WTException(var1.toString());
         }
      }
   }
}
