package com.hni.pdmlink.transfer;

import java.util.Comparator;
import wt.enterprise.RevisionControlled;

class HniTransfer$1 implements Comparator<RevisionControlled> {
   // $FF: synthetic field
   final HniTransfer this$0;

   HniTransfer$1(HniTransfer var1) {
      this.this$0 = var1;
   }

   public int compare(RevisionControlled var1, RevisionControlled var2) {
      return this.this$0.getDocNumber(var1).compareTo(this.this$0.getDocNumber(var2));
   }
}
