package com.hni.pdmlink.action;

import java.util.Comparator;
import wt.lifecycle.LifeCycleManaged;

public class ReleaseSystemHelper$SortByIteration implements Comparator {
   // $FF: synthetic field
   final ReleaseSystemHelper this$0;

   public ReleaseSystemHelper$SortByIteration(ReleaseSystemHelper var1) {
      this.this$0 = var1;
   }

   public int compare(Object var1, Object var2) {
      return ((LifeCycleManaged)var1).getPersistInfo().getCreateStamp().compareTo(((LifeCycleManaged)var2).getPersistInfo().getCreateStamp());
   }
}
