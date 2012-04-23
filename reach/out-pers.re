card DUMMY != 0 ? fail "This property can be checked only on STGs without dummies" :
exists t1 in TRANSITIONS s.t. sig t1 in LOCAL {
  @t1 &
  exists t2 in TRANSITIONS s.t. sig t2 != sig t1 & card (pre t1 * (pre t2 \ post t2)) != 0 {
    @t2 &
    forall t3 in tran sig t1 \ {t1} s.t. card (pre t3 * (pre t2 \ post t2)) = 0 {
       exists p in pre t3 \ post t2 { ~$p }
    }
  }
}

