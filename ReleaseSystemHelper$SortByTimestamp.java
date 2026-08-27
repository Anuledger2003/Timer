package com.hni.pdmlink.action;

import java.util.Comparator;
import wt.lifecycle.LifeCycleHistory;

public class ReleaseSystemHelper$SortByTimestamp implements Comparator {
   // $FF: synthetic field
   final ReleaseSystemHelper this$0;

   public ReleaseSystemHelper$SortByTimestamp(ReleaseSystemHelper var1) {
      this.this$0 = var1;
   }

   public int compare(Object var1, Object var2) {
      return ((LifeCycleHistory)var1).getCreateTimestamp().compareTo(((LifeCycleHistory)var2).getCreateTimestamp());
   }
}
